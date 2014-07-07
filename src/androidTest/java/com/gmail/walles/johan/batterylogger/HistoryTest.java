package com.gmail.walles.johan.batterylogger;

import android.test.AndroidTestCase;
import com.androidplot.xy.XYSeries;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

public class HistoryTest extends AndroidTestCase {
    private File testStorage;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        testStorage = File.createTempFile("historytest", ".txt");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        assertTrue(testStorage.delete());
    }

    private void assertValues(int index, double ... expectedValues) throws Exception {
        XYSeries batteryDrain = new History(testStorage).getBatteryDrain().get(index);
        double actualValues[] = new double[batteryDrain.size()];
        for (int i = 0; i < batteryDrain.size(); i++) {
            actualValues[i] = batteryDrain.getY(i).doubleValue();
        }
        assertEquals(Arrays.toString(expectedValues), Arrays.toString(actualValues));
    }

    private void assertDrainTimestamps(int index, Date ... expectedTimestamps) throws Exception {
        XYSeries series = new History(testStorage).getBatteryDrain().get(index);
        Date actualTimestamps[] = new Date[series.size()];
        for (int i = 0; i < series.size(); i++) {
            actualTimestamps[i] = History.toDate(series.getX(i));
        }
        assertEquals(Arrays.toString(expectedTimestamps), Arrays.toString(actualTimestamps));
    }

    private void assertEventTimestamps(Date ... expectedTimestamps) throws Exception {
        XYSeries series = new History(testStorage).getEvents();
        Date actualTimestamps[] = new Date[series.size()];
        for (int i = 0; i < series.size(); i++) {
            actualTimestamps[i] = History.toDate(series.getX(i));
        }
        assertEquals(Arrays.toString(expectedTimestamps), Arrays.toString(actualTimestamps));
    }

    private void assertEventDescriptions(String... expectedDescriptions) throws Exception {
        EventSeries events = new History(testStorage).getEvents();
        String actualDescriptions[] = new String[events.size()];
        for (int i = 0; i < events.size(); i++) {
            actualDescriptions[i] = events.getDescription(i);
        }
        assertEquals(Arrays.toString(expectedDescriptions), Arrays.toString(actualDescriptions));
    }

    private void assertNoEvents() throws Exception {
        assertEventTimestamps(/* Empty */);
        assertEventDescriptions(/* Empty */);
    }

    private void assertBatteryDrainSize(int expectedSize) throws Exception {
        assertEquals(expectedSize, new History(testStorage).getBatteryDrain().size());
    }

    public void testBlank() throws Exception {
        assertBatteryDrainSize(0);
        assertNoEvents();
    }

    public void testOnlyBatteryEvents() throws Exception {
        History testMe = new History(testStorage);
        testMe.addBatteryLevelEvent(100, new Date(1 * History.HOUR_MS));
        assertBatteryDrainSize(0);
        assertNoEvents();

        testMe.addBatteryLevelEvent(98, new Date(3 * History.HOUR_MS));
        assertBatteryDrainSize(1);
        assertNoEvents();

        // Drain timestamp should be between the sample timestamps
        assertDrainTimestamps(0, new Date(2 * History.HOUR_MS));
        // From 100% to 98% in two hours = 1%/h
        assertValues(0, 1.0);

        testMe.addBatteryLevelEvent(94, new Date(5 * History.HOUR_MS));
        assertBatteryDrainSize(1);
        assertNoEvents();

        assertDrainTimestamps(0, new Date(2 * History.HOUR_MS), new Date(4 * History.HOUR_MS));
        // From 100% to 98% in two hours = 1%/h
        // From 98% to 94% in two hours = 2%/h
        assertValues(0, 1.0, 2.0);
    }

    public void testChargingEvents() throws Exception {
        History testMe = new History(testStorage);
        testMe.addBatteryLevelEvent(51, new Date(1 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(50, new Date(3 * History.HOUR_MS));

        testMe.addStartChargingEvent(new Date(5 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(51, new Date(7 * History.HOUR_MS));
        testMe.addStopChargingEvent(new Date(9 * History.HOUR_MS));

        testMe.addBatteryLevelEvent(50, new Date(11 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(47, new Date(13 * History.HOUR_MS));

        assertBatteryDrainSize(2);
        assertValues(0, 0.5);
        assertDrainTimestamps(0, new Date(2 * History.HOUR_MS));
        assertValues(1, 1.5);
        assertDrainTimestamps(1, new Date(12 * History.HOUR_MS));

        assertEventTimestamps(new Date(5 * History.HOUR_MS), new Date(9 * History.HOUR_MS));
        assertEventDescriptions("Start charging", "Stop charging");
    }

    public void testMissingStartChargingEvent() throws Exception {
        History testMe = new History(testStorage);
        testMe.addBatteryLevelEvent(51, new Date(1 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(50, new Date(3 * History.HOUR_MS));

        testMe.addBatteryLevelEvent(51, new Date(7 * History.HOUR_MS));
        testMe.addStopChargingEvent(new Date(9 * History.HOUR_MS));

        testMe.addBatteryLevelEvent(50, new Date(11 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(47, new Date(13 * History.HOUR_MS));

        // Everything before the stop charging event should be ignored
        assertBatteryDrainSize(1);
        assertValues(0, 1.5);
        assertDrainTimestamps(0, new Date(12 * History.HOUR_MS));

        assertEventTimestamps(new Date(9 * History.HOUR_MS));
        assertEventDescriptions("Stop charging");
    }

    public void testRebootEvents() throws Exception {
        History testMe = new History(testStorage);
        testMe.addBatteryLevelEvent(51, new Date(1 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(50, new Date(3 * History.HOUR_MS));

        testMe.addSystemHaltingEvent(new Date(5 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(51, new Date(7 * History.HOUR_MS));
        testMe.addSystemBootingEvent(new Date(9 * History.HOUR_MS));

        testMe.addBatteryLevelEvent(50, new Date(11 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(47, new Date(13 * History.HOUR_MS));

        assertBatteryDrainSize(2);
        assertValues(0, 0.5);
        assertDrainTimestamps(0, new Date(2 * History.HOUR_MS));
        assertValues(1, 1.5);
        assertDrainTimestamps(1, new Date(12 * History.HOUR_MS));

        assertEventTimestamps(new Date(5 * History.HOUR_MS), new Date(9 * History.HOUR_MS));
        assertEventDescriptions("System shutting down", "System starting up");
    }

    /**
     * This would happen at unclean shutdowns; device crashes, battery runs out or is removed.
     */
    public void testMissingShutdownEvent() throws Exception {
        History testMe = new History(testStorage);
        testMe.addBatteryLevelEvent(51, new Date(1 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(50, new Date(3 * History.HOUR_MS));

        testMe.addBatteryLevelEvent(48, new Date(7 * History.HOUR_MS));
        testMe.addSystemBootingEvent(new Date(9 * History.HOUR_MS));

        testMe.addBatteryLevelEvent(46, new Date(11 * History.HOUR_MS));
        testMe.addBatteryLevelEvent(45, new Date(13 * History.HOUR_MS));

        // Assume unclean shutdown between last known event before the boot event and the boot event
        assertBatteryDrainSize(2);
        assertValues(0, 0.5, 0.5);
        assertDrainTimestamps(0, new Date(2 * History.HOUR_MS), new Date(5 * History.HOUR_MS));
        assertValues(1, 0.5);
        assertDrainTimestamps(1, new Date(12 * History.HOUR_MS));

        assertEventDescriptions("Unclean shutdown", "System starting up");
        assertEventTimestamps(new Date(8 * History.HOUR_MS), new Date(9 * History.HOUR_MS));
    }
}
