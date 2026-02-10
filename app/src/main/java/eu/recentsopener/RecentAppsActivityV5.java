package eu.recentsopener;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RecentAppsActivityV5 stores the last used timestamp for each package in
 * shared preferences. On each refresh it updates this map with
 * aggregated UsageStats and then sorts the map by timestamp. If
 * aggregated stats are unavailable, the stored map alone is used.
 * This variant is intended to persist recents across reboots on
 * Android TV devices where UsageStats may be cleared.
 */
public class RecentAppsActivityV5 extends AppCompatActivity {
    private final List<AppEntry> recentApps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_apps);
        Button btnCloseAll = findViewById(R.id.btn_close_all);
        Button btnCloseOthers = findViewById(R.id.btn_close_others);
        ListView listView = findViewById(R.id.listView);
        btnCloseAll.setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.close_all_stub), Toast.LENGTH_SHORT).show());
        btnCloseOthers.setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.close_others_stub), Toast.LENGTH_SHORT).show());
        if (!hasUsageAccess()) {
            requestUsageAccess();
            return;
        }
        loadRecents();
        RecentAppsAdapter adapter = new RecentAppsAdapter(this, recentApps);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            AppEntry entry = recentApps.get(position);
            if (!PrefsHelper.isExcluded(this, entry.packageName)) {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(entry.packageName);
                if (launchIntent != null) {
                    PrefsHelper.updateHistory(this, entry.packageName);
                    startActivity(launchIntent);
                    finish();
                } else {
                    if (entry.packageName.contains("settings")) {
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
            loadRecents();
            RecentAppsAdapter newAdapter = new RecentAppsAdapter(this, recentApps);
            listView.setAdapter(newAdapter);
            return true;
        });
    }

    private boolean hasUsageAccess() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long now = System.currentTimeMillis();
        UsageEvents events = usm.queryEvents(now - 1000 * 60 * 60, now);
        return events != null && events.hasNextEvent();
    }

    private void requestUsageAccess() {
        Toast.makeText(this, getString(R.string.grant_usage_access), Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void loadRecents() {
        recentApps.clear();
        long end = System.currentTimeMillis();
        long begin = end - 1000L * 60 * 60 * 24;
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        Map<String, UsageStats> stats = usm.queryAndAggregateUsageStats(begin, end);
        // Update the stored last used map with the latest data
        if (stats != null) {
            for (Map.Entry<String, UsageStats> entry : stats.entrySet()) {
                String pkg = entry.getKey();
                if (pkg == null) continue;
                long last = entry.getValue().getLastTimeUsed();
                if (last > 0) {
                    PrefsHelper.updateLastUsedTime(this, pkg, last);
                }
            }
        }
        // Retrieve the merged map and sort by last time used descending
        Map<String, Long> map = PrefsHelper.getLastUsedMap(this);
        List<Map.Entry<String, Long>> list = new ArrayList<>(map.entrySet());
        Collections.sort(list, (a, b) -> Long.compare(b.getValue(), a.getValue()));
        List<String> packagesInOrder = new ArrayList<>();
        for (Map.Entry<String, Long> e : list) {
            String pkg = e.getKey();
            if (pkg == null) continue;
            // Skip our own package; this list is for user-facing apps only
            if (pkg.equals(getPackageName())) continue;
            // Skip excluded packages
            if (PrefsHelper.isExcluded(this, pkg)) continue;
            packagesInOrder.add(pkg);
            // Limit to 50 entries
            if (packagesInOrder.size() >= 50) break;
        }
        PackageManager pm = getPackageManager();
        for (String pkg : packagesInOrder) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
                String label = pm.getApplicationLabel(appInfo).toString();
                Drawable icon = pm.getApplicationIcon(appInfo);
                recentApps.add(new AppEntry(pkg, label, icon));
            } catch (PackageManager.NameNotFoundException ignore) {
            }
        }
    }

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
                String text = entry.label + " (" + entry.packageName + ")";
                textView.setText(text);
                if (PrefsHelper.isExcluded(getContext(), entry.packageName)) {
                    textView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                } else {
                    textView.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
                }
            }
            return view;
        }
    }
}