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

    /**
     * The delay (in milliseconds) between automated steps when performing the
     * force‑stop sequence. Adjust this value if the UI on your device needs
     * more or less time to update between focus changes and button presses.
     */
    // Delay between each step of the force‑stop automation. Increased from 500ms to 1000ms
    // to improve reliability on newer Android TV versions where dialogs and button states
    // take longer to update.
    private static final int FORCE_SEQUENCE_DELAY_MS = 1000;

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
            // Step 2: after another delay click the OK button on the confirmation dialog
            handler.postDelayed(() -> {
                clickButtonByText(new String[]{"OK", "Ok", "OK ", "OKAY", "Ok ", "O. K.", "Beenden"});
                // Step 3: after another delay go back to the previous screen
                handler.postDelayed(() -> {
                    svc.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                }, FORCE_SEQUENCE_DELAY_MS);
            }, FORCE_SEQUENCE_DELAY_MS);
        }, FORCE_SEQUENCE_DELAY_MS);
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
        try {
            // First try to find the force stop button by resource ID on common packages
            // Possible resource IDs for the buttons we might need to click. These include
            // both the Force stop button on the application details screen and the OK
            // button on the confirmation dialog. On many Android devices the
            // confirmation button uses the framework ID android:id/button1. We also
            // include IDs found on Google TV/Android TV variants.
            String[] idCandidates = new String[]{
                    "com.android.settings:id/force_stop_button",
                    "com.android.settings:id/left_button",
                    "com.android.tv.settings:id/force_stop_button",
                    "com.google.android.tv.settings:id/force_stop_button",
                    "android:id/button1",
                    "com.android.settings:id/button1",
                    "com.google.android.tv.settings:id/button1",
                    "com.android.systemui:id/button1"
            };
            for (String viewId : idCandidates) {
                try {
                    java.util.List<android.view.accessibility.AccessibilityNodeInfo> nodesById = root.findAccessibilityNodeInfosByViewId(viewId);
                    if (nodesById != null && !nodesById.isEmpty()) {
                        for (android.view.accessibility.AccessibilityNodeInfo node : nodesById) {
                            if (node != null && node.isEnabled()) {
                                // climb up to a clickable ancestor if necessary
                                android.view.accessibility.AccessibilityNodeInfo clickable = node;
                                while (clickable != null && !clickable.isClickable()) {
                                    clickable = clickable.getParent();
                                }
                                if (clickable != null) {
                                    clickable.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
                                    for (android.view.accessibility.AccessibilityNodeInfo n : nodesById) {
                                        n.recycle();
                                    }
                                    return;
                                }
                            }
                        }
                    }
                } catch (Exception ignore) {
                    // ignore invalid view IDs
                }
            }
            // If not found by ID, search by visible text across locales
            for (String text : candidates) {
                if (text == null || text.isEmpty()) continue;
                java.util.List<android.view.accessibility.AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
                if (nodes != null && !nodes.isEmpty()) {
                    for (android.view.accessibility.AccessibilityNodeInfo node : nodes) {
                        if (node != null && node.isEnabled()) {
                            android.view.accessibility.AccessibilityNodeInfo clickable = node;
                            while (clickable != null && !clickable.isClickable()) {
                                clickable = clickable.getParent();
                            }
                            if (clickable != null) {
                                clickable.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
                                for (android.view.accessibility.AccessibilityNodeInfo n : nodes) {
                                    n.recycle();
                                }
                                return;
                            }
                        }
                    }
                    // Recycle nodes if none clicked
                    for (android.view.accessibility.AccessibilityNodeInfo n : nodes) {
                        n.recycle();
                    }
                }
            }
        } finally {
            root.recycle();
        }
    }
}