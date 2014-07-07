package com.gmail.walles.johan.batterylogger;

import android.app.Service;
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
    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Event listener service starting...");

        try {
            new History(this).addInfoEvent("Battery logging started", new Date());
        } catch (IOException e) {
            // FIXME: Should we shut down the service when this happens?
            Log.e(TAG, "Unable to log events", e);
        }

        // Listen for changes to battery charge
        registerReceiver(new EventReceiver(), new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
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
