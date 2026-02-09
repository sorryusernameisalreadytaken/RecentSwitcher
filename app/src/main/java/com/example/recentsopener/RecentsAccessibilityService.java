package com.example.recentsopener;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/**
 * RecentsAccessibilityService is an AccessibilityService that holds a static
 * reference to itself once bound by the system. This allows other
 * components (such as the MainActivity) to request the service to perform
 * global actions, for example opening the recents (overview) screen.
 */
public class RecentsAccessibilityService extends AccessibilityService {

    // Static reference to the most recently connected service instance. The system
    // binds the service when the user enables it via the accessibility settings.
    private static RecentsAccessibilityService sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sInstance == this) {
            sInstance = null;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // We do not need to respond to events for this simple service.
    }

    @Override
    public void onInterrupt() {
        // No-op
    }

    /**
     * Request the system to show the recents (overview) screen. If the service
     * is not yet connected or has been destroyed, this method will return
     * without performing any action.
     */
    public static void showRecents() {
        RecentsAccessibilityService service = sInstance;
        if (service != null) {
            // GLOBAL_ACTION_RECENTS will toggle the overview screen on most
            // Android devices when executed by an accessibility service.
            service.performGlobalAction(GLOBAL_ACTION_RECENTS);
        }
    }

    /**
     * Indicates whether the accessibility service is currently connected.
     * The service sets the static instance when it is created and clears it
     * when destroyed.
     *
     * @return true if the service instance is non-null, false otherwise.
     */
    public static boolean isServiceEnabled() {
        return sInstance != null;
    }
}