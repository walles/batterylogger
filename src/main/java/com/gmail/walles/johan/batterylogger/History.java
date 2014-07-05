package com.gmail.walles.johan.batterylogger;

import com.androidplot.xy.XYSeries;

import java.util.Date;
import java.util.List;

public class History {
    public void addBatteryLevelEvent(int percentage, Date timestamp) {

    }

    public void addStartChargingEvent(Date timestamp) {

    }

    public void addStopChargingEvent(Date timestamp) {

    }

    /**
     * System halting.
     */
    public void addSystemHaltEvent(Date timestamp) {

    }

    /**
     * System starting up.
     */
    public void addSystemBootEvent(Date timestamp) {

    }

    /**
     * Add these series to a plot and you'll see how battery drain speed has changed over time.
     */
    public List<XYSeries> getBatteryDrain() {
        return null;
    }

    /**
     * Add this to a plot and you'll hopefully see what events affect your battery usage.
     */
    public EventSeries getEvents() {
        return null;
    }

    public static Date toDate(Number x) {
        return new Date(x.intValue() * 1000L);
    }
}
