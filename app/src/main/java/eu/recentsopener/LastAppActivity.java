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

        String target = null;
        if (previousPackage != null && !excluded.contains(previousPackage)) {
            target = previousPackage;
        } else if (lastPackage != null && !excluded.contains(lastPackage)) {
            target = lastPackage;
        }

        // If no previous/last candidate, attempt to determine the most recently used
        // app from usage statistics. We check aggregated UsageStats first and then
        // fall back to usage events. Only apps not in the exclusion list and not
        // our own package are considered. When evaluating aggregated stats we
        // further verify that the most recent usage event for the package
        // represents a foreground transition (RESUMED or MOVE_TO_FOREGROUND).
        if (target == null) {
            try {
                long end = System.currentTimeMillis();
                long begin = end - 1000L * 60 * 60; // last 1 hour
                android.app.usage.UsageStatsManager usm =
                        (android.app.usage.UsageStatsManager) getSystemService(android.content.Context.USAGE_STATS_SERVICE);
                java.util.Map<String, android.app.usage.UsageStats> stats =
                        usm.queryAndAggregateUsageStats(begin, end);
                String pkgCandidate = null;
                long latestTime = 0L;
                // Build a map of last event type per package
                java.util.Map<String, Integer> lastEventMap = new java.util.HashMap<>();
                try {
                    android.app.usage.UsageEvents evs = usm.queryEvents(begin, end);
                    android.app.usage.UsageEvents.Event ev = new android.app.usage.UsageEvents.Event();
                    while (evs != null && evs.hasNextEvent()) {
                        evs.getNextEvent(ev);
                        String pkg = ev.getPackageName();
                        if (pkg != null) {
                            lastEventMap.put(pkg, ev.getEventType());
                        }
                    }
                } catch (Exception ignore) {
                    // ignore
                }
                if (stats != null && !stats.isEmpty()) {
                    for (java.util.Map.Entry<String, android.app.usage.UsageStats> e : stats.entrySet()) {
                        String pkg = e.getKey();
                        if (pkg == null || pkg.equals(getPackageName())) {
                            continue;
                        }
                        if (excluded.contains(pkg)) {
                            continue;
                        }
                        android.app.usage.UsageStats u = e.getValue();
                        long lastUsed = u.getLastTimeUsed();
                        if (lastUsed <= 0) {
                            continue;
                        }
                        // Check last event: require foreground event
                        Integer lastType = lastEventMap.get(pkg);
                        if (lastType != null &&
                                (lastType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND || lastType == 1 /* ACTIVITY_RESUMED */)) {
                            if (lastUsed > latestTime) {
                                latestTime = lastUsed;
                                pkgCandidate = pkg;
                            }
                        }
                    }
                }
                // If no candidate from aggregated stats, try event-based
                if (pkgCandidate == null) {
                    android.app.usage.UsageEvents events = usm.queryEvents(begin, end);
                    android.app.usage.UsageEvents.Event event = new android.app.usage.UsageEvents.Event();
                    while (events != null && events.hasNextEvent()) {
                        events.getNextEvent(event);
                        int type = event.getEventType();
                        if (type == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND ||
                                type == 1 /* ACTIVITY_RESUMED */ ||
                                type == 2 /* ACTIVITY_PAUSED */ ||
                                type == 23 /* ACTIVITY_STOPPED */) {
                            String pkg = event.getPackageName();
                            if (pkg == null) continue;
                            if (pkg.equals(getPackageName()) || excluded.contains(pkg)) {
                                continue;
                            }
                            // Use the first package found (most recent event)
                            pkgCandidate = pkg;
                        }
                    }
                }
                // Assign candidate if found
                if (pkgCandidate != null) {
                    target = pkgCandidate;
                }
            } catch (Exception e) {
                // ignore errors and fall back to null
            }
        }

        if (target != null) {
            android.content.pm.PackageManager pm = getPackageManager();
            android.content.Intent launchIntent = pm.getLeanbackLaunchIntentForPackage(target);
            if (launchIntent == null) {
                launchIntent = pm.getLaunchIntentForPackage(target);
            }
            if (launchIntent != null) {
                // Before launching, update history so that Alt‑Tab toggles between packages
                PrefsHelper.updateHistory(this, target);
                startActivity(launchIntent);
            } else {
                // Attempt to launch system settings if this is a settings package
                if (target.contains("settings")) {
                    android.content.Intent settingsIntent = new android.content.Intent(android.provider.Settings.ACTION_SETTINGS);
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        PrefsHelper.updateHistory(this, target);
                        startActivity(settingsIntent);
                    } catch (Exception e) {
                        Toast.makeText(this, target + " cannot be launched", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, target + " cannot be launched", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            // No candidate found; inform the user
            Toast.makeText(this, getString(R.string.no_last_app), Toast.LENGTH_SHORT).show();
        }
        // Immediately finish to avoid leaving our activity in the task stack
        finish();
    }
}