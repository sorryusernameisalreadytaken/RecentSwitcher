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
    private static final String KEY_EXCLUDED_APPS = "excluded_apps";

    private PrefsHelper() {
        // no instances
    }

    /**
     * Stores the name of the most recently launched package. This value
     * persists between sessions and is used by LastAppActivity to
     * implement Alt-Tab-like behaviour.
     */
    public static void setLastPackage(Context context, String pkg) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LAST_PACKAGE, pkg).apply();
    }

    /**
     * Returns the most recently launched package or null if none has
     * been recorded.
     */
    public static String getLastPackage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LAST_PACKAGE, null);
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
}