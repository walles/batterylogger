/*
 * Copyright 2014 Johan Walles <johan.walles@gmail.com>
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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Debug;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import java.io.File;

public class MainActivity extends ActionBarActivity {
    /**
     * Enable this to generate a trace file from when the widget is added to when it is removed.
     *
     * @see #getTraceFileName()
     */
    private static final boolean GENERATE_TRACEFILES = false;

    public static final String TAG = "BatteryLogger";
    public static final int REFRESH_PLOT_EVERY_MINUTES = 60;

    private long lastShown = -(24 * 60 * 60 * 1000);

    @SuppressLint("SdCardPath")
    private static String getTraceFileName() {
        if (BatteryPlotFragment.isRunningOnEmulator()) {
            // This path needs to be hard coded since {@link #getCacheDir()} returns null when called
            // from the constructor where this is used.
            return "/data/data/com.gmail.walles.johan.batterylogger/johan.trace";
        }

        if (new File("/storage/sdcard").isDirectory()) {
            return "/storage/sdcard/batterylogger.trace";
        }

        if (new File("/storage/extSdCard").isDirectory()) {
            return "/storage/extSdCard/batterylogger.trace";
        }

        throw new RuntimeException("Don't know where the external SD card is");
    }

    public MainActivity() {
        super();

        if (GENERATE_TRACEFILES) {
            String traceFileName = getTraceFileName();
            Log.i(TAG, "Traces will be saved to " + traceFileName);
            Debug.startMethodTracing(traceFileName);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!hasFocus) {
            return;
        }

        refreshBatteryPlotFragmentIfNeeded();
    }

    private void refreshBatteryPlotFragmentIfNeeded() {
        final long now = SystemClock.elapsedRealtime();

        long minutesSinceLastShow = (now - lastShown) / (1000 * 60);
        if (minutesSinceLastShow < REFRESH_PLOT_EVERY_MINUTES) {
            return;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new BatteryPlotFragment())
                .commit();
        lastShown = now;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (GENERATE_TRACEFILES) {
            Debug.stopMethodTracing();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SystemSamplingService.enable(this);

        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            refreshBatteryPlotFragmentIfNeeded();
        }
    }
}
