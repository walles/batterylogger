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

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import timber.log.Timber;

public class ContactDeveloperUtil {
    private static final String DEVELOPER_EMAIL = "johan.walles@gmail.com";

    /**
     * Get info about an installed package.
     *
     * @return Null if the package wasn't found.
     */
    @Nullable
    private static PackageInfo getPackageInfo(Context context, @NonNull String packageName) {
        try {
            final PackageManager packageManager = context.getPackageManager();

            return packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Timber.w("Couldn't find %s", packageName);
            return null;
        }
    }

    @NonNull
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

    @NonNull
    private static String getEmailSubject(Context context) {
        String versionName = getVersion(context);
        String appName = getApplicationName(context);

        return appName + " " + versionName;
    }

    private static void sendMail(Context context,
                                 String subject,
                                 CharSequence attachmentText)
    {
        final Intent emailIntent = getSendMailIntent(context, subject, attachmentText);
        if (emailIntent == null) return;

        try {
            context.startActivity(emailIntent);
        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.app_name)
                    .setMessage("No e-mail app installed")
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // This method intentionally left blank
                        }
                    })
                    .show();
        }
    }

    // Inspired by
    // http://answers.unity3d.com/questions/725503/how-to-send-an-email-with-an-attachment-on-android.html
    private static Intent getSendMailIntent(
            Context context,
            String subject, @Nullable CharSequence attachmentText)
    {
        // Create the intent
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);

        // add the address, subject and body of the mail
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { DEVELOPER_EMAIL });
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);

        // Use an empty text to encourage people to write something
        emailIntent.putExtra(Intent.EXTRA_TEXT, "");

        // From: https://developer.android.com/guide/components/intents-common.html
        emailIntent.setData(Uri.parse("mailto:"));

        // set the MIME type
        emailIntent.setType("message/rfc822");

        if (attachmentText != null) {
            try {
                addAttachmentToEmailIntent(context, emailIntent, attachmentText);
            } catch (IOException e) {
                // Store exception in message body
                StringWriter stringWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(stringWriter);
                e.printStackTrace(pw);
                emailIntent.putExtra(Intent.EXTRA_TEXT, stringWriter.toString());
            }
        }

        return emailIntent;
    }

    private static void addAttachmentToEmailIntent(
            Context context, Intent emailIntent, CharSequence attachmentText)
            throws IOException
    {
        // Store the attachment text in a place where the mail app can get at it
        File attachmentFile = getAttachmentFile(context);
        PrintWriter writer = null;
        try {
            // Write to temporary file first and then mv into attachment file?
            File tempFile =
                    File.createTempFile("attachment", ".txt", attachmentFile.getParentFile());
            writer = new PrintWriter(tempFile);
            writer.print(attachmentText);
            writer.close();

            if (!tempFile.renameTo(attachmentFile)) {
                throw new IOException(
                        "Failed renaming " + tempFile.getAbsolutePath()
                        + " to " + attachmentFile.getAbsolutePath());
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        // Get the Uri from the external file and add it to the intent
        final Uri attachmentUri = Uri.parse("content://" + LogProvider.AUTHORITY + "/"
                + attachmentFile.getName());
        emailIntent.putExtra(Intent.EXTRA_STREAM, attachmentUri);
        Timber.v("E-mail attachment URI: %s", attachmentUri);
    }

    public static File getAttachmentFile(Context context) {
        return new File(context.getCacheDir(), "attachment.txt");
    }

    public static void sendMail(Context context, CharSequence attachmentText) {
        sendMail(context, getEmailSubject(context), attachmentText);
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
        final Intent sendMailIntent = getSendMailIntent(context, "Subject", null);

        PackageManager packageManager = context.getPackageManager();
        ResolveInfo sendMailActivity = packageManager.resolveActivity(
                sendMailIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (sendMailActivity == null) {
            contactDeveloper.setEnabled(false);
            return;
        }

        // We're clickable
        contactDeveloper.setEnabled(true);

        Timber.i("E-mail app is %s", sendMailActivity.activityInfo.name);
        if (sendMailActivity.activityInfo.name.endsWith("ResolverActivity")) {
            // This is the resolver activity, don't set an icon
            return;
        }

        Drawable icon = sendMailActivity.loadIcon(packageManager);
        contactDeveloper.setIcon(icon);
        return;
    }
}
