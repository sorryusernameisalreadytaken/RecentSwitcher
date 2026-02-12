package eu.recentsopener;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ExcludedAppsActivity shows a list of all packages that the user has
 * excluded from the recents list. Tapping an entry will remove it
 * from the exclusion set, allowing it to appear in the recents list
 * and be considered for last-app switching. If there are no excluded
 * apps, the activity will immediately inform the user and close.
 */
public class ExcludedAppsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excluded_apps);

        ListView listView = findViewById(R.id.excluded_list);

        Set<String> excluded = PrefsHelper.getExcludedApps(this);
        if (excluded.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_excluded_apps), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Build a list of entries with icon and label for each excluded package
        PackageManager pm = getPackageManager();
        List<ExcludedEntry> entries = new ArrayList<>();
        for (String pkg : excluded) {
            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                String label = pm.getApplicationLabel(info).toString();
                Drawable icon = pm.getApplicationIcon(info);
                entries.add(new ExcludedEntry(pkg, label, icon));
            } catch (PackageManager.NameNotFoundException e) {
                // fallback: use package name as label and default icon
                entries.add(new ExcludedEntry(pkg, pkg, pm.getDefaultActivityIcon()));
            }
        }
        ExcludedAdapter adapter = new ExcludedAdapter(this, entries);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            ExcludedEntry entry = entries.get(position);
            PrefsHelper.removeExcludedApp(this, entry.packageName);
            Toast.makeText(this, getString(R.string.app_included, entry.label), Toast.LENGTH_SHORT).show();
            entries.remove(position);
            adapter.notifyDataSetChanged();
            // If list is empty after removal, close activity
            if (entries.isEmpty()) {
                finish();
            }
        });
    }

    private static class ExcludedEntry {
        final String packageName;
        final String label;
        final Drawable icon;

        ExcludedEntry(String packageName, String label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }

    private static class ExcludedAdapter extends ArrayAdapter<ExcludedEntry> {
        private final android.view.LayoutInflater inflater;

        ExcludedAdapter(android.content.Context ctx, List<ExcludedEntry> apps) {
            super(ctx, 0, apps);
            inflater = android.view.LayoutInflater.from(ctx);
        }

        @Override
        public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
            android.view.View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_recent_app, parent, false);
            }
            ExcludedEntry entry = getItem(position);
            android.widget.ImageView iconView = view.findViewById(R.id.app_icon);
            android.widget.TextView textView = view.findViewById(R.id.app_text);
            // Hide the gear/settings button in the excluded list to avoid confusion. Users should
            // manage exclusions via tap on the list item rather than launching app settings.
            android.widget.ImageButton settingsButton = view.findViewById(R.id.settings_button);
            if (settingsButton != null) {
                settingsButton.setVisibility(android.view.View.GONE);
            }
            if (entry != null) {
                iconView.setImageDrawable(entry.icon);
                // Display label and package name
                String text = entry.label + " (" + entry.packageName + ")";
                textView.setText(text);
                // Use a theme-aware warning colour to indicate exclusion
                int colour = ContextCompat.getColor(getContext(), R.color.recent_app_text_color_excluded);
                textView.setTextColor(colour);
            }
            return view;
        }
    }
}