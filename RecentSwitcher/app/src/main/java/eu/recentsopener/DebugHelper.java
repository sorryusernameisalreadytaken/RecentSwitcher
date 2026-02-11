package eu.recentsopener;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * DebugHelper provides methods to generate a diagnostic report of usage
 * events and aggregated usage statistics. The report is written to a text
 * file in the app's external files directory so that the user can retrieve
 * it via a file manager. This helper is intended for troubleshooting cases
 * where certain apps do not appear in the recents list.
 */
public final class DebugHelper {
    private DebugHelper() {
        // no instances
    }

    /**
     * Collects usage events and statistics for the last {@code durationMs}
     * milliseconds and writes them to a file. The report includes the
     * timestamp, event type and package name for each usage event,
     * followed by a summary of aggregated usage stats (package name,
     * lastTimeUsed, totalTimeInForeground). The list of excluded apps
     * and the last/previous packages are also recorded. A toast message
     * is shown indicating where the file was saved.
     *
     * @param context application context used for accessing system services and file system
     * @param durationMs time range in milliseconds to include in the report
     */
    public static void collectDebugInfo(Context context, long durationMs) {
        long end = System.currentTimeMillis();
        long begin = end - durationMs;
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        // Determine output directory: prefer external files, fall back to internal
        File dir = context.getExternalFilesDir(null);
        if (dir == null) {
            dir = context.getFilesDir();
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File outFile = new File(dir, "debug_report_" + timestamp + ".txt");
        FileWriter writer = null;
        try {
            writer = new FileWriter(outFile);
            writer.write("Debug report generated at " + new Date().toString() + "\n\n");
            // Write excluded apps and history
            writer.write("Excluded apps: " + PrefsHelper.getExcludedApps(context) + "\n");
            writer.write("Last package: " + PrefsHelper.getLastPackage(context) + "\n");
            writer.write("Previous package: " + PrefsHelper.getPreviousPackage(context) + "\n\n");
            // Write usage events
            writer.write("UsageEvents (last " + durationMs / 1000 + "s):\n");
            UsageEvents events = usm.queryEvents(begin, end);
            UsageEvents.Event event = new UsageEvents.Event();
            while (events != null && events.hasNextEvent()) {
                events.getNextEvent(event);
                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                        .format(new Date(event.getTimeStamp()));
                writer.write(time + "," + event.getEventType() + "," + event.getPackageName() + "\n");
            }
            writer.write("\nUsageStats (last " + durationMs / 1000 + "s):\n");
            List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end);
            if (stats != null) {
                for (UsageStats s : stats) {
                    writer.write(s.getPackageName() + "," + s.getLastTimeUsed() + "," + s.getTotalTimeInForeground() + "\n");
                }
            }
            writer.flush();
            android.widget.Toast.makeText(context,
                    "Debug report saved: " + outFile.getAbsolutePath(),
                    android.widget.Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            android.widget.Toast.makeText(context,
                    "Failed to write debug report: " + e.getMessage(),
                    android.widget.Toast.LENGTH_LONG).show();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignore) {
                    // ignored
                }
            }
        }
    }

    /**
     * Collects usage events and statistics for a default time range (5 minutes).
     * This method calls {@link #collectDebugInfo(Context, long)} with a
     * predefined duration. It is kept for backwards compatibility.
     *
     * @param context application context used for accessing system services and file system
     */
    public static void collectDebugInfo(Context context) {
        // Default to last 5 minutes (300 seconds)
        collectDebugInfo(context, 1000L * 60 * 5);
    }
}