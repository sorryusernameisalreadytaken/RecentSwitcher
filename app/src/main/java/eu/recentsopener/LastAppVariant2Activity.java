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
 * Variant 2 of the last‑app switcher. This implementation relies on
 * aggregated usage statistics to find the most recently used package.
 * Instead of scanning discrete usage events, it queries UsageStats for
 * the last few hours and selects the package with the largest
 * lastTimeUsed. If no suitable candidate is found, it falls back to
 * the default algorithm in {@link LastAppActivity}.
 */
public class LastAppVariant2Activity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve the set of excluded packages so they are never launched
        Set<String> excluded = PrefsHelper.getExcludedApps(this);
        String target = null;
        try {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm != null) {
                long now = System.currentTimeMillis();
                long begin = now - 1000L * 60 * 60 * 4; // last 4 hours
                List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, now);
                long newest = 0L;
                if (stats != null) {
                    for (UsageStats s : stats) {
                        String pkg = s.getPackageName();
                        // Skip our own package and excluded apps
                        if (pkg == null || pkg.equals(getPackageName()) || excluded.contains(pkg)) {
                            continue;
                        }
                        long lastTime = s.getLastTimeUsed();
                        if (lastTime > newest) {
                            newest = lastTime;
                            target = pkg;
                        }
                    }
                }
            }
        } catch (Exception ignore) {
            // ignore and fallback
        }

        // If no candidate found, fall back to the standard implementation
        if (target == null) {
            // Use previous and last packages as in the original algorithm
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

    /**
     * Launch the given package name using a leanback intent if available.
     * Updates the history and handles failure cases.
     *
     * @param target package name to launch or null
     */
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
                // Provide a fallback for settings packages
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