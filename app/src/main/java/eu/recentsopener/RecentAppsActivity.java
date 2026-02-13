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
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.util.Log;

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
                // Capture the package name of the currently selected item (if any) to restore
                // selection after the list refreshes. Using the package name rather than the
                // index allows the selection to remain on the same entry even if the ordering
                // changes.
                int selectedPosition = listView.getSelectedItemPosition();
                String selectedPackage = null;
                if (selectedPosition >= 0 && selectedPosition < recentApps.size()) {
                    selectedPackage = recentApps.get(selectedPosition).packageName;
                }
                // Reload recents and determine if anything changed
                boolean changed = loadRecentsInternal();
                // Notify the adapter only when data actually changed
                if (changed && adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                // Restore focus/selection on the previously selected package if it still exists
                if (selectedPackage != null) {
                    int index = -1;
                    for (int i = 0; i < recentApps.size(); i++) {
                        if (selectedPackage.equals(recentApps.get(i).packageName)) {
                            index = i;
                            break;
                        }
                    }
                    if (index >= 0) {
                        listView.setSelection(index);
                    }
                }
                // Log the current focused package and whether the gear was focused during this
                // refresh. This helps track navigation state over time.
                String focusedPkg = null;
                int selPos = listView.getSelectedItemPosition();
                if (selPos >= 0 && selPos < recentApps.size()) {
                    focusedPkg = recentApps.get(selPos).packageName;
                }
                android.util.Log.d("RecentAppsActivity",
                        "Refresh: focusedPkg=" + focusedPkg + ", focusedWasGear=" + lastFocusedWasGear + 
                        ", time=" + System.currentTimeMillis());
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

    /**
     * Index of the variant to use for the recents list. Variant 1 reproduces
     * the original focus behaviour where the row itself is not focusable and
     * the gear button can be focused. Variant 2 makes the row focusable
     * before its children. Variant 3 disables focus on the gear entirely.
     * Variant 4 makes the row focusable and the gear not focusable while
     * maintaining after‑descendants focus order. The default is 1.
     */
    private int variantIndex = 1;

    // Duplicate fields (refresh interval, handler, runnable and listView) removed. These are defined earlier in the class.

    /**
     * Adapter for the recents list. Stored as a field so that it can be
     * refreshed in onResume() once usage access is granted.
     */
    private RecentAppsAdapter adapter;

    /**
     * Tracks whether the last focused view was the gear button. This flag
     * is updated via onFocusChange listeners attached to each list item.
     */
    private boolean lastFocusedWasGear = false;

    // Additional bulk close buttons for experimental closing strategies
    private Button btnCloseAllVariant2;
    private Button btnCloseAllVariant3;
    private Button btnCloseAllVariant4;

    /**
     * Maintains the list of package names from the previous load of recent apps. This
     * is used to detect whether the recents list has actually changed and to
     * suppress unnecessary UI updates. Avoiding repeated notifyDataSetChanged()
     * calls when the underlying data has not changed reduces flicker in the
     * list, as the focus highlight is preserved between refreshes.
     */
    private final java.util.List<String> previousPackageOrder = new java.util.ArrayList<>();

    /**
     * Records the package name of the last focused app row or gear. This is
     * updated whenever a focus change occurs and is logged on refresh.
     */
    private String lastFocusedPkg = null;

    /** Tag used for logcat output. */
    private static final String TAG = "RecentAppsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_apps);

        // Read the requested variant index passed via the intent. Default to 1 if not provided.
        variantIndex = getIntent().getIntExtra("variant", 1);

        // Initialise the handler used for periodic list refreshes on the main thread. Without
        // instantiation the handler would remain null and schedule/removal calls would crash.
        refreshHandler = new android.os.Handler(getMainLooper());

        Button btnCloseAll = findViewById(R.id.btn_close_all);
        Button btnCloseOthers = findViewById(R.id.btn_close_others);
        // Initialise optional variant buttons for bulk closing apps
        btnCloseAllVariant2 = findViewById(R.id.btn_close_all_variant2);
        btnCloseAllVariant3 = findViewById(R.id.btn_close_all_variant3);
        btnCloseAllVariant4 = findViewById(R.id.btn_close_all_variant4);

        // Store listView as a field so refresh handler can access it
        listView = findViewById(R.id.listView);
        // Allow child views (e.g. gear buttons) inside list items to take focus
        listView.setItemsCanFocus(true);
        // Configure focus behaviour based on the selected variant. Variants 1 and 5 share
        // classic after-descendants behaviour with gear focusable. Variants 2 and 6 use
        // BEFORE_DESCENDANTS with row focusable and gear focusable. Variants 3 and 7 use
        // BEFORE_DESCENDANTS with row focusable and gear not focusable. Variants 4 and 8
        // use AFTER_DESCENDANTS with row focusable and gear not focusable. Default to
        // variant1 behaviour if index is out of range.
        switch (variantIndex) {
            // Variants that use BEFORE_DESCENDANTS focus order
            case 2:
            case 3:
            case 6:
            case 7:
            case 9:
            case 11:
            case 13:
            // New before-descendants variants
            case 15:
            case 17:
            case 19:
            case 21:
            case 23:
            case 25:
            case 27:
            case 29:
            case 31:
            case 32:
                listView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
                break;
            // Variants that use AFTER_DESCENDANTS focus order
            case 4:
            case 8:
            case 5:
            case 1:
            case 10:
            case 12:
            case 14:
            // New after-descendants variants
            case 16:
            case 18:
            case 20:
            case 22:
            case 24:
            case 26:
            case 28:
            case 30:
            default:
                listView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
                break;
        }

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
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            int pos = listView.getSelectedItemPosition();
            if (pos == android.widget.AdapterView.INVALID_POSITION) {
                return false;
            }
            // DPAD‑RIGHT: focus the gear button within the current row
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
            // DPAD‑LEFT: open app settings and attempt force‑stop if applicable
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
                        appEntry.packageName.startsWith("com.android.settings") ||
                        // Some devices or ROMs use misspelled identifiers. Skip auto close for these as well.
                        appEntry.packageName.startsWith("com.andrpid.tv.settings") ||
                        appEntry.packageName.startsWith("com.andrpid.settings");
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
    /**
     * Reloads the list of recent apps and returns whether the underlying data
     * actually changed. If the ordering or content of the packages remains
     * unchanged, no modifications are made to {@link #recentApps} and the
     * method returns false. This allows callers to suppress unnecessary
     * notifications to the adapter and thereby reduces flicker.
     *
     * @return true if the recents list was updated, false otherwise
     */
    private boolean loadRecentsInternal() {
        // Ensure excluded apps are initialised on every load. This prevents freshly installed
        // instances from showing excluded packages before the default list has been persisted.
        PrefsHelper.getExcludedApps(this);
        long end = System.currentTimeMillis();
        long begin = end - 1000L * 60 * 60 * 24; // last 24 hours
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents events = usm.queryEvents(begin, end);
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.List<String> packagesInOrder = new java.util.ArrayList<>();
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
        java.util.Collections.reverse(packagesInOrder);
        PackageManager pm = getPackageManager();
        // Build a new list of AppEntry objects without mutating recentApps yet. This allows
        // comparison with the existing list to detect whether anything has actually changed.
        java.util.List<AppEntry> newEntries = new java.util.ArrayList<>();
        java.util.List<String> newPackageOrder = new java.util.ArrayList<>();
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
                newEntries.add(new AppEntry(pkg, label, icon));
                newPackageOrder.add(pkg);
            } catch (PackageManager.NameNotFoundException e) {
                // skip unknown packages
            }
        }
        // Determine whether the ordering of package names has changed. If not, we can avoid
        // updating recentApps and preserve the existing list and focus.
        boolean changed = false;
        if (newPackageOrder.size() != previousPackageOrder.size()) {
            changed = true;
        } else {
            for (int i = 0; i < newPackageOrder.size(); i++) {
                if (!newPackageOrder.get(i).equals(previousPackageOrder.get(i))) {
                    changed = true;
                    break;
                }
            }
        }
        // Compute diff summary for logging purposes before mutating previousPackageOrder
        java.util.Set<String> added = new java.util.HashSet<>(newPackageOrder);
        added.removeAll(previousPackageOrder);
        java.util.Set<String> removed = new java.util.HashSet<>(previousPackageOrder);
        removed.removeAll(newPackageOrder);
        java.util.Set<String> moved = new java.util.HashSet<>();
        for (String pkg : newPackageOrder) {
            int oldIndex = previousPackageOrder.indexOf(pkg);
            int newIndex = newPackageOrder.indexOf(pkg);
            if (oldIndex >= 0 && oldIndex != newIndex) {
                moved.add(pkg);
            }
        }
        android.util.Log.d("RecentAppsActivity", "Diff summary: added=" + added + ", removed=" + removed + ", moved=" + moved);
        if (changed) {
            previousPackageOrder.clear();
            previousPackageOrder.addAll(newPackageOrder);
            recentApps.clear();
            recentApps.addAll(newEntries);
        }
        return changed;
    }

    /**
     * Backwards‑compatible wrapper that reloads the recents list without
     * returning a value. Existing callers that do not care about whether
     * the data changed can continue to use this method.
     */
    private void loadRecents() {
        loadRecentsInternal();
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
        // Set initial selection to the first item only if no item is currently selected.
        // This prevents the list from jumping back to the top when returning from other
        // activities (e.g. app settings). We still request focus on the list to ensure
        // DPAD navigation remains on the list rather than on nested child views.
        if (!recentApps.isEmpty()) {
            if (listView.getSelectedItemPosition() == android.widget.AdapterView.INVALID_POSITION) {
                listView.setSelection(0);
            }
            listView.requestFocus();
        }
        // Show or hide the close buttons based on accessibility service state
        android.widget.Button btnCloseAll = findViewById(R.id.btn_close_all);
        android.widget.Button btnCloseOthers = findViewById(R.id.btn_close_others);
        boolean serviceEnabled = RecentsAccessibilityService.isServiceEnabled();
        btnCloseAll.setVisibility(serviceEnabled ? android.view.View.VISIBLE : android.view.View.GONE);
        btnCloseOthers.setVisibility(serviceEnabled ? android.view.View.VISIBLE : android.view.View.GONE);
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
        final long stepDelay = 2000L;
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
        // Layout resources for each variant (1-indexed). Variants 1–4 correspond to the
        // classic behaviours described in the activity documentation. Variants 5–8 are
        // similar but provide alternate focus orders and combinations of row/gear
        // focusability. See the corresponding XML files under res/layout.
        private final int[] ITEM_LAYOUTS = new int[] {
                R.layout.item_recent_app_v1,
                R.layout.item_recent_app_v2,
                R.layout.item_recent_app_v3,
                R.layout.item_recent_app_v4,
                R.layout.item_recent_app_v5,
                R.layout.item_recent_app_v6,
                R.layout.item_recent_app_v7,
                R.layout.item_recent_app_v8,
                // Newly added variants (v9–v12) that extend v2/v3 behaviour
                R.layout.item_recent_app_v9,
                R.layout.item_recent_app_v10,
                R.layout.item_recent_app_v11,
                R.layout.item_recent_app_v12,
                // Table‑like variants (v13–v14) with separate columns for app details and action
                R.layout.item_recent_app_v13,
                R.layout.item_recent_app_v14,
                // New experimental variants (v15–v22) exploring nextFocus properties and DPAD navigation
                R.layout.item_recent_app_v15,
                R.layout.item_recent_app_v16,
                R.layout.item_recent_app_v17,
                R.layout.item_recent_app_v18,
                R.layout.item_recent_app_v19,
                R.layout.item_recent_app_v20,
                R.layout.item_recent_app_v21,
                R.layout.item_recent_app_v22,
                // Extended variants (v23–v32) built upon the table‑style layouts
                R.layout.item_recent_app_v23,
                R.layout.item_recent_app_v24,
                R.layout.item_recent_app_v25,
                R.layout.item_recent_app_v26,
                R.layout.item_recent_app_v27,
                R.layout.item_recent_app_v28,
                R.layout.item_recent_app_v29,
                R.layout.item_recent_app_v30,
                R.layout.item_recent_app_v31,
                R.layout.item_recent_app_v32
        };

        public RecentAppsAdapter(Context ctx, List<AppEntry> apps) {
            super(ctx, 0, apps);
            inflater = LayoutInflater.from(ctx);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            int variant = variantIndex;
            // Clamp variant to available layouts
            int layoutIndex = Math.max(1, Math.min(variant, ITEM_LAYOUTS.length)) - 1;
            if (view == null) {
                view = inflater.inflate(ITEM_LAYOUTS[layoutIndex], parent, false);
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
                // Attach focus listeners for instrumentation. When the app cell or gear
                // receives focus we log the package name, position and timestamp and
                // update lastFocusedWasGear accordingly. This helps diagnose DPAD
                // navigation behaviour on Android TV.
                final AppEntry currentEntry = entry;
                View appCell = view.findViewById(R.id.app_cell);
                if (appCell != null) {
                    appCell.setOnFocusChangeListener((v, hasFocus) -> {
                        if (hasFocus) {
                            lastFocusedWasGear = false;
                            android.util.Log.d("RecentAppsActivity",
                                    "Focus app_cell pkg=" + currentEntry.packageName + " pos=" + position + " t=" + System.currentTimeMillis());
                        }
                    });
                }
                if (settingsButton != null) {
                    settingsButton.setOnFocusChangeListener((v, hasFocus) -> {
                        if (hasFocus) {
                            lastFocusedWasGear = true;
                            android.util.Log.d("RecentAppsActivity",
                                    "Focus gear pkg=" + currentEntry.packageName + " pos=" + position + " t=" + System.currentTimeMillis());
                        }
                    });
                }
                // Highlight excluded packages in red or apply theme-aware colour
                if (PrefsHelper.isExcluded(getContext(), entry.packageName)) {
                    int colour = ContextCompat.getColor(getContext(), R.color.recent_app_text_color_excluded);
                    textView.setTextColor(colour);
                } else {
                    int colour = ContextCompat.getColor(getContext(), R.color.recent_app_text_color);
                    textView.setTextColor(colour);
                }
                // Make gear focusable or not based on variant. For variants where the gear
                // should not be focusable we disable its focusability here. In other cases
                // we allow default focus so that DPAD‑RIGHT can move to it.
                boolean gearFocusable;
                switch (variant) {
                    case 3:
                    case 4:
                    case 6:
                    case 8:
                        gearFocusable = false;
                        break;
                    default:
                        gearFocusable = true;
                        break;
                }
                settingsButton.setFocusable(gearFocusable);
                settingsButton.setFocusableInTouchMode(gearFocusable);
                // If gear is visible, set click listener to open app settings
                settingsButton.setOnClickListener(v -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + entry.packageName));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        getContext().startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), entry.packageName + " cannot be opened in settings", Toast.LENGTH_SHORT).show();
                    }
                });
                // Add key listener to gear so that DPAD_UP/DOWN moves between rows and DPAD_LEFT
                // returns focus to the row. DPAD_CENTER and DPAD_RIGHT fall through to default.
                settingsButton.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        int next = position + 1;
                        if (next < recentApps.size()) {
                            listView.setSelection(next);
                            listView.requestFocus();
                        }
                        return true;
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        int prev = position - 1;
                        if (prev >= 0) {
                            listView.setSelection(prev);
                            listView.requestFocus();
                        }
                        return true;
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        // return focus to the list row
                        listView.requestFocus();
                        return true;
                    }
                    return false;
                });
            }
            return view;
        }
    }
}