package com.gmail.walles.johan.batterylogger;

import java.io.File;
import java.util.Collection;
import java.util.Date;

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
    public SystemState(Date timestamp, int batteryPercentage, boolean charging, Date bootTimestamp) {
        if (timestamp.before(bootTimestamp)) {
            throw new IllegalArgumentException("Sample timestamp must be after boot timestamp");
        }
    }

    public void addInstalledApp(String dottedName, String displayName, String versionName) {

    }

    public Collection<HistoryEvent> getEventsSince(SystemState then) {
        return null;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    public void writeToFile(File file) {

    }

    public static SystemState readFromFile(File file) {
        return null;
    }
}
