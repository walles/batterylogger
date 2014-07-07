package com.gmail.walles.johan.batterylogger;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import static com.gmail.walles.johan.batterylogger.MainActivity.TAG;

/**
 * Listens for battery related events and logs them.
 */
public class EventListenerService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Event listener service starting...");

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
