/*
 * Copyright 2015 Johan Walles <johan.walles@gmail.com>
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import timber.log.Timber;

/**
 * Functionality for recording and accessing events logged by the app.
 */
public class LogCollector {
    /**
     * Make sure logs are being collected. Call periodically or at app /service startup.
     */
    public static void keepAlive(Context context) {
        if (isAlive(context)) {
            return;
        }

        // Kill any defunct log collecting processes before starting a new one
        kill(context);

        File logfile = getLogFile(context);
        try {
            Runtime.getRuntime().exec(createLogcatCommandLine(context));

            Timber.i("Background logcat started, logging into %s", logfile.getParent());
        } catch (IOException e) {
            Timber.e(e, "Executing logcat failed");
        }
    }

    private static Date getLatestLogMessageTimestamp(Context context) {
        // 0 == not found == at the epoch
        long newest = 0;

        File[] logFiles = getLogDir(context).listFiles();
        for (File logfile : logFiles) {
            if (!logfile.isFile()) {
                continue;
            }

            if (!logfile.getName().startsWith("log")) {
                continue;
            }

            newest = Math.max(newest, logfile.lastModified());
        }

        return new Date(newest);
    }

    static boolean isAlive(Context context) {
        long latestLogMessageAgeMs =
                System.currentTimeMillis() - getLatestLogMessageTimestamp(context).getTime();
        if (latestLogMessageAgeMs < -1000) {
            Timber.w("Log collector last collected %dms in the future", -latestLogMessageAgeMs);
        } else if (latestLogMessageAgeMs < 10000) {
            Timber.v("Log collector last collected %dms ago, assuming alive", latestLogMessageAgeMs);
            return true;
        }

        if (getLogCollectorPids(context).isEmpty()) {
            Timber.v("No log collector processes found, assuming dead");
            return false;
        }

        // Log a canary message
        Timber.v("Checking whether the log collector is up...");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Timber.w(e, "Sleeping for log message to be collected failed");
        }

        latestLogMessageAgeMs =
                System.currentTimeMillis() - getLatestLogMessageTimestamp(context).getTime();
        if (latestLogMessageAgeMs < 10000) {
            // We recently caught a message
            Timber.v("It's alive");
            return true;
        }

        Timber.v("It's dead");
        return false;
    }

    /**
     * Create a logcat command line for rotating logs into where
     * {@link #getLogFile(android.content.Context)} points.
     */
    private static String[] createLogcatCommandLine(Context context) {
        return new String[] {
                "logcat",
                "-v", "time",
                "-f", getLogFile(context).getAbsolutePath(),
                "-n", "3",
                "-r", "16"
        };
    }

    /**
     * This method decides where logcat should rotate its logs into.
     */
    private static File getLogFile(Context context) {
        return new File(getLogDir(context), "log");
    }

    /**
     * This method decides where logcat should rotate its logs into.
     */
    private static File getLogDir(Context context) {
        return context.getDir("logs", Context.MODE_PRIVATE);
    }

    private static Collection<Integer> getLogCollectorPids(Context context) {
        long t0 = System.currentTimeMillis();

        final String logcatCommandLine[] = createLogcatCommandLine(context);

        Collection<Integer> pids = new LinkedList<>();
        for (File directory : new File("/proc").listFiles()) {
            try {
                if (!"logcat".equals(new File(directory, "exe").getCanonicalFile().getName())) {
                    continue;
                }
            } catch (IOException e) {
                // Permission denied; not our logcat
                continue;
            }

            File cmdline = new File(directory, "cmdline");

            BufferedReader cmdlineReader;
            try {
                cmdlineReader = new BufferedReader(new FileReader(cmdline));
            } catch (FileNotFoundException e) {
                continue;
            }
            try {
                String line = cmdlineReader.readLine();
                if (line == null) {
                    continue;
                }
                String processCommandLine[] = line.split("\0");
                if (!Arrays.equals(logcatCommandLine, processCommandLine)) {
                    continue;
                }

                try {
                    pids.add(Integer.valueOf(directory.getName()));
                } catch (NumberFormatException e) {
                    Timber.w("Couldn't parse into pid: %s", directory.getName());
                    continue;
                }
            } catch (IOException e) {
                Timber.w("Reading command line failed: %s", cmdline.getAbsolutePath());
                continue;
            } finally {
                try {
                    cmdlineReader.close();
                } catch (IOException e) {
                    // Closing is a best-effort operation, this exception intentionally ignored
                    Timber.w(e, "Failed to close %s", cmdline);
                }
            }
        }

        long t1 = System.currentTimeMillis();
        long deltaMs = t1 - t0;
        Timber.v("Listing logcat PIDs took %dms", deltaMs);

        return pids;
    }

    /**
     * Find and kill any old logcat invocations by us that are running on the system.
     */
    static void kill(Context context) {
        long t0 = System.currentTimeMillis();
        Timber.d("Cleaning up old logcat processes...");
        int killCount = 0;
        for (Integer pid: getLogCollectorPids(context)) {
            Timber.i("Killing old logcat process: %d", pid);
            android.os.Process.killProcess(pid);
            killCount++;
        }

        long t1 = System.currentTimeMillis();
        Timber.i("Killed %d old logcats in %dms", killCount, t1 - t0);
    }

    public static CharSequence readLogs(Context context) {
        File[] allFiles = getLogDir(context).listFiles();
        List<File> logFiles = new LinkedList<>();
        for (File file : allFiles) {
            if (!file.isFile()) {
                continue;
            }

            if (!file.getName().startsWith("log")) {
                continue;
            }

            logFiles.add(file);
        }

        Collections.sort(logFiles);
        Collections.reverse(logFiles);

        StringBuilder returnMe = new StringBuilder();
        for (File logFile : logFiles) {
            returnMe.append("Log file: ");
            returnMe.append(logFile.getAbsolutePath());
            returnMe.append("\n");
            try {
                // Scanner trick to read whole file into string from:
                // http://stackoverflow.com/a/7449797/473672
                returnMe.append(new Scanner(logFile).useDelimiter("\\A").next());
            } catch (NoSuchElementException e) {
                // NoSuchElementException is what Scanner.next() throws if the file is empty:
                // http://stackoverflow.com/questions/3402735/what-is-simplest-way-to-read-a-file-into-string#comment-36399547
                //
                // Just ignore this.
            } catch (FileNotFoundException e) {
                Timber.e(e, "Reading log file failed: %s", logFile.getAbsolutePath());

                StringWriter stringWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stringWriter));
                returnMe.append(stringWriter.getBuffer());
            }
        }
        return returnMe;
    }
}
