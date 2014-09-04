package com.gmail.walles.johan.batterylogger;

import com.androidplot.xy.XYSeries;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

public class DrainLinesCreatorTest extends TestCase {
    private static final Date THEN = new Date(System.currentTimeMillis() - 86400 * 1000);
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
                HistoryEvent.createBatteryLevelEvent(THEN, 50)
        ));
        assertEquals(0, testMe.getDrainLines().size());
    }

    public void testDischargeLineWithTwoEvents() {
        Date dates[] = SystemState.between(THEN, NOW, 3);
        DrainLinesCreator testMe = new DrainLinesCreator(Arrays.asList(
                HistoryEvent.createStopChargingEvent(dates[0]),
                HistoryEvent.createBatteryLevelEvent(dates[1], 50),
                HistoryEvent.createBatteryLevelEvent(dates[2], 40)
        ));
        assertEquals(1, testMe.getDrainLines().size());

        XYSeries drainLine = testMe.getDrainLines().get(0);
        assertEquals(2, drainLine.size());

        Number y = drainLine.getY(0);
        assertEquals(y, drainLine.getY(1));
        assertEquals(drainLine.getX(0).doubleValue(), History.toDouble(dates[0]));
        assertEquals(drainLine.getX(1).doubleValue(), History.toDouble(dates[2]));
    }
}