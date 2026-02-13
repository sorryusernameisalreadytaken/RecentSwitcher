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
        // Configure the list to allow focus on list rows before child views (gear) and
        // allow children to take focus when DPAD‑RIGHT is pressed. This mirrors
        // variant 3 behaviour in the recents list where the row is focusable and
        // the gear is not focusable by default. We still allow the gear to be
        // clicked via DPAD‑RIGHT.
        listView.setItemsCanFocus(true);
        listView.setDescendantFocusability(android.view.ViewGroup.FOCUS_BEFORE_DESCENDANTS);

        ExcludedAdapter adapter = new ExcludedAdapter(this, entries);
        listView.setAdapter(adapter);

        // Tap on an excluded app removes it from the list (short press) and long press does
        // the same but with a hint. Short press is sufficient here.
        listView.setOnItemClickListener((parent, view, position, id) -> {
            ExcludedEntry entry = entries.get(position);
            PrefsHelper.removeExcludedApp(this, entry.packageName);
            Toast.makeText(this, getString(R.string.app_included, entry.label), Toast.LENGTH_SHORT).show();
            entries.remove(position);
            adapter.notifyDataSetChanged();
            if (entries.isEmpty()) {
                finish();
            }
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            ExcludedEntry entry = entries.get(position);
            PrefsHelper.removeExcludedApp(this, entry.packageName);
            Toast.makeText(this, getString(R.string.app_included, entry.label), Toast.LENGTH_SHORT).show();
            entries.remove(position);
            adapter.notifyDataSetChanged();
            if (entries.isEmpty()) {
                finish();
            }
            return true;
        });
        // Intercept DPAD‑LEFT and DPAD‑RIGHT to provide quick access to the app settings. Both
        // directions open the application details page for the selected entry. Unlike the
        // recents list, no auto force‑close is triggered here.
        listView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != android.view.KeyEvent.ACTION_DOWN) {
                return false;
            }
            int pos = listView.getSelectedItemPosition();
            if (pos == android.widget.AdapterView.INVALID_POSITION) {
                return false;
            }
            if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT ||
                    keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (pos < entries.size()) {
                    ExcludedEntry appEntry = entries.get(pos);
                    android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.parse("package:" + appEntry.packageName));
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, appEntry.packageName + " cannot be opened in settings", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            }
            return false;
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
                // Use the recent app item layout (variant 3). We hide extra action icons below.
                view = inflater.inflate(R.layout.item_recent_app_v3, parent, false);
            }
            ExcludedEntry entry = getItem(position);
            android.widget.ImageView iconView = view.findViewById(R.id.app_icon);
            android.widget.TextView textView = view.findViewById(R.id.app_text);
            // Show the gear/settings button so that users can open app settings. Do not hide it.
            android.widget.ImageButton settingsButton = view.findViewById(R.id.settings_button);
            if (settingsButton != null) {
                settingsButton.setVisibility(android.view.View.VISIBLE);
                // Make the gear not focusable by default; DPAD‑RIGHT will request focus explicitly.
                settingsButton.setFocusable(false);
                settingsButton.setFocusableInTouchMode(false);
                settingsButton.setOnClickListener(v -> {
                    // Open the app settings page for this package
                    android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.parse("package:" + entry.packageName));
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        getContext().startActivity(intent);
                    } catch (Exception e) {
                        android.widget.Toast.makeText(getContext(), entry.packageName + " cannot be opened in settings", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }
            // Hide the left and right action icons in the excluded list; only the gear remains.
            android.view.View leftArrow = view.findViewById(R.id.left_arrow);
            android.view.View leftClose = view.findViewById(R.id.left_close);
            android.view.View rightArrow = view.findViewById(R.id.right_arrow);
            if (leftArrow != null) leftArrow.setVisibility(android.view.View.GONE);
            if (leftClose != null) leftClose.setVisibility(android.view.View.GONE);
            if (rightArrow != null) rightArrow.setVisibility(android.view.View.GONE);
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