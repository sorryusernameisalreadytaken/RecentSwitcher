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
     * List of packages that are excluded by default. These are primarily
     * system or launcher apps on Android TV that should not appear in the
     * recent list or be considered for last‑app switching. They remain
     * removable by the user via the excluded apps manager.
     */
    private static final java.util.Set<String> DEFAULT_EXCLUDED;
    static {
        java.util.Set<String> defaults = new java.util.HashSet<>();
        defaults.add("com.spocky.projengmenu");
        defaults.add("com.google.android.apps.tv.launcherx");
        defaults.add("com.google.android.packageinstaller");
        defaults.add("com.google.android.apps.tv.dreamx");
        defaults.add("com.google.android.chromecast.chromecastservice");
        DEFAULT_EXCLUDED = java.util.Collections.unmodifiableSet(defaults);
    }

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
        if (excluded == null) {
            // Initialise with default excluded packages on first access
            excluded = new HashSet<>(DEFAULT_EXCLUDED);
            prefs.edit().putStringSet(KEY_EXCLUDED_APPS, excluded).apply();
            return new HashSet<>(excluded);
        }
        // Defensive copy; do not modify the persisted set directly
        return new HashSet<>(excluded);
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