package com.example.recentsopener;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
            RecentsAccessibilityService.showRecents();
            finish();
        });

        btnEnableService.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            // Ensure that the settings screen launches as a new task so the
            // system does not close this activity unexpectedly. This is
            // important on some TV devices.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
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
}