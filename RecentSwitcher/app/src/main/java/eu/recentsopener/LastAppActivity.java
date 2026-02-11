package eu.recentsopener;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
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
        // Prefer using usage events to determine the most recent package if none recorded
        if (previousPackage != null && !excluded.contains(previousPackage)) {
            target = previousPackage;
        } else if (lastPackage != null && !excluded.contains(lastPackage)) {
            target = lastPackage;
        } else {
            // Attempt to compute the most recent used package via usage events
            try {
                android.app.usage.UsageStatsManager usm = (android.app.usage.UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
                long now = System.currentTimeMillis();
                long begin = now - 1000L * 60 * 60; // last hour
                android.app.usage.UsageEvents events = usm.queryEvents(begin, now);
                java.util.Set<String> seen = new java.util.HashSet<>();
                java.util.List<String> pkgs = new java.util.ArrayList<>();
                android.app.usage.UsageEvents.Event ev = new android.app.usage.UsageEvents.Event();
                while (events != null && events.hasNextEvent()) {
                    events.getNextEvent(ev);
                    int type = ev.getEventType();
                    if (type == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND ||
                        type == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                        String pkg = ev.getPackageName();
                        if (pkg != null && !pkg.equals(getPackageName())) {
                            if (seen.contains(pkg)) pkgs.remove(pkg);
                            pkgs.add(pkg);
                            seen.add(pkg);
                        }
                    }
                }
                java.util.Collections.reverse(pkgs);
                for (String pkg : pkgs) {
                    if (!excluded.contains(pkg)) {
                        target = pkg;
                        break;
                    }
                }
            } catch (Exception ignore) {
                // ignore errors and fallback
            }
        }

        if (target != null) {
            Intent launchIntent = getPackageManager().getLeanbackLaunchIntentForPackage(target);
            if (launchIntent == null) {
                launchIntent = getPackageManager().getLaunchIntentForPackage(target);
            }
            if (launchIntent != null) {
                // Before launching, update history so that Alt‑Tab toggles between packages
                PrefsHelper.updateHistory(this, target);
                startActivity(launchIntent);
            } else {
                // Attempt to launch system settings if this is a settings package
                if (target.contains("settings")) {
                    Intent settingsIntent = new Intent(android.provider.Settings.ACTION_SETTINGS);
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