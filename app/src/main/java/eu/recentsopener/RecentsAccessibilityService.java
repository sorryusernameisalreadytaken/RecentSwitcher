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
    // Delay between each step of the force‑stop automation. Increased further to 2000ms to
    // improve reliability on newer Android TV versions (e.g. TV OS 14) where dialogs and
    // button states can take multiple seconds to become interactable. Adjust this value
    // if actions are still being missed on your device.
    private static final int FORCE_SEQUENCE_DELAY_MS = 2000;

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
            // Attempt to locate the force‑stop button by matching possible labels in English
            // and German. Additional fallbacks like "Stop", "Stoppen", "Anhalten" and "Schließen"
            // have been added to support new UI variants on Android TV 14 where the button text
            // may differ.
            clickButtonByText(new String[]{
                    "Force stop", "Stoppen erzwingen", "Stopp erzwingen", "Beenden erzwingen",
                    "Force Stop", "Stop", "Stoppen", "Anhalten", "Schließen", "Beenden"
            });
            // Step 2: after another delay click the OK button on the confirmation dialog
            handler.postDelayed(() -> {
                // Click the confirmation button. Possible labels include OK/Ok as well as
                // German equivalents like "Ja", "Bestätigen" or alternate capitalization.
                clickButtonByText(new String[]{
                        "OK", "Ok", "OK ", "OKAY", "Ok ", "O. K.", "Beenden",
                        "Ja", "JA", "Bestätigen", "Confirm", "OKAY "
                });
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
            String[] idCandidates = new String[]{
                    "com.android.settings:id/force_stop_button",
                    "com.android.settings:id/left_button",
                    "com.android.tv.settings:id/force_stop_button",
                    "com.google.android.tv.settings:id/force_stop_button"
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