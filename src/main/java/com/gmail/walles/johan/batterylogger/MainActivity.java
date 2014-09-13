package com.gmail.walles.johan.batterylogger;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Debug;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;

public class MainActivity extends ActionBarActivity {
    /**
     * Enable this to generate a trace file from when the widget is added to when it is removed.
     *
     * @see #TRACE_FILE_NAME
     */
    private static final boolean GENERATE_TRACEFILES = false;

    /**
     * This path needs to be hard coded since {@link #getCacheDir()} returns null when called from the constructor
     * where this is used.
     *
     * @see #GENERATE_TRACEFILES
     */
    @SuppressWarnings("FieldCanBeLocal")
    @SuppressLint("SdCardPath")
    private final String TRACE_FILE_NAME = "/data/data/com.gmail.walles.johan.batterylogger/johan.trace";

    public static final String TAG = "BatteryLogger";
    public static final int REFRESH_PLOT_EVERY_MINUTES = 60;

    private long lastShown = -(24 * 60 * 60 * 1000);

    public MainActivity() {
        super();

        if (GENERATE_TRACEFILES) {
            Debug.startMethodTracing(TRACE_FILE_NAME);
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
