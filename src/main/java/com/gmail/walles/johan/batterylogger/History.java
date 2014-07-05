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

    private final ArrayList<HistoryEvent> events = new ArrayList<HistoryEvent>();

    public void addBatteryLevelEvent(int percentage, Date timestamp) {
        events.add(HistoryEvent.createBatteryLevelEvent(percentage, timestamp));
    }

    public void addStartChargingEvent(Date timestamp) {
        events.add(HistoryEvent.createStartChargingEvent(timestamp));
    }

    public void addStopChargingEvent(Date timestamp) {
        events.add(HistoryEvent.createStopChargingEvent(timestamp));
    }

    /**
     * System halting.
     */
    public void addSystemHaltingEvent(Date timestamp) {
        events.add(HistoryEvent.createSystemHaltingEvent(timestamp));
    }

    /**
     * System starting up.
     */
    public void addSystemBootingEvent(Date timestamp) {
        events.add(HistoryEvent.createSystemBootingEvent(timestamp));
    }

    /**
     * Add these series to a plot and you'll see how battery drain speed has changed over time.
     */
    public List<XYSeries> getBatteryDrain() {
        List<XYSeries> returnMe = new LinkedList<XYSeries>();
        SimpleXYSeries xySeries = null;

        boolean charging = false;
        boolean systemDown = false;
        HistoryEvent lastLevelEvent = null;
        for (HistoryEvent event : events) {
            switch (event.getType()) {
                case CHARGING_START:
                    charging = true;
                    lastLevelEvent = null;
                    xySeries = null;
                    continue;
                case CHARGING_STOP:
                    if (!charging) {
                        // Missing start event; everything before this point is untrustworthy
                        lastLevelEvent = null;
                        xySeries = null;
                        returnMe = new LinkedList<XYSeries>();
                    }
                    charging = false;
                    continue;
                case SYSTEM_SHUTDOWN:
                    systemDown = true;
                    lastLevelEvent = null;
                    xySeries = null;
                    continue;
                case SYSTEM_BOOT:
                    if (!systemDown) {
                        // Missing start event; everything before this point is untrustworthy
                        lastLevelEvent = null;
                        xySeries = null;
                        returnMe = new LinkedList<XYSeries>();
                    }
                    systemDown = false;
                    continue;
                case BATTERY_LEVEL:
                    // Handled below
                    break;
                default:
                    Log.w(TAG, "Drain: Unsupported event type " + event.getType());
                    continue;
            }
            if (charging || systemDown) {
                continue;
            }

            if (lastLevelEvent == null) {
                lastLevelEvent = event;
                continue;
            }

            double deltaHours =
                    (event.getTimestamp().getTime() - lastLevelEvent.getTimestamp().getTime()) / (double)HOUR_MS;
            double drain = (lastLevelEvent.getPercentage() - event.getPercentage()) / deltaHours;
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

    /**
     * Add this to a plot and you'll hopefully see what events affect your battery usage.
     */
    public EventSeries getEvents() {
        EventSeries returnMe = new EventSeries();
        for (HistoryEvent event : events) {
            if (event.getType() == HistoryEvent.Type.BATTERY_LEVEL) {
                continue;
            }

            String description;
            switch (event.getType()) {
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
