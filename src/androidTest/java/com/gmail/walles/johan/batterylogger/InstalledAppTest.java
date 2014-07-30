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