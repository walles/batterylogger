package com.gmail.walles.johan.batterylogger;

import android.content.Context;
import android.util.Log;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static com.gmail.walles.johan.batterylogger.MainActivity.TAG;

public class History {
    private static final int FAKE_HISTORY_DAYS_OLD_START = 10;
    private static final int FAKE_HISTORY_DAYS_OLD_END = 0;

    private static final long MAX_HISTORY_FILE_SIZE = 400 * 1024;

    public static final long HOUR_MS = 3600 * 1000;
    public static final long FIVE_MINUTES_MS = 5 * 60 * 1000;

    @Nullable
    private List<HistoryEvent> eventsFromStorage;

    private final File storage;

    /**
     * Unit-testing only constructor.
     */
    History(File storage) {
        this.storage = storage;
    }

    /**
     * Create a history object that logs its events to a default location.
     */
    public History(Context context) {
        this.storage = new File(context.getFilesDir(), "events.log");
    }

    private void dropOldHistory() throws IOException {
        if (eventsFromStorage == null) {
            eventsFromStorage = readEventsFromStorage();
        }

        // Skip the first quarter of the list
        List<HistoryEvent> truncated =
                eventsFromStorage.subList(eventsFromStorage.size() / 4, eventsFromStorage.size() - 1);
        PrintWriter printWriter = null;
        try {
            File tmp = File.createTempFile("history-", ".txt", storage.getParentFile());
            printWriter = new PrintWriter(new FileWriter(tmp));
            for (HistoryEvent event : truncated) {
                printWriter.println(event.serializeToString());
            }
            printWriter.close();
            printWriter = null;

            if (!tmp.renameTo(storage)) {
                throw new IOException(
                        "Renaming " + tmp.getAbsolutePath()
                                + " to " + storage.getAbsolutePath()
                                + " failed while truncating history");
            }
            eventsFromStorage = truncated;
        } finally {
            if (printWriter != null) {
                printWriter.close();
            }
        }
    }

    public void addEvent(HistoryEvent event) throws IOException {
        if (eventsFromStorage != null) {
            // Add in memory
            eventsFromStorage.add(event);
        }

        PrintWriter printWriter = null;
        try {
            printWriter = new PrintWriter(new FileWriter(storage, true));
            printWriter.println(event.serializeToString());
        } finally {
            if (printWriter != null) {
                printWriter.close();
            }
        }

        if (storage.length() > MAX_HISTORY_FILE_SIZE) {
            dropOldHistory();
        }
    }

    /**
     * Add these series to a plot and you'll see how battery drain speed has changed over time.
     */
    public List<XYSeries> getBatteryDrain() throws IOException {
        List<XYSeries> returnMe = new LinkedList<XYSeries>();
        SimpleXYSeries xySeries = null;

        boolean systemDown = false;
        HistoryEvent lastLevelEvent = null;
        if (eventsFromStorage == null) {
            eventsFromStorage = readEventsFromStorage();
        }
        for (HistoryEvent event : eventsFromStorage) {
            switch (event.getType()) {
                case SYSTEM_SHUTDOWN:
                    systemDown = true;
                    lastLevelEvent = null;
                    xySeries = null;
                    continue;
                case SYSTEM_BOOT:
                    if (!systemDown) {
                        // Missing shutdown event; assume an unclean shutdown and start on a new series
                        lastLevelEvent = null;
                        xySeries = null;
                    }
                    systemDown = false;
                    continue;
                case BATTERY_LEVEL:
                    // Handled below
                    break;
                case INFO:
                    // Doesn't affect drain
                    continue;
                default:
                    Log.w(TAG, "Drain: Unsupported event type " + event.getType());
                    continue;
            }
            if (systemDown) {
                continue;
            }

            if (lastLevelEvent == null) {
                lastLevelEvent = event;
                continue;
            }

            double deltaHours =
                    (event.getTimestamp().getTime() - lastLevelEvent.getTimestamp().getTime()) / (double)HOUR_MS;
            double drain = (lastLevelEvent.getPercentage() - event.getPercentage()) / deltaHours;

            if (drain <= 0) {
                // This happens while charging, start on a new series
                lastLevelEvent = event;
                xySeries = null;
                continue;
            }

            Date drainTimestamp = new Date((event.getTimestamp().getTime() + lastLevelEvent.getTimestamp().getTime()) / 2);

            if (xySeries == null) {
                xySeries = new SimpleXYSeries("Battery drain");
                returnMe.add(xySeries);
            }
            xySeries.addLast(toDouble(drainTimestamp), drain);

            lastLevelEvent = event;
        }

        return returnMe;
    }

    static XYSeries medianLine(XYSeries series) {
        SimpleXYSeries returnMe = new SimpleXYSeries(series.getTitle() + " Median");

        if (series.size() == 0) {
            return returnMe;
        }
        if (series.size() == 1) {
            returnMe.addLast(series.getX(0), series.getY(0));
            return returnMe;
        }

        double left = Double.MAX_VALUE;
        double right = Double.MIN_VALUE;
        double values[] = new double[series.size()];
        for (int i = 0; i < series.size(); i++) {
            double x = series.getX(i).doubleValue();
            if (x < left) {
                left = x;
            }
            if (x > right) {
                right = x;
            }

            values[i] = series.getY(i).doubleValue();
        }
        Arrays.sort(values);

        double median;
        if (series.size() % 2 == 1) {
            median = values[series.size() / 2];
        } else {
            median = values[series.size() / 2] + values[series.size() / 2 - 1];
            median /= 2.0;
        }
        returnMe.addLast(left, median);
        returnMe.addLast(right, median);

        return returnMe;
    }

    /**
     * Calls {@link #getBatteryDrain()} and creates one median line for each series.
     */
    public List<XYSeries> getDrainMedians() throws IOException {
        List<XYSeries> returnMe = new LinkedList<XYSeries>();
        for (XYSeries drain : getBatteryDrain()) {
            returnMe.add(medianLine(drain));
        }
        return returnMe;
    }

    private ArrayList<HistoryEvent> readEventsFromStorage() throws IOException {
        ArrayList<HistoryEvent> returnMe = new ArrayList<HistoryEvent>();

        if (storage == null || !storage.exists()) {
            return returnMe;
        }

        BufferedReader reader = null;
        int lineNumber = 1;
        try {
            reader = new BufferedReader(new FileReader(storage));
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    returnMe.add(HistoryEvent.deserializeFromString(line));
                } catch (ParseException e) {
                    // Log this but keep going and hope we get the gist of it
                    Log.w(TAG, "Reading storage file failed at line " + lineNumber + ": " + storage.getAbsolutePath(), e);
                }
                lineNumber++;
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        Log.i(TAG, returnMe.size() + " events read from " + storage.getAbsolutePath());

        return returnMe;
    }

    /**
     * Add this to a plot and you'll hopefully see what events affect your battery usage.
     */
    public EventSeries getEvents() throws IOException {
        EventSeries returnMe = new EventSeries();
        if (eventsFromStorage == null) {
            eventsFromStorage = readEventsFromStorage();
        }

        boolean systemDown = false;
        Date lastTimestamp;
        Date currentTimestamp = null;
        for (HistoryEvent event : eventsFromStorage) {
            lastTimestamp = currentTimestamp;
            currentTimestamp = event.getTimestamp();

            if (event.getType() == HistoryEvent.Type.BATTERY_LEVEL) {
                continue;
            }

            String description;
            switch (event.getType()) {
                case SYSTEM_BOOT:
                    if (!systemDown) {
                        // Assume an unclean shutdown and insert a fake unclean-shutdown event
                        Date uncleanShutdownTimestamp;
                        if (lastTimestamp == null) {
                            uncleanShutdownTimestamp = new Date(event.getTimestamp().getTime() - FIVE_MINUTES_MS);
                        } else {
                            uncleanShutdownTimestamp = new Date((event.getTimestamp().getTime() + lastTimestamp.getTime()) / 2);
                        }

                        returnMe.add(toDouble(uncleanShutdownTimestamp), "Unclean shutdown");
                    }

                    description = "System starting up";
                    systemDown = false;
                    break;
                case SYSTEM_SHUTDOWN:
                    description = "System shutting down";
                    systemDown = true;
                    break;
                case INFO:
                    description = event.getMessage();
                    break;
                default:
                    description = "Unknown event type " + event.getType();
            }
            returnMe.add(toDouble(event.getTimestamp()), description);
        }
        return returnMe;
    }

    public static Date toDate(Number x) {
        return new Date(x.intValue() * 1000L);
    }

    public static double toDouble(Date timestamp) {
        return timestamp.getTime() / 1000L;
    }

    public boolean isEmpty() throws IOException {
        if (eventsFromStorage == null) {
            eventsFromStorage = readEventsFromStorage();
        }

        return eventsFromStorage.isEmpty();
    }

    /**
     * Used by {@link #createFakeHistory()}
     */
    private History() {
        // We don't want to persist the fake history
        storage = null;
    }

    public static History createFakeHistory() {
        History history = new History();
        history.eventsFromStorage = new ArrayList<HistoryEvent>();

        Calendar now = new GregorianCalendar();
        now.add(Calendar.DAY_OF_MONTH, -FAKE_HISTORY_DAYS_OLD_START);

        Calendar end = new GregorianCalendar();
        end.add(Calendar.DAY_OF_MONTH, -FAKE_HISTORY_DAYS_OLD_END);

        int charge = 50;
        Random random = new Random();
        boolean wasCharging = false;
        int upgradeCounter = 42;
        while (now.before(end)) {
            int hourOfDay = now.get(Calendar.HOUR_OF_DAY);
            boolean charging = hourOfDay < 8 || hourOfDay > 21;

            if (charging) {
                charge += 15;
            } else {
                charge -= (random.nextInt(3) + 3);
            }
            history.eventsFromStorage.add(HistoryEvent.createBatteryLevelEvent(now.getTime(), charge));
            if (charging != wasCharging) {
                history.eventsFromStorage.add(HistoryEvent.createInfoEvent(now.getTime(),
                        charging ? "Start charging" : "Stop charging"));
            }
            if (upgradeCounter-- == 0) {
                // Upgrade message is an actual message from running the app on a phone, I wanted a
                // long one
                Date timestamp = new Date(now.getTime().getTime() + 15 * 60 * 1000);
                history.eventsFromStorage.add(
                        HistoryEvent.createInfoEvent(timestamp,
                                "Google Play Musik upgraded from 5.6.160sp.1258283 to 5.6.160sp.1258284"));
            }

            now.add(Calendar.HOUR_OF_DAY, 1);
            wasCharging = charging;
        }

        return history;
    }
}
