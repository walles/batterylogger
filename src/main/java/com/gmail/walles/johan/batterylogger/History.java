/*
 * Copyright 2014 Johan Walles <johan.walles@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;

import static com.gmail.walles.johan.batterylogger.MainActivity.TAG;

public class History {
    private static final int FAKE_HISTORY_DAYS_OLD_START = 30;
    private static final int FAKE_HISTORY_DAYS_OLD_END = 0;

    private static final long MAX_HISTORY_FILE_SIZE = 400 * 1024;

    public static final long HOUR_MS = 3600 * 1000;
    public static final long FIVE_MINUTES_MS = 5 * 60 * 1000;

    private static final long EPOCH_MS = System.currentTimeMillis();

    @Nullable
    private List<HistoryEvent> eventsFromStorage;

    @Nullable
    private final File storage;

    /**
     * Unit-testing only constructor.
     */
    History(@Nullable File storage) {
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

        if (storage == null) {
            return;
        }

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

        if (storage == null) {
            return;
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
     * Add this series to a plot and you'll see how battery drain speed has changed over time.
     */
    public XYSeries getBatteryDrain() throws IOException {
        SimpleXYSeries returnMe = new SimpleXYSeries("Battery drain");

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
                    continue;
                case SYSTEM_BOOT:
                    if (!systemDown) {
                        // Missing shutdown event; assume an unclean shutdown and reset aggregation
                        lastLevelEvent = null;
                    }
                    systemDown = false;
                    continue;
                case BATTERY_LEVEL:
                    // Handled below
                    break;
                case INFO:
                case START_CHARGING:
                case STOP_CHARGING:
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
                // This happens while charging, reset aggregation
                lastLevelEvent = event;
                continue;
            }

            Date drainTimestamp = new Date((event.getTimestamp().getTime() + lastLevelEvent.getTimestamp().getTime()) / 2);

            returnMe.addLast(toDouble(drainTimestamp), drain);

            lastLevelEvent = event;
        }

        return returnMe;
    }

    public List<XYSeries> getDrainLines() throws IOException {
        if (eventsFromStorage == null) {
            eventsFromStorage = readEventsFromStorage();
        }
        return new DrainLinesCreator(eventsFromStorage).getDrainLines();
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

            String description;
            switch (event.getType()) {
                case START_CHARGING:
                case STOP_CHARGING:
                case BATTERY_LEVEL:
                    continue;

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

                    description = "System starting up (" +
                            (event.isCharging() ? "charging" : "not charging") +
                            ")";
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
        return new Date(x.intValue() * 1000L + EPOCH_MS);
    }

    public static double toDouble(Date timestamp) {
        return (timestamp.getTime() - EPOCH_MS) / 1000L;
    }

    public static double deltaMsToDouble(long deltaMs) {
        return deltaMs / 1000.0;
    }

    public static long doubleToDeltaMs(Number x) {
        return Math.round(x.doubleValue() * 1000.0);
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

    public static History createFakeHistory() throws IOException {
        History history = new History();
        history.eventsFromStorage = new ArrayList<HistoryEvent>();
        //noinspection ConstantConditions
        if (FAKE_HISTORY_DAYS_OLD_START == FAKE_HISTORY_DAYS_OLD_END) {
            return history;
        }

        Calendar now = new GregorianCalendar();
        now.add(Calendar.DAY_OF_MONTH, -FAKE_HISTORY_DAYS_OLD_START);

        Calendar end = new GregorianCalendar();
        end.add(Calendar.DAY_OF_MONTH, -FAKE_HISTORY_DAYS_OLD_END);

        long timeSpanMs = end.getTime().getTime() - now.getTime().getTime();
        Date upgradeTimestamp = new Date(now.getTime().getTime() + timeSpanMs * 2 / 7);
        Date downtimeStart = new Date(now.getTime().getTime() + timeSpanMs * 3 / 7);
        Date downtimeEnd = new Date(now.getTime().getTime() + timeSpanMs * 4 / 7);

        Date bootTimestamp = new Date(now.getTime().getTime() - 86400 * 1000);

        int charge = 50;
        Random random = new Random();

        SystemState previous = new SystemState(now.getTime(), charge, false, bootTimestamp);
        String packageVersion = "5.6.160sp.1258283";
        previous.addInstalledApp("a.b.c", "Google Play Music", packageVersion);

        while (now.before(end)) {
            now.add(Calendar.HOUR_OF_DAY, 1);
            int hourOfDay = now.get(Calendar.HOUR_OF_DAY);
            boolean charging = hourOfDay < 8 || hourOfDay > 21;

            if (downtimeStart != null && now.getTime().getTime() > downtimeStart.getTime()) {
                bootTimestamp = downtimeEnd;
                downtimeStart = null;
                continue;
            }
            if (now.getTime().getTime() < bootTimestamp.getTime()) {
                continue;
            }

            if (charging) {
                charge += 15;
                if (charge > 100) {
                    charge = 100;
                }
            } else {
                charge -= (random.nextInt(3) + 3);
                if (charge < 0) {
                    charge = 0;
                }
            }

            if (upgradeTimestamp != null && now.getTime().getTime() > upgradeTimestamp.getTime()) {
                upgradeTimestamp = null;
                packageVersion = "5.6.160sp.1258284";
            }

            SystemState current = new SystemState(now.getTime(), charge, charging, bootTimestamp);
            current.addInstalledApp("a.b.c", "Google Play Music", packageVersion);

            for (HistoryEvent event : current.getEventsSince(previous)) {
                history.addEvent(event);
            }

            previous = current;
        }

        return history;
    }
}
