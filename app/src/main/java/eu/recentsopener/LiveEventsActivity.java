package eu.recentsopener;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * LiveEventsActivity displays a continuously updating list of recent usage
 * information. It combines aggregated usage statistics and the latest
 * UsageEvents for the last few minutes to show which apps have been
 * recently active. Each entry includes the package label, package name,
 * timestamp of last activity, event type and data source (stats or events).
 * Excluded apps are highlighted in red but cannot be removed here.
 */
public class LiveEventsActivity extends AppCompatActivity {
    /** Duration of history to display in milliseconds. Increased from 5 minutes to 60 minutes so
     *  that more events are visible to the user (one hour). */
    private static final long HISTORY_DURATION_MS = 1000L * 60 * 60;

    /** Data model representing one live entry. */
    private static class LiveEntry {
        final String packageName;
        final String label;
        final Drawable icon;
        final long lastTime;
        final int eventType;
        final String source;
        LiveEntry(String pkg, String label, Drawable icon, long lastTime, int eventType, String source) {
            this.packageName = pkg;
            this.label = label;
            this.icon = icon;
            this.lastTime = lastTime;
            this.eventType = eventType;
            this.source = source;
        }
    }

    private final List<LiveEntry> entries = new ArrayList<>();
    private ArrayAdapter<LiveEntry> adapter;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_events);

        ListView listView = findViewById(R.id.listViewLive);
        adapter = new ArrayAdapter<LiveEntry>(this, 0, entries) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    view = LayoutInflater.from(getContext()).inflate(R.layout.item_recent_app, parent, false);
                }
                LiveEntry entry = getItem(position);
                ImageView iconView = view.findViewById(R.id.app_icon);
                TextView textView = view.findViewById(R.id.app_text);
                if (entry != null) {
                    iconView.setImageDrawable(entry.icon);
                    // Build display string: Label (package) - event/time/source
                    String timeStr = DateFormat.format("HH:mm:ss", entry.lastTime).toString();
                    String eventName = getEventTypeName(entry.eventType);
                    String text = entry.label + " (" + entry.packageName + ")\n" + eventName + " @ " + timeStr + " [" + entry.source + "]";
                    textView.setText(text);
                    // Highlight excluded packages or apply theme-aware colour
                    if (PrefsHelper.isExcluded(getContext(), entry.packageName)) {
                        int colour = ContextCompat.getColor(getContext(), R.color.recent_app_text_color_excluded);
                        textView.setTextColor(colour);
                    } else {
                        int colour = ContextCompat.getColor(getContext(), R.color.recent_app_text_color);
                        textView.setTextColor(colour);
                    }
                    // Hide the settings gear in the live events list; there are no actions
                    android.widget.ImageButton settingsButton = view.findViewById(R.id.settings_button);
                    if (settingsButton != null) {
                        settingsButton.setVisibility(android.view.View.GONE);
                    }
                }
                return view;
            }
        };
        listView.setAdapter(adapter);

        // Allow long‑press on an item to exclude that app from the recents list. This is
        // analogous to the behaviour in the recent apps list: the user can hold the OK
        // button (or perform a long touch) to add the app to the exclusion list. Excluded
        // apps remain visible here but will be highlighted and omitted from other lists.
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            LiveEntry entry = entries.get(position);
            if (entry != null) {
                // Add to exclusion list only if not already excluded
                boolean alreadyExcluded = PrefsHelper.isExcluded(LiveEventsActivity.this, entry.packageName);
                if (!alreadyExcluded) {
                    PrefsHelper.addExcludedApp(LiveEventsActivity.this, entry.packageName);
                    android.widget.Toast.makeText(LiveEventsActivity.this, getString(R.string.app_excluded, entry.label), android.widget.Toast.LENGTH_SHORT).show();
                    // refresh UI to highlight the entry
                    adapter.notifyDataSetChanged();
                } else {
                    // If already excluded we do nothing special here; a long press on the recents screen
                    // is used for re‑including.
                    android.widget.Toast.makeText(LiveEventsActivity.this, getString(R.string.app_excluded, entry.label), android.widget.Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        });

        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start periodic updates
        handler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop updates when not visible
        handler.removeCallbacks(updateRunnable);
    }

    /** Runnable that updates the live list and re-schedules itself. */
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updateLiveList();
            // schedule again in 2 seconds
            handler.postDelayed(this, 2000);
        }
    };

    /**
     * Updates the list of live entries by querying recent usage events
     * and aggregated statistics. Combines both sources and sorts by
     * last activity time descending.
     */
    private void updateLiveList() {
        long end = System.currentTimeMillis();
        long begin = end - HISTORY_DURATION_MS;
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) {
            return;
        }
        // Gather aggregated stats
        Map<String, UsageStats> statsMap = usm.queryAndAggregateUsageStats(begin, end);
        // Gather latest event per package
        Map<String, EventRecord> eventMap = new HashMap<>();
        UsageEvents events = usm.queryEvents(begin, end);
        UsageEvents.Event event = new UsageEvents.Event();
        while (events != null && events.hasNextEvent()) {
            events.getNextEvent(event);
            String pkg = event.getPackageName();
            // Skip our own package
            if (pkg == null || pkg.equals(getPackageName())) {
                continue;
            }
            // Keep the most recent event for each package
            EventRecord rec = eventMap.get(pkg);
            if (rec == null || event.getTimeStamp() > rec.time) {
                eventMap.put(pkg, new EventRecord(event.getEventType(), event.getTimeStamp()));
            }
        }
        // Build unified list
        List<LiveEntry> newEntries = new ArrayList<>();
        PackageManager pm = getPackageManager();
        for (String pkg : statsMap.keySet()) {
            if (pkg.equals(getPackageName())) continue;
            UsageStats s = statsMap.get(pkg);
            long lastTime = s.getLastTimeUsed();
            int evType = -1;
            String source = "Stats";
            EventRecord rec = eventMap.get(pkg);
            if (rec != null) {
                evType = rec.type;
                // Use event time if more recent than stats
                if (rec.time > lastTime) {
                    lastTime = rec.time;
                    source = "Event";
                }
            }
            // Add entry if there's any activity in window
            if (lastTime >= begin) {
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                    String label = pm.getApplicationLabel(ai).toString();
                    Drawable icon = pm.getApplicationIcon(ai);
                    newEntries.add(new LiveEntry(pkg, label, icon, lastTime, evType, source));
                } catch (PackageManager.NameNotFoundException ignore) {
                    // ignore unknown packages
                }
            }
        }
        // Also include packages that appear only in events
        for (Map.Entry<String, EventRecord> entry : eventMap.entrySet()) {
            String pkg = entry.getKey();
            if (pkg.equals(getPackageName())) continue;
            // Skip if already added from statsMap
            boolean exists = false;
            for (LiveEntry e : newEntries) {
                if (e.packageName.equals(pkg)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                EventRecord rec = entry.getValue();
                if (rec.time >= begin) {
                    try {
                        ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                        String label = pm.getApplicationLabel(ai).toString();
                        Drawable icon = pm.getApplicationIcon(ai);
                        newEntries.add(new LiveEntry(pkg, label, icon, rec.time, rec.type, "Event"));
                    } catch (PackageManager.NameNotFoundException ignore) {
                        // ignore
                    }
                }
            }
        }
        // Sort by lastTime descending
        Collections.sort(newEntries, (a, b) -> Long.compare(b.lastTime, a.lastTime));
        // Update UI on main thread
        entries.clear();
        entries.addAll(newEntries);
        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    /** Map record for storing event type and time. */
    private static class EventRecord {
        final int type;
        final long time;
        EventRecord(int type, long time) {
            this.type = type;
            this.time = time;
        }
    }

    /**
     * Returns a human‑readable name for a usage event type. The Android
     * platform defines multiple constants that may map to the same numeric
     * value (for example MOVE_TO_FOREGROUND and ACTIVITY_RESUMED can be
     * identical). Using conditional checks avoids duplicate case labels.
     * If the NOTIFICATION constant is unavailable on this API level we
     * fall back to checking the numeric code 10.
     */
    private static String getEventTypeName(int eventType) {
        if (eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
            return "MOVE_TO_FOREGROUND";
        }
        if (eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
            return "MOVE_TO_BACKGROUND";
        }
        if (eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
            return "ACTIVITY_RESUMED";
        }
        if (eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
            return "ACTIVITY_PAUSED";
        }
        if (eventType == UsageEvents.Event.ACTIVITY_STOPPED) {
            return "ACTIVITY_STOPPED";
        }
        if (eventType == UsageEvents.Event.USER_INTERACTION) {
            return "USER_INTERACTION";
        }
        // Notification interruption: constant may not exist on all API levels
        try {
            int notifCode = android.app.usage.UsageEvents.Event.class.getField("NOTIFICATION_INTERRUPTION").getInt(null);
            if (eventType == notifCode) {
                return "NOTIFICATION";
            }
        } catch (Exception ignored) {
            // If field doesn't exist, treat code 10 as notification
            if (eventType == 10) {
                return "NOTIFICATION";
            }
        }
        return "TYPE_" + eventType;
    }
}