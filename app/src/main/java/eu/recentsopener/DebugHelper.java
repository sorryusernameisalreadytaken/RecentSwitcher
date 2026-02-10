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
 * DebugHelper provides helper methods to collect diagnostic information
 * about the app usage history. It gathers raw usage events and
 * aggregated usage statistics for the last 24 hours and writes them
 * to a text file in the application's external files directory.
 *
 * <p>The resulting report is useful for troubleshooting cases where
 * certain packages do not appear in the recent apps list. It also
 * records the current list of excluded packages and the last/previous
 * launch history as stored by {@link PrefsHelper}. A toast message
 * indicates where the report has been saved.</p>
 */
public final class DebugHelper {
    private DebugHelper() {
        // static helper
    }

    /**
     * Collects usage events and aggregated usage stats for the last 24
     * hours and writes them to a timestamped file. The file is created
     * in the application's external files directory (falling back to
     * internal storage if necessary). A toast will notify the user of
     * the location of the saved report.
     *
     * @param context the context used to access system services and
     *                file locations
     */
    public static void collectDebugInfo(Context context) {
        long end = System.currentTimeMillis();
        long begin = end - 1000L * 60 * 60 * 24;
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        // Determine the output directory. Prefer external files so the
        // user can retrieve the report via a file manager. If that is
        // unavailable, fall back to the internal files directory.
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
            // Write the list of excluded apps and history
            writer.write("Excluded apps: " + PrefsHelper.getExcludedApps(context) + "\n");
            writer.write("Last package: " + PrefsHelper.getLastPackage(context) + "\n");
            writer.write("Previous package: " + PrefsHelper.getPreviousPackage(context) + "\n\n");

            // Write raw usage events
            writer.write("UsageEvents (last 24h):\n");
            UsageEvents events = usm.queryEvents(begin, end);
            UsageEvents.Event event = new UsageEvents.Event();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            while (events != null && events.hasNextEvent()) {
                events.getNextEvent(event);
                String time = sdf.format(new Date(event.getTimeStamp()));
                writer.write(time + "," + event.getEventType() + "," + event.getPackageName() + "\n");
            }
            writer.write("\nUsageStats (last 24h):\n");
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
                    // ignore
                }
            }
        }
    }
}