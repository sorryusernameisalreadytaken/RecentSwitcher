package eu.recentsopener;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity provides a simple user interface for viewing the recent
 * apps list using the UsageStats API, switching back to the last app and
 * managing excluded applications. The accessibility service can still be
 * enabled via a dedicated button but is not used for showing the system
 * overview on TV devices without a recents button.
 */
public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnEnableService;
    private Button btnShowRecentApps;
    private Button btnShowRecentAppsV2;
    private Button btnShowRecentAppsV3;
    private Button btnShowRecentAppsV4;
    private Button btnShowRecentAppsV5;
    private Button btnShowRecentAppsV6;
    private Button btnShowRecentAppsV7;
    private Button btnCollectDebug;
    private Button btnOpenLastApp;
    private Button btnManageExcluded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        btnEnableService = findViewById(R.id.btn_enable_service);
        btnShowRecentApps = findViewById(R.id.btn_show_recent_apps);
        btnShowRecentAppsV2 = findViewById(R.id.btn_show_recent_apps_v2);
        btnShowRecentAppsV3 = findViewById(R.id.btn_show_recent_apps_v3);
        btnShowRecentAppsV4 = findViewById(R.id.btn_show_recent_apps_v4);
        btnShowRecentAppsV5 = findViewById(R.id.btn_show_recent_apps_v5);
        btnShowRecentAppsV6 = findViewById(R.id.btn_show_recent_apps_v6);
        btnShowRecentAppsV7 = findViewById(R.id.btn_show_recent_apps_v7);
        btnCollectDebug = findViewById(R.id.btn_collect_debug);
        btnOpenLastApp = findViewById(R.id.btn_open_last_app);
        btnManageExcluded = findViewById(R.id.btn_manage_excluded);

        // Launch Variant A recents list (aggregated usage stats with event fallback)
        btnShowRecentApps.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RecentAppsActivity.class)));

        // Launch Variant B recents list (event-only)
        btnShowRecentAppsV2.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RecentAppsActivityV2.class)));

        // Launch Variant C recents list (daily usage stats)
        btnShowRecentAppsV3.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RecentAppsActivityV3.class)));

        // Launch Variant D recents list (unfiltered aggregated usage stats)
        btnShowRecentAppsV4.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RecentAppsActivityV4.class)));

        // Launch Variant E recents list (last-used map)
        btnShowRecentAppsV5.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RecentAppsActivityV5.class)));

        // Launch Variant F recents list (accessibility events history)
        btnShowRecentAppsV6.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RecentAppsActivityV6.class)));

        // Launch Variant G recents list (no filtering, diagnostic)
        btnShowRecentAppsV7.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RecentAppsActivityV7.class)));

        // Show the last app without requiring accessibility service
        btnOpenLastApp.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LastAppActivity.class)));

        // Launch activity to manage excluded apps
        btnManageExcluded.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ExcludedAppsActivity.class)));

        // Collect debug information and save to a file
        btnCollectDebug.setOnClickListener(v -> DebugHelper.collectDebugInfo(MainActivity.this));

        // Launch the accessibility settings screen
        btnEnableService.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
    }

    /**
     * Update the status message and button visibility based on whether the
     * accessibility service is currently enabled. If it is enabled the
     * recents button is visible; otherwise the enable button is shown.
     * The other buttons remain visible regardless of service state.
     */
    private void updateUi() {
        // Always hide the open recents button since we no longer trigger
        // the system overview. Still show a status message about the
        // accessibility service and provide an enable button if it is not
        // active. The other buttons remain visible regardless of service state.
        boolean serviceEnabled = RecentsAccessibilityService.isServiceEnabled();
        if (serviceEnabled) {
            tvStatus.setText(R.string.service_enabled);
            btnEnableService.setVisibility(View.GONE);
        } else {
            tvStatus.setText(R.string.service_not_enabled);
            btnEnableService.setVisibility(View.VISIBLE);
        }
    }
}