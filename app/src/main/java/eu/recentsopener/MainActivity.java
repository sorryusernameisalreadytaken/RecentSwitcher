package eu.recentsopener;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MainActivity provides a simple user interface for triggering the recents
 * (overview) screen via an accessibility service. If the accessibility
 * service is not yet enabled, the activity will prompt the user to
 * enable it in the system settings.
 */
public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnOpenRecents;
    private Button btnEnableService;
    private Button btnShowRecentApps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if we were launched via one of the custom shortcut actions.
        Intent intent = getIntent();
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if ("eu.recentsopener.OPEN_LAST_APP".equals(action)) {
                // Respond to broadcast/shortcut: open the most recent app directly
                String pkg = getMostRecentApp(this);
                if (pkg != null) {
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(pkg);
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(launchIntent);
                    } else {
                        Toast.makeText(this, R.string.no_launch_intent, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, R.string.no_recent_app, Toast.LENGTH_SHORT).show();
                }
                finish();
                return;
            }
            if ("eu.recentsopener.SHOW_RECENT_LIST".equals(action)) {
                // Respond to broadcast/shortcut: open the full recent list
                Intent listIntent = new Intent(this, RecentAppsActivity.class);
                listIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(listIntent);
                finish();
                return;
            }
        }

        tvStatus = findViewById(R.id.tv_status);
        btnOpenRecents = findViewById(R.id.btn_open_recents);
        btnEnableService = findViewById(R.id.btn_enable_service);

        // Handle showing the list of recent apps via UsageStats API
        btnShowRecentApps = findViewById(R.id.btn_show_recent_apps);
        btnShowRecentApps.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RecentAppsActivity.class));
        });

        // Configure button behaviour. When the recents button is tapped we
        // delegate to the accessibility service to perform the action and
        // finish the activity. When the enable button is tapped we
        // launch the accessibility settings screen.
        btnOpenRecents.setOnClickListener(v -> {
            // If accessibility service is available, show the system recents
            if (RecentsAccessibilityService.isServiceEnabled()) {
                RecentsAccessibilityService.showRecents();
            } else {
                // Otherwise try to open the most recent app directly using
                // UsageStatsManager. If no app is found we show a toast.
                String pkg = getMostRecentApp(MainActivity.this);
                if (pkg != null) {
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(pkg);
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(launchIntent);
                    } else {
                        Toast.makeText(MainActivity.this, R.string.no_launch_intent, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, R.string.no_recent_app, Toast.LENGTH_SHORT).show();
                }
            }
            finish();
        });

        btnEnableService.setOnClickListener(v -> {
            Intent settingsIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            // Ensure that the settings screen launches as a new task so the
            // system does not close this activity unexpectedly. This is
            // important on some TV devices.
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(settingsIntent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update the UI every time the activity resumes in case the user
        // enabled the accessibility service while the settings screen was
        // open.
        updateUi();
    }

    /**
     * Update the status message and button visibility based on whether the
     * accessibility service is currently enabled. If it is enabled the
     * recents button is visible; otherwise the enable button is shown.
     */
    private void updateUi() {
        boolean serviceEnabled = RecentsAccessibilityService.isServiceEnabled();
        if (serviceEnabled) {
            tvStatus.setText(R.string.service_enabled);
            btnOpenRecents.setVisibility(Button.VISIBLE);
            btnEnableService.setVisibility(Button.GONE);
        } else {
            tvStatus.setText(R.string.service_not_enabled);
            btnOpenRecents.setVisibility(Button.GONE);
            btnEnableService.setVisibility(Button.VISIBLE);
        }
    }

    /**
     * Retrieve the most recent foreground app package name using the
     * UsageStatsManager API. This method queries usage events from the
     * last hour and returns the most recent package that is not this
     * application's package and is not marked as excluded in the
     * preferences. If no suitable app is found the method returns null.
     */
    private String getMostRecentApp(Context context) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        long endTime = System.currentTimeMillis();
        long startTime = endTime - 60 * 60 * 1000; // last hour
        UsageEvents events = usm.queryEvents(startTime, endTime);
        UsageEvents.Event event = new UsageEvents.Event();
        String lastPackage = null;
        long lastTime = 0;
        String thisPkg = getPackageName();
        // Copy the excluded set into a HashSet for quick lookup
        Set<String> excluded = PrefsHelper.getExcludedApps(context);
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                String pkg = event.getPackageName();
                if (!pkg.equals(thisPkg) && !excluded.contains(pkg)) {
                    if (event.getTimeStamp() > lastTime) {
                        lastTime = event.getTimeStamp();
                        lastPackage = pkg;
                    }
                }
            }
        }
        return lastPackage;
    }
}