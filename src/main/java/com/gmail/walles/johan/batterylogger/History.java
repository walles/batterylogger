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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static com.gmail.walles.johan.batterylogger.MainActivity.TAG;

public class History {
    public static final long HOUR_MS = 3600 * 1000;
    public static final long FIVE_MINUTES_MS = 5 * 60 * 1000;

    @Nullable
    private ArrayList<HistoryEvent> eventsFromStorage;

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

    private void addEvent(HistoryEvent event) throws IOException {
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
    }

    public void addBatteryLevelEvent(int percentage, Date timestamp) throws IOException {
        addEvent(HistoryEvent.createBatteryLevelEvent(timestamp, percentage));
    }

    public void addInfoEvent(String message, Date timestamp) throws IOException {
        addEvent(HistoryEvent.createInfoEvent(timestamp, message));
    }

    /**
     * System halting.
     */
    public void addSystemHaltingEvent(Date timestamp) throws IOException {
        addEvent(HistoryEvent.createSystemHaltingEvent(timestamp));
    }

    /**
     * System starting up.
     */
    public void addSystemBootingEvent(Date timestamp) throws IOException {
        addEvent(HistoryEvent.createSystemBootingEvent(timestamp));
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

    private ArrayList<HistoryEvent> readEventsFromStorage() throws IOException {
        ArrayList<HistoryEvent> returnMe = new ArrayList<HistoryEvent>();

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
}