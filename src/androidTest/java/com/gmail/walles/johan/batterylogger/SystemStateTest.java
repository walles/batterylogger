package com.gmail.walles.johan.batterylogger;

import junit.framework.TestCase;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

public class SystemStateTest extends TestCase {
    private final Date now = new Date();
    private final Date then = new Date(System.currentTimeMillis() - History.FIVE_MINUTES_MS);

    public void testEquals() {
        assertTrue(new SystemState(now, 27, true).equals(new SystemState(now, 27, true)));
        assertFalse(new SystemState(now, 27, true).equals(new SystemState(then, 27, true)));
        assertFalse(new SystemState(now, 27, true).equals(new SystemState(now, 36, true)));
        assertFalse(new SystemState(now, 27, true).equals(new SystemState(now, 27, false)));

        SystemState a = new SystemState(now, 27, false);
        a.addInstalledApp("a.b.c", "Griseknoa", "1.2.3");

        SystemState b = new SystemState(now, 27, false);
        b.addInstalledApp("a.b.c", "Griseknoa", "1.2.3");
        assertEquals(a, b);

        SystemState c = new SystemState(now, 27, false);
        c.addInstalledApp("x.y.z", "Griseknoa", "1.2.3");
        assertFalse(a.equals(c));

        SystemState d = new SystemState(now, 27, false);
        d.addInstalledApp("a.b.c", "Charles-Ingvar", "1.2.3");
        assertFalse(a.equals(d));

        SystemState e = new SystemState(now, 27, false);
        e.addInstalledApp("a.b.c", "Griseknoa", "4.5.6");
        assertFalse(a.equals(e));
    }

    private void assertEvents(Collection<HistoryEvent> testMe, HistoryEvent ... expected) {
        String expectedString = Arrays.toString(expected);
        String actualString = Arrays.toString(testMe.toArray());
        assertEquals(expectedString, actualString);
    }

    private void assertNoEvents(Collection<HistoryEvent> testMe) {
        assertEvents(testMe);
    }

    public void testBatteryEvent() {
        SystemState a = new SystemState(then, 27, false);

        SystemState b = new SystemState(now, 26, false);
        assertEvents(b.getEventsSince(a), HistoryEvent.createBatteryLevelEvent(now, 26));

        SystemState c = new SystemState(now, 28, false);
        assertNoEvents(c.getEventsSince(a));

        SystemState d = new SystemState(now, 29, false);
        assertNoEvents(d.getEventsSince(a));
    }

    public void testStartChargingEvent() {
        SystemState a = new SystemState(then, 27, false);
        SystemState b = new SystemState(now, 27, true);

        Date betweenThenAndNow = new Date((now.getTime() + then.getTime()) / 2);

        assertEvents(b.getEventsSince(a), HistoryEvent.createInfoEvent(betweenThenAndNow, "Start charging"));
    }

    public void testStopChargingEvent() {
        SystemState a = new SystemState(then, 27, true);
        SystemState b = new SystemState(now, 27, false);

        Date betweenThenAndNow = new Date((now.getTime() + then.getTime()) / 2);

        assertEvents(b.getEventsSince(a), HistoryEvent.createInfoEvent(betweenThenAndNow, "Stop charging"));
    }

    public void testInstallEvent() {
        SystemState a = new SystemState(then, 27, false);

        SystemState b = new SystemState(now, 27, false);
        b.addInstalledApp("a.b.c", "ABC", "1.2.3");

        Date betweenThenAndNow = new Date((now.getTime() + then.getTime()) / 2);

        assertEvents(b.getEventsSince(a), HistoryEvent.createInfoEvent(betweenThenAndNow, "ABC 1.2.3 installed"));
    }

    public void testUninstallEvent() {
        SystemState a = new SystemState(then, 27, false);
        a.addInstalledApp("a.b.c", "ABC", "1.2.3");

        SystemState b = new SystemState(now, 27, false);

        Date betweenThenAndNow = new Date((now.getTime() + then.getTime()) / 2);

        assertEvents(b.getEventsSince(a), HistoryEvent.createInfoEvent(betweenThenAndNow, "ABC 1.2.3 uninstalled"));
    }

    public void testUpgradeEvent() {
        SystemState a = new SystemState(then, 27, false);
        a.addInstalledApp("a.b.c", "ABC", "1.2.3");

        SystemState b = new SystemState(now, 27, false);
        b.addInstalledApp("a.b.c", "ABC", "2.3.4");

        Date betweenThenAndNow = new Date((now.getTime() + then.getTime()) / 2);

        assertEvents(b.getEventsSince(a), HistoryEvent.createInfoEvent(betweenThenAndNow, "ABC upgraded from 1.2.3 to 2.3.4"));
    }

    public void testPersistence() throws Exception {
        File tempFile = File.createTempFile("systemstate-", ".txt");
        try {
            SystemState a = new SystemState(then, 27, false);
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
}