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
    /**
     * Reference to the ListView displaying excluded apps. Stored so that adapter and
     * gear key handlers can restore focus when navigating with the DPAD.
     */
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excluded_apps);

        // Obtain and store a reference to the ListView. We enable child focusability so that
        // the settings gear can receive focus via DPAD‑right without disrupting the default
        // up/down navigation between rows. The list row layout has been made focusable (see
        // item_recent_app.xml), so we want the list itself to delegate focus to its rows before
        // prioritising nested children. Using FOCUS_AFTER_DESCENDANTS here avoids the gear
        // automatically taking focus when the list is navigated.
        listView = findViewById(R.id.excluded_list);
        listView.setItemsCanFocus(true);
        listView.setDescendantFocusability(android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS);

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
        ExcludedAdapter adapter = new ExcludedAdapter(this, entries, listView);
        listView.setAdapter(adapter);

        // When the user taps an excluded entry we simply inform them that a long press
        // is required to re‑include the app. This prevents accidental removal when
        // navigating with the DPAD. A long press (DPAD‑center hold) will trigger the
        // removal below.
        listView.setOnItemClickListener((parent, view, position, id) -> {
            ExcludedEntry entry = entries.get(position);
            Toast.makeText(this, getString(R.string.long_press_to_include, entry.label), Toast.LENGTH_SHORT).show();
        });
        // Long press on an excluded app will remove it from the exclusion list and notify the adapter.
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            ExcludedEntry entry = entries.get(position);
            PrefsHelper.removeExcludedApp(this, entry.packageName);
            Toast.makeText(this, getString(R.string.app_included, entry.label), Toast.LENGTH_SHORT).show();
            entries.remove(position);
            adapter.notifyDataSetChanged();
            // Close the activity if no excluded apps remain
            if (entries.isEmpty()) {
                finish();
            }
            return true;
        });

        // Intercept DPAD navigation on the excluded list. Pressing DPAD‑right moves focus to the
        // gear button within the current row. DPAD‑left is consumed to prevent accidental back
        // navigation; users must long‑press the centre button to re‑include an app instead.
        listView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != android.view.KeyEvent.ACTION_DOWN) {
                return false;
            }
            int pos = listView.getSelectedItemPosition();
            if (pos == android.widget.AdapterView.INVALID_POSITION) {
                return false;
            }
            // Move focus to the gear on DPAD‑right
            if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                android.view.View row = listView.getChildAt(pos - listView.getFirstVisiblePosition());
                if (row != null) {
                    android.widget.ImageButton gear = row.findViewById(R.id.settings_button);
                    if (gear != null) {
                        gear.requestFocus();
                        return true;
                    }
                }
                return false;
            }
            // Consume DPAD‑left to avoid unintended navigation
            if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
                return true;
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
        /** Reference to the ListView so we can restore focus from the gear button back to the list. */
        private final ListView parentListView;

        ExcludedAdapter(android.content.Context ctx, List<ExcludedEntry> apps, ListView listView) {
            super(ctx, 0, apps);
            inflater = android.view.LayoutInflater.from(ctx);
            this.parentListView = listView;
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
            // Retrieve the gear button and configure it for excluded entries. The gear remains visible
            // so that users can access the system app settings. Clicking the gear launches the
            // Settings details screen. Navigating away via DPAD‑up/down/left will return focus to
            // the list.
            android.widget.ImageButton settingsButton = view.findViewById(R.id.settings_button);
            if (entry != null) {
                iconView.setImageDrawable(entry.icon);
                // Display label and package name
                String text = entry.label + " (" + entry.packageName + ")";
                textView.setText(text);
                // Use a theme-aware warning colour to indicate exclusion
                int colour = ContextCompat.getColor(getContext(), R.color.recent_app_text_color_excluded);
                textView.setTextColor(colour);
                if (settingsButton != null) {
                    // Show the gear button
                    settingsButton.setVisibility(android.view.View.VISIBLE);
                    // Open the app details settings when clicked
                    settingsButton.setOnClickListener(v -> {
                        android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(android.net.Uri.parse("package:" + entry.packageName));
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            getContext().startActivity(intent);
                        } catch (Exception e) {
                            android.widget.Toast.makeText(getContext(), entry.packageName + " cannot be opened in settings", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                    // Handle navigation away from the gear: pressing left/up/down returns focus to the list
                    final int pos = position;
                    settingsButton.setOnKeyListener((v, kCode, kEvent) -> {
                        if (kEvent.getAction() != android.view.KeyEvent.ACTION_DOWN) {
                            return false;
                        }
                        // On the gear, handle DPAD navigation. Up/Down jump directly to the
                        // neighbouring rows. Left returns focus to the current row without doing
                        // anything further. Other keys are not handled here.
                        if (kCode == android.view.KeyEvent.KEYCODE_DPAD_UP) {
                            int target = pos > 0 ? pos - 1 : 0;
                            parentListView.requestFocus();
                            parentListView.setSelection(target);
                            return true;
                        }
                        if (kCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                            int max = parentListView.getAdapter() != null ? parentListView.getAdapter().getCount() - 1 : pos;
                            int target = pos < max ? pos + 1 : max;
                            parentListView.requestFocus();
                            parentListView.setSelection(target);
                            return true;
                        }
                        if (kCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
                            parentListView.requestFocus();
                            parentListView.setSelection(pos);
                            return true;
                        }
                        return false;
                    });
                }
            }
            return view;
        }
    }
}