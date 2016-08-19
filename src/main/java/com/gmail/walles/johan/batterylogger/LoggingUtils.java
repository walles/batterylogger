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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class LoggingUtils {
    private static Class<Timber> initializedLoggingClass = null;

    private static String version;

    private LoggingUtils() {
        // Don't let people instantiate this class
    }

    private static boolean isCrashlyticsEnabled() {
        return !EmulatorUtils.isRunningOnEmulator();
    }

    public static void logCustom(CustomEvent event) {
        if (isCrashlyticsEnabled()) {
            event.putCustomAttribute("App Version", version); //NON-NLS
            Answers.getInstance().logCustom(event);
        }
    }

    public static void setUpLogging(Context context) {
        Timber.Tree tree;
        if (isCrashlyticsEnabled()) {
            tree = new CrashlyticsTree(context);
        } else {
            tree = new LocalTree();
        }

        if (initializedLoggingClass != Timber.class) {
            initializedLoggingClass = Timber.class;
            Timber.plant(tree);
            Timber.v("Logging tree planted: %s", tree.getClass());
        }

        LogCollector.keepAlive(context);

        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Timber.w(e, "Unable to find out my own version");
            version = "<unknown>"; //NON-NLS
        }
    }

    private static class CrashlyticsTree extends Timber.Tree {
        public CrashlyticsTree(Context context) {
            Fabric.with(context, new Crashlytics());
        }

        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (BuildConfig.DEBUG) {
                tag = "DEBUG";
            } else if (TextUtils.isEmpty(tag)) {
                tag = "BatteryLogger";
            }

            // This call logs to *both* Crashlytics and LogCat, and will log the Exception backtrace
            // to LogCat on exceptions.
            Crashlytics.log(priority, tag, message);

            if (t != null) {
                Crashlytics.logException(t);
            }
        }
    }

    private static class LocalTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (BuildConfig.DEBUG) {
                tag = "DEBUG";
            } else if (TextUtils.isEmpty(tag)) {
                tag = "BatteryLogger";
            }

            // Empirical evidence shows any exception stack trace is already part of the message, so
            // no need to print the exception explicitly here.
            Log.println(priority, tag, message);
        }
    }
}
