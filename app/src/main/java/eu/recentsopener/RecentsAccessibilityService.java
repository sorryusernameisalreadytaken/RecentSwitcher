package eu.recentsopener;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/**
 * RecentsAccessibilityService is an AccessibilityService that holds a static
 * reference to itself once bound by the system. This allows other
 * components (such as the MainActivity) to request the service to perform
 * global actions, for example opening the recents (overview) screen.
 *
 * Note: This service is only required when using the "Open Recents" button.
 * The rest of the app works without the accessibility service enabled.
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

    /**
     * Returns the currently bound service instance or {@code null} if the
     * service is not connected. External callers can use this to request
     * additional actions such as automated navigation in other UIs.
     */
    public static RecentsAccessibilityService getInstance() {
        return sInstance;
    }

    /**
     * Performs a sequence of clicks to close the current application via
     * the system settings page. This method searches the view hierarchy for
     * a button labelled "Force stop" (English) or its German equivalent and
     * clicks it, then confirms the dialog by pressing the affirmative button
     * ("OK" or localized variant) and finally returns to the previous screen.
     * Each step is executed with a short delay to allow the UI to update.
     *
     * Note: This operation requires the accessibility service to have
     * permission to retrieve window content (canRetrieveWindowContent=true).
     */
    public void performForceStopSequence() {
        final RecentsAccessibilityService svc = this;
        if (svc == null) return;
        // Handler tied to the main looper of the service
        android.os.Handler handler = new android.os.Handler(getMainLooper());
        // Step 1: wait 500ms, then click the Force stop button if present
        handler.postDelayed(() -> {
            clickButtonByText(new String[]{"Force stop", "Stoppen erzwingen", "Stopp erzwingen", "Beenden erzwingen"});
            // Step 2: after another 500ms click the OK button on the confirmation dialog
            handler.postDelayed(() -> {
                clickButtonByText(new String[]{"OK", "Ok", "OK ", "OKAY", "Ok ", "O. K.", "Beenden"});
                // Step 3: after another 500ms go back to the previous screen
                handler.postDelayed(() -> {
                    svc.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                }, 500);
            }, 500);
        }, 500);
    }

    /**
     * Searches the active window for a button whose text matches one of
     * the provided options and performs a click on the first match. If no
     * matching node is found this method returns without action.
     *
     * @param candidates Array of possible button labels in different locales
     */
    private void clickButtonByText(String[] candidates) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return;
        }
        android.view.accessibility.AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }
        for (String text : candidates) {
            if (text == null || text.isEmpty()) continue;
            java.util.List<android.view.accessibility.AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
            if (nodes != null && !nodes.isEmpty()) {
                for (android.view.accessibility.AccessibilityNodeInfo node : nodes) {
                    // Only click if the node is clickable and enabled
                    if (node.isClickable() && node.isEnabled()) {
                        node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
                        // recycle nodes to avoid leaks
                        for (android.view.accessibility.AccessibilityNodeInfo n : nodes) {
                            n.recycle();
                        }
                        root.recycle();
                        return;
                    }
                }
                // Recycle nodes if none clicked
                for (android.view.accessibility.AccessibilityNodeInfo n : nodes) {
                    n.recycle();
                }
            }
        }
        root.recycle();
    }
}