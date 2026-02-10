package eu.recentsopener;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import java.util.Set;

/**
 * LastAppActivity provides a simple entry point for external tools (e.g.
 * Key Mapper) to switch directly back to the last launched app. When
 * started it immediately reads the last package from the shared
 * preferences and attempts to launch it. If the package is excluded
 * or cannot be launched, a short toast message is shown instead.
 */
public class LastAppActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Determine the best candidate to launch: prefer the previous package
        String previousPackage = PrefsHelper.getPreviousPackage(this);
        String lastPackage = PrefsHelper.getLastPackage(this);
        Set<String> excluded = PrefsHelper.getExcludedApps(this);

        // Determine the best candidate to launch: prefer the previous package, then last
        // package and finally fall back to the most recent UsageStats entry. Excluded
        // packages are ignored throughout.
        String target = null;
        if (previousPackage != null && !excluded.contains(previousPackage)) {
            target = previousPackage;
        } else if (lastPackage != null && !excluded.contains(lastPackage)) {
            target = lastPackage;
        } else {
            // Fallback: query the most recent app using aggregated usage stats over the last hour.
            // If aggregated stats are unavailable, fall back to usage events. Excluded and
            // self packages are ignored throughout.
            android.app.usage.UsageStatsManager usm = (android.app.usage.UsageStatsManager) getSystemService(android.content.Context.USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            long begin = now - 1000L * 60 * 60; // last hour
            java.util.List<android.app.usage.UsageStats> stats = usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, begin, now);
            if (stats != null && !stats.isEmpty()) {
                // Build a list of candidates sorted by lastTimeUsed descending
                java.util.List<android.app.usage.UsageStats> filtered = new java.util.ArrayList<>();
                for (android.app.usage.UsageStats s : stats) {
                    String pkg = s.getPackageName();
                    if (getPackageName().equals(pkg)) continue;
                    if (excluded.contains(pkg)) continue;
                    if (s.getLastTimeUsed() == 0) continue;
                    filtered.add(s);
                }
                java.util.Collections.sort(filtered, (a, b) -> Long.compare(b.getLastTimeUsed(), a.getLastTimeUsed()));
                for (android.app.usage.UsageStats s : filtered) {
                    String pkg = s.getPackageName();
                    target = pkg;
                    break;
                }
            } else {
                // If aggregated stats are unavailable, fall back to usage events
                android.app.usage.UsageEvents events = usm.queryEvents(begin, now);
                java.util.LinkedHashSet<String> candidates = new java.util.LinkedHashSet<>();
                android.app.usage.UsageEvents.Event e = new android.app.usage.UsageEvents.Event();
                while (events.hasNextEvent()) {
                    events.getNextEvent(e);
                    int type = e.getEventType();
                    if (type == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND ||
                            type == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED ||
                            type == 16 ||
                            type == android.app.usage.UsageEvents.Event.NOTIFICATION_INTERRUPTION ||
                            type == 10) {
                        String pkg = e.getPackageName();
                        if (!getPackageName().equals(pkg) && !excluded.contains(pkg)) {
                            // maintain order by removing duplicates before re‑adding
                            candidates.remove(pkg);
                            candidates.add(pkg);
                        }
                    }
                }
                if (!candidates.isEmpty()) {
                    java.util.Iterator<String> it = candidates.iterator();
                    String last = null;
                    while (it.hasNext()) {
                        last = it.next();
                    }
                    target = last;
                }
            }
        }

        if (target != null) {
            // Attempt to launch the determined package
            android.content.Intent launchIntent = getPackageManager().getLaunchIntentForPackage(target);
            if (launchIntent != null) {
                // Before launching, update history so that Alt‑Tab toggles between packages
                PrefsHelper.updateHistory(this, target);
                startActivity(launchIntent);
            } else {
                // Attempt to launch system settings if this is a settings package
                if (target.contains("settings")) {
                    android.content.Intent settingsIntent = new android.content.Intent(android.provider.Settings.ACTION_SETTINGS);
                    settingsIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        PrefsHelper.updateHistory(this, target);
                        startActivity(settingsIntent);
                    } catch (Exception e1) {
                        android.widget.Toast.makeText(this, target + " cannot be launched", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } else {
                    android.widget.Toast.makeText(this, target + " cannot be launched", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            // No candidate found; inform the user
            android.widget.Toast.makeText(this, getString(R.string.no_last_app), android.widget.Toast.LENGTH_SHORT).show();
        }
        // Immediately finish to avoid leaving our activity in the task stack
        finish();
    }
}