package eu.recentsopener;

import android.app.Activity;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.util.List;
import java.util.Set;

/**
 * Variant 5 of the last‑app switcher. This version uses aggregated
 * usage statistics and selects the package with the highest total time
 * spent in the foreground over the last several hours. This heuristic
 * may favour apps that the user interacts with for longer periods.
 */
public class LastAppVariant5Activity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Set<String> excluded = PrefsHelper.getExcludedApps(this);
        String target = null;
        try {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm != null) {
                long now = System.currentTimeMillis();
                long begin = now - 1000L * 60 * 60 * 6; // last 6 hours
                List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, now);
                long maxForeground = 0L;
                if (stats != null) {
                    for (UsageStats s : stats) {
                        String pkg = s.getPackageName();
                        if (pkg == null || pkg.equals(getPackageName()) || excluded.contains(pkg)) {
                            continue;
                        }
                        long time = s.getTotalTimeInForeground();
                        if (time > maxForeground) {
                            maxForeground = time;
                            target = pkg;
                        }
                    }
                }
            }
        } catch (Exception ignore) {
            // ignore errors
        }
        // fallback to previous/last if nothing found
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