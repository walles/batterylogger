package com.gmail.walles.johan.batterylogger;

import junit.framework.TestCase;

import java.util.Date;

public class HistoryEventTest extends TestCase {
    private void assertRecycling(HistoryEvent event) throws Exception {
        HistoryEvent recycled = HistoryEvent.deserializeFromString(event.serializeToString());
        assertEquals(event, recycled);
    }

    public void testCreateBatteryLevelEvent() throws Exception {
        assertRecycling(HistoryEvent.createBatteryLevelEvent(new Date(12345678), 42));
    }

    public void testCreateInfoEvent() throws Exception {
        assertRecycling(HistoryEvent.createInfoEvent(new Date(12345678), "some message"));
    }

    public void testCreateSystemHaltingEvent() throws Exception {
        assertRecycling(HistoryEvent.createSystemHaltingEvent(new Date(12345678)));
    }

    public void testCreateSystemBootingEvent() throws Exception {
        assertRecycling(HistoryEvent.createSystemBootingEvent(new Date(12345678), true));
        assertRecycling(HistoryEvent.createSystemBootingEvent(new Date(12345678), false));
    }

    public void testCreateStartChargingEvent() throws Exception {
        assertRecycling(HistoryEvent.createStartChargingEvent(new Date(12345678)));
    }

    public void testCreateStopChargingEvent() throws Exception {
        assertRecycling(HistoryEvent.createStopChargingEvent(new Date(12345678)));
    }
}