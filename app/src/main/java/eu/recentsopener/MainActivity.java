package eu.recentsopener;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity provides a simple user interface for triggering the system
 * recents screen via an accessibility service, viewing the recent apps list
 * using the UsageStats API and managing excluded applications. Only the
 * "Open Recents" button requires the accessibility service; other
 * functionality works without it. Alt‑Tab behaviour is provided via
 * the LastAppActivity for external key‑mapping, but no longer via a
 * dedicated button in the main UI.
 */
public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnOpenRecents;
    private Button btnEnableService;
    private Button btnShowRecentApps;
    // Button to switch directly to the last app has been removed. Alt-tab functionality
    // is now triggered through the recents list or via key-mapper aliases.
    private Button btnManageExcluded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        btnOpenRecents = findViewById(R.id.btn_open_recents);
        btnEnableService = findViewById(R.id.btn_enable_service);
        btnShowRecentApps = findViewById(R.id.btn_show_recent_apps);
        btnManageExcluded = findViewById(R.id.btn_manage_excluded);

        // Show the list of recent apps via UsageStats API
        btnShowRecentApps.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RecentAppsActivity.class)));


        // Launch activity to manage excluded apps
        btnManageExcluded.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ExcludedAppsActivity.class)));

        // Open the system recents screen if the accessibility service is enabled;
        // otherwise fall back to our own recent apps list. This makes the
        // "Letzte Apps öffnen" button useful even without special permissions.
        btnOpenRecents.setOnClickListener(v -> {
            if (RecentsAccessibilityService.isServiceEnabled()) {
                RecentsAccessibilityService.showRecents();
                finish();
            } else {
                // Without the service, show our own recent apps list
                startActivity(new Intent(MainActivity.this, RecentAppsActivity.class));
            }
        });

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
        boolean serviceEnabled = RecentsAccessibilityService.isServiceEnabled();
        if (serviceEnabled) {
            tvStatus.setText(R.string.service_enabled);
            // Hide the enable button once the service is active
            btnEnableService.setVisibility(View.GONE);
        } else {
            tvStatus.setText(R.string.service_not_enabled);
            btnEnableService.setVisibility(View.VISIBLE);
        }
        // Always show the recents button so the user can fall back to the list
        btnOpenRecents.setVisibility(View.VISIBLE);
    }
}