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

import com.androidplot.xy.XYSeries;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class DrainLinesCreatorTest extends TestCase {
    private static final Date BEFORE = new Date(System.currentTimeMillis() - 86400 * 1000);
    private static final Date NOW = new Date();

    public void testMedianLine() {
        try {
            DrainLinesCreator.median(Collections.<Double>emptyList());
            fail("Computing median of one number should fail");
        } catch (IllegalArgumentException e) {
            // Expected exception intentionally ignored
        }

        assertEquals(5.0, DrainLinesCreator.median(Arrays.asList(5.0)));
        assertEquals(5.5, DrainLinesCreator.median(Arrays.asList(5.0, 6.0)));
        assertEquals(6.0, DrainLinesCreator.median(Arrays.asList(5.0, 6.0, 7.0)));
        assertEquals(6.5, DrainLinesCreator.median(Arrays.asList(5.0, 6.0, 7.0, 7.5)));
    }

    public void testWithZeroEvents() {
        DrainLinesCreator testMe = new DrainLinesCreator(Collections.<HistoryEvent>emptyList());
        assertEquals(0, testMe.getDrainLines().size());
    }

    public void testWithOneEvent() {
        DrainLinesCreator testMe = new DrainLinesCreator(Arrays.asList(
                HistoryEvent.createBatteryLevelEvent(NOW, 50)
        ));
        assertEquals(0, testMe.getDrainLines().size());
    }

    public void testDischargeLineWithZeroEvents() {
        DrainLinesCreator testMe = new DrainLinesCreator(Arrays.asList(
                HistoryEvent.createStopChargingEvent(NOW)
        ));
        assertEquals(0, testMe.getDrainLines().size());
    }

    public void testDischargeLineWithOneEvent() {
        DrainLinesCreator testMe = new DrainLinesCreator(Arrays.asList(
                HistoryEvent.createStopChargingEvent(NOW),
                HistoryEvent.createBatteryLevelEvent(BEFORE, 50)
        ));
        assertEquals(0, testMe.getDrainLines().size());
    }

    public void testDischargeLineWithTwoEvents() {
        Date dates[] = SystemState.between(BEFORE, NOW, 3);
        DrainLinesCreator testMe = new DrainLinesCreator(Arrays.asList(
                HistoryEvent.createStopChargingEvent(dates[0]),
                HistoryEvent.createBatteryLevelEvent(dates[1], 50),
                HistoryEvent.createBatteryLevelEvent(dates[2], 40)
        ));
        assertEquals(1, testMe.getDrainLines().size());

        XYSeries drainLine = testMe.getDrainLines().get(0);
        assertEquals(2, drainLine.size());

        Number y = drainLine.getY(0);
        assertTrue(y.doubleValue() > 0.0);
        assertFalse(Double.isInfinite(y.doubleValue()));

        assertEquals(y, drainLine.getY(1));
        assertEquals(drainLine.getX(0).doubleValue(), History.toDouble(dates[0]));
        assertEquals(drainLine.getX(1).doubleValue(), History.toDouble(dates[2]));
    }

    public void testZeroPercentDischarge() {
        Date dates[] = SystemState.between(BEFORE, NOW, 6);
        Date bootTimestamp = dates[0];
        SystemState a = new SystemState(dates[1], 50, true, bootTimestamp);
        SystemState b = new SystemState(dates[2], 50, false, bootTimestamp);
        SystemState c = new SystemState(dates[3], 50, false, bootTimestamp);
        SystemState d = new SystemState(dates[4], 50, false, bootTimestamp);
        SystemState e = new SystemState(dates[5], 50, false, bootTimestamp);

        List<HistoryEvent> events = new LinkedList<HistoryEvent>();
        events.addAll(b.getEventsSince(a));
        events.addAll(c.getEventsSince(b));
        events.addAll(d.getEventsSince(c));
        events.addAll(e.getEventsSince(c));

        DrainLinesCreator testMe = new DrainLinesCreator(events);
        assertEquals(0, testMe.getDrainLines().size());
    }

    public void testCharging() {
        DrainLinesCreator testMe = new DrainLinesCreator(Arrays.asList(
                HistoryEvent.createStartChargingEvent(BEFORE),
                HistoryEvent.createBatteryLevelEvent(NOW, 50)
        ));

        assertEquals(1, testMe.getDrainLines().size());

        XYSeries drainLine = testMe.getDrainLines().get(0);
        assertEquals(2, drainLine.size());

        Number y = drainLine.getY(0);
        assertEquals(0.0, y.doubleValue());
        assertEquals(y, drainLine.getY(1));
        assertEquals(drainLine.getX(0).doubleValue(), History.toDouble(BEFORE));
        assertEquals(drainLine.getX(1).doubleValue(), History.toDouble(NOW));
    }

    public void testAverage() {
        assertEquals(4.0, DrainLinesCreator.average(Arrays.asList(4.0)));
        assertEquals(4.5, DrainLinesCreator.average(Arrays.asList(4.0, 5.0)));
        assertEquals(5.0, DrainLinesCreator.average(Arrays.asList(4.0, 4.0, 7.0)));
    }
}