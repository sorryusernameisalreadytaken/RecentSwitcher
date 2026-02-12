package eu.recentsopener;

import android.app.Activity;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Variant 3 of the last‑app switcher. This implementation scans recent
 * usage events similar to the default implementation, but it deliberately
 * skips the most recent package and instead returns the second candidate.
 * This can help avoid picking apps that constantly refresh in the
 * background (e.g. streaming services) from taking precedence. If no
 * suitable second candidate is found, it falls back to the default
 * algorithm using the previous/last packages.
 */
public class LastAppVariant3Activity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Set<String> excluded = PrefsHelper.getExcludedApps(this);
        String target = null;
        try {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            long begin = now - 1000L * 60 * 60; // last hour
            UsageEvents events = usm.queryEvents(begin, now);
            Set<String> seen = new HashSet<>();
            List<String> pkgs = new ArrayList<>();
            UsageEvents.Event ev = new UsageEvents.Event();
            while (events != null && events.hasNextEvent()) {
                events.getNextEvent(ev);
                int type = ev.getEventType();
                if (type == UsageEvents.Event.MOVE_TO_FOREGROUND || type == UsageEvents.Event.ACTIVITY_RESUMED) {
                    String pkg = ev.getPackageName();
                    if (pkg != null && !pkg.equals(getPackageName())) {
                        if (seen.contains(pkg)) pkgs.remove(pkg);
                        pkgs.add(pkg);
                        seen.add(pkg);
                    }
                }
            }
            Collections.reverse(pkgs);
            int skip = 1; // skip the most recent package
            for (String pkg : pkgs) {
                if (excluded.contains(pkg)) {
                    continue;
                }
                if (skip > 0) {
                    skip--;
                    continue;
                }
                target = pkg;
                break;
            }
        } catch (Exception ignore) {
            // ignore errors
        }
        // Fallback to previous/last packages if no second candidate found
        if (target == null) {
            String previousPackage = PrefsHelper.getPreviousPackage(this);
            String lastPackage = PrefsHelper.getLastPackage(this);
            if (previousPackage != null && !excluded.contains(previousPackage)) {
                target = previousPackage;
            } else if (lastPackage != null && !excluded.contains(lastPackage)) {
                target = lastPackage;
            }
        }
        launchTarget(target);
        finish();
    }

    private void launchTarget(String target) {
        if (target != null) {
            Intent launchIntent = getPackageManager().getLeanbackLaunchIntentForPackage(target);
            if (launchIntent == null) {
                launchIntent = getPackageManager().getLaunchIntentForPackage(target);
            }
            if (launchIntent != null) {
                PrefsHelper.updateHistory(this, target);
                startActivity(launchIntent);
            } else {
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
            Toast.makeText(this, getString(R.string.no_last_app), Toast.LENGTH_SHORT).show();
        }
    }
}