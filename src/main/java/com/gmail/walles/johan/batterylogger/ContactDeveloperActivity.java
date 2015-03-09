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

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Show collected system logs and offer user to compose an e-mail to the developer.
 */
public class ContactDeveloperActivity extends ActionBarActivity {
    private static final String TAG = "ContactDeveloper";
    private static final String DEVELOPER_EMAIL = "johan.walles@gmail.com";

    TextView logView = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_developer_layout);

        logView = (TextView)findViewById(R.id.logView);

        logView.setText("Reading logs, please stand by...");
        new AsyncTask<Void, Void, CharSequence>() {
            @Override
            protected void onPostExecute(CharSequence logText) {
                logView.setText(logText);
            }

            @Override
            protected CharSequence doInBackground(Void... voids) {
                return LogCollector.readLogs(ContactDeveloperActivity.this);
            }
        }.execute();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contact_developer, menu);

        MenuItem contactDeveloper = menu.findItem(R.id.contact_developer);
        contactDeveloper.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                sendMail(DEVELOPER_EMAIL, getEmailSubject(), "", logView.getText());

                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Get info about an installed package.
     *
     * @return Null if the package wasn't found.
     */
    @Nullable
    private PackageInfo getPackageInfo(@NotNull String packageName) {
        try {
            final PackageManager packageManager = getPackageManager();

            return packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Couldn't find " + packageName);
            return null;
        }
    }

    @NotNull
    private String getVersion() {
        String packageName = getPackageName();
        if (packageName == null) {
            return "(no version for null package)";
        }

        PackageInfo packageInfo = getPackageInfo(packageName);
        if (packageInfo == null) {
            return "(unknown version)";
        }

        return packageInfo.versionName;
    }

    public String getApplicationName() {
        int stringId = getApplicationInfo().labelRes;
        return getString(stringId);
    }

    @NotNull
    private String getEmailSubject() {
        String versionName = getVersion();
        String appName = getApplicationName();

        return appName + " " + versionName;
    }

    // Inspired by
    // http://answers.unity3d.com/questions/725503/how-to-send-an-email-with-an-attachment-on-android.html
    private void sendMail(String address, String subject, String message, CharSequence attachmentText) {
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
            return;
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        // Get the Uri from the external file and add it to the intent
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(externalFile));

        // finally start the activity
        startActivity(emailIntent);
    }
}
