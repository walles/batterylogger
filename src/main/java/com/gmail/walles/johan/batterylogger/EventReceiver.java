package com.gmail.walles.johan.batterylogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static com.gmail.walles.johan.batterylogger.MainActivity.TAG;

public class EventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (isSystemBoot(intent)) {
            SystemSamplingService.enable(context);
        } else {
            Log.i(TAG, "Ignoring unknown intent action: " + intent.getAction());
        }
    }

    private static boolean isSystemBoot(Intent intent) {
        return Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction());
    }
}
