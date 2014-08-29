package com.gmail.walles.johan.batterylogger;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.gmail.walles.johan.batterylogger.MainActivity.TAG;

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

    private final Date timestamp;

    public int getBatteryPercentage() {
        return batteryPercentage;
    }

    private final int batteryPercentage;
    private final boolean charging;
    private final Date bootTimestamp;

    public Collection<InstalledApp> getInstalledApps() {
        return installedApps.values();
    }

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

    /**
     * @param events Packaging events will be added to this collection
     */
    private void addPackagingEventsSince(SystemState then, Collection<HistoryEvent> events) {
        Set<String> added = new HashSet<String>(installedApps.keySet());
        added.removeAll(then.installedApps.keySet());
        for (String dottedName : added) {
            InstalledApp installedApp = installedApps.get(dottedName);
            events.add(HistoryEvent.createInfoEvent(null,
                    installedApp.displayName + " " + installedApp.versionName + " installed"));
        }

        Set<String> removed = new HashSet<String>(then.installedApps.keySet());
        removed.removeAll(installedApps.keySet());
        for (String dottedName : removed) {
            InstalledApp installedApp = then.installedApps.get(dottedName);
            events.add(HistoryEvent.createInfoEvent(null,
                    installedApp.displayName + " " + installedApp.versionName + " uninstalled"));
        }

        Set<String> retained = new HashSet<String>(installedApps.keySet());
        retained.retainAll(then.installedApps.keySet());
        for (String dottedName : retained) {
            InstalledApp installedThen = then.installedApps.get(dottedName);
            InstalledApp installedNow = installedApps.get(dottedName);
            if (installedThen.equals(installedNow)) {
                continue;
            }
            events.add(HistoryEvent.createInfoEvent(null,
                    installedNow.displayName
                            + " upgraded from " + installedThen.versionName
                            + " to " + installedNow.versionName));
        }
    }

    public Collection<HistoryEvent> getEventsSince(SystemState then) {
        if (timestamp.before(then.timestamp)) {
            throw new IllegalArgumentException("Timestamp of other state must be older than mine");
        }

        List<HistoryEvent> returnMe = new LinkedList<HistoryEvent>();

        boolean reboot = false;
        if (!bootTimestampsMatch(then)) {
            returnMe.add(HistoryEvent.createSystemHaltingEvent(new Date(then.timestamp.getTime() + 1)));
            returnMe.add(HistoryEvent.createSystemBootingEvent(bootTimestamp, charging));
            reboot = true;
        }

        if (batteryPercentage < then.batteryPercentage) {
            returnMe.add(HistoryEvent.createBatteryLevelEvent(timestamp, batteryPercentage));
        }

        if (!reboot) {
            HistoryEvent chargingEvent = null;
            if (charging && !then.charging) {
                chargingEvent = HistoryEvent.createStartChargingEvent(null);
            }
            if (then.charging && !charging) {
                chargingEvent = HistoryEvent.createStopChargingEvent(null);
            }
            if (chargingEvent != null) {
                returnMe.add(chargingEvent);
            }
        }

        addPackagingEventsSince(then, returnMe);

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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean bootTimestampsMatch(SystemState that) {
        long bootMsDelta = Math.abs(this.bootTimestamp.getTime() - that.bootTimestamp.getTime());
        if (bootMsDelta > 20 * 1000) {
            // Boot time differs by more than 20s
            return false;
        }
        return true;
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
        if (!bootTimestampsMatch(that)) {
            return false;
        }

        if (!this.installedApps.equals(that.installedApps)) {
            return false;
        }

        return true;
    }

    public void writeToFile(File file) throws IOException {
        File tmp = new File(file.getAbsolutePath() + ".tmp");

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(tmp));

            writer.println(timestamp.getTime());
            writer.println(batteryPercentage);
            writer.println(bootTimestamp.getTime());
            writer.println(charging);
            for (InstalledApp app : installedApps.values()) {
                app.println(writer);
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        if (!tmp.renameTo(file)) {
            throw new IOException("Rename failed: " + tmp.getAbsolutePath() + "->" + file.getAbsolutePath());
        }
    }

    public static SystemState readFromFile(File file) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            Date timestamp = new Date(Long.valueOf(reader.readLine()));
            int batteryPercentage = Integer.valueOf(reader.readLine());
            Date bootTimestamp = new Date(Long.valueOf(reader.readLine()));
            boolean charging = Boolean.valueOf(reader.readLine());
            SystemState returnMe = new SystemState(timestamp, batteryPercentage, charging, bootTimestamp);

            while (true) {
                InstalledApp app = InstalledApp.readLines(reader);
                if (app == null) {
                    break;
                }

                returnMe.addInstalledApp(app.dottedName, app.displayName, app.versionName);
            }

            return returnMe;
        } catch (NumberFormatException e) {
            IOException throwMe = new IOException("Number parsing failed");
            throwMe.initCause(e);
            throw throwMe;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
     * When was the currently running system booted?
     */
    static Date computeCurrentBootTimestamp() {
        return new Date(System.currentTimeMillis() - SystemClock.elapsedRealtime());
    }

    public static SystemState readFromSystem(Context context) throws IOException {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);
        if (batteryStatus == null) {
            throw new IOException("Battery status unavailable");
        }

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        if (status == -1) {
            throw new IOException("Battery charging status unavailable");
        }
        boolean charging =
                (status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL);

        int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        if (batteryLevel == -1) {
            throw new IOException("Battery level unavailable");
        }
        int batteryScale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (batteryScale == -1) {
            throw new IOException("Battery scale unavailable");
        }
        int batteryPercentage = (100 * batteryLevel) / batteryScale;

        SystemState returnMe =
                new SystemState(new Date(), batteryPercentage, charging, computeCurrentBootTimestamp());

        // Add installed apps
        PackageManager packageManager = context.getPackageManager();
        if (packageManager == null) {
            throw new IOException("Package manager not available");
        }
        List<PackageInfo> installedPackages = packageManager.getInstalledPackages(0);
        for (PackageInfo packageInfo : installedPackages) {
            if (packageInfo.applicationInfo == null) {
                throw new IOException(
                        "Package info without application info: "
                                + packageInfo.packageName + " "
                                + packageInfo.versionName);
            }

            String dottedName = packageInfo.applicationInfo.packageName;
            if (dottedName == null) {
                throw new IOException("Dotted name unavailable for: "
                        + packageInfo.packageName + " "
                        + packageInfo.versionName);
            }

            CharSequence displayName = packageInfo.applicationInfo.loadLabel(packageManager);
            if (displayName == null) {
                displayName = dottedName;
            }

            String versionName = packageInfo.versionName;
            if (versionName == null || versionName.length() == 0) {
                versionName = Integer.toString(packageInfo.versionCode);
            }

            returnMe.addInstalledApp(
                    dottedName,
                    displayName.toString(),
                    versionName);
        }

        // Add OS fingerprint
        returnMe.addInstalledApp(
                "an.identifier.for.the.os.build.griseknoa",
                "Android OS",
                Build.FINGERPRINT);

        Log.v(TAG, String.format("System state sampled: %s, %d%%, %d apps",
                charging ? "charging" : "not charging",
                batteryPercentage,
                returnMe.getAppCount()));

        return returnMe;
    }

    public int getAppCount() {
        return installedApps.size();
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public Date getBootTimestamp() {
        return bootTimestamp;
    }
}
