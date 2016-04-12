/*
 * Copyright 2015 Johan Walles <johan.walles@gmail.com>
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import timber.log.Timber;

public class EventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (isSystemBoot(intent)) {
            Timber.i("System is booting, got intent action %s", intent.getAction());
            SystemSamplingService.enable(context);
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            Timber.i("I was upgraded, got intent action %s", intent.getAction());
            SystemSamplingService.enable(context);
        } else {
            Timber.w("Ignoring unknown intent action %s", intent.getAction());
        }
    }

    private static boolean isSystemBoot(Intent intent) {
        return Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction());
    }
}
