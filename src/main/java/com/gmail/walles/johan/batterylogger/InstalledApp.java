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

import android.support.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class InstalledApp {
    public final String dottedName;
    public final String displayName;
    public final String versionName;

    public InstalledApp(String dottedName, String displayName, String versionName) {
        this.dottedName = dottedName.trim();
        this.displayName = displayName.trim();
        this.versionName = versionName.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstalledApp that = (InstalledApp) o;

        if (!displayName.equals(that.displayName)) return false;
        if (!dottedName.equals(that.dottedName)) return false;
        if (!versionName.equals(that.versionName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = dottedName.hashCode();
        result = 31 * result + displayName.hashCode();
        result = 31 * result + versionName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "InstalledApp{" +
                "dottedName='" + dottedName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", versionName='" + versionName + '\'' +
                '}';
    }

    public void println(PrintWriter writer) throws IOException {
        writer.println(URLEncoder.encode(dottedName, "UTF-8"));
        writer.println("  " + URLEncoder.encode(displayName, "UTF-8"));
        writer.println("  " + URLEncoder.encode(versionName, "UTF-8"));
    }

    @Nullable
    public static InstalledApp readLines(BufferedReader reader) throws IOException {
        String dottedName = reader.readLine();
        if (dottedName == null) {
            return null;
        }
        dottedName = URLDecoder.decode(dottedName, "UTF-8");

        String displayName = reader.readLine();
        validateIndentation(dottedName, "display name", displayName);
        displayName = URLDecoder.decode(displayName.trim(), "UTF-8");

        String versionName = reader.readLine();
        validateIndentation(dottedName + " " + displayName, "version string", versionName);
        versionName = URLDecoder.decode(versionName.trim(), "UTF-8");

        return new InstalledApp(dottedName, displayName, versionName);
    }

    private static void validateIndentation(String name, String description, String indented) throws IOException {
        if (indented == null) {
            throw new IOException("Unexpected end of file while reading " + description + ": <" + name + ">");
        }
        if (indented.length() < 2) {
            throw new IOException("Too short " + description + " for " + name + ": <" + indented + ">");
        }
        if (indented.charAt(0) != ' ' || indented.charAt(1) != ' '
                || (indented.length() > 2 && indented.charAt(2) == ' '))
        {
            throw new IOException("Mis-indented " + description + " for " + name + ": <" + indented + ">");
        }
    }
}
