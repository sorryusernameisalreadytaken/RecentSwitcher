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

        // Determine a target package: try preferences first, then fall back to usage stats
        String target = null;
        if (previousPackage != null && !excluded.contains(previousPackage)) {
            target = previousPackage;
        } else if (lastPackage != null && !excluded.contains(lastPackage)) {
            target = lastPackage;
        } else {
            // Fallback: query UsageStats for the most recent app excluding this one
            android.app.usage.UsageStatsManager usm = (android.app.usage.UsageStatsManager) getSystemService(android.content.Context.USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            long begin = now - 1000L * 60 * 60; // last hour
            android.app.usage.UsageEvents events = usm.queryEvents(begin, now);
            java.util.Set<String> candidates = new java.util.LinkedHashSet<>();
            android.app.usage.UsageEvents.Event evt = new android.app.usage.UsageEvents.Event();
            while (events.hasNextEvent()) {
                events.getNextEvent(evt);
                int type = evt.getEventType();
                if (type == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND ||
                        type == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    String pkg = evt.getPackageName();
                    if (!getPackageName().equals(pkg) && !excluded.contains(pkg)) {
                        // maintain order by removing duplicates
                        candidates.remove(pkg);
                        candidates.add(pkg);
                    }
                }
            }
            // choose the most recent candidate (last element in the set)
            java.util.List<String> list = new java.util.ArrayList<>(candidates);
            if (!list.isEmpty()) {
                target = list.get(list.size() - 1);
            }
        }

        if (target != null) {
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
                    } catch (Exception e) {
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