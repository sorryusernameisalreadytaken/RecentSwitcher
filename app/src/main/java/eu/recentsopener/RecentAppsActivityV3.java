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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RecentAppsActivityV3 displays a list of recently used packages by
 * querying usage statistics on a daily interval. It merges the
 * statistics per package and sorts them by the last time used in
 * descending order. This variant does not fall back to event-based
 * history and is intended to test whether aggregating daily stats
 * produces a more complete recents list on certain TV devices.
 */
public class RecentAppsActivityV3 extends AppCompatActivity {
    private final List<AppEntry> recentApps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_apps);
        Button btnCloseAll = findViewById(R.id.btn_close_all);
        Button btnCloseOthers = findViewById(R.id.btn_close_others);
        ListView listView = findViewById(R.id.listView);

        // Close buttons remain stubs
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

    /**
     * Loads the recents list using daily UsageStats. The method merges
     * multiple stats entries per package by taking the most recent
     * lastTimeUsed and sums of foreground time are not used here. The
     * resulting map is sorted by lastTimeUsed descending. Packages
     * belonging to this app or marked as excluded are omitted.
     */
    private void loadRecents() {
        recentApps.clear();
        long end = System.currentTimeMillis();
        long begin = end - 1000L * 60 * 60 * 24;
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end);
        Map<String, UsageStats> merged = new HashMap<>();
        if (stats != null) {
            for (UsageStats s : stats) {
                String pkg = s.getPackageName();
                if (pkg == null) continue;
                if (pkg.equals(getPackageName())) continue;
                if (PrefsHelper.isExcluded(this, pkg)) continue;
                long last = s.getLastTimeUsed();
                if (last <= 0) continue;
                UsageStats existing = merged.get(pkg);
                if (existing == null || last > existing.getLastTimeUsed()) {
                    merged.put(pkg, s);
                }
            }
        }
        // Convert to list and sort by lastTimeUsed descending
        List<Map.Entry<String, UsageStats>> entries = new ArrayList<>();
        for (Map.Entry<String, UsageStats> e : merged.entrySet()) {
            entries.add(e);
        }
        Collections.sort(entries, (a, b) -> {
            long t1 = a.getValue().getLastTimeUsed();
            long t2 = b.getValue().getLastTimeUsed();
            return Long.compare(t2, t1);
        });
        List<String> packagesInOrder = new ArrayList<>();
        for (Map.Entry<String, UsageStats> e : entries) {
            packagesInOrder.add(e.getKey());
        }
        // Build AppEntry objects
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