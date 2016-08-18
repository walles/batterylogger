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

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;

import java.io.File;
import java.io.IOException;

import timber.log.Timber;

/**
 * Sample the system state at regular intervals.
 */
public class SystemSamplingService extends IntentService {
    private static final String SAMPLE_ACTION = "history event sample action";
    private static final String SYSTEM_STATE_FILE_NAME = "system-state.txt";
    private long lastSamplingEndTimestamp = 0;

    public SystemSamplingService() {
        super("System Sampling Service");
    }

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

        // Prevent duplicate alarms
        alarmManager.cancel(pendingIntent);

        alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                // Don't start sampling immediately, this makes us not sample during startup,
                // and thus improves app startup performance a lot.
                SystemClock.elapsedRealtime() + 30 * 1000,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                pendingIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        LoggingUtils.setUpLogging(this);
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

    @Override
    protected void onHandleIntent(Intent intent) {
        long now = System.currentTimeMillis();
        long deltaMinutes = (now - lastSamplingEndTimestamp) / (1000 * 60);
        if (deltaMinutes <= 5) {
            // Ignore events that are too close together
            Timber.i("Ignoring sampling event %d minutes after last ended", deltaMinutes);
            return;
        }

        try {
            handleIntent(intent);
        } catch (IOException e) {
            Timber.w(e, "Failed to handle sampling intent");
        }
        lastSamplingEndTimestamp = System.currentTimeMillis();
    }
}
