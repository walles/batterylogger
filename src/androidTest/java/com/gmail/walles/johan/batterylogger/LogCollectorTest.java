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

import android.test.AndroidTestCase;

public class LogCollectorTest extends AndroidTestCase {
    public void testLogCollector() throws Exception {
        LogCollector.kill(getContext());
        assertFalse(LogCollector.isAlive(getContext()));

        LogCollector.keepAlive(getContext());
        Thread.sleep(500);
        assertTrue("Log collector should be alive after calling keepAlive()",
                LogCollector.isAlive(getContext()));
    }
}
