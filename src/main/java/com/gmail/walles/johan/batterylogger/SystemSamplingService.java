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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import timber.log.Timber;

/**
 * Sample the system state at regular intervals.
 */
public class SystemSamplingService extends Service {
    private static final String SAMPLE_ACTION = "history event sample action";
    private static final String SYSTEM_STATE_FILE_NAME = "system-state.txt";

    /**
     * Start sampling the system state at regular intervals.
     */
    public static void enable(Context context) {
        Timber.v("Setting repeating system state sampling alarm...");

        Intent intent = new Intent(context, SystemSamplingService.class);
        intent.setAction(SAMPLE_ACTION);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        if (pendingIntent == null) {
            // This shouldn't happen without passing FLAG_NO_CREATE to the getService() call
            throw new RuntimeException("Pending intent was null");
        }

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                // Don't start sampling immediately, this makes us not sample during startup,
                // and thus improves app startup performance a lot.
                SystemClock.elapsedRealtime() + 30 * 1000,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                pendingIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogCollector.keepAlive(this);

        final Intent finalIntent = intent;

        Thread thread = new Thread("Sampling Thread " + new Date()) {
            @Override
            public void run() {
                try {
                    handleIntent(finalIntent);
                } catch (Exception e) {
                    Timber.e(e, "Failed to handle incoming intent");
                }
            }
        };
        thread.start();

        return START_NOT_STICKY;
    }

    private void handleIntent(Intent intent) throws IOException {
        if (!SAMPLE_ACTION.equals(intent.getAction())) {
            Timber.w("Ignoring unknown action %s", intent.getAction());
            return;
        }

        final File stateFile = new File(getFilesDir(), SYSTEM_STATE_FILE_NAME);

        SystemState state = SystemState.readFromSystem(this);

        if (stateFile.isFile()) {
            SystemState previousState = SystemState.readFromFile(stateFile);
            History history = new History(this);
            for (HistoryEvent event : state.getEventsSince(previousState)) {
                history.addEvent(event);
            }
        }

        state.writeToFile(stateFile);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding unsupported, please use startService() instead");
    }
}
