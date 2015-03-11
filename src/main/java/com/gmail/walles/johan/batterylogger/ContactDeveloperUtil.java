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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class ContactDeveloperUtil {
    private static final String TAG = "ContactDeveloper";
    private static final String DEVELOPER_EMAIL = "johan.walles@gmail.com";

    /**
     * Get info about an installed package.
     *
     * @return Null if the package wasn't found.
     */
    @Nullable
    private static PackageInfo getPackageInfo(Context context, @NotNull String packageName) {
        try {
            final PackageManager packageManager = context.getPackageManager();

            return packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Couldn't find " + packageName);
            return null;
        }
    }

    @NotNull
    private static String getVersion(Context context) {
        String packageName = context.getPackageName();
        if (packageName == null) {
            return "(no version for null package)";
        }

        PackageInfo packageInfo = getPackageInfo(context, packageName);
        if (packageInfo == null) {
            return "(unknown version)";
        }

        return packageInfo.versionName;
    }

    private static String getApplicationName(Context context) {
        int stringId = context.getApplicationInfo().labelRes;
        return context.getString(stringId);
    }

    @NotNull
    private static String getEmailSubject(Context context) {
        String versionName = getVersion(context);
        String appName = getApplicationName(context);

        return appName + " " + versionName;
    }

    private static void sendMail(Context context,
                                 String address,
                                 String subject,
                                 String message,
                                 CharSequence attachmentText)
    {
        final Intent emailIntent = getSendMailIntent(address, subject, message, attachmentText);
        if (emailIntent == null) return;

        context.startActivity(emailIntent);
    }

    // Inspired by
    // http://answers.unity3d.com/questions/725503/how-to-send-an-email-with-an-attachment-on-android.html
    @Nullable
    private static Intent getSendMailIntent(String address, String subject, String message, CharSequence attachmentText) {
        // Create the intent
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);

        // add the address, subject and body of the mail
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { address });
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, message);

        // set the MIME type
        emailIntent.setType("message/rfc822");

        // grant access to the uri (for the attached file, although I'm not sure if the grant access
        // is required)
        emailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Store the attachment text in a place where the mail app can get at it
        File externalFile = new File(Environment.getExternalStorageDirectory(), "attachment.txt");
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(externalFile);
            writer.print(attachmentText);
        } catch (IOException e) {
            Log.e(TAG, "Failed to write attachment to " + externalFile.getAbsolutePath(), e);
            return null;
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        // Get the Uri from the external file and add it to the intent
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(externalFile));
        return emailIntent;
    }

    public static void sendMail(Context context, CharSequence attachmentText) {
        sendMail(context, DEVELOPER_EMAIL, getEmailSubject(context), "", attachmentText);
    }

    public static void sendMail(final Context context) {
        new AsyncTask<Void, Void, CharSequence>() {
            @Override
            protected void onPostExecute(CharSequence logText) {
                sendMail(context, logText);
            }

            @Override
            protected CharSequence doInBackground(Void... voids) {
                return LogCollector.readLogs(context);
            }
        }.execute();
    }

    public static void setUpMenuItem(Context context, MenuItem contactDeveloper) {
        Intent sendMailIntent = getSendMailIntent(DEVELOPER_EMAIL, "", "", "");
        if (sendMailIntent == null) {
            contactDeveloper.setEnabled(false);
            return;
        }

        PackageManager packageManager = context.getPackageManager();
        ResolveInfo sendMailActivity = packageManager.resolveActivity(
                sendMailIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (sendMailActivity == null) {
            contactDeveloper.setEnabled(false);
            return;
        }

        Log.i(TAG, "E-mail app is " + sendMailActivity.activityInfo.name);
        if (sendMailActivity.activityInfo.name.endsWith("ResolverActivity")) {
            // This is the resolver activity, don't set an icon
            return;
        }

        Drawable icon = sendMailActivity.loadIcon(packageManager);
        contactDeveloper.setIcon(icon);
        return;
    }
}