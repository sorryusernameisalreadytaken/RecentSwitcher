package eu.recentsopener;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.provider.Settings;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RecentAppsActivity displays a list of recently used packages using the
 * UsageStatsManager API. It allows the user to grant usage access if not
 * already granted and to launch one of the recently used apps when tapped.
 * Long-press toggles exclusion of an app. Excluded apps remain in the list
 * but are highlighted in red and will not be launched when tapped.
 */
public class RecentAppsActivity extends AppCompatActivity {
    /**
     * List of AppEntry objects representing recently used apps. Each entry
     * contains the package name, user-facing label and application icon.
     */
    private final List<AppEntry> recentApps = new ArrayList<>();

    /**
     * Handler and runnable used to periodically refresh the recents list. The list
     * is refreshed at a fixed interval while this activity is in the foreground
     * so that closed or newly started apps appear/disappear without requiring
     * navigating away and back again.
     */
    private android.os.Handler refreshHandler;
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            // Only refresh if usage access is granted; otherwise the list would be empty
            if (hasUsageAccess()) {
                loadRecents();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
            // Schedule the next refresh if the handler still exists
            if (refreshHandler != null) {
                refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        }
    };

    /**
     * Refresh interval in milliseconds. The recents list will be reloaded on
     * this cadence while the activity is visible.
     */
    private static final long REFRESH_INTERVAL_MS = 1000L;

    /**
     * Reference to the ListView that displays the recents. Stored so that we
     * can set focus and selection when necessary.
     */
    private ListView listView;

    // Duplicate fields (refresh interval, handler, runnable and listView) removed. These are defined earlier in the class.

    /**
     * Adapter for the recents list. Stored as a field so that it can be
     * refreshed in onResume() once usage access is granted.
     */
    private RecentAppsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_apps);

        // Initialise the handler used for periodic list refreshes on the main thread. Without
        // instantiation the handler would remain null and schedule/removal calls would crash.
        refreshHandler = new android.os.Handler(getMainLooper());

        Button btnCloseAll = findViewById(R.id.btn_close_all);
        Button btnCloseOthers = findViewById(R.id.btn_close_others);
        Button btnCloseAllV2 = findViewById(R.id.btn_close_all_variant2);
        Button btnCloseAllV3 = findViewById(R.id.btn_close_all_variant3);
        // Additional variant button for experimental bulk closing strategy
        Button btnCloseAllV4 = findViewById(R.id.btn_close_all_variant4);
        // Store listView as a field so refresh handler can access it
        listView = findViewById(R.id.listView);
        // Allow child views (e.g. gear buttons) inside list items to take focus and be reachable via DPAD.
        // We rely on the row itself being focusable (see item_recent_app.xml) so that the list entry
        // receives initial focus before any nested children. Setting descendant focusability to
        // AFTER_DESCENDANTS allows the list view to delegate focus handling to its items without
        // prioritising the child buttons over the row itself. This combination ensures the gear
        // does not automatically take focus when the list is navigated.
        listView.setItemsCanFocus(true);
        listView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        // Close all apps via accessibility automation. Only visible/working when the
        // accessibility service is enabled. We exclude our own app and any
        // system settings packages. Excluded apps are skipped.
        btnCloseAll.setOnClickListener(v -> {
            if (!RecentsAccessibilityService.isServiceEnabled()) {
                Toast.makeText(this, R.string.service_not_enabled, Toast.LENGTH_SHORT).show();
                return;
            }
            java.util.List<String> pkgs = new java.util.ArrayList<>();
            for (AppEntry entry : recentApps) {
                String pkg = entry.packageName;
                // Skip our own app
                if (pkg.equals(getPackageName())) continue;
                // Skip excluded apps
                if (PrefsHelper.isExcluded(this, pkg)) continue;
                // Skip system settings packages
                if (pkg.startsWith("com.android.tv.settings") || pkg.startsWith("com.google.android.tv.settings") || pkg.startsWith("com.android.settings")) continue;
                pkgs.add(pkg);
            }
            performBulkClose(pkgs);
        });

        // Alternative variant 1: force stop apps but ensure we press back after each to return
        btnCloseAllV2.setOnClickListener(v -> {
            if (!RecentsAccessibilityService.isServiceEnabled()) {
                Toast.makeText(this, R.string.service_not_enabled, Toast.LENGTH_SHORT).show();
                return;
            }
            java.util.List<String> pkgs = new java.util.ArrayList<>();
            for (AppEntry entry : recentApps) {
                String pkg = entry.packageName;
                if (pkg.equals(getPackageName())) continue;
                if (PrefsHelper.isExcluded(this, pkg)) continue;
                if (pkg.startsWith("com.android.tv.settings") || pkg.startsWith("com.google.android.tv.settings") || pkg.startsWith("com.android.settings")) continue;
                pkgs.add(pkg);
            }
            performBulkCloseVariant2(pkgs);
        });

        // Alternative variant 2: force stop apps and perform extra back navigation after all
        btnCloseAllV3.setOnClickListener(v -> {
            if (!RecentsAccessibilityService.isServiceEnabled()) {
                Toast.makeText(this, R.string.service_not_enabled, Toast.LENGTH_SHORT).show();
                return;
            }
            java.util.List<String> pkgs = new java.util.ArrayList<>();
            for (AppEntry entry : recentApps) {
                String pkg = entry.packageName;
                if (pkg.equals(getPackageName())) continue;
                if (PrefsHelper.isExcluded(this, pkg)) continue;
                if (pkg.startsWith("com.android.tv.settings") || pkg.startsWith("com.google.android.tv.settings") || pkg.startsWith("com.android.settings")) continue;
                pkgs.add(pkg);
            }
            performBulkCloseVariant3(pkgs);
        });

        // Variant 3 (index 4): experimental close strategy which reloads the recents list after
        // each app is closed and defers closing system settings packages until the end. This
        // allows the activity to recapture focus on the first entry after each stop and avoids
        // leaving the system settings app at the top of the stack.
        btnCloseAllV4.setOnClickListener(v -> {
            if (!RecentsAccessibilityService.isServiceEnabled()) {
                Toast.makeText(this, R.string.service_not_enabled, Toast.LENGTH_SHORT).show();
                return;
            }
            java.util.List<String> pkgs = new java.util.ArrayList<>();
            for (AppEntry entry : recentApps) {
                String pkg = entry.packageName;
                if (pkg.equals(getPackageName())) continue;
                if (PrefsHelper.isExcluded(this, pkg)) continue;
                pkgs.add(pkg);
            }
            performBulkCloseVariant4(pkgs);
        });

        // Close all apps except the one that was in the foreground immediately before
        // this activity was opened. This approximates "Close other apps" logic.
        btnCloseOthers.setOnClickListener(v -> {
            if (!RecentsAccessibilityService.isServiceEnabled()) {
                Toast.makeText(this, R.string.service_not_enabled, Toast.LENGTH_SHORT).show();
                return;
            }
            // Identify the app that was foregrounded before this activity (may be null)
            String excludePkg = getPreviousForegroundApp();
            java.util.List<String> pkgs = new java.util.ArrayList<>();
            for (AppEntry entry : recentApps) {
                String pkg = entry.packageName;
                if (pkg.equals(getPackageName())) continue;
                if (excludePkg != null && pkg.equals(excludePkg)) continue;
                if (PrefsHelper.isExcluded(this, pkg)) continue;
                if (pkg.startsWith("com.android.tv.settings") || pkg.startsWith("com.google.android.tv.settings") || pkg.startsWith("com.android.settings")) continue;
                pkgs.add(pkg);
            }
            performBulkClose(pkgs);
        });

        // If usage access is not granted, prompt the user to enable it. We still
        // continue and set up the adapter so that the list can be populated
        // once permission is granted. Without permission the list will remain empty.
        boolean accessGranted = hasUsageAccess();
        if (!accessGranted) {
            requestUsageAccess();
        }

        // Populate the list if we already have access; otherwise it stays empty
        if (accessGranted) {
            // Initialise excluded apps on first access
            PrefsHelper.getExcludedApps(this);
            loadRecents();
        }
        adapter = new RecentAppsAdapter(this, recentApps);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            AppEntry entry = recentApps.get(position);
            // Launch the selected app if it is not excluded
            if (!PrefsHelper.isExcluded(this, entry.packageName)) {
                // Attempt to acquire a TV‑optimised launch intent first. Some
                // Android TV apps only declare a LEANBACK_LAUNCHER category and
                // therefore getLaunchIntentForPackage() returns null. See
                // Google issue 242899915 for details【618002977037848†L92-L100】.
                PackageManager pm = getPackageManager();
                Intent launchIntent = pm.getLeanbackLaunchIntentForPackage(entry.packageName);
                if (launchIntent == null) {
                    launchIntent = pm.getLaunchIntentForPackage(entry.packageName);
                }
                if (launchIntent != null) {
                    // Update the last/previous history before launching
                    PrefsHelper.updateHistory(this, entry.packageName);
                    startActivity(launchIntent);
                    finish();
                } else {
                    // Special-case system settings packages
                    if (entry.packageName != null && entry.packageName.contains("settings")) {
                        Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
                        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            PrefsHelper.updateHistory(this, entry.packageName);
                            startActivity(settingsIntent);
                            finish();
                        } catch (Exception e) {
                            Toast.makeText(this, entry.packageName + " cannot be launched", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, entry.packageName + " cannot be launched", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                // Inform the user that the app is excluded
                Toast.makeText(this, getString(R.string.app_excluded, entry.label), Toast.LENGTH_SHORT).show();
            }
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            AppEntry entry = recentApps.get(position);
            boolean currentlyExcluded = PrefsHelper.isExcluded(RecentAppsActivity.this, entry.packageName);
            // Save the current scroll position so that we can restore it after updating the list
            int index = listView.getFirstVisiblePosition();
            View topView = listView.getChildAt(0);
            int top = (topView == null) ? 0 : topView.getTop();
            if (currentlyExcluded) {
                // Remove from exclusion list and reinstate this app in the recents list
                PrefsHelper.removeExcludedApp(RecentAppsActivity.this, entry.packageName);
                Toast.makeText(RecentAppsActivity.this, getString(R.string.app_included, entry.label), Toast.LENGTH_SHORT).show();
                // Rebuild the recents list to include the newly included app
                loadRecents();
            } else {
                // Add to exclusion list and remove from the displayed list
                PrefsHelper.addExcludedApp(RecentAppsActivity.this, entry.packageName);
                Toast.makeText(RecentAppsActivity.this, getString(R.string.app_excluded, entry.label), Toast.LENGTH_SHORT).show();
                recentApps.remove(position);
            }
            adapter.notifyDataSetChanged();
            // Restore scroll position
            listView.setSelectionFromTop(index, top);
            return true;
        });


        // Allow DPAD-LEFT to open the system settings screen for the selected app.
        // Intercept DPAD‑LEFT and DPAD‑RIGHT presses on list items. DPAD‑RIGHT moves focus to the
        // gear button on the current row so that the user can press enter to open settings. DPAD‑LEFT
        // opens the system settings for the selected app and optionally triggers the force‑stop
        // automation if the accessibility service is enabled.
        listView.setOnKeyListener((v, keyCode, event) -> {
            // Only handle key down events. Up events should be allowed to bubble normally.
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            int pos = listView.getSelectedItemPosition();
            if (pos == android.widget.AdapterView.INVALID_POSITION) {
                return false;
            }
            // When navigating within the list, DPAD‑right should move focus to the gear on the
            // current row. If no gear exists the event is ignored so that the system can
            // determine the next focus.
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                View row = listView.getChildAt(pos - listView.getFirstVisiblePosition());
                if (row != null) {
                    android.widget.ImageButton gear = row.findViewById(R.id.settings_button);
                    if (gear != null) {
                        gear.requestFocus();
                        return true;
                    }
                }
                return false;
            }
            // DPAD‑left on a list item should open the app settings and trigger a forced stop
            // through the accessibility service. If the selected app is a system settings package
            // we skip the force stop automation.
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (pos < recentApps.size()) {
                    AppEntry appEntry = recentApps.get(pos);
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + appEntry.packageName));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(intent);
                        boolean isSettings = appEntry.packageName.startsWith("com.android.tv.settings") ||
                                appEntry.packageName.startsWith("com.google.android.tv.settings") ||
                                appEntry.packageName.startsWith("com.android.settings");
                        if (!isSettings) {
                            if (RecentsAccessibilityService.isServiceEnabled()) {
                                RecentsAccessibilityService svc = RecentsAccessibilityService.getInstance();
                                if (svc != null) {
                                    svc.performForceStopSequence();
                                }
                            } else {
                                Toast.makeText(this, R.string.service_not_enabled, Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, appEntry.packageName + " cannot be opened in settings", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Returns true if usage access is already granted. We query for events in
     * the last hour and check if any are available.
     */
    private boolean hasUsageAccess() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long now = System.currentTimeMillis();
        UsageEvents events = usm.queryEvents(now - 1000 * 60 * 60, now);
        return events != null && events.hasNextEvent();
    }

    /**
     * Launch the system settings screen where the user can grant usage access
     * to this application. A toast is shown beforehand explaining why.
     */
    private void requestUsageAccess() {
        Toast.makeText(this, getString(R.string.grant_usage_access), Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Loads the list of recent packages from usage events in the last 24
     * hours. Duplicates are removed while maintaining order. The list is
     * reversed so that the most recently used app appears first. Excluded
     * apps remain in the list and are highlighted by the adapter.
     */
    private void loadRecents() {
        // Ensure excluded apps are initialised on every load. This prevents freshly installed
        // instances from showing excluded packages before the default list has been persisted.
        PrefsHelper.getExcludedApps(this);
        recentApps.clear();
        long end = System.currentTimeMillis();
        long begin = end - 1000L * 60 * 60 * 24; // last 24 hours
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents events = usm.queryEvents(begin, end);
        Set<String> seen = new HashSet<>();
        List<String> packagesInOrder = new ArrayList<>();
        UsageEvents.Event event = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                String pkg = event.getPackageName();
                // Skip our own app
                if (!getPackageName().equals(pkg)) {
                    // Maintain order of last occurrence
                    if (seen.contains(pkg)) {
                        packagesInOrder.remove(pkg);
                    }
                    packagesInOrder.add(pkg);
                    seen.add(pkg);
                }
            }
        }
        Collections.reverse(packagesInOrder);
        PackageManager pm = getPackageManager();
        for (String pkg : packagesInOrder) {
            // Do not display excluded packages in the recents list
            if (PrefsHelper.isExcluded(this, pkg)) {
                continue;
            }
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
                // Skip stopped applications; these are not currently running and cannot be force‑closed via UI.
                if ((appInfo.flags & ApplicationInfo.FLAG_STOPPED) != 0) {
                    continue;
                }
                String label = pm.getApplicationLabel(appInfo).toString();
                Drawable icon = pm.getApplicationIcon(appInfo);
                recentApps.add(new AppEntry(pkg, label, icon));
            } catch (PackageManager.NameNotFoundException e) {
                // skip unknown packages
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean access = hasUsageAccess();
        // If usage access was just granted and the list is empty, initialise excluded defaults and load recents
        if (access && recentApps.isEmpty()) {
            PrefsHelper.getExcludedApps(this);
            loadRecents();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
        // Set up views depending on whether the accessibility service is enabled. When enabled,
        // both "Close all" and "Close other" buttons are visible. We also request initial focus
        // on the "Close other apps" button so that a freshly opened activity begins there. If the
        // service is disabled we hide the buttons and place focus on the first list item.
        android.widget.Button btnCloseAll = findViewById(R.id.btn_close_all);
        android.widget.Button btnCloseOthers = findViewById(R.id.btn_close_others);
        android.widget.Button btnCloseAllV2 = findViewById(R.id.btn_close_all_variant2);
        android.widget.Button btnCloseAllV3 = findViewById(R.id.btn_close_all_variant3);
        android.widget.Button btnCloseAllV4 = findViewById(R.id.btn_close_all_variant4);
        boolean serviceEnabled = RecentsAccessibilityService.isServiceEnabled();
        btnCloseAll.setVisibility(serviceEnabled ? android.view.View.VISIBLE : android.view.View.GONE);
        btnCloseOthers.setVisibility(serviceEnabled ? android.view.View.VISIBLE : android.view.View.GONE);
        btnCloseAllV2.setVisibility(serviceEnabled ? android.view.View.VISIBLE : android.view.View.GONE);
        btnCloseAllV3.setVisibility(serviceEnabled ? android.view.View.VISIBLE : android.view.View.GONE);
        btnCloseAllV4.setVisibility(serviceEnabled ? android.view.View.VISIBLE : android.view.View.GONE);
        // Determine initial focus: prefer the "Close other" button when visible, otherwise the list
        if (serviceEnabled) {
            // When the accessibility service is enabled we want to start from the "Close other"
            // button. Pre-select the first list row so that up/down navigation from the button
            // behaves consistently.
            if (!recentApps.isEmpty()) {
                listView.setSelection(0);
            }
            btnCloseOthers.requestFocus();
        } else {
            if (!recentApps.isEmpty()) {
                listView.setSelection(0);
                listView.requestFocus();
            }
        }
        // Start periodic refresh if access granted
        refreshHandler.removeCallbacks(refreshRunnable);
        if (access) {
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop refreshing when the activity is no longer visible
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    /**
     * Performs a bulk closing of the provided packages. Each package will be
     * closed sequentially by opening its settings page and invoking the
     * force‑stop automation. A small delay is inserted between each action
     * to allow the UI to update.
     *
     * @param packages list of package names to close
     */
    private void performBulkClose(java.util.List<String> packages) {
        if (packages == null || packages.isEmpty()) {
            return;
        }
        // Only proceed if the accessibility service is connected
        RecentsAccessibilityService svc = RecentsAccessibilityService.getInstance();
        if (svc == null) {
            Toast.makeText(this, R.string.service_not_enabled, Toast.LENGTH_SHORT).show();
            return;
        }
        android.os.Handler handler = new android.os.Handler(getMainLooper());
        // Delay between each close operation (ms). This allows the UI to load the settings screen
        // and for the force‑stop automation to execute. Adjust if your device is slower or faster.
        // Delay between closing each app in the bulk close operation. Increased from 2000ms to
        // 3000ms to allow slower devices more time to open settings and process force‑stop actions.
        // Increase delay between each close operation to 4000 ms. Android TV 14 may take
        // several seconds to load the app settings screen and present the force‑stop dialog.
        final long stepDelay = 4000L;
        for (int i = 0; i < packages.size(); i++) {
            final String pkg = packages.get(i);
            long delay = i * stepDelay;
            handler.postDelayed(() -> {
                // Open the app details settings
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + pkg));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                    // Trigger automation after a short delay
                    handler.postDelayed(() -> {
                        RecentsAccessibilityService service = RecentsAccessibilityService.getInstance();
                        if (service != null) {
                            service.performForceStopSequence();
                        }
                    }, 500);
                } catch (Exception e) {
                    Toast.makeText(this, pkg + " cannot be opened in settings", Toast.LENGTH_SHORT).show();
                }
            }, delay);
        }

        // After all packages have been processed, refresh the recents list to remove
        // any apps that were successfully stopped. We schedule this after the last
        // operation plus an extra delay to allow the UI to finish closing.
        long finalDelay = packages.size() * stepDelay + 1000L;
        handler.postDelayed(() -> {
            loadRecents();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
                // Restore selection/focus to the first entry
                if (!recentApps.isEmpty()) {
                    listView.setSelection(0);
                    listView.requestFocus();
                }
            }
        }, finalDelay);
    }

    /**
     * Alternative variant for bulk closing apps. After triggering the force‑stop
     * sequence on each app we explicitly send a global BACK action to ensure
     * the settings screen is closed before moving to the next app. This may
     * help on devices where the standard automation sometimes leaves the UI
     * stuck in the app settings.
     *
     * @param packages list of packages to close
     */
    private void performBulkCloseVariant2(java.util.List<String> packages) {
        if (packages == null || packages.isEmpty()) {
            return;
        }
        RecentsAccessibilityService svc = RecentsAccessibilityService.getInstance();
        if (svc == null) {
            Toast.makeText(this, R.string.service_not_enabled, Toast.LENGTH_SHORT).show();
            return;
        }
        android.os.Handler handler = new android.os.Handler(getMainLooper());
        // Increase delay for variant 2 to 4000 ms to allow UI transitions on Android TV 14.
        final long stepDelay = 4000L;
        for (int i = 0; i < packages.size(); i++) {
            final String pkg = packages.get(i);
            long delay = i * stepDelay;
            handler.postDelayed(() -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + pkg));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                    // After a short delay, perform the normal force stop automation
                    handler.postDelayed(() -> {
                        RecentsAccessibilityService service = RecentsAccessibilityService.getInstance();
                        if (service != null) {
                            service.performForceStopSequence();
                            // After another delay send a BACK command to close the settings screen
                            handler.postDelayed(() -> {
                                RecentsAccessibilityService svc2 = RecentsAccessibilityService.getInstance();
                                if (svc2 != null) {
                                    svc2.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
                                }
                            }, 1000);
                        }
                    }, 500);
                } catch (Exception e) {
                    Toast.makeText(this, pkg + " cannot be opened in settings", Toast.LENGTH_SHORT).show();
                }
            }, delay);
        }
        // Refresh list after operations
        long finalDelay = packages.size() * stepDelay + 2000L;
        handler.postDelayed(() -> {
            loadRecents();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
                if (!recentApps.isEmpty()) {
                    listView.setSelection(0);
                    listView.requestFocus();
                }
            }
        }, finalDelay);
    }

    /**
     * Another alternative variant for bulk closing apps. This variant triggers
     * the force stop sequence and then, once all apps have been processed, sends
     * two BACK actions in quick succession. This may help return to the recents
     * list if the previous approach leaves one screen behind.
     *
     * @param packages list of packages to close
     */
    private void performBulkCloseVariant3(java.util.List<String> packages) {
        if (packages == null || packages.isEmpty()) {
            return;
        }
        RecentsAccessibilityService svc = RecentsAccessibilityService.getInstance();
        if (svc == null) {
            Toast.makeText(this, R.string.service_not_enabled, Toast.LENGTH_SHORT).show();
            return;
        }
        android.os.Handler handler = new android.os.Handler(getMainLooper());
        // Increase delay for variant 3 to 4000 ms as well.
        final long stepDelay = 4000L;
        for (int i = 0; i < packages.size(); i++) {
            final String pkg = packages.get(i);
            long delay = i * stepDelay;
            handler.postDelayed(() -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + pkg));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                    handler.postDelayed(() -> {
                        RecentsAccessibilityService service = RecentsAccessibilityService.getInstance();
                        if (service != null) {
                            service.performForceStopSequence();
                        }
                    }, 500);
                } catch (Exception e) {
                    Toast.makeText(this, pkg + " cannot be opened in settings", Toast.LENGTH_SHORT).show();
                }
            }, delay);
        }
        long finalDelay = packages.size() * stepDelay + 2000L;
        handler.postDelayed(() -> {
            // Send two back actions to return to the recents list/activity
            RecentsAccessibilityService service = RecentsAccessibilityService.getInstance();
            if (service != null) {
                service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
                handler.postDelayed(() -> {
                    RecentsAccessibilityService svc2 = RecentsAccessibilityService.getInstance();
                    if (svc2 != null) {
                        svc2.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
                    }
                }, 500);
            }
            // Refresh list in case some apps were stopped
            loadRecents();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
                if (!recentApps.isEmpty()) {
                    listView.setSelection(0);
                    listView.requestFocus();
                }
            }
        }, finalDelay);
    }

    /**
     * Experimental variant for bulk closing apps. This variant reloads the recents list after
     * each individual app closure and defers closing any system settings packages until all
     * other apps have been processed. A shadow copy of the list of packages to be closed is
     * created at the time the action is triggered. After each force stop the accessibility
     * service performs a global BACK action to return to this activity. The recents list is
     * refreshed so that subsequent closes act on the up-to-date set of running apps.
     *
     * @param packages list of package names to close
     */
    private void performBulkCloseVariant4(java.util.List<String> packages) {
        if (packages == null || packages.isEmpty()) {
            return;
        }
        RecentsAccessibilityService svc = RecentsAccessibilityService.getInstance();
        if (svc == null) {
            Toast.makeText(this, R.string.service_not_enabled, Toast.LENGTH_SHORT).show();
            return;
        }
        // Make a shallow copy and reorder so that system settings packages are processed last.
        java.util.List<String> queue = new java.util.ArrayList<>(packages);
        java.util.List<String> deferred = new java.util.ArrayList<>();
        java.util.Iterator<String> it = queue.iterator();
        while (it.hasNext()) {
            String pkg = it.next();
            if (pkg.startsWith("com.android.tv.settings") || pkg.startsWith("com.google.android.tv.settings") || pkg.startsWith("com.android.settings")) {
                deferred.add(pkg);
                it.remove();
            }
        }
        queue.addAll(deferred);
        android.os.Handler handler = new android.os.Handler(getMainLooper());
        // Use a delay similar to other variants to allow UI to settle between operations.
        // Increase delay for variant 4 to 4000 ms to match other variants.
        final long stepDelay = 4000L;
        for (int i = 0; i < queue.size(); i++) {
            final String pkg = queue.get(i);
            long delay = i * stepDelay;
            handler.postDelayed(() -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + pkg));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                    // After a short delay, trigger the force stop sequence
                    handler.postDelayed(() -> {
                        RecentsAccessibilityService service = RecentsAccessibilityService.getInstance();
                        if (service != null) {
                            service.performForceStopSequence();
                            // After another delay send BACK to return to this activity
                            handler.postDelayed(() -> {
                                RecentsAccessibilityService svc2 = RecentsAccessibilityService.getInstance();
                                if (svc2 != null) {
                                    svc2.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
                                }
                                // Immediately refresh the recents list to reflect any changes
                                loadRecents();
                                if (adapter != null) {
                                    adapter.notifyDataSetChanged();
                                    if (!recentApps.isEmpty()) {
                                        listView.setSelection(0);
                                    }
                                }
                            }, 1000);
                        }
                    }, 500);
                } catch (Exception e) {
                    Toast.makeText(this, pkg + " cannot be opened in settings", Toast.LENGTH_SHORT).show();
                }
            }, delay);
        }
        // Final refresh after all packages have been processed
        long finalDelay = queue.size() * stepDelay + 2000L;
        handler.postDelayed(() -> {
            loadRecents();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
                if (!recentApps.isEmpty()) {
                    listView.setSelection(0);
                    listView.requestFocus();
                }
            }
        }, finalDelay);
    }

    /**
     * Retrieves the package name of the app that was in the foreground most
     * recently, ignoring this app. Scans usage events over the last few
     * minutes. Returns null if no suitable event is found.
     */
    /**
     * Determines which package was in the foreground immediately before this app.
     * It scans recent usage events and returns the most recently foregrounded
     * package that is not this app. If none are found, returns null.
     */
    private String getPreviousForegroundApp() {
        try {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            long begin = now - 1000L * 60 * 5; // last 5 minutes
            UsageEvents events = usm.queryEvents(begin, now);
            UsageEvents.Event event = new UsageEvents.Event();
            String lastNonSelf = null;
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                int type = event.getEventType();
                if (type == UsageEvents.Event.MOVE_TO_FOREGROUND || type == UsageEvents.Event.ACTIVITY_RESUMED) {
                    String pkg = event.getPackageName();
                    if (pkg != null && !pkg.equals(getPackageName())) {
                        lastNonSelf = pkg;
                    }
                }
            }
            return lastNonSelf;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * A simple model class describing an app to be displayed in the recents
     * list. Holds the package name, user-visible label and application
     * icon.
     */
    private static class AppEntry {
        final String packageName;
        final String label;
        final Drawable icon;

        AppEntry(String packageName, String label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }

    /**
     * Custom adapter that renders each AppEntry in the recents list. The
     * icon is displayed on the left, followed by the app name and package
     * name. Excluded apps are highlighted in red.
     */
    private class RecentAppsAdapter extends ArrayAdapter<AppEntry> {
        private final LayoutInflater inflater;
        public RecentAppsAdapter(Context ctx, List<AppEntry> apps) {
            super(ctx, 0, apps);
            inflater = LayoutInflater.from(ctx);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_recent_app, parent, false);
            }
            AppEntry entry = getItem(position);
            ImageView iconView = view.findViewById(R.id.app_icon);
            TextView textView = view.findViewById(R.id.app_text);
            android.widget.ImageButton settingsButton = view.findViewById(R.id.settings_button);
                if (entry != null) {
                iconView.setImageDrawable(entry.icon);
                // Build display text as "Label (package)"
                String text = entry.label + " (" + entry.packageName + ")";
                textView.setText(text);
                // Highlight excluded packages in red or apply theme-aware colour
                if (PrefsHelper.isExcluded(getContext(), entry.packageName)) {
                    int colour = ContextCompat.getColor(getContext(), R.color.recent_app_text_color_excluded);
                    textView.setTextColor(colour);
                } else {
                    int colour = ContextCompat.getColor(getContext(), R.color.recent_app_text_color);
                    textView.setTextColor(colour);
                }
                // Set click listener on the settings gear to open the application details settings
                settingsButton.setOnClickListener(v -> {
                    // Build intent to show app details settings for this package
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + entry.packageName));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        getContext().startActivity(intent);
                    } catch (Exception e) {
                        android.widget.Toast.makeText(getContext(), entry.packageName + " cannot be opened in settings", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
                // When the gear is focused and the user presses DPAD‑left, DPAD‑up or DPAD‑down,
                // move focus back to the surrounding ListView and restore the selected position.
                settingsButton.setOnKeyListener((v, kCode, kEvent) -> {
                    if (kEvent.getAction() != KeyEvent.ACTION_DOWN) {
                        return false;
                    }
                    // When the gear has focus, intercept DPAD navigation. Up and down move
                    // directly to the previous or next row in the list. Left returns focus to
                    // this same row without triggering the force‑stop automation.
                    if (kCode == KeyEvent.KEYCODE_DPAD_UP) {
                        int target = position > 0 ? position - 1 : 0;
                        RecentAppsActivity.this.listView.requestFocus();
                        RecentAppsActivity.this.listView.setSelection(target);
                        return true;
                    }
                    if (kCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        int max = RecentAppsActivity.this.listView.getAdapter() != null ? RecentAppsActivity.this.listView.getAdapter().getCount() - 1 : position;
                        int target = position < max ? position + 1 : max;
                        RecentAppsActivity.this.listView.requestFocus();
                        RecentAppsActivity.this.listView.setSelection(target);
                        return true;
                    }
                    if (kCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        RecentAppsActivity.this.listView.requestFocus();
                        RecentAppsActivity.this.listView.setSelection(position);
                        return true;
                    }
                    return false;
                });
            }
            return view;
        }
    }
}