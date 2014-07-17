package com.gmail.walles.johan.batterylogger;

import junit.framework.TestCase;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

public class SystemStateTest extends TestCase {
    private final Date now = new Date();
    private final Date then = new Date(now.getTime() - History.FIVE_MINUTES_MS);
    private final Date bootTimestamp = new Date(then.getTime() - History.FIVE_MINUTES_MS);

    public void testConstructor() {
        try {
            new SystemState(then, 27, false, now);
            fail("Expected IAE when boot is in the future");
        } catch (IllegalArgumentException ignored) {
            // Expected exception intentionally ignored
        }
    }

    public void testEquals() {
        assertTrue(new SystemState(now, 27, true, bootTimestamp).equals(new SystemState(now, 27, true, bootTimestamp)));
        assertFalse(new SystemState(now, 27, true, bootTimestamp).equals(new SystemState(then, 27, true, bootTimestamp)));
        assertFalse(new SystemState(now, 27, true, bootTimestamp).equals(new SystemState(now, 36, true, bootTimestamp)));
        assertFalse(new SystemState(now, 27, true, bootTimestamp).equals(new SystemState(now, 27, false, bootTimestamp)));
        assertFalse(new SystemState(now, 27, true, bootTimestamp).equals(new SystemState(now, 27, false, then)));

        SystemState a = new SystemState(now, 27, false, bootTimestamp);
        a.addInstalledApp("a.b.c", "Griseknoa", "1.2.3");

        SystemState b = new SystemState(now, 27, false, bootTimestamp);
        b.addInstalledApp("a.b.c", "Griseknoa", "1.2.3");
        assertEquals(a, b);

        SystemState c = new SystemState(now, 27, false, bootTimestamp);
        c.addInstalledApp("x.y.z", "Griseknoa", "1.2.3");
        assertFalse(a.equals(c));

        SystemState d = new SystemState(now, 27, false, bootTimestamp);
        d.addInstalledApp("a.b.c", "Charles-Ingvar", "1.2.3");
        assertFalse(a.equals(d));

        SystemState e = new SystemState(now, 27, false, bootTimestamp);
        e.addInstalledApp("a.b.c", "Griseknoa", "4.5.6");
        assertFalse(a.equals(e));
    }

    public void testUnorderedEquals() {
        SystemState a = new SystemState(now, 27, false, bootTimestamp);
        a.addInstalledApp("a.b.c", "Griseknoa", "1.2.3");
        a.addInstalledApp("d.e.f", "Snickarboa", "4.5.6");

        SystemState b = new SystemState(now, 27, false, bootTimestamp);
        b.addInstalledApp("d.e.f", "Snickarboa", "4.5.6");
        b.addInstalledApp("a.b.c", "Griseknoa", "1.2.3");

        assertEquals(a, b);
    }

    private void assertEvents(Collection<HistoryEvent> testMe, HistoryEvent ... expected) {
        assertNotNull("Events must be non-null, were null", testMe);
        String expectedString = Arrays.toString(expected);
        String actualString = Arrays.toString(testMe.toArray());
        assertEquals(expectedString, actualString);
    }

    private void assertNoEvents(Collection<HistoryEvent> testMe) {
        assertEvents(testMe);
    }

    public void testBatteryEvent() {
        SystemState a = new SystemState(then, 27, false, bootTimestamp);

        SystemState b = new SystemState(now, 26, false, bootTimestamp);
        assertEvents(b.getEventsSince(a), HistoryEvent.createBatteryLevelEvent(now, 26));

        SystemState c = new SystemState(now, 28, false, bootTimestamp);
        assertNoEvents(c.getEventsSince(a));

        SystemState d = new SystemState(now, 29, false, bootTimestamp);
        assertNoEvents(d.getEventsSince(a));
    }

    public void testStartChargingEvent() {
        SystemState a = new SystemState(then, 27, false, bootTimestamp);
        SystemState b = new SystemState(now, 27, true, bootTimestamp);

        assertEvents(b.getEventsSince(a), HistoryEvent.createInfoEvent(between(then, now), "Start charging"));
    }

    public void testStopChargingEvent() {
        SystemState a = new SystemState(then, 27, true, bootTimestamp);
        SystemState b = new SystemState(now, 27, false, bootTimestamp);

        assertEvents(b.getEventsSince(a), HistoryEvent.createInfoEvent(between(then, now), "Stop charging"));
    }

    private static Date between(Date t0, Date t1) {
        return new Date((t0.getTime() + t1.getTime()) / 2);
    }

    public void testInstallEvent() {
        SystemState a = new SystemState(then, 27, false, bootTimestamp);

        SystemState b = new SystemState(now, 27, false, bootTimestamp);
        b.addInstalledApp("a.b.c", "ABC", "1.2.3");

        assertEvents(b.getEventsSince(a), HistoryEvent.createInfoEvent(between(then, now), "ABC 1.2.3 installed"));
    }

    public void testUninstallEvent() {
        SystemState a = new SystemState(then, 27, false, bootTimestamp);
        a.addInstalledApp("a.b.c", "ABC", "1.2.3");

        SystemState b = new SystemState(now, 27, false, bootTimestamp);

        assertEvents(b.getEventsSince(a), HistoryEvent.createInfoEvent(between(then, now), "ABC 1.2.3 uninstalled"));
    }

    public void testUpgradeEvent() {
        SystemState a = new SystemState(then, 27, false, bootTimestamp);
        a.addInstalledApp("a.b.c", "ABC", "1.2.3");

        SystemState b = new SystemState(now, 27, false, bootTimestamp);
        b.addInstalledApp("a.b.c", "ABC", "2.3.4");

        assertEvents(b.getEventsSince(a), HistoryEvent.createInfoEvent(between(then, now), "ABC upgraded from 1.2.3 to 2.3.4"));
    }

    public void testPersistence() throws Exception {
        File tempFile = File.createTempFile("systemstate-", ".txt");
        try {
            SystemState a = new SystemState(then, 27, false, bootTimestamp);
            a.addInstalledApp("a.b.c", "ABC", "1.2.3");

            a.writeToFile(tempFile);
            SystemState b = SystemState.readFromFile(tempFile);

            assertEquals(a, b);
        } finally {
            if (tempFile != null) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        }
    }

    public void testHaltAndBootEvents() {
        Date boot1 = new Date(0);
        Date sample1 = new Date(1000);
        Date boot2 = new Date(2000);
        Date sample2 = new Date(3000);

        SystemState beforeReboot = new SystemState(sample1, 27, false, boot1);
        SystemState afterReboot = new SystemState(sample2, 27, false, boot2);

        assertEvents(afterReboot.getEventsSince(beforeReboot),
                HistoryEvent.createSystemHaltingEvent(new Date(sample1.getTime() + 1)),
                HistoryEvent.createSystemBootingEvent(boot2));
    }

    public void testHaltAndBootAndChargeEvents() {
        Date boot1 = new Date(0);
        Date sample1 = new Date(1000);
        Date boot2 = new Date(2000);
        Date sample2 = new Date(3000);

        SystemState beforeReboot = new SystemState(sample1, 27, false, boot1);
        SystemState afterReboot = new SystemState(sample2, 27, true, boot2);

        assertEvents(afterReboot.getEventsSince(beforeReboot),
                HistoryEvent.createSystemHaltingEvent(new Date(sample1.getTime() + 1)),
                HistoryEvent.createInfoEvent(between(sample1, boot2), "Start charging"),
                HistoryEvent.createSystemBootingEvent(boot2));
    }

    /**
     * Verify that different events resulting in text in the graph don't overlap each other.
     */
    public void testPreventOverlappingEvents() {
        SystemState a = new SystemState(then, 27, true, bootTimestamp);
        a.addInstalledApp("a.b.c", "Upgrader", "1.2.3");
        a.addInstalledApp("d.e.f", "Remover", "2.3.4");

        SystemState b = new SystemState(now, 26, false, bootTimestamp);
        b.addInstalledApp("a.b.c", "Upgrader", "1.2.5");
        b.addInstalledApp("g.h.i", "Adder", "5.6.7");

        Date datesBetween[] = SystemState.between(then, now, 4);
        // Note that the actual order here is arbitrary
        assertEvents(b.getEventsSince(a),
                HistoryEvent.createInfoEvent(datesBetween[0], "Stop charging"),
                HistoryEvent.createInfoEvent(datesBetween[1], "Adder 5.6.7 installed"),
                HistoryEvent.createInfoEvent(datesBetween[2], "Remover 2.3.4 uninstalled"),
                HistoryEvent.createInfoEvent(datesBetween[3], "Upgrader upgraded from 1.2.3 to 1.2.5"),
                HistoryEvent.createBatteryLevelEvent(now, 26));
    }

    public void testBetween() {
        Date dates[] = SystemState.between(then, now, 1);
        assertEquals(1, dates.length);
        assertEquals(between(then, now), dates[0]);
    }

    /**
     * Two SystemStates should be equal even if their boot timestamps are a little bit off. Since the boot timestamps
     * are calculated will millisecond precision we need some margin of error.
     */
    public void testBootTimeLeniency() {
        SystemState a = new SystemState(now, 27, false, new Date(0));
        SystemState b = new SystemState(now, 27, false, new Date(10 * 1000));
        assertEquals(a, b);

        SystemState c = new SystemState(now, 27, false, new Date(100 * 1000));
        assertFalse(a.equals(c));
    }
}