package eu.recentsopener;

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
import java.util.List;

/**
 * RecentAppsActivityV6 displays a recents list based on the sequence of
 * packages observed by the accessibility service. Whenever a window
 * state change occurs (event type TYPE_WINDOW_STATE_CHANGED), the
 * service records the package name in a history. This activity
 * reverses that history so that the most recent event appears at the
 * top. It does not query UsageStats at all. This requires the
 * accessibility service to be enabled.
 */
public class RecentAppsActivityV6 extends AppCompatActivity {
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
        // Load recents purely from the accessibility history
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

    private void loadRecents() {
        recentApps.clear();
        List<String> history = PrefsHelper.getAccessibilityHistory(this);
        // The history is oldest to newest; reverse to show most recent first
        Collections.reverse(history);
        PackageManager pm = getPackageManager();
        for (String pkg : history) {
            // Optionally skip our own package and excluded packages
            if (pkg.equals(getPackageName())) continue;
            if (PrefsHelper.isExcluded(this, pkg)) continue;
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