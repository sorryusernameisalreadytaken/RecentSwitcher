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
 * Variant 4 of the last‑app switcher. Similar to the default algorithm
 * but considers only the events from the last 10 minutes instead of
 * the last hour. This may help in situations where background
 * services keep generating events over long periods.
 */
public class LastAppVariant4Activity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Set<String> excluded = PrefsHelper.getExcludedApps(this);
        String target = null;
        try {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            long begin = now - 1000L * 60 * 10; // last 10 minutes
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
            for (String pkg : pkgs) {
                if (!excluded.contains(pkg)) {
                    target = pkg;
                    break;
                }
            }
        } catch (Exception ignore) {
            // ignore errors
        }
        if (target == null) {
            // fallback to previous/last packages
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