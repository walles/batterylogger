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

import junit.framework.TestCase;

import org.junit.Assert;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class DrainLinesCreatorTest extends TestCase {
    private static final Date BEFORE = new Date(System.currentTimeMillis() - 3600 * 1000);
    private static final Date NOW = new Date();

    /**
     * How close do dates need to be for us to be satisfied?
     */
    private static final double DELTA_MS = 1000.0;

    public void testMedianLine() {
        try {
            DrainLinesCreator.median(Collections.<Double>emptyList());
            Assert.fail("Computing median of one number should fail");
        } catch (IllegalArgumentException e) {
            // Expected exception intentionally ignored
        }

        //noinspection ArraysAsListWithZeroOrOneArgument
        Assert.assertEquals(5.0, DrainLinesCreator.median(Arrays.asList(5.0)), 0.0);
        Assert.assertEquals(5.5, DrainLinesCreator.median(Arrays.asList(5.0, 6.0)), 0.0);
        Assert.assertEquals(6.0, DrainLinesCreator.median(Arrays.asList(5.0, 6.0, 7.0)), 0.0);
        Assert.assertEquals(6.5, DrainLinesCreator.median(Arrays.asList(5.0, 6.0, 7.0, 7.5)), 0.0);
    }

    public void testWithZeroEvents() {
        DrainLinesCreator testMe = new DrainLinesCreator(Collections.<HistoryEvent>emptyList());
        Assert.assertEquals(0, testMe.getDrainLines().size());
    }

    public void testWithOneEvent() {
        DrainLinesCreator testMe = new DrainLinesCreator(Collections.singletonList(
            HistoryEvent.createBatteryLevelEvent(NOW, 50)
        ));
        Assert.assertEquals(0, testMe.getDrainLines().size());
    }

    public void testDischargeLineWithZeroEvents() {
        DrainLinesCreator testMe = new DrainLinesCreator(Collections.singletonList(
            HistoryEvent.createStopChargingEvent(NOW)
        ));
        Assert.assertEquals(0, testMe.getDrainLines().size());
    }

    public void testDischargeLineWithOneEvent() {
        DrainLinesCreator testMe = new DrainLinesCreator(Arrays.asList(
                HistoryEvent.createStopChargingEvent(NOW),
                HistoryEvent.createBatteryLevelEvent(BEFORE, 50)
        ));
        Assert.assertEquals(0, testMe.getDrainLines().size());
    }

    public void testDischargeLineWithTwoEvents() {
        Date dates[] = SystemState.between(BEFORE, NOW, 3);
        DrainLinesCreator testMe = new DrainLinesCreator(Arrays.asList(
                HistoryEvent.createStopChargingEvent(dates[0]),
                HistoryEvent.createBatteryLevelEvent(dates[1], 50),
                HistoryEvent.createBatteryLevelEvent(dates[2], 40)
        ));
        Assert.assertEquals(1, testMe.getDrainLines().size());

        DrainSample drainLine = testMe.getDrainLines().get(0);

        Assert.assertTrue(drainLine.drainSpeed > 0.0);
        Assert.assertFalse(Double.isInfinite(drainLine.drainSpeed));

        Assert.assertEquals(drainLine.startMsSinceEpoch, dates[0].getTime(), DELTA_MS);
        Assert.assertEquals(drainLine.endMsSinceEpoch, dates[2].getTime(), DELTA_MS);
    }

    public void testZeroPercentDischarge() {
        Date dates[] = SystemState.between(BEFORE, NOW, 6);
        Date bootTimestamp = dates[0];
        SystemState a = new SystemState(dates[1], 50, true, bootTimestamp);
        SystemState b = new SystemState(dates[2], 50, false, bootTimestamp);
        SystemState c = new SystemState(dates[3], 50, false, bootTimestamp);
        SystemState d = new SystemState(dates[4], 50, false, bootTimestamp);
        SystemState e = new SystemState(dates[5], 50, false, bootTimestamp);

        List<HistoryEvent> events = new LinkedList<>();
        events.addAll(b.getEventsSince(a));
        events.addAll(c.getEventsSince(b));
        events.addAll(d.getEventsSince(c));
        events.addAll(e.getEventsSince(d));

        DrainLinesCreator testMe = new DrainLinesCreator(events);
        Assert.assertEquals(0, testMe.getDrainLines().size());
    }

    /**
     * Regression test for https://github.com/walles/batterylogger/issues/1
     */
    public void testNegativeDischarge1() {
        Date dates[] = SystemState.between(BEFORE, NOW, 6);
        Date bootTimestamp = dates[0];
        SystemState a = new SystemState(dates[1], 50, true, bootTimestamp);
        SystemState b = new SystemState(dates[2], 50, false, bootTimestamp);
        // b -> c = charging
        SystemState c = new SystemState(dates[3], 51, false, bootTimestamp);
        // c -> d = draining
        SystemState d = new SystemState(dates[4], 50, false, bootTimestamp);
        // d -> e = charging
        SystemState e = new SystemState(dates[5], 51, false, bootTimestamp);

        List<HistoryEvent> events = new LinkedList<>();
        events.addAll(b.getEventsSince(a));
        events.addAll(c.getEventsSince(b));
        events.addAll(d.getEventsSince(c));
        events.addAll(e.getEventsSince(d));

        DrainLinesCreator testMe = new DrainLinesCreator(events);

        // Expect one draining line; the charging parts should just be ignored
        Assert.assertEquals(1, testMe.getDrainLines().size());
        final DrainSample drainLine = testMe.getDrainLines().get(0);
        Assert.assertTrue(drainLine.drainSpeed > 0.0);
    }

    /**
     * Regression test for https://github.com/walles/batterylogger/issues/2
     */
    public void testNegativeDischarge2() {
        Date dates[] = SystemState.between(BEFORE, NOW, 6);
        Date bootTimestamp = dates[0];
        SystemState a = new SystemState(dates[1], 50, true, bootTimestamp);
        SystemState b = new SystemState(dates[2], 50, false, bootTimestamp);
        // b -> c = charging
        SystemState c = new SystemState(dates[3], 51, false, bootTimestamp);
        // c -> d = draining
        SystemState d = new SystemState(dates[4], 50, false, bootTimestamp);
        // d -> e = charging
        SystemState e = new SystemState(dates[5], 51, false, bootTimestamp);

        List<HistoryEvent> events = new LinkedList<>();
        events.addAll(b.getEventsSince(a));
        events.addAll(d.getEventsSince(c));
        events.addAll(e.getEventsSince(d));

        DrainLinesCreator testMe = new DrainLinesCreator(events);

        // Expect zero draining lines; we're mostly discharging here
        Assert.assertEquals(0, testMe.getDrainLines().size());
    }

    public void testCharging() {
        DrainLinesCreator testMe = new DrainLinesCreator(Arrays.asList(
                HistoryEvent.createStartChargingEvent(BEFORE),
                HistoryEvent.createBatteryLevelEvent(NOW, 50)
        ));

        Assert.assertEquals(1, testMe.getDrainLines().size());

        DrainSample drainLine = testMe.getDrainLines().get(0);

        Assert.assertEquals(0.0, drainLine.drainSpeed, 0.0);
        Assert.assertEquals(drainLine.startMsSinceEpoch, BEFORE.getTime(), DELTA_MS);
        Assert.assertEquals(drainLine.endMsSinceEpoch, NOW.getTime(), DELTA_MS);
    }

    @SuppressWarnings("ConstantConditions")
    public void testAverage() {
        //noinspection ArraysAsListWithZeroOrOneArgument
        Assert.assertEquals(4.0, DrainLinesCreator.average(Arrays.asList(4.0)), 0.0);
        Assert.assertEquals(4.5, DrainLinesCreator.average(Arrays.asList(4.0, 5.0)), 0.0);
        Assert.assertEquals(5.0, DrainLinesCreator.average(Arrays.asList(4.0, 4.0, 7.0)), 0.0);
    }
}
