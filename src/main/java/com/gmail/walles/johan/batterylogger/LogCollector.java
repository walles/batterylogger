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
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * Functionality for recording and accessing events logged by the app.
 */
public class LogCollector {
    private static final String TAG = "LogCollector";
    
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

            Log.i(TAG, "Background logcat started, logging into " + logfile.getParent());
        } catch (IOException e) {
            Log.e(TAG, "Executing logcat failed", e);
        }
    }

    static boolean isAlive(Context context) {
        // FIXME: Find the most current log file
        
        // FIXME: Find the last-modified time stamp of the log file
        
        // FIXME: Log a canary message
        
        // FIXME: Get the last-modified time stamp of the log file
        
        // FIXME: Return true if our log file was updated
        
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
        File logdir = context.getDir("logs", Context.MODE_PRIVATE);
        return new File(logdir, "log");
    }

    /**
     * Find and kill any old logcat invocations by us that are running on the system.
     */
    static void kill(Context context) {
        long t0 = System.currentTimeMillis();
        Log.d(TAG, "Cleaning up old logcat processes...");

        final String logcatCommandLine[] = createLogcatCommandLine(context);

        int killCount = 0;
        for (File directory : new File("/proc").listFiles()) {
            if (!directory.isDirectory()) {
                continue;
            }

            File cmdline = new File(directory, "cmdline");
            if (!cmdline.exists()) {
                continue;
            }
            if (!cmdline.canRead()) {
                continue;
            }

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

                int pid;
                try {
                    pid = Integer.parseInt(directory.getName());
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Couldn't parse into pid: " + directory.getName());
                    continue;
                }
                Log.i(TAG, "Killing old logcat process: " + pid);
                android.os.Process.killProcess(pid);
                killCount++;
            } catch (IOException e) {
                Log.w(TAG, "Reading command line failed: " + cmdline.getAbsolutePath());
                //noinspection UnnecessaryContinue
                continue;
            } finally {
                try {
                    cmdlineReader.close();
                } catch (IOException e) {
                    // Closing is a best-effort operation, this exception intentionally ignored
                    Log.w(TAG, "Failed to close " + cmdline, e);
                }
            }
        }

        long t1 = System.currentTimeMillis();
        Log.i(TAG, "Killed " + killCount + " old logcats in " + (t1 - t0) + "ms");
    }
}
