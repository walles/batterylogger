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
        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(1 * History.HOUR_MS), 100));
        assertBatteryDrainSize(0);
        assertNoEvents();

        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(3 * History.HOUR_MS), 98));
        assertBatteryDrainSize(1);
        assertNoEvents();

        // Drain timestamp should be between the sample timestamps
        assertDrainTimestamps(0, new Date(2 * History.HOUR_MS));
        // From 100% to 98% in two hours = 1%/h
        assertValues(0, 1.0);

        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(5 * History.HOUR_MS), 94));
        assertBatteryDrainSize(1);
        assertNoEvents();

        assertDrainTimestamps(0, new Date(2 * History.HOUR_MS), new Date(4 * History.HOUR_MS));
        // From 100% to 98% in two hours = 1%/h
        // From 98% to 94% in two hours = 2%/h
        assertValues(0, 1.0, 2.0);
    }

    public void testRebootEvents() throws Exception {
        History testMe = new History(testStorage);
        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(1 * History.HOUR_MS), 51));
        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(3 * History.HOUR_MS), 50));

        testMe.addEvent(HistoryEvent.createSystemHaltingEvent(new Date(5 * History.HOUR_MS)));
        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(7 * History.HOUR_MS), 51));
        testMe.addEvent(HistoryEvent.createSystemBootingEvent(new Date(9 * History.HOUR_MS), true));

        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(11 * History.HOUR_MS), 50));
        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(13 * History.HOUR_MS), 47));

        assertBatteryDrainSize(2);
        assertValues(0, 0.5);
        assertDrainTimestamps(0, new Date(2 * History.HOUR_MS));
        assertValues(1, 1.5);
        assertDrainTimestamps(1, new Date(12 * History.HOUR_MS));

        assertEventTimestamps(new Date(5 * History.HOUR_MS), new Date(9 * History.HOUR_MS));
        assertEventDescriptions("System shutting down", "System starting up (charging)");
    }

    /**
     * This would happen at unclean shutdowns; device crashes, battery runs out or is removed.
     */
    public void testMissingShutdownEvent() throws Exception {
        History testMe = new History(testStorage);
        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(1 * History.HOUR_MS), 51));
        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(3 * History.HOUR_MS), 50));

        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(7 * History.HOUR_MS), 48));
        testMe.addEvent(HistoryEvent.createSystemBootingEvent(new Date(9 * History.HOUR_MS), false));

        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(11 * History.HOUR_MS), 46));
        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(13 * History.HOUR_MS), 45));

        // Assume unclean shutdown between last known event before the boot event and the boot event
        assertBatteryDrainSize(2);
        assertValues(0, 0.5, 0.5);
        assertDrainTimestamps(0, new Date(2 * History.HOUR_MS), new Date(5 * History.HOUR_MS));
        assertValues(1, 0.5);
        assertDrainTimestamps(1, new Date(12 * History.HOUR_MS));

        assertEventDescriptions("Unclean shutdown", "System starting up (not charging)");
        assertEventTimestamps(new Date(8 * History.HOUR_MS), new Date(9 * History.HOUR_MS));
    }

    public void testMaintainFileSize() throws Exception {
        History testMe = new History(testStorage);
        Date now = new Date();
        long lastFileSize = 0;

        char[] array = new char[10 * 1024];
        Arrays.fill(array, 'x');
        final String longEventDescription = new String(array);

        while (true) {
            testMe.addEvent(HistoryEvent.createInfoEvent(now, longEventDescription));

            long fileSize = testStorage.length();
            assertTrue("File should have been truncated before 500kb: " + fileSize,
                    fileSize < 500 * 1024);

            if (fileSize < lastFileSize) {
                // It got truncated
                assertTrue("File should have been allowed to grow to at least 350kb: " + lastFileSize,
                        lastFileSize > 350 * 1024);
                assertTrue("File should have been truncated to 250kb-350kb: " + fileSize,
                        fileSize > 250 * 1024 && fileSize < 350 * 1024);
                return;
            }

            lastFileSize = fileSize;
        }
    }
}
