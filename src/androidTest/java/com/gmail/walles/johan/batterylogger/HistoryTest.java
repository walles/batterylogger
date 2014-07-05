package com.gmail.walles.johan.batterylogger;

import com.androidplot.xy.XYSeries;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Date;

public class HistoryTest extends TestCase {
    private final static long HOUR_MS = 1000 * 3600;

    public void testBlank() {
        History testMe = new History();
        assertEquals(0, testMe.getBatteryDrain().size());
        assertEquals(0, testMe.getEvents().size());
    }

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

    public void testOnlyBatteryEvents() {
        History testMe = new History();
        testMe.addBatteryLevelEvent(100, new Date(1 * HOUR_MS));
        assertEquals(0, testMe.getBatteryDrain().size());
        assertEquals(0, testMe.getEvents().size());

        testMe.addBatteryLevelEvent(98, new Date(3 * HOUR_MS));
        assertEquals(1, testMe.getBatteryDrain().size());
        assertEquals(0, testMe.getEvents().size());

        XYSeries batteryDrainSeries = testMe.getBatteryDrain().get(0);
        // Drain timestamp should be between the sample timestamps
        assertTimestamps(batteryDrainSeries, new Date(2 * HOUR_MS));
        // From 100% to 98% in two hours = 1%/h
        assertValues(batteryDrainSeries, 1.0);

        testMe.addBatteryLevelEvent(94, new Date(5 * HOUR_MS));
        assertEquals(1, testMe.getBatteryDrain().size());
        assertEquals(0, testMe.getEvents().size());

        batteryDrainSeries = testMe.getBatteryDrain().get(0);
        assertTimestamps(batteryDrainSeries, new Date(2 * HOUR_MS), new Date(4 * HOUR_MS));
        // From 100% to 98% in two hours = 1%/h
        // From 98% to 94% in two hours = 2%/h
        assertValues(batteryDrainSeries, 1.0, 2.0);
    }

    public void testChargingEvents() {
        fail("Test not implemented");
    }

    public void testRebootEvents() {
        fail("Test not implemented");
    }
}
