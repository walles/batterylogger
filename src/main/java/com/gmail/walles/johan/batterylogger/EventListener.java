package com.gmail.walles.johan.batterylogger;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.Date;

import static com.gmail.walles.johan.batterylogger.MainActivity.TAG;

/**
 * Listens for battery related events and logs them.
 */
public class EventListener extends Service {
    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Event listener service starting...");

        // Listen for changes to battery charge
        registerReceiver(new BroadcastReceiver() {
            private int lastLevel = -1;

            @Override
            public void onReceive(Context context, Intent intent) {
                // FIXME: Check the intent type before starting to process it as a battery one
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -2);
                if (level == -2) {
                    return;
                }
                if (level == lastLevel) {
                    // We can get all sorts of info here; just log changes to the battery level
                    return;
                }

                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -3);
                if (scale == -3) {
                    Log.w(TAG, "Got no scale information from battery event");
                    return;
                }

                int percentage = (level * 100) / scale;
                Log.v(TAG, "Logging battery level " + level + "/" + scale + "=" + percentage + "%");
                try {
                    new History(EventListener.this).addBatteryLevelEvent(percentage, new Date());
                } catch (IOException e) {
                    // FIXME: Should we shut down if this happens?
                    Log.e(TAG, "Logging battery event failed", e);
                }

                lastLevel = level;
            }
        }, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
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
