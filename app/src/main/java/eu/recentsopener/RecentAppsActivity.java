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
     * Adapter for the recents list. Stored as a field so that it can be
     * refreshed in onResume() once usage access is granted.
     */
    private RecentAppsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_apps);

        Button btnCloseAll = findViewById(R.id.btn_close_all);
        Button btnCloseOthers = findViewById(R.id.btn_close_others);
        // ListView is declared final so we can capture it inside the long‑click listener
        final ListView listView = findViewById(R.id.listView);

        // The close buttons are stubs: inform users that closing other apps is not allowed
        btnCloseAll.setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.close_all_stub), Toast.LENGTH_SHORT).show());
        btnCloseOthers.setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.close_others_stub), Toast.LENGTH_SHORT).show());

        // If usage access is not granted, prompt the user to enable it. We still
        // continue and set up the adapter so that the list can be populated
        // once permission is granted. Without permission the list will remain empty.
        boolean accessGranted = hasUsageAccess();
        if (!accessGranted) {
            requestUsageAccess();
        }

        // Populate the list if we already have access; otherwise it stays empty
        if (accessGranted) {
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
        listView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                int pos = listView.getSelectedItemPosition();
                if (pos != android.widget.AdapterView.INVALID_POSITION && pos < recentApps.size()) {
                    AppEntry appEntry = recentApps.get(pos);
                    // Build intent to show app details settings for this package
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + appEntry.packageName));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(intent);
                        // If the accessibility service is active, attempt to automate the force‑stop sequence
                        if (RecentsAccessibilityService.isServiceEnabled()) {
                            RecentsAccessibilityService svc = RecentsAccessibilityService.getInstance();
                            if (svc != null) {
                                svc.performForceStopSequence();
                            }
                        } else {
                            // Inform the user that the accessibility service must be enabled for automation
                            Toast.makeText(this, R.string.service_not_enabled, Toast.LENGTH_SHORT).show();
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
        // After returning from the usage access settings, the recents list may still be empty.
        // If usage access has been granted and no apps are loaded yet, refresh the list.
        if (recentApps.isEmpty() && hasUsageAccess()) {
            loadRecents();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
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
            }
            return view;
        }
    }
}