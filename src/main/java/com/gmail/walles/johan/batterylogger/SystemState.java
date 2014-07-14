package com.gmail.walles.johan.batterylogger;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Snapshot of system state containing
 * <ul>
 *     <li>Installed applications</li>
 *     <li>Battery level</li>
 *     <li>Charging / not-charging status</li>
 *     <li>OS fingerprint</li>
 *     <li>When this system was last booted</li>
 * </ul>
 */
public class SystemState {
    private static class InstalledApp {
        public final String packageName;
        public final String displayName;
        public final String versionName;

        public InstalledApp(String packageName, String displayName, String versionName) {
            this.packageName = packageName;
            this.displayName = displayName;
            this.versionName = versionName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            InstalledApp that = (InstalledApp) o;

            if (!displayName.equals(that.displayName)) return false;
            if (!packageName.equals(that.packageName)) return false;
            if (!versionName.equals(that.versionName)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = packageName.hashCode();
            result = 31 * result + displayName.hashCode();
            result = 31 * result + versionName.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "InstalledApp{" +
                    "packageName='" + packageName + '\'' +
                    ", displayName='" + displayName + '\'' +
                    ", versionName='" + versionName + '\'' +
                    '}';
        }
    }

    private final Date timestamp;
    private final int batteryPercentage;
    private final boolean charging;
    private final Date bootTimestamp;
    private final Map<String, InstalledApp> installedApps = new HashMap<String, InstalledApp>();

    public SystemState(Date timestamp, int batteryPercentage, boolean charging, Date bootTimestamp) {
        if (timestamp.before(bootTimestamp)) {
            throw new IllegalArgumentException("Sample timestamp must be after boot timestamp");
        }

        this.timestamp = timestamp;
        this.batteryPercentage = batteryPercentage;
        this.charging = charging;
        this.bootTimestamp = bootTimestamp;
    }

    public void addInstalledApp(String dottedName, String displayName, String versionName) {
        installedApps.put(dottedName, new InstalledApp(dottedName, displayName, versionName));
    }

    /**
     * Create amount dates between (but not including) t0 and t1.
     */
    static Date[] between(Date t0, Date t1, int amount) {
        Date returnMe[] = new Date[amount];
        long span = t1.getTime() - t0.getTime();
        for (int i = 0; i < amount; i++) {
            returnMe[i] = new Date(t0.getTime() + ((i + 1) * span) / (amount + 1));
        }
        return returnMe;
    }

    public Collection<HistoryEvent> getEventsSince(SystemState then) {
        if (timestamp.before(then.timestamp)) {
            throw new IllegalArgumentException("Timestamp of other state must be older than mine");
        }

        List<HistoryEvent> returnMe = new LinkedList<HistoryEvent>();

        if (!bootTimestamp.equals(then.bootTimestamp)) {
            returnMe.add(HistoryEvent.createSystemHaltingEvent(new Date(then.timestamp.getTime() + 1)));
            returnMe.add(HistoryEvent.createSystemBootingEvent(bootTimestamp));
        }

        if (batteryPercentage < then.batteryPercentage) {
            returnMe.add(HistoryEvent.createBatteryLevelEvent(timestamp, batteryPercentage));
        }

        if (charging && !then.charging) {
            returnMe.add(HistoryEvent.createInfoEvent(null, "Start charging"));
        }
        if (then.charging && !charging) {
            returnMe.add(HistoryEvent.createInfoEvent(null, "Stop charging"));
        }

        // Add dates to all events that need it
        int needTimestampCount = 0;
        for (HistoryEvent event : returnMe) {
            if (!event.isComplete()) {
                needTimestampCount++;
            }
        }
        if (needTimestampCount > 0) {
            Date timestamps[] = between(then.timestamp, timestamp, needTimestampCount);
            int nextFreeTimestampIndex = 0;
            for (HistoryEvent event : returnMe) {
                if (!event.isComplete()) {
                    event.setTimestamp(timestamps[nextFreeTimestampIndex++]);
                }
            }
        }

        // Now that everyone has their dates, sort!
        Collections.sort(returnMe);

        return returnMe;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "SystemState{" +
                "timestamp=" + timestamp +
                ", " + batteryPercentage + "%" +
                ", charging=" + charging +
                ", bootTimestamp=" + bootTimestamp +
                ", installedApps=" + installedApps +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(SystemState.class)) {
            return false;
        }

        SystemState that = (SystemState)o;
        if (this.charging != that.charging) {
            return false;
        }
        if (this.batteryPercentage != that.batteryPercentage) {
            return false;
        }
        if (!this.timestamp.equals(that.timestamp)) {
            return false;
        }
        if (!this.bootTimestamp.equals(that.bootTimestamp)) {
            return false;
        }

        if (!this.installedApps.equals(that.installedApps)) {
            return false;
        }

        return true;
    }

    public void writeToFile(File file) {

    }

    public static SystemState readFromFile(File file) {
        return null;
    }
}
