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

import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import timber.log.Timber;

public class TimberUtil {
    private static Class<Timber> initializedLoggingClass = null;

    private TimberUtil() {
        // Don't let people instantiate this class
    }

    public static void setUpLogging() {
        if (initializedLoggingClass == Timber.class) {
            return;
        }
        initializedLoggingClass = Timber.class;

        Timber.plant(new AndroidLogTree());
        if (!BuildConfig.DEBUG) {
            Timber.plant(new CrashlyticsTree());
        }
    }

    private static class CrashlyticsTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (t == null && priority < Log.WARN) {
                return;
            }

            if (TextUtils.isEmpty(tag)) {
                tag = "BatteryLogger";
            }

            Crashlytics.log(priority, tag, message);

            if (t != null) {
                Crashlytics.logException(t);
            }
        }
    }

    private static class AndroidLogTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (TextUtils.isEmpty(tag)) {
                tag = "BatteryLogger";
            }
            if (t != null) {
                message += "\n" + Log.getStackTraceString(t);
            }
            Log.println(priority, tag, message);
        }
    }
}
