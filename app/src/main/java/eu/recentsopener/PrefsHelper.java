package eu.recentsopener;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * PrefsHelper encapsulates access to the shared preferences used by
 * RecentAppSwitcher. It stores the last launched package and the set
 * of excluded packages. Excluded packages will not appear in the
 * recents list and will be ignored when switching to the last app.
 */
public final class PrefsHelper {
    private static final String PREF_NAME = "recent_app_switcher_prefs";
    private static final String KEY_LAST_PACKAGE = "last_package";
    /**
     * Key used to store the package name that was previously launched
     * before the current last package. This enables true Alt‑Tab behaviour
     * by remembering not just the most recently launched app but also
     * the one used immediately prior. When switching back, the previous
     * package becomes the new last package and vice versa.
     */
    private static final String KEY_PREVIOUS_PACKAGE = "previous_package";
    private static final String KEY_EXCLUDED_APPS = "excluded_apps";

    /**
     * Key used to persist a map of package names to their last known
     * foreground timestamps. This map enables variant E (history
     * persistence) to reconstruct a recents list even after a device
     * reboot by merging system UsageStats data with locally stored
     * times. The value is stored as a JSON object where keys are
     * package names and values are epoch milliseconds.
     */
    private static final String KEY_LAST_USED_MAP = "last_used_map";

    /**
     * Key used to persist a list of package names captured via
     * accessibility events. This list enables variant F to display
     * a history of foreground packages observed by the accessibility
     * service. The value is stored as a JSON array of package names
     * ordered from oldest to newest.
     */
    private static final String KEY_ACCESSIBILITY_HISTORY = "accessibility_history";

    private PrefsHelper() {
        // no instances
    }

    /**
     * Updates the history of launched packages. When a new package is
     * launched, the current last package (if any) is shifted into the
     * previous slot and the new package becomes the last package. If
     * the new package is the same as the current last package then no
     * change is made. This method should be called whenever the user
     * explicitly launches an app via the recent apps list or via the
     * last‑app shortcut.
     *
     * @param context Context used to access SharedPreferences.
     * @param newPackage The package name of the newly launched app.
     */
    public static void updateHistory(Context context, String newPackage) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String currentLast = prefs.getString(KEY_LAST_PACKAGE, null);
        if (currentLast != null && !currentLast.equals(newPackage)) {
            // Shift the current last package into the previous slot
            prefs.edit()
                    .putString(KEY_PREVIOUS_PACKAGE, currentLast)
                    .putString(KEY_LAST_PACKAGE, newPackage)
                    .apply();
        } else {
            // Just record the new package as last
            prefs.edit().putString(KEY_LAST_PACKAGE, newPackage).apply();
        }
    }

    /**
     * Retrieves the most recently launched package. This is the app that
     * the user launched last via the recent apps list or via our
     * last‑app shortcut. May return null if no launch has been recorded.
     */
    public static String getLastPackage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LAST_PACKAGE, null);
    }

    /**
     * Retrieves the package name that was launched immediately prior to
     * the current last package. This value can be null if the user has
     * only launched one app via the switcher. Excluded apps are not
     * filtered here; callers should check the exclusion list.
     */
    public static String getPreviousPackage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PREVIOUS_PACKAGE, null);
    }

    /**
     * Returns the current set of excluded packages. This set may be
     * empty but will never be null.
     */
    public static Set<String> getExcludedApps(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> excluded = prefs.getStringSet(KEY_EXCLUDED_APPS, null);
        // Create a defensive copy so callers cannot modify the stored set
        return excluded != null ? new HashSet<>(excluded) : new HashSet<>();
    }

    /**
     * Adds a package name to the exclusion set.
     */
    public static void addExcludedApp(Context context, String pkg) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> excluded = prefs.getStringSet(KEY_EXCLUDED_APPS, null);
        if (excluded == null) {
            excluded = new HashSet<>();
        } else {
            excluded = new HashSet<>(excluded);
        }
        excluded.add(pkg);
        prefs.edit().putStringSet(KEY_EXCLUDED_APPS, excluded).apply();
    }

    /**
     * Removes a package name from the exclusion set.
     */
    public static void removeExcludedApp(Context context, String pkg) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> excluded = prefs.getStringSet(KEY_EXCLUDED_APPS, null);
        if (excluded == null || !excluded.contains(pkg)) {
            return;
        }
        excluded = new HashSet<>(excluded);
        excluded.remove(pkg);
        prefs.edit().putStringSet(KEY_EXCLUDED_APPS, excluded).apply();
    }

    /**
     * Returns true if the given package is currently excluded from the
     * recents list and Alt-Tab behaviour.
     */
    public static boolean isExcluded(Context context, String pkg) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> excluded = prefs.getStringSet(KEY_EXCLUDED_APPS, null);
        return excluded != null && excluded.contains(pkg);
    }

    // ----------------------------------------------------------------------
    // Last-used time map (for variant E)
    // ----------------------------------------------------------------------

    /**
     * Retrieves a mapping of package names to their last foreground
     * timestamps. The map may be empty but will never be null. The
     * timestamps are epoch milliseconds. The data is stored as a
     * JSON object in the preferences. Malformed JSON will result in
     * an empty map being returned.
     */
    public static java.util.Map<String, Long> getLastUsedMap(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(KEY_LAST_USED_MAP, null);
        java.util.Map<String, Long> map = new java.util.HashMap<>();
        if (jsonString == null) {
            return map;
        }
        try {
            org.json.JSONObject jsonObject = new org.json.JSONObject(jsonString);
            java.util.Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                long value = jsonObject.getLong(key);
                map.put(key, value);
            }
        } catch (Exception ignore) {
            // ignore malformed JSON
        }
        return map;
    }

    /**
     * Persists the provided map of last used times to the shared
     * preferences. The map is converted to a JSON object string.
     */
    private static void saveLastUsedMap(Context context, java.util.Map<String, Long> map) {
        org.json.JSONObject jsonObject = new org.json.JSONObject(map);
        String jsonString = jsonObject.toString();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LAST_USED_MAP, jsonString).apply();
    }

    /**
     * Updates the stored last used map with a single package/time pair.
     * If the package is already present and the provided time is more
     * recent than the stored one, the value is replaced. Otherwise
     * the package/time is added.
     */
    public static void updateLastUsedTime(Context context, String pkg, long time) {
        java.util.Map<String, Long> map = getLastUsedMap(context);
        Long existing = map.get(pkg);
        if (existing == null || time > existing) {
            map.put(pkg, time);
            saveLastUsedMap(context, map);
        }
    }

    // ----------------------------------------------------------------------
    // Accessibility history (for variant F)
    // ----------------------------------------------------------------------

    /**
     * Retrieves the list of packages recorded by the accessibility
     * service. The list is ordered from oldest to newest. The list
     * may be empty but will never be null. Malformed JSON results in
     * an empty list.
     */
    public static java.util.List<String> getAccessibilityHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(KEY_ACCESSIBILITY_HISTORY, null);
        java.util.List<String> list = new java.util.ArrayList<>();
        if (jsonString == null) {
            return list;
        }
        try {
            org.json.JSONArray array = new org.json.JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                String pkg = array.optString(i, null);
                if (pkg != null) {
                    list.add(pkg);
                }
            }
        } catch (Exception ignore) {
            // ignore malformed JSON
        }
        return list;
    }

    /**
     * Appends a package name to the accessibility history. If the
     * package already exists in the history, it is removed before
     * being appended so that newer entries appear at the end. The
     * history is truncated to the most recent 50 entries to prevent
     * unbounded growth.
     */
    public static void addAccessibilityEvent(Context context, String pkg) {
        if (pkg == null) return;
        java.util.List<String> list = getAccessibilityHistory(context);
        // Remove existing occurrences
        list.remove(pkg);
        list.add(pkg);
        // Limit to 50 entries
        if (list.size() > 50) {
            list = list.subList(list.size() - 50, list.size());
        }
        org.json.JSONArray array = new org.json.JSONArray();
        for (String p : list) {
            array.put(p);
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ACCESSIBILITY_HISTORY, array.toString()).apply();
    }
}