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

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.crashlytics.android.answers.CustomEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import timber.log.Timber;

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

    private static final long TWO_HOURS_MS = 2 * 60 * 60 * 1000;

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

    private final Map<String, InstalledApp> installedApps = new HashMap<>();

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
        Set<String> added = new HashSet<>(installedApps.keySet());
        added.removeAll(then.installedApps.keySet());
        for (String dottedName : added) {
            InstalledApp installedApp = installedApps.get(dottedName);
            events.add(HistoryEvent.createInfoEvent(null,
                    installedApp.displayName + " " + installedApp.versionName + " installed"));
        }

        Set<String> removed = new HashSet<>(then.installedApps.keySet());
        removed.removeAll(installedApps.keySet());
        for (String dottedName : removed) {
            InstalledApp installedApp = then.installedApps.get(dottedName);
            events.add(HistoryEvent.createInfoEvent(null,
                    installedApp.displayName + " " + installedApp.versionName + " uninstalled"));
        }

        Set<String> retained = new HashSet<>(installedApps.keySet());
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

    private String toIsoString(Date date) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.ENGLISH);
        df.setTimeZone(tz);
        return df.format(date);
    }

    private String toDescriptionString() {
        InstalledApp batterylogger = installedApps.get("com.gmail.walles.johan.batterylogger");
        String batteryLoggerVersion;
        if (batterylogger != null) {
            batteryLoggerVersion = batterylogger.versionName;
        } else {
            batteryLoggerVersion = "<Not installed>";
        }
        return String.format(Locale.ENGLISH, "Timestamp: %s, Boot: %s, Version: %s",
            toIsoString(timestamp),
            toIsoString(bootTimestamp),
            batteryLoggerVersion);
    }

    private void logSamplingGap(SystemState before) {
        Timber.w(new RuntimeException(),
            "System sampling gap unexpectedly large:\nnow:  %s\nthen: %s",
            toDescriptionString(),
            before.toDescriptionString());
    }

    public Collection<HistoryEvent> getEventsSince(SystemState before) {
        if (before.timestamp.after(timestamp)) {
            throw new IllegalArgumentException(
                String.format("Timestamp of previous state (%s) must be before mine (%s)",
                    before.timestamp.toString(), timestamp.toString()));
        }

        if (timestamp.getTime() - before.timestamp.getTime() > TWO_HOURS_MS) {
            logSamplingGap(before);
        }

        List<HistoryEvent> returnMe = new LinkedList<>();

        boolean reboot = false;
        if (!bootTimestampsMatch(before)) {
            returnMe.add(HistoryEvent.createSystemHaltingEvent(new Date(before.timestamp.getTime() + 1)));
            returnMe.add(HistoryEvent.createSystemBootingEvent(bootTimestamp, charging));
            reboot = true;
        }

        boolean discharging = !charging;
        boolean wasDischarging = !before.charging;
        if ((discharging || wasDischarging) && batteryPercentage != before.batteryPercentage) {
            returnMe.add(HistoryEvent.createBatteryLevelEvent(timestamp, batteryPercentage));
        }

        if (!reboot) {
            HistoryEvent chargingEvent = null;
            if (charging && !before.charging) {
                chargingEvent = HistoryEvent.createStartChargingEvent(null);
            }
            if (before.charging && !charging) {
                chargingEvent = HistoryEvent.createStopChargingEvent(null);
            }
            if (chargingEvent != null) {
                returnMe.add(chargingEvent);
            }
        }

        addPackagingEventsSince(before, returnMe);

        // Add dates to all events that need it
        int needTimestampCount = 0;
        for (HistoryEvent event : returnMe) {
            if (!event.isComplete()) {
                needTimestampCount++;
            }
        }
        if (needTimestampCount > 0) {
            Date timestamps[] = between(before.timestamp, timestamp, needTimestampCount);
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

    private String describeFile(File file) {
        if (!file.exists()) {
            return file.getAbsolutePath() + " doesn't exist";
        }

        String type;
        if (file.isDirectory()) {
            type = "directory";
        } else if (file.isFile()) {
            type = "file";
        } else {
            type = "neither dir nor file";
        }

        String readable = file.canRead() ? "readable" : "not readable";
        String writable = file.canWrite() ? "writable" : "not writable";
        String executable = file.canRead() ? "executable" : "not executable";

        return String.format(Locale.ENGLISH, "%s: %s, %s, %s, %s, %d",
            file, type, readable, writable, executable, file.length());
    }

    private void renameFile(File from, File to) throws IOException {
        if (from.renameTo(to)) {
            return;
        }

        throw new IOException(String.format(
            "Rename failed: %s->%s\n%s\n%s",
            from.getAbsolutePath(),
            to.getAbsolutePath(),
            describeFile(from),
            describeFile(to)));
    }

    public void writeToFile(File file) throws IOException {
        File tmp = new File(file.getAbsolutePath() + ".tmp");

        FileWriter writer = null;
        try {
            writer = new FileWriter(tmp);

            writer.append(Long.toString(timestamp.getTime())).append("\n");
            writer.append(Integer.toString(batteryPercentage)).append("\n");
            writer.append(Long.toString(bootTimestamp.getTime())).append("\n");
            writer.append(Boolean.toString(charging)).append("\n");
            for (InstalledApp app : installedApps.values()) {
                app.println(writer);
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        renameFile(tmp, file);
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

    public static Date getBootTimestamp() {
        return new Date(System.currentTimeMillis() - SystemClock.elapsedRealtime());
    }

    @NonNull
    private static List<PackageInfo> getInstalledPackages(PackageManager pm) throws IOException {
        try {
            return pm.getInstalledPackages(0);
        } catch (Exception e) {
            Timber.w("Getting installed packages Plan A failed: <%s>", e.getMessage());
        }

        return getInstalledPackagesManually(pm);
    }

    @NonNull
    // Workaround for https://code.google.com/p/android/issues/detail?id=69276
    // From: http://stackoverflow.com/a/30062632/473672
    private static List<PackageInfo> getInstalledPackagesManually(PackageManager pm) throws IOException {
        Process process;
        List<PackageInfo> result = new ArrayList<>();
        BufferedReader bufferedReader = null;
        try {
            process = Runtime.getRuntime().exec("pm list packages");
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                final String packageName = line.substring(line.indexOf(':') + 1);
                final PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
                result.add(packageInfo);
            }
            process.waitFor();
        } catch (InterruptedException | PackageManager.NameNotFoundException e) {
            throw new IOException("Listing installed packages Plan B failed", e);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    Timber.w(e, "Closing stream from \"pm\" command failed");
                }
            }
        }

        if (result.isEmpty()) {
            throw new IOException("Listing installed packages Plan B didn't find any packages");
        }

        return result;
    }

    public static SystemState readFromSystem(Context context) throws IOException {
        long t0 = System.currentTimeMillis();

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

        SystemState returnMe = new SystemState(new Date(), batteryPercentage, charging, getBootTimestamp());

        // Add installed apps
        PackageManager packageManager = context.getPackageManager();
        if (packageManager == null) {
            throw new IOException("Package manager not available");
        }
        List<PackageInfo> installedPackages = getInstalledPackages(packageManager);
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

        long t1 = System.currentTimeMillis();
        long dtMillis = t1 - t0;
        Timber.v("System state sampled in %dms: %s, %d%%, %d apps",
                dtMillis,
                charging ? "charging" : "not charging",
                batteryPercentage,
                returnMe.getAppCount());
        LoggingUtils.logCustom(new CustomEvent("Sampling").putCustomAttribute(
            "Duration (seconds)",
            dtMillis / 1000.0));

        return returnMe;
    }

    public int getAppCount() {
        return installedApps.size();
    }
}
