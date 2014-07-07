package com.gmail.walles.johan.batterylogger;

import junit.framework.TestCase;

import java.util.Date;

public class HistoryEventTest extends TestCase {
    public void testCreateBatteryLevelEvent() throws Exception {
        HistoryEvent testMe = HistoryEvent.createBatteryLevelEvent(new Date(12345678), 42);
        HistoryEvent recycled = HistoryEvent.deserializeFromString(testMe.serializeToString());
        assertEquals(testMe, recycled);
    }

    public void testCreateInfoEvent() throws Exception {
        HistoryEvent testMe = HistoryEvent.createInfoEvent(new Date(12345678), "some message");
        HistoryEvent recycled = HistoryEvent.deserializeFromString(testMe.serializeToString());
        assertEquals(testMe, recycled);
    }

    public void testCreateSystemHaltingEvent() throws Exception {
        HistoryEvent testMe = HistoryEvent.createSystemHaltingEvent(new Date(12345678));
        HistoryEvent recycled = HistoryEvent.deserializeFromString(testMe.serializeToString());
        assertEquals(testMe, recycled);
    }

    public void testCreateSystemBootingEvent() throws Exception {
        HistoryEvent testMe = HistoryEvent.createSystemBootingEvent(new Date(12345678));
        HistoryEvent recycled = HistoryEvent.deserializeFromString(testMe.serializeToString());
        assertEquals(testMe, recycled);
    }
}