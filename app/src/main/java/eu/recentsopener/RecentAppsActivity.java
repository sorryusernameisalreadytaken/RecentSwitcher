package eu.recentsopener;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_apps);

        Button btnCloseAll = findViewById(R.id.btn_close_all);
        Button btnCloseOthers = findViewById(R.id.btn_close_others);
        ListView listView = findViewById(R.id.listView);

        // The close buttons are stubs: inform users that closing other apps is not allowed
        btnCloseAll.setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.close_all_stub), Toast.LENGTH_SHORT).show());
        btnCloseOthers.setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.close_others_stub), Toast.LENGTH_SHORT).show());

        // If usage access is not granted, prompt the user and bail. We do not
        // combine this activity with the accessibility service; the usage
        // permission is required to populate the recents list.
        if (!hasUsageAccess()) {
            requestUsageAccess();
            return;
        }

        // Load the recent apps and attach a custom adapter that shows the
        // icon, label and package name. Long-press toggles exclusion.
        loadRecents();
        RecentAppsAdapter adapter = new RecentAppsAdapter(this, recentApps);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            AppEntry entry = recentApps.get(position);
            // Launch the selected app if it is not excluded
            if (!PrefsHelper.isExcluded(this, entry.packageName)) {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(entry.packageName);
                if (launchIntent != null) {
                    // Update the last/previous history before launching
                    PrefsHelper.updateHistory(this, entry.packageName);
                    startActivity(launchIntent);
                    finish();
                } else {
                    // Special-case system settings packages
                    if (entry.packageName.contains("settings")) {
                        Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
                        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            // Update history for settings as well
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
            boolean excluded = PrefsHelper.isExcluded(this, entry.packageName);
            if (excluded) {
                PrefsHelper.removeExcludedApp(this, entry.packageName);
                Toast.makeText(this, getString(R.string.app_included, entry.label), Toast.LENGTH_SHORT).show();
            } else {
                PrefsHelper.addExcludedApp(this, entry.packageName);
                Toast.makeText(this, getString(R.string.app_excluded, entry.label), Toast.LENGTH_SHORT).show();
            }
            // Refresh list to update highlighting but keep order
            loadRecents();
            RecentAppsAdapter newAdapter = new RecentAppsAdapter(this, recentApps);
            listView.setAdapter(newAdapter);
            return true;
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
            int type = event.getEventType();
            // Consider both MOVE_TO_FOREGROUND and ACTIVITY_RESUMED events as indicators
            // that an app was recently brought to the foreground. Some TV apps only
            // emit ACTIVITY_RESUMED and never MOVE_TO_FOREGROUND, so include both.
            // Use the literal value 16 for ACTIVITY_RESUMED in case the constant
            // does not exist on this API level.
            if (type == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    type == UsageEvents.Event.ACTIVITY_RESUMED ||
                    type == 16 ||
                    type == UsageEvents.Event.NOTIFICATION_INTERRUPTION ||
                    type == 10) {
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
            // Skip excluded packages entirely from the recents list
            if (PrefsHelper.isExcluded(this, pkg)) {
                continue;
            }
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
                String label = pm.getApplicationLabel(appInfo).toString();
                Drawable icon = pm.getApplicationIcon(appInfo);
                recentApps.add(new AppEntry(pkg, label, icon));
            } catch (PackageManager.NameNotFoundException e) {
                // skip unknown packages
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
                // Highlight excluded packages in red
                if (PrefsHelper.isExcluded(getContext(), entry.packageName)) {
                    textView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                } else {
                    // Use a darker text colour for better contrast on grey backgrounds
                    textView.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
                }
            }
            return view;
        }
    }
}