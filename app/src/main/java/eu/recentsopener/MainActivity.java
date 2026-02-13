package eu.recentsopener;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
    private Button btnOpenLastApp;
    private Button btnListActions;
    private Button btnManageExcluded;
    private Button btnCollectDebug;
    private Button btnShowLiveEvents;
    // Buttons for launching different recent‑apps list variants
    private Button btnShowRecentAppsVariant1;
    private Button btnShowRecentAppsVariant2;
    private Button btnShowRecentAppsVariant3;
    private Button btnShowRecentAppsVariant4;

    // Additional variant buttons for recents list
    private Button btnShowRecentAppsVariant5;
    private Button btnShowRecentAppsVariant6;
    private Button btnShowRecentAppsVariant7;
    private Button btnShowRecentAppsVariant8;

    // Further variant buttons for recents list (v9–v12)
    private Button btnShowRecentAppsVariant9;
    private Button btnShowRecentAppsVariant10;
    private Button btnShowRecentAppsVariant11;
    private Button btnShowRecentAppsVariant12;

    // Table‑style variants (v13–v14)
    private Button btnShowRecentAppsVariant13;
    private Button btnShowRecentAppsVariant14;

    // Experimental variants (v15–v22)
    private Button btnShowRecentAppsVariant15;
    private Button btnShowRecentAppsVariant16;
    private Button btnShowRecentAppsVariant17;
    private Button btnShowRecentAppsVariant18;
    private Button btnShowRecentAppsVariant19;
    private Button btnShowRecentAppsVariant20;
    private Button btnShowRecentAppsVariant21;
    private Button btnShowRecentAppsVariant22;

    // Additional experimental variants (v23–v32)
    private Button btnShowRecentAppsVariant23;
    private Button btnShowRecentAppsVariant24;
    private Button btnShowRecentAppsVariant25;
    private Button btnShowRecentAppsVariant26;
    private Button btnShowRecentAppsVariant27;
    private Button btnShowRecentAppsVariant28;
    private Button btnShowRecentAppsVariant29;
    private Button btnShowRecentAppsVariant30;
    private Button btnShowRecentAppsVariant31;
    private Button btnShowRecentAppsVariant32;

    // Additional experimental variants (v23–v32)
    private Button btnShowRecentAppsVariant23;
    private Button btnShowRecentAppsVariant24;
    private Button btnShowRecentAppsVariant25;
    private Button btnShowRecentAppsVariant26;
    private Button btnShowRecentAppsVariant27;
    private Button btnShowRecentAppsVariant28;
    private Button btnShowRecentAppsVariant29;
    private Button btnShowRecentAppsVariant30;
    private Button btnShowRecentAppsVariant31;
    private Button btnShowRecentAppsVariant32;

    // Buttons for closing a fixed set of packages using different strategies
    private Button btnCloseSpecificAppsVariant1;
    private Button btnCloseSpecificAppsVariant2;
    private Button btnCloseSpecificAppsVariant3;
    private Button btnCloseSpecificAppsVariant4;

    // List of packages targeted by the specific close buttons
    private static final String[] SPECIFIC_CLOSE_PACKAGES = new String[] {
            "org.polymorphicshade.tubular",
            "ca.devmesh.seerrtv",
            "org.jellyfin.androidtv",
            "de.swr.avp.ard.tv"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        btnEnableService = findViewById(R.id.btn_enable_service);
        btnShowRecentApps = findViewById(R.id.btn_show_recent_apps);
        // Variant buttons for recents list
        btnShowRecentAppsVariant1 = findViewById(R.id.btn_show_recent_apps_variant1);
        btnShowRecentAppsVariant2 = findViewById(R.id.btn_show_recent_apps_variant2);
        btnShowRecentAppsVariant3 = findViewById(R.id.btn_show_recent_apps_variant3);
        btnShowRecentAppsVariant4 = findViewById(R.id.btn_show_recent_apps_variant4);
        btnOpenLastApp = findViewById(R.id.btn_open_last_app);
        btnListActions = findViewById(R.id.btn_list_actions);
        btnManageExcluded = findViewById(R.id.btn_manage_excluded);
        btnCollectDebug = findViewById(R.id.btn_collect_debug);
        btnShowLiveEvents = findViewById(R.id.btn_show_live_events);

        // Additional variant buttons for recents list
        btnShowRecentAppsVariant5 = findViewById(R.id.btn_show_recent_apps_variant5);
        btnShowRecentAppsVariant6 = findViewById(R.id.btn_show_recent_apps_variant6);
        btnShowRecentAppsVariant7 = findViewById(R.id.btn_show_recent_apps_variant7);
        btnShowRecentAppsVariant8 = findViewById(R.id.btn_show_recent_apps_variant8);

        // Initialise buttons for recents list variants v9–v12
        btnShowRecentAppsVariant9 = findViewById(R.id.btn_show_recent_apps_variant9);
        btnShowRecentAppsVariant10 = findViewById(R.id.btn_show_recent_apps_variant10);
        btnShowRecentAppsVariant11 = findViewById(R.id.btn_show_recent_apps_variant11);
        btnShowRecentAppsVariant12 = findViewById(R.id.btn_show_recent_apps_variant12);

        // Initialise buttons for table‑style recents list variants v13–v14
        btnShowRecentAppsVariant13 = findViewById(R.id.btn_show_recent_apps_variant13);
        btnShowRecentAppsVariant14 = findViewById(R.id.btn_show_recent_apps_variant14);

        // Initialise buttons for experimental recents list variants v15–v22
        btnShowRecentAppsVariant15 = findViewById(R.id.btn_show_recent_apps_variant15);
        btnShowRecentAppsVariant16 = findViewById(R.id.btn_show_recent_apps_variant16);
        btnShowRecentAppsVariant17 = findViewById(R.id.btn_show_recent_apps_variant17);
        btnShowRecentAppsVariant18 = findViewById(R.id.btn_show_recent_apps_variant18);
        btnShowRecentAppsVariant19 = findViewById(R.id.btn_show_recent_apps_variant19);
        btnShowRecentAppsVariant20 = findViewById(R.id.btn_show_recent_apps_variant20);
        btnShowRecentAppsVariant21 = findViewById(R.id.btn_show_recent_apps_variant21);
        btnShowRecentAppsVariant22 = findViewById(R.id.btn_show_recent_apps_variant22);

        // Bind additional variant buttons (v23–v32)
        btnShowRecentAppsVariant23 = findViewById(R.id.btn_show_recent_apps_variant23);
        btnShowRecentAppsVariant24 = findViewById(R.id.btn_show_recent_apps_variant24);
        btnShowRecentAppsVariant25 = findViewById(R.id.btn_show_recent_apps_variant25);
        btnShowRecentAppsVariant26 = findViewById(R.id.btn_show_recent_apps_variant26);
        btnShowRecentAppsVariant27 = findViewById(R.id.btn_show_recent_apps_variant27);
        btnShowRecentAppsVariant28 = findViewById(R.id.btn_show_recent_apps_variant28);
        btnShowRecentAppsVariant29 = findViewById(R.id.btn_show_recent_apps_variant29);
        btnShowRecentAppsVariant30 = findViewById(R.id.btn_show_recent_apps_variant30);
        btnShowRecentAppsVariant31 = findViewById(R.id.btn_show_recent_apps_variant31);
        btnShowRecentAppsVariant32 = findViewById(R.id.btn_show_recent_apps_variant32);

        // Initialise buttons for experimental variants V23–V32
        btnShowRecentAppsVariant23 = findViewById(R.id.btn_show_recent_apps_variant23);
        btnShowRecentAppsVariant24 = findViewById(R.id.btn_show_recent_apps_variant24);
        btnShowRecentAppsVariant25 = findViewById(R.id.btn_show_recent_apps_variant25);
        btnShowRecentAppsVariant26 = findViewById(R.id.btn_show_recent_apps_variant26);
        btnShowRecentAppsVariant27 = findViewById(R.id.btn_show_recent_apps_variant27);
        btnShowRecentAppsVariant28 = findViewById(R.id.btn_show_recent_apps_variant28);
        btnShowRecentAppsVariant29 = findViewById(R.id.btn_show_recent_apps_variant29);
        btnShowRecentAppsVariant30 = findViewById(R.id.btn_show_recent_apps_variant30);
        btnShowRecentAppsVariant31 = findViewById(R.id.btn_show_recent_apps_variant31);
        btnShowRecentAppsVariant32 = findViewById(R.id.btn_show_recent_apps_variant32);
        // Buttons for specific test closers
        btnCloseSpecificAppsVariant1 = findViewById(R.id.btn_close_specific_apps_variant1);
        btnCloseSpecificAppsVariant2 = findViewById(R.id.btn_close_specific_apps_variant2);
        btnCloseSpecificAppsVariant3 = findViewById(R.id.btn_close_specific_apps_variant3);
        btnCloseSpecificAppsVariant4 = findViewById(R.id.btn_close_specific_apps_variant4);

        // Show the list of recent apps via UsageStats API
        btnShowRecentApps.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 1);
            startActivity(intent);
        });

        // Launch variant 1 of the recents list
        btnShowRecentAppsVariant1.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 1);
            startActivity(intent);
        });
        // Launch variant 2 of the recents list
        btnShowRecentAppsVariant2.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 2);
            startActivity(intent);
        });
        // Launch variant 3 of the recents list
        btnShowRecentAppsVariant3.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 3);
            startActivity(intent);
        });
        // Launch variant 4 of the recents list
        btnShowRecentAppsVariant4.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 4);
            startActivity(intent);
        });

        // Launch variants 5–8 of the recents list
        btnShowRecentAppsVariant5.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 5);
            startActivity(intent);
        });
        btnShowRecentAppsVariant6.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 6);
            startActivity(intent);
        });
        btnShowRecentAppsVariant7.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 7);
            startActivity(intent);
        });
        btnShowRecentAppsVariant8.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 8);
            startActivity(intent);
        });

        // Launch variants 9–12 of the recents list
        btnShowRecentAppsVariant9.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 9);
            startActivity(intent);
        });
        btnShowRecentAppsVariant10.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 10);
            startActivity(intent);
        });
        btnShowRecentAppsVariant11.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 11);
            startActivity(intent);
        });
        btnShowRecentAppsVariant12.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 12);
            startActivity(intent);
        });

        // Launch table‑style variants 13–14 of the recents list
        btnShowRecentAppsVariant13.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 13);
            startActivity(intent);
        });
        btnShowRecentAppsVariant14.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 14);
            startActivity(intent);
        });

        // Launch experimental variants (v15–v22) of the recents list
        btnShowRecentAppsVariant15.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 15);
            startActivity(intent);
        });
        btnShowRecentAppsVariant16.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 16);
            startActivity(intent);
        });
        btnShowRecentAppsVariant17.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 17);
            startActivity(intent);
        });
        btnShowRecentAppsVariant18.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 18);
            startActivity(intent);
        });
        btnShowRecentAppsVariant19.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 19);
            startActivity(intent);
        });
        btnShowRecentAppsVariant20.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 20);
            startActivity(intent);
        });
        btnShowRecentAppsVariant21.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 21);
            startActivity(intent);
        });
        btnShowRecentAppsVariant22.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 22);
            startActivity(intent);
        });

        // Launch additional variants (23–32) of the recents list
        btnShowRecentAppsVariant23.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 23);
            startActivity(intent);
        });
        btnShowRecentAppsVariant24.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 24);
            startActivity(intent);
        });
        btnShowRecentAppsVariant25.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 25);
            startActivity(intent);
        });
        btnShowRecentAppsVariant26.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 26);
            startActivity(intent);
        });
        btnShowRecentAppsVariant27.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 27);
            startActivity(intent);
        });
        btnShowRecentAppsVariant28.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 28);
            startActivity(intent);
        });
        btnShowRecentAppsVariant29.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 29);
            startActivity(intent);
        });
        btnShowRecentAppsVariant30.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 30);
            startActivity(intent);
        });
        btnShowRecentAppsVariant31.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 31);
            startActivity(intent);
        });
        btnShowRecentAppsVariant32.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 32);
            startActivity(intent);
        });

        // Launch experimental variants (v23–v32) of the recents list
        btnShowRecentAppsVariant23.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 23);
            startActivity(intent);
        });
        btnShowRecentAppsVariant24.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 24);
            startActivity(intent);
        });
        btnShowRecentAppsVariant25.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 25);
            startActivity(intent);
        });
        btnShowRecentAppsVariant26.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 26);
            startActivity(intent);
        });
        btnShowRecentAppsVariant27.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 27);
            startActivity(intent);
        });
        btnShowRecentAppsVariant28.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 28);
            startActivity(intent);
        });
        btnShowRecentAppsVariant29.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 29);
            startActivity(intent);
        });
        btnShowRecentAppsVariant30.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 30);
            startActivity(intent);
        });
        btnShowRecentAppsVariant31.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 31);
            startActivity(intent);
        });
        btnShowRecentAppsVariant32.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecentAppsActivity.class);
            intent.putExtra("variant", 32);
            startActivity(intent);
        });

        // Click listeners for closing specific packages using different strategies
        btnCloseSpecificAppsVariant1.setOnClickListener(v -> closeSpecificApps(1));
        btnCloseSpecificAppsVariant2.setOnClickListener(v -> closeSpecificApps(2));
        btnCloseSpecificAppsVariant3.setOnClickListener(v -> closeSpecificApps(3));
        btnCloseSpecificAppsVariant4.setOnClickListener(v -> closeSpecificApps(4));

        // Show the last app. Require usage access permission similarly to the recents list.
        btnOpenLastApp.setOnClickListener(v -> {
            // Determine if usage access is granted by querying UsageEvents
            boolean accessGranted;
            try {
                android.app.usage.UsageStatsManager usm = (android.app.usage.UsageStatsManager) getSystemService(android.content.Context.USAGE_STATS_SERVICE);
                long now = System.currentTimeMillis();
                android.app.usage.UsageEvents events = usm.queryEvents(now - 1000 * 60 * 60, now);
                accessGranted = events != null && events.hasNextEvent();
            } catch (Exception e) {
                accessGranted = false;
            }
            if (!accessGranted) {
                // Show a toast and open the usage access settings similar to RecentsActivity
                Toast.makeText(MainActivity.this, getString(R.string.grant_usage_access), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                startActivity(new Intent(MainActivity.this, LastAppActivity.class));
            }
        });


        // Trigger the system recents panel via the accessibility service. This replicates
        // the original "List Actions" behaviour where a global recents overlay is shown.
        btnListActions.setOnClickListener(v -> {
            if (RecentsAccessibilityService.isServiceEnabled()) {
                RecentsAccessibilityService.showRecents();
            } else {
                // Inform the user that the accessibility service must be enabled first
                Toast.makeText(MainActivity.this, R.string.service_not_enabled, Toast.LENGTH_SHORT).show();
            }
        });

        // Launch activity to manage excluded apps
        btnManageExcluded.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ExcludedAppsActivity.class)));

        // Generate a debug report containing usage events/stats for troubleshooting
        btnCollectDebug.setOnClickListener(v -> DebugHelper.collectDebugInfo(MainActivity.this));

        // Show the live events diagnostic screen
        btnShowLiveEvents.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LiveEventsActivity.class)));

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
        // Determine whether the accessibility service is currently enabled. We
        // update the status message accordingly and always append a note that
        // the service is not utilised by this app. When the service is not
        // enabled we also show a button to jump to the accessibility settings.
        boolean serviceEnabled = RecentsAccessibilityService.isServiceEnabled();
        String status = getString(serviceEnabled ? R.string.service_enabled : R.string.service_not_enabled);
        String note = getString(R.string.service_note_not_used);
        tvStatus.setText(status + "\n" + note);
        btnEnableService.setVisibility(serviceEnabled ? View.GONE : View.VISIBLE);
    }

    /**
     * Closes a fixed set of packages using the accessibility service. Different
     * variant values adjust delays and back navigation between each close to
     * experiment with reliability. Packages that cannot be closed (e.g. settings
     * packages or this app) are skipped. The list of packages is defined in
     * {@link #SPECIFIC_CLOSE_PACKAGES}.
     *
     * @param variant which automation strategy to use (1–4)
     */
    private void closeSpecificApps(int variant) {
        if (!RecentsAccessibilityService.isServiceEnabled()) {
            Toast.makeText(this, R.string.service_not_enabled, Toast.LENGTH_SHORT).show();
            return;
        }
        final RecentsAccessibilityService svc = RecentsAccessibilityService.getInstance();
        if (svc == null) {
            Toast.makeText(this, R.string.service_not_enabled, Toast.LENGTH_SHORT).show();
            return;
        }
        android.os.Handler handler = new android.os.Handler(getMainLooper());
        // Configure delays based on variant
        long baseDelay;
        long forceDelay;
        boolean performBack;
        int backCount;
        switch (variant) {
            case 2:
                baseDelay = 2500L;
                forceDelay = 800L;
                performBack = true;
                backCount = 1;
                break;
            case 3:
                baseDelay = 3000L;
                forceDelay = 800L;
                performBack = true;
                backCount = 2;
                break;
            case 4:
                baseDelay = 3500L;
                forceDelay = 800L;
                performBack = true;
                backCount = 3;
                break;
            case 1:
            default:
                baseDelay = 2000L;
                forceDelay = 800L;
                performBack = false;
                backCount = 0;
                break;
        }
        int idx = 0;
        for (String pkg : SPECIFIC_CLOSE_PACKAGES) {
            // Skip our own package and system settings packages (including some misspelled variants)
            if (pkg.equals(getPackageName()) ||
                    pkg.startsWith("com.android.tv.settings") ||
                    pkg.startsWith("com.google.android.tv.settings") ||
                    pkg.startsWith("com.android.settings") ||
                    pkg.startsWith("com.andrpid.tv.settings") ||
                    pkg.startsWith("com.andrpid.settings")) {
                continue;
            }
            final String targetPkg = pkg;
            long delay = idx * baseDelay;
            handler.postDelayed(() -> {
                // Open the application details settings for this package
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + targetPkg));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                    // After a small delay, trigger the force stop automation
                    handler.postDelayed(() -> {
                        RecentsAccessibilityService service = RecentsAccessibilityService.getInstance();
                        if (service != null) {
                            service.performForceStopSequence();
                            if (performBack) {
                                // Post additional back actions if requested
                                for (int i = 0; i < backCount; i++) {
                                    int finalI = i;
                                    handler.postDelayed(() -> service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK), finalI * 400L);
                                }
                            }
                        }
                    }, forceDelay);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, targetPkg + " cannot be opened in settings", Toast.LENGTH_SHORT).show();
                }
            }, delay);
            idx++;
        }
    }
}