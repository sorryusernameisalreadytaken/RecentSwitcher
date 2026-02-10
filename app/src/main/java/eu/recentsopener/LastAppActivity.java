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

        if (target != null) {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(target);
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