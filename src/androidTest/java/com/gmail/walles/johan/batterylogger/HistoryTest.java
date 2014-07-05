package com.gmail.walles.johan.batterylogger;

import com.androidplot.xy.XYSeries;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Date;

public class HistoryTest extends TestCase {
    private void assertValues(XYSeries batteryDrain, double ... expectedValues) {
        // FIXME: This method should go via persistent storage to verify that as well
        double actualValues[] = new double[batteryDrain.size()];
        for (int i = 0; i < batteryDrain.size(); i++) {
            actualValues[i] = batteryDrain.getY(i).doubleValue();
        }
        assertEquals(Arrays.toString(expectedValues), Arrays.toString(actualValues));
    }

    private void assertTimestamps(XYSeries series, Date ... expectedTimestamps) {
        // FIXME: This method should go via persistent storage to verify that as well
        Date actualTimestamps[] = new Date[series.size()];
        for (int i = 0; i < series.size(); i++) {
            actualTimestamps[i] = History.toDate(series.getX(i));
        }
        assertEquals(Arrays.toString(expectedTimestamps), Arrays.toString(actualTimestamps));
    }

    private void assertDescriptions(EventSeries events, String ... expectedDescriptions) {
        // FIXME: This method should go via persistent storage to verify that as well
        String actualDescriptions[] = new String[events.size()];
        for (int i = 0; i < events.size(); i++) {
            actualDescriptions[i] = events.getDescription(i);
        }
        assertEquals(Arrays.toString(expectedDescriptions), Arrays.toString(actualDescriptions));
    }

    public void testBlank() {
        History testMe = new History();
        assertEquals(0, testMe.getBatteryDrain().size());
        assertEquals(0, testMe.getEvents().size());
    }

    public void testOnlyBatteryEvents() {
        History testMe = new History();
        testMe.addBatteryLevelEvent(100, new Date(1 * History.HOUR_MS));
        assertEquals(0, testMe.getBatteryDrain().size());
        assertEquals(0, testMe.getEvents().size());

        testMe.addBatteryLevelEvent(98, new Date(3 * History.HOUR_MS));
        assertEquals(1, testMe.getBatteryDrain().size());
        assertEquals(0, testMe.getEvents().size());

        XYSeries batteryDrainSeries = testMe.getBatteryDrain().get(0);
        // Drain timestamp should be between the sample timestamps
        assertTimestamps(batteryDrainSeries, new Date(2 * History.HOUR_MS));
        // From 100% to 98% in two hours = 1%/h
        assertValues(batteryDrainSeries, 1.0);

        testMe.addBatteryLevelEvent(94, new Date(5 * History.HOUR_MS));
        assertEquals(1, testMe.getBatteryDrain().size());
        assertEquals(0, testMe.getEvents().size());

        batteryDrainSeries = testMe.getBatteryDrain().get(0);
        assertTimestamps(batteryDrainSeries, new Date(2 * History.HOUR_MS), new Date(4 * History.HOUR_MS));
        // From 100% to 98% in two hours = 1%/h
        // From 98% to 94% in two hours = 2%/h
        assertValues(batteryDrainSeries, 1.0, 2.0);
    }

    public void testChargingEvents() {
        History testMe = new History();
        testMe.addBatteryLevelEvent(51, new Date(1 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(50, new Date(3 * History.HOUR_MS));

        testMe.addStartChargingEvent(new Date(5 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(51, new Date(7 * History.HOUR_MS));
        testMe.addStopChargingEvent(new Date(9 * History.HOUR_MS));

        testMe.addBatteryLevelEvent(50, new Date(11 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(47, new Date(13 * History.HOUR_MS));

        assertEquals(2, testMe.getBatteryDrain().size());
        assertValues(testMe.getBatteryDrain().get(0), 0.5);
        assertTimestamps(testMe.getBatteryDrain().get(0), new Date(2 * History.HOUR_MS));
        assertValues(testMe.getBatteryDrain().get(1), 1.5);
        assertTimestamps(testMe.getBatteryDrain().get(1), new Date(12 * History.HOUR_MS));

        assertTimestamps(testMe.getEvents(), new Date(5 * History.HOUR_MS), new Date(9 * History.HOUR_MS));
        assertDescriptions(testMe.getEvents(), "Start charging", "Stop charging");
    }

    public void testMissingStartChargingEvent() {
        History testMe = new History();
        testMe.addBatteryLevelEvent(51, new Date(1 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(50, new Date(3 * History.HOUR_MS));

        testMe.addBatteryLevelEvent(51, new Date(7 * History.HOUR_MS));
        testMe.addStopChargingEvent(new Date(9 * History.HOUR_MS));

        testMe.addBatteryLevelEvent(50, new Date(11 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(47, new Date(13 * History.HOUR_MS));

        // Everything before the stop charging event should be ignored
        assertEquals(1, testMe.getBatteryDrain().size());
        assertValues(testMe.getBatteryDrain().get(0), 1.5);
        assertTimestamps(testMe.getBatteryDrain().get(0), new Date(12 * History.HOUR_MS));

        assertTimestamps(testMe.getEvents(), new Date(9 * History.HOUR_MS));
        assertDescriptions(testMe.getEvents(), "Stop charging");
    }

    public void testRebootEvents() {
        History testMe = new History();
        testMe.addBatteryLevelEvent(51, new Date(1 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(50, new Date(3 * History.HOUR_MS));

        testMe.addSystemHaltingEvent(new Date(5 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(51, new Date(7 * History.HOUR_MS));
        testMe.addSystemBootingEvent(new Date(9 * History.HOUR_MS));

        testMe.addBatteryLevelEvent(50, new Date(11 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(47, new Date(13 * History.HOUR_MS));

        assertEquals(2, testMe.getBatteryDrain().size());
        assertValues(testMe.getBatteryDrain().get(0), 0.5);
        assertTimestamps(testMe.getBatteryDrain().get(0), new Date(2 * History.HOUR_MS));
        assertValues(testMe.getBatteryDrain().get(1), 1.5);
        assertTimestamps(testMe.getBatteryDrain().get(1), new Date(12 * History.HOUR_MS));

        assertTimestamps(testMe.getEvents(), new Date(5 * History.HOUR_MS), new Date(9 * History.HOUR_MS));
        assertDescriptions(testMe.getEvents(), "System shutting down", "System starting up");
    }

    public void testMissingShutdownEvent() {
        History testMe = new History();
        testMe.addBatteryLevelEvent(51, new Date(1 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(50, new Date(3 * History.HOUR_MS));

        testMe.addBatteryLevelEvent(51, new Date(7 * History.HOUR_MS));
        testMe.addSystemBootingEvent(new Date(9 * History.HOUR_MS));

        testMe.addBatteryLevelEvent(50, new Date(11 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(47, new Date(13 * History.HOUR_MS));

        // Everything before the system booting event should be ignored
        assertEquals(1, testMe.getBatteryDrain().size());
        assertValues(testMe.getBatteryDrain().get(0), 1.5);
        assertTimestamps(testMe.getBatteryDrain().get(0), new Date(12 * History.HOUR_MS));

        assertTimestamps(testMe.getEvents(), new Date(9 * History.HOUR_MS));
        assertDescriptions(testMe.getEvents(), "System starting up");
    }
}
