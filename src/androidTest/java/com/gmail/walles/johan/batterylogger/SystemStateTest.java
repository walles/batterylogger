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

import android.test.AndroidTestCase;

import java.io.File;
import java.util.Date;

public class SystemStateTest extends AndroidTestCase {
    /**
     * Sanity check the current system state.
     */
    public void testReadFromSystem() throws Exception {
        //noinspection ConstantConditions
        SystemState testMe = SystemState.readFromSystem(getContext());

        int appCount = testMe.getAppCount();
        assertTrue("App count is " + appCount, testMe.getAppCount() > 10);

        int batteryPercentage = testMe.getBatteryPercentage();
        assertTrue("Battery percentage is " + batteryPercentage + "%", testMe.getBatteryPercentage() >= 0);

        File tempFile = File.createTempFile("systemstate-", ".txt");
        try {
            testMe.writeToFile(tempFile);
            assertEquals(testMe, SystemState.readFromFile(tempFile));
        } finally {
            //noinspection ConstantConditions
            if (tempFile != null) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        }
    }

    public void testGetBootTimestamp() throws Exception {
        SystemState.getBootTimestamp();
        Date b0 = SystemState.getBootTimestamp();
        Thread.sleep(1000, 0);
        Date b1 = SystemState.getBootTimestamp();

        long dt = Math.abs(b1.getTime() - b0.getTime());
        assertTrue("Too much drift over one second: " + dt + "ms", dt < 200);

        assertTrue("Boot timestamp can't be in the future", b0.before(new Date()));
    }
}
