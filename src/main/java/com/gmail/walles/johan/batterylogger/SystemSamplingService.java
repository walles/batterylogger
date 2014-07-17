package com.gmail.walles.johan.batterylogger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import static com.gmail.walles.johan.batterylogger.MainActivity.TAG;

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
        Log.v(TAG, "Setting repeating system state sampling alarm...");

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
                AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                pendingIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return START_NOT_STICKY;
    }

    private void handleIntent(Intent intent) {
        if (!SAMPLE_ACTION.equals(intent.getAction())) {
            Log.w(TAG, "Ignoring unknown action " + intent.getAction());
            return;
        }

        final File stateFile = new File(getFilesDir(), SYSTEM_STATE_FILE_NAME);

        try {
            SystemState state = SystemState.readFromSystem(this);

            if (stateFile.isFile()) {
                SystemState previousState = SystemState.readFromFile(stateFile);
                History history = new History(this);
                for (HistoryEvent event : state.getEventsSince(previousState)) {
                    history.addEvent(event);
                }
            }

            state.writeToFile(stateFile);

            Log.v(TAG, "System state sampled");
        } catch (IOException e) {
            Log.e(TAG, "Event logging failed", e);
        }
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding unsupported, please use startService() instead");
    }
}
