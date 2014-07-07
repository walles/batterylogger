package com.gmail.walles.johan.batterylogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Date;

import static com.gmail.walles.johan.batterylogger.MainActivity.TAG;

public class EventReceiver extends BroadcastReceiver {
    private int lastBatteryPercentage = -1;

    @Nullable
    private Boolean lastCharging;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
            handleBatteryChanged(context, intent);
        }
    }

    private void handleBatteryChanged(Context context, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -2);
        if (level == -2) {
            return;
        }

        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -3);
        if (scale == -3) {
            Log.w(TAG, "Got no scale information from battery event");
            return;
        }

        logBatteryLevel(level, scale, context);

        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging =
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        logCharging(charging, context);
    }

    private void logBatteryLevel(int level, int scale, Context context) {
        int percentage = (level * 100) / scale;
        if (percentage == lastBatteryPercentage) {
            // Only log changes to the battery percentage
            return;
        }

        Log.v(TAG, "Logging battery level " + level + "/" + scale + "=" + percentage + "%");
        try {
            new History(context).addBatteryLevelEvent(percentage, new Date());
        } catch (IOException e) {
            // FIXME: Should we shut down if this happens?
            Log.e(TAG, "Logging battery event failed", e);
        }

        lastBatteryPercentage = level;
    }

    private void logCharging(boolean charging, Context context) {
        if (lastCharging == null) {
            lastCharging = charging;
        }
        if (charging == lastCharging) {
            // Only log changes to the charging status
            return;
        }

        Log.v(TAG, "Logging new charging state: " + (charging ? "Charging" : "Not charging"));
        try {
            if (charging) {
                new History(context).addInfoEvent("Charger plugged in", new Date());
            } else {
                new History(context).addInfoEvent("Charger disconnected", new Date());
            }
        } catch (IOException e) {
            // FIXME: Should we shut down if this happens?
            Log.e(TAG, "Logging battery event failed", e);
        }

        lastCharging = charging;
    }
}
