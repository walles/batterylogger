/*
 * Copyright 2016 Johan Walles <johan.walles@gmail.com>
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

import com.gmail.walles.johan.batterylogger.plot.DrainSample;
import com.gmail.walles.johan.batterylogger.plot.PlotEvent;

import junit.framework.TestCase;

import org.junit.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryTest extends TestCase {
    private File testStorage;
    private long now;
    private History testMe;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        now = System.currentTimeMillis();
        testStorage = File.createTempFile("historytest", ".txt");
        testMe = new History(testStorage);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            Assert.assertTrue(testStorage.delete());
        } finally {
            super.tearDown();
        }
    }

    private void assertValues(double ... expectedValues) throws Exception {
        List<DrainSample> batteryDrain = new History(testStorage).getBatteryDrain();
        double actualValues[] = new double[batteryDrain.size()];
        for (int i = 0; i < batteryDrain.size(); i++) {
            actualValues[i] = batteryDrain.get(i).drainSpeed;
        }
        Assert.assertEquals(Arrays.toString(expectedValues), Arrays.toString(actualValues));
    }

    private void assertTimestampEquals(Date expectedTimestamps[], List<Date> actualTimestamps) {
        Date actualTimestampsArray[] = new Date[actualTimestamps.size()];
        for (int i = 0; i < actualTimestamps.size(); i++) {
            actualTimestampsArray[i] = actualTimestamps.get(i);
        }

        String diff =
                "Expected: " + Arrays.toString(expectedTimestamps) + "\n" +
                "Actual  : " + Arrays.toString(actualTimestampsArray);

        // Fuzzy compare the arrays, accepting some time diff
        Assert.assertEquals(diff, expectedTimestamps.length, actualTimestampsArray.length);
        for (int i = 0; i < expectedTimestamps.length; i++) {
            Date expected = expectedTimestamps[i];
            Date actual = actualTimestampsArray[i];
            long diffMs = Math.abs(expected.getTime() - actual.getTime());

            Assert.assertTrue("\n" + diff, diffMs < 1000L);
        }
    }

    private void assertDrainTimestamps(Date ... expectedTimestamps) throws Exception {
        List<Date> actualTimestamps = new ArrayList<>();
        for (DrainSample sample : new History(testStorage).getBatteryDrain()) {
            actualTimestamps.add(new Date((long)sample.startMsSinceEpoch));
            actualTimestamps.add(new Date((long)sample.endMsSinceEpoch));
        }

        assertTimestampEquals(expectedTimestamps, actualTimestamps);
    }

    private void assertEventTimestamps(Date ... expectedTimestamps) throws Exception {
        List<Date> actualTimestamps = new ArrayList<>();
        for (PlotEvent event : new History(testStorage).getEvents()) {
            actualTimestamps.add(new Date((long)event.msSinceEpoch));
        }

        assertTimestampEquals(expectedTimestamps, actualTimestamps);
    }

    private void assertEventDescriptions(String... expectedDescriptions) throws Exception {
        List<PlotEvent> events = new History(testStorage).getEvents();
        String actualDescriptions[] = new String[events.size()];
        for (int i = 0; i < events.size(); i++) {
            actualDescriptions[i] = events.get(i).description;
        }
        Assert.assertEquals(Arrays.toString(expectedDescriptions), Arrays.toString(actualDescriptions));
    }

    private void assertNoEvents() throws Exception {
        assertEventTimestamps(/* Empty */);
        assertEventDescriptions(/* Empty */);
    }

    public void testBlank() throws Exception {
        assertNoEvents();
    }

    public void testOnlyBatteryEvents() throws Exception {
        testMe.addEvent(
                HistoryEvent.createBatteryLevelEvent(new Date(now + 1 * History.HOUR_MS), 100));
        assertNoEvents();

        testMe.addEvent(
                HistoryEvent.createBatteryLevelEvent(new Date(now + 3 * History.HOUR_MS), 98));
        assertNoEvents();

        // Drain timestamp should be between the sample timestamps
        assertDrainTimestamps(
            new Date(now + 1 * History.HOUR_MS),
            new Date(now + 3 * History.HOUR_MS));
        // From 100% to 98% in two hours = 1%/h
        assertValues(1.0);

        testMe.addEvent(
                HistoryEvent.createBatteryLevelEvent(new Date(now + 5 * History.HOUR_MS), 94));
        assertNoEvents();

        assertDrainTimestamps(
            new Date(now + 1 * History.HOUR_MS), new Date(now + 3 * History.HOUR_MS),
            new Date(now + 3 * History.HOUR_MS), new Date(now + 5 * History.HOUR_MS));
        // From 100% to 98% in two hours = 1%/h
        // From 98% to 94% in two hours = 2%/h
        assertValues(1.0, 2.0);
    }

    public void testRebootEvents() throws Exception {
        testMe.addEvent(
                HistoryEvent.createBatteryLevelEvent(new Date(now + 1 * History.HOUR_MS), 51));
        testMe.addEvent(
                HistoryEvent.createBatteryLevelEvent(new Date(now + 3 * History.HOUR_MS), 50));

        testMe.addEvent(
                HistoryEvent.createSystemHaltingEvent(new Date(now + 5 * History.HOUR_MS)));
        testMe.addEvent(
                HistoryEvent.createBatteryLevelEvent(new Date(now + 7 * History.HOUR_MS), 51));
        testMe.addEvent(
                HistoryEvent.createSystemBootingEvent(new Date(now + 9 * History.HOUR_MS), true));

        testMe.addEvent(
                HistoryEvent.createBatteryLevelEvent(new Date(now + 11 * History.HOUR_MS), 50));
        testMe.addEvent(
                HistoryEvent.createBatteryLevelEvent(new Date(now + 13 * History.HOUR_MS), 47));

        assertValues(0.5, 1.5);
        assertDrainTimestamps(
                new Date(now + 1 * History.HOUR_MS), new Date(now + 3 * History.HOUR_MS),
                new Date(now + 11 * History.HOUR_MS), new Date(now + 13 * History.HOUR_MS));

        assertEventTimestamps(
                new Date(now + 5 * History.HOUR_MS),
                new Date(now + 9 * History.HOUR_MS));
        assertEventDescriptions("System shutting down", "System starting up (charging)");
    }

    /**
     * This would happen at unclean shutdowns; device crashes, battery runs out or is removed.
     */
    public void testMissingShutdownEvent() throws Exception {
        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(now + 1 * History.HOUR_MS), 51));
        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(now + 3 * History.HOUR_MS), 50));

        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(now + 7 * History.HOUR_MS), 48));
        testMe.addEvent(HistoryEvent.createSystemBootingEvent(new Date(now + 9 * History.HOUR_MS), false));

        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(now + 11 * History.HOUR_MS), 46));
        testMe.addEvent(HistoryEvent.createBatteryLevelEvent(new Date(now + 13 * History.HOUR_MS), 45));

        // Assume unclean shutdown between last known event before the boot event and the boot event
        assertValues(0.5, 0.5, 0.5);
        assertDrainTimestamps(
            new Date(now + 1 * History.HOUR_MS), new Date(now + 3 * History.HOUR_MS),
            new Date(now + 3 * History.HOUR_MS), new Date(now + 7 * History.HOUR_MS),
            new Date(now + 11 * History.HOUR_MS), new Date(now + 13 * History.HOUR_MS));

        assertEventDescriptions("Unclean shutdown", "System starting up (not charging)");
        assertEventTimestamps(
                new Date(now + 8 * History.HOUR_MS),
                new Date(now + 9 * History.HOUR_MS));
    }

    public void testMaintainFileSize() throws Exception {
        Date now = new Date();
        long lastFileSize = 0;

        char[] array = new char[10 * 1024];
        Arrays.fill(array, 'x');
        final String longEventDescription = new String(array);

        while (true) {
            testMe.addEvent(HistoryEvent.createInfoEvent(now, longEventDescription));

            long fileSize = testStorage.length();
            Assert.assertTrue("File should have been truncated before 500kb: " + fileSize,
                    fileSize < 500 * 1024);

            if (fileSize < lastFileSize) {
                // It got truncated
                Assert.assertTrue("File should have been allowed to grow to at least 350kb: " + lastFileSize,
                        lastFileSize > 350 * 1024);
                Assert.assertTrue("File should have been truncated to 250kb-350kb: " + fileSize,
                        fileSize > 250 * 1024 && fileSize < 350 * 1024);
                return;
            }

            lastFileSize = fileSize;
        }
    }

    /**
     * Verify history file truncation by event age.
     */
    public void testMaintainHistoryAge() throws Exception {
        Assert.assertEquals(0, testMe.getHistoryAgeDays());

        Date now = new Date();

        // Log 34 events into a History file, with 24h between each
        for (long i = 34; i >= 0; i--) {
            Date then = new Date(now.getTime() - i * 86400 * 1000);
            testMe.addEvent(HistoryEvent.createInfoEvent(then, "Something happened"));
        }

        Assert.assertEquals(34, testMe.getHistoryAgeDays());
        Assert.assertEquals(34, new History(testStorage).getHistoryAgeDays());

        // Record the size of the history file
        long fileSize0 = testStorage.length();

        // Truncate the file
        testMe.dropOldHistory();

        // Verify that the file has shrunk
        Assert.assertTrue(String.format(Locale.getDefault(), "Old size=%d, new size=%d",
            fileSize0, testStorage.length()), testStorage.length() < fileSize0);

        // Verify that the age of the oldest event in the file is what we expect
        Assert.assertEquals(27, testMe.getHistoryAgeDays());
        Assert.assertEquals(27, new History(testStorage).getHistoryAgeDays());
    }
}
