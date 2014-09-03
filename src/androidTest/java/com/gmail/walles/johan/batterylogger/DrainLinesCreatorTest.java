package com.gmail.walles.johan.batterylogger;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;

public class DrainLinesCreatorTest extends TestCase {
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

    public void testCornerCases() {
        DrainLinesCreator testMe = new DrainLinesCreator(Collections.<HistoryEvent>emptyList());
        assertEquals(0, testMe.getDrainLines().size());
    }
}