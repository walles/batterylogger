package com.gmail.walles.johan.batterylogger;

import android.util.Log;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static com.gmail.walles.johan.batterylogger.MainActivity.TAG;

public class History {
    public final static long HOUR_MS = 1000 * 3600;

    private static class Event {
        enum Type {
            BATTERY_LEVEL,
            CHARGING_START,
            CHARGING_STOP,
            SYSTEM_SHUTDOWN,
            SYSTEM_BOOT
        }

        private final Date timestamp;
        private final Type type;
        private int percentage;

        private Event(Date timestamp, Type type) {
            this.timestamp = timestamp;
            this.type = type;
        }

        public static Event createBatteryLevelEvent(int percentage, Date timestamp) {
            Event event = new Event(timestamp, Type.BATTERY_LEVEL);
            event.percentage = percentage;
            return event;
        }

        public static Event createStartChargingEvent(Date timestamp) {
            return new Event(timestamp, Type.CHARGING_START);
        }

        public static Event createStopChargingEvent(Date timestamp) {
            return new Event(timestamp, Type.CHARGING_STOP);
        }

        public static Event createSystemHaltingEvent(Date timestamp) {
            return new Event(timestamp, Type.SYSTEM_SHUTDOWN);
        }

        public static Event createSystemBootingEvent(Date timestamp) {
            return new Event(timestamp, Type.SYSTEM_BOOT);
        }
    }

    private final ArrayList<Event> events = new ArrayList<Event>();

    public void addBatteryLevelEvent(int percentage, Date timestamp) {
        events.add(Event.createBatteryLevelEvent(percentage, timestamp));
    }

    public void addStartChargingEvent(Date timestamp) {
        events.add(Event.createStartChargingEvent(timestamp));
    }

    public void addStopChargingEvent(Date timestamp) {
        events.add(Event.createStopChargingEvent(timestamp));
    }

    /**
     * System halting.
     */
    public void addSystemHaltingEvent(Date timestamp) {
        events.add(Event.createSystemHaltingEvent(timestamp));
    }

    /**
     * System starting up.
     */
    public void addSystemBootingEvent(Date timestamp) {
        events.add(Event.createSystemBootingEvent(timestamp));
    }

    /**
     * Add these series to a plot and you'll see how battery drain speed has changed over time.
     */
    public List<XYSeries> getBatteryDrain() {
        List<XYSeries> returnMe = new LinkedList<XYSeries>();
        SimpleXYSeries xySeries = null;

        boolean charging = false;
        Event lastLevelEvent = null;
        for (Event event : events) {
            switch (event.type) {
                case CHARGING_START:
                    charging = true;
                    lastLevelEvent = null;
                    xySeries = null;
                    continue;
                case CHARGING_STOP:
                    charging = false;
                    continue;
                case BATTERY_LEVEL:
                    // Handled below
                    break;
                default:
                    Log.w(TAG, "Drain: Unsupported event type " + event.type);
                    continue;
            }
            if (charging) {
                continue;
            }

            if (event.type != Event.Type.BATTERY_LEVEL) {
                continue;
            }

            if (lastLevelEvent == null) {
                lastLevelEvent = event;
                continue;
            }

            double deltaHours =
                    (event.timestamp.getTime() - lastLevelEvent.timestamp.getTime()) / (double)HOUR_MS;
            double drain = (lastLevelEvent.percentage - event.percentage) / deltaHours;
            Date drainTimestamp = new Date((event.timestamp.getTime() + lastLevelEvent.timestamp.getTime()) / 2);

            if (xySeries == null) {
                xySeries = new SimpleXYSeries("Battery drain");
                returnMe.add(xySeries);
            }
            xySeries.addLast(toDouble(drainTimestamp), drain);

            lastLevelEvent = event;
        }

        return returnMe;
    }

    /**
     * Add this to a plot and you'll hopefully see what events affect your battery usage.
     */
    public EventSeries getEvents() {
        EventSeries returnMe = new EventSeries();
        for (Event event : events) {
            if (event.type == Event.Type.BATTERY_LEVEL) {
                continue;
            }

            String description;
            switch (event.type) {
                case CHARGING_START:
                    description = "Start charging";
                    break;
                case CHARGING_STOP:
                    description = "Stop charging";
                    break;
                case SYSTEM_BOOT:
                    description = "System starting up";
                    break;
                case SYSTEM_SHUTDOWN:
                    description = "System shutting down";
                    break;
                default:
                    description = "Unknown event type " + event.type;
            }
            returnMe.add(toDouble(event.timestamp), description);
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
