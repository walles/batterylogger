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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;

import timber.log.Timber;

public class MainActivity extends ActionBarActivity {
    private static final String PREF_SHOW_LEGEND = "show legend";

    /**
     * Enable this to generate a trace file from when the widget is added to when it is removed.
     *
     * @see #getTraceFileName()
     */
    private static final boolean GENERATE_TRACE_FILES = false;

    private static final int REFRESH_PLOT_EVERY_MINUTES = 60;

    private long lastShown = -(24 * 60 * 60 * 1000);
    private BatteryPlotFragment batteryPlotFragment;

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

        if (GENERATE_TRACE_FILES) {
            String traceFileName = getTraceFileName();
            Timber.i("Traces will be saved to %s", traceFileName);
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

        refreshBatteryPlotFragment();
    }

    private void refreshBatteryPlotFragment() {
        batteryPlotFragment = new BatteryPlotFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, batteryPlotFragment)
                .commit();

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean showLegend = preferences.getBoolean(PREF_SHOW_LEGEND, true);
        batteryPlotFragment.setShowLegend(showLegend);

        lastShown = SystemClock.elapsedRealtime();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (GENERATE_TRACE_FILES) {
            Debug.stopMethodTracing();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TimberUtil.setUpLogging(this);

        LogCollector.keepAlive(this);

        SystemSamplingService.enable(this);

        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            refreshBatteryPlotFragmentIfNeeded();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        // Set the legend checkbox to the correct state
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        MenuItem toggleLegend = menu.findItem(R.id.toggle_legend);
        boolean showLegend = preferences.getBoolean(PREF_SHOW_LEGEND, true);
        toggleLegend.setChecked(showLegend);

        // Set up Support callback
        MenuItem support = menu.findItem(R.id.support);
        support.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                final Context context = MainActivity.this;
                int stringId = context.getApplicationInfo().labelRes;
                String applicationLabel = context.getString(stringId);

                String versionName = "<Unknown>";
                try {
                    versionName = context.getPackageManager()
                            .getPackageInfo(context.getPackageName(), 0).versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    Timber.w(e, "Getting version name failed");
                }

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(applicationLabel)
                        .setMessage("Version " + versionName)
                        .setNeutralButton(R.string.view_app_logs,
                                         new DialogInterface.OnClickListener() {
                                             @Override
                                             public void onClick(DialogInterface dialogInterface, int i) {
                                                 final Intent viewLogsIntent =
                                                 new Intent(context, LogViewerActivity.class);
                                                 context.startActivity(viewLogsIntent);
                                             }
                                         })
                        .setPositiveButton(R.string.contact_developer_condensed,
                                          new DialogInterface.OnClickListener() {
                                              @Override
                                              public void onClick(DialogInterface dialogInterface, int i) {
                                                  ContactDeveloperUtil.sendMail(MainActivity.this);
                                              }
                                          })
                        .setIcon(R.drawable.ic_launcher)
                        .show();

                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.toggle_legend) {
            SharedPreferences preferences = getPreferences(MODE_PRIVATE);
            boolean showLegend = preferences.getBoolean(PREF_SHOW_LEGEND, true);
            showLegend = !showLegend;
            preferences.edit().putBoolean(PREF_SHOW_LEGEND, showLegend).apply();

            item.setChecked(showLegend);

            batteryPlotFragment.setShowLegend(showLegend);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
