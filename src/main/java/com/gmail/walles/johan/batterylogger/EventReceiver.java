package com.gmail.walles.johan.batterylogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
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
        } else if (isSystemHalt(intent)) {
            Log.v(TAG, "Logging system halt");
            try {
                new History(context).addSystemHaltingEvent(new Date());
            } catch (IOException e) {
                // FIXME: Should we shut down if this happens?
                Log.e(TAG, "Logging system shutdown event failed", e);
            }
        } else if (isSystemBoot(intent)) {
            Log.v(TAG, "Logging system boot");
            try {
                new History(context).addSystemBootingEvent(new Date());
            } catch (IOException e) {
                // FIXME: Should we shut down if this happens?
                Log.e(TAG, "Logging system boot event failed", e);
            }

            // Auto-launch after reboot
            EventListenerService.startService(context, false);
        } else if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
            handlePackageEvent(context, intent, "Installed");
        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
            handlePackageEvent(context, intent, "Removed");
        } else if (Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {
            handlePackageEvent(context, intent, "Upgrade installed");
        } else if (Intent.ACTION_PACKAGE_DATA_CLEARED.equals(intent.getAction())) {
            handlePackageEvent(context, intent, "Data cleared");
        } else {
            Log.i(TAG, "Ignoring unknown intent action: " + intent.getAction());
        }
    }

    private static String describePackage(Context context, @Nullable Uri packageUri) {
        if (packageUri == null) {
            return "(Null package URI)";
        }
        String packageName = packageUri.getSchemeSpecificPart();
        if (packageName == null) {
            return "(Null package name)";
        }
        if (packageName.length() == 0) {
            return "(Empty package name)";
        }

        final PackageManager packageManager = context.getPackageManager();
        if (packageManager == null) {
            Log.w(TAG, "Couldn't retrieve package manager");
            return packageName;
        }

        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Couldn't find package " + packageName);
            return packageName;
        }

        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        CharSequence applicationName = packageName;
        if (applicationInfo != null) {
            applicationName = packageInfo.applicationInfo.loadLabel(packageManager);
        }
        if (applicationName == null) {
            applicationName = packageName;
        }

        if (packageInfo.versionName == null || packageInfo.versionName.length() == 0) {
            return applicationName.toString();
        }

        return applicationName + " " + packageInfo.versionName;
    }

    private void handlePackageEvent(Context context, Intent intent, String eventDescription) {
        // From here: http://www.xinotes.net/notes/note/1335/
        boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
        if (replacing) {
            return;
        }

        String packageDescription = describePackage(context, intent.getData());
        Log.v(TAG, "Logging " + intent.getAction() + " event for " + packageDescription);
        try {
            new History(context).addInfoEvent(eventDescription + ": " + packageDescription, new Date());
        } catch (IOException e) {
            // FIXME: Should we shut down if this happens?
            Log.e(TAG, "Logging " + intent.getAction() + " event failed", e);
        }
    }

    private static boolean isSystemBoot(Intent intent) {
        return Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction());
    }

    private static boolean isSystemHalt(Intent intent) {
        return Intent.ACTION_SHUTDOWN.equals(intent.getAction())
                || "android.intent.action.QUICKBOOT_POWEROFF".equals(intent.getAction());
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
