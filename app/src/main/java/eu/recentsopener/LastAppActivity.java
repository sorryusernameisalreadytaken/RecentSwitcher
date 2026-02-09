package eu.recentsopener;

import android.app.Activity;
import android.content.Intent;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
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

        String targetPackage = null;
        Set<String> excluded = PrefsHelper.getExcludedApps(this);
        // Prefer the package stored in preferences if present and not excluded
        String lastPackage = PrefsHelper.getLastPackage(this);
        if (lastPackage != null && !excluded.contains(lastPackage)) {
            targetPackage = lastPackage;
        }
        // If no stored package is available, determine the last used app via UsageStats
        if (targetPackage == null) {
            targetPackage = findLastUsedPackage(excluded);
        }
        if (targetPackage != null) {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(targetPackage);
            if (launchIntent != null) {
                startActivity(launchIntent);
            } else if (targetPackage.contains("settings")) {
                // Attempt to open system settings when no launcher intent exists
                Intent settingsIntent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(settingsIntent);
                } catch (Exception e) {
                    Toast.makeText(this, targetPackage + " cannot be launched", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, targetPackage + " cannot be launched", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, getString(R.string.no_last_app), Toast.LENGTH_SHORT).show();
        }
        // Immediately finish to avoid leaving our activity in the task stack
        finish();
    }

    /**
     * Finds the most recently foregrounded package that is not this app and
     * not in the excluded set using UsageStatsManager. Requires the
     * PACKAGE_USAGE_STATS permission to function correctly. Returns null
     * if none can be found or permission is missing.
     */
    private String findLastUsedPackage(Set<String> excluded) {
        try {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            long begin = now - 1000L * 60 * 60; // search last hour
            UsageEvents events = usm.queryEvents(begin, now);
            UsageEvents.Event event = new UsageEvents.Event();
            String candidate = null;
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    String pkg = event.getPackageName();
                    // Skip our own app and excluded packages
                    if (getPackageName().equals(pkg) || excluded.contains(pkg)) {
                        continue;
                    }
                    candidate = pkg;
                }
            }
            return candidate;
        } catch (Exception e) {
            // If we cannot query usage stats (permission missing), return null
            return null;
        }
    }
}