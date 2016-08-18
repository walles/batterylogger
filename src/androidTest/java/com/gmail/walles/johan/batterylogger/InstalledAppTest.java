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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

public class InstalledAppTest extends AndroidTestCase {
    private void testPersistence(InstalledApp testMe) throws IOException {
        StringWriter disk = new StringWriter();
        PrintWriter printWriter = new PrintWriter(disk);
        testMe.println(printWriter);
        printWriter.close();

        InstalledApp recycled = InstalledApp.readLines(new BufferedReader(new StringReader(disk.toString())));
        assertEquals(testMe, recycled);
    }

    public void testPersistence() throws Exception {
        //noinspection ConstantConditions
        SystemState systemState = SystemState.readFromSystem(getContext());
        for (InstalledApp testMe : systemState.getInstalledApps()) {
            testPersistence(testMe);
        }
    }

    public void testNewlines() throws Exception {
        testPersistence(new InstalledApp("a\nb", "c\nd", "e\nf"));
    }

    public void testSpaces() throws Exception {
        testPersistence(new InstalledApp(" a ", " b ", " c "));
    }

    public void testEmpty() throws Exception {
        testPersistence(new InstalledApp("", "", ""));
    }
}
