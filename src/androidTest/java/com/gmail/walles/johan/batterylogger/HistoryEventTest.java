package com.gmail.walles.johan.batterylogger;

import junit.framework.TestCase;

import java.util.Date;

public class HistoryEventTest extends TestCase {
    public void testCreateBatteryLevelEvent() throws Exception {
        HistoryEvent testMe = HistoryEvent.createBatteryLevelEvent(42, new Date(12345678));
        HistoryEvent recycled = HistoryEvent.deserializeFromString(testMe.serializeToString());
        assertEquals(testMe, recycled);
    }

    public void testCreateStartChargingEvent() throws Exception {
        HistoryEvent testMe = HistoryEvent.createStartChargingEvent(new Date(12345678));
        HistoryEvent recycled = HistoryEvent.deserializeFromString(testMe.serializeToString());
        assertEquals(testMe, recycled);
    }

    public void testCreateStopChargingEvent() throws Exception {
        HistoryEvent testMe = HistoryEvent.createStopChargingEvent(new Date(12345678));
        HistoryEvent recycled = HistoryEvent.deserializeFromString(testMe.serializeToString());
        assertEquals(testMe, recycled);
    }

    public void testCreateSystemHaltingEvent() throws Exception {
        HistoryEvent testMe = HistoryEvent.createSystemHaltingEvent(new Date(12345678));
        HistoryEvent recycled = HistoryEvent.deserializeFromString(testMe.serializeToString());
        assertEquals(testMe, recycled);
    }

    public void testCreateSystemBootingEvent() throws Exception {
        HistoryEvent testMe = HistoryEvent.createSystemBootingEvent(new Date(12345678), true);
        HistoryEvent recycled = HistoryEvent.deserializeFromString(testMe.serializeToString());
        assertEquals(testMe, recycled);
    }
}