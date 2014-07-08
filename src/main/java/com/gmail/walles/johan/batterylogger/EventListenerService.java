package com.gmail.walles.johan.batterylogger;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.Date;

import static com.gmail.walles.johan.batterylogger.MainActivity.TAG;

/**
 * Listens for battery related events and logs them.
 */
public class EventListenerService extends Service {
    private boolean running = false;

    private static final String LOG_START_EXTRA = "should log start";
    public static void startService(Context context, boolean logStart) {
        Intent intent = new Intent(context, EventListenerService.class);
        intent.putExtra(LOG_START_EXTRA, logStart);

        boolean started = (context.startService(intent) != null);
        if (!started) {
            throw new RuntimeException("Unable to start event listener service");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean shouldLogStart = intent.getBooleanExtra(LOG_START_EXTRA, true);

        if (!running) {
            Log.i(TAG, "Event listener service starting...");

            if (shouldLogStart) {
                try {
                    new History(this).addInfoEvent("Battery logging started", new Date());
                } catch (IOException e) {
                    // FIXME: Should we shut down the service when this happens?
                    Log.e(TAG, "Unable to log events", e);
                }
            }

            // Listen for changes to battery charge
            registerReceiver(new EventReceiver(), new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }

        running = true;
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "Event listener service going down");
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Binding unsupported, please use startService() instead");
    }
}
