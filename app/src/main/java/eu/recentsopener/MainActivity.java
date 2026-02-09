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
 * using the UsageStats API, switching back to the last app and managing
 * excluded applications. Only the "Open Recents" button requires the
 * accessibility service; other functionality works without it.
 */
public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnOpenRecents;
    private Button btnEnableService;
    private Button btnShowRecentApps;
    private Button btnOpenLastApp;
    private Button btnManageExcluded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        btnOpenRecents = findViewById(R.id.btn_open_recents);
        btnEnableService = findViewById(R.id.btn_enable_service);
        btnShowRecentApps = findViewById(R.id.btn_show_recent_apps);
        btnOpenLastApp = findViewById(R.id.btn_open_last_app);
        btnManageExcluded = findViewById(R.id.btn_manage_excluded);

        // Show the list of recent apps via UsageStats API
        btnShowRecentApps.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RecentAppsActivity.class)));

        // Show the last app without requiring accessibility service
        btnOpenLastApp.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LastAppActivity.class)));

        // Launch activity to manage excluded apps
        btnManageExcluded.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ExcludedAppsActivity.class)));

        // Delegate to the accessibility service to show the system recents screen
        btnOpenRecents.setOnClickListener(v -> {
            RecentsAccessibilityService.showRecents();
            finish();
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
            btnOpenRecents.setVisibility(View.VISIBLE);
            btnEnableService.setVisibility(View.GONE);
        } else {
            tvStatus.setText(R.string.service_not_enabled);
            btnOpenRecents.setVisibility(View.GONE);
            btnEnableService.setVisibility(View.VISIBLE);
        }
    }
}