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

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Show collected system logs and offer user to compose an e-mail to the developer.
 */
public class LogViewerActivity extends ActionBarActivity {
    private TextView logView;
    private boolean logsLoaded = false;
    private MenuItem contactDeveloper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LoggingUtil.setUpLogging(this);

        setContentView(R.layout.contact_developer_layout);

        logView = (TextView)findViewById(R.id.logView);
        final ScrollView verticalScrollView =
                (ScrollView)findViewById(R.id.verticalScrollView);

        logView.setText("Reading logs, please stand by...");
        new AsyncTask<Void, Void, CharSequence>() {
            @Override
            protected void onPostExecute(CharSequence logText) {
                logView.setText(logText);
                logsLoaded = true;

                // Scroll log view to bottom
                verticalScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        verticalScrollView.smoothScrollTo(0, Integer.MAX_VALUE);
                    }
                });

                setUpContactDeveloper();
            }

            @Override
            protected CharSequence doInBackground(Void... voids) {
                return LogCollector.readLogs(LogViewerActivity.this);
            }
        }.execute();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void setUpContactDeveloper() {
        if (contactDeveloper == null) {
            return;
        }

        if (!logsLoaded) {
            contactDeveloper.setEnabled(false);
            return;
        }

        contactDeveloper.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                ContactDeveloperUtil.sendMail(LogViewerActivity.this, logView.getText());

                return true;
            }
        });
        ContactDeveloperUtil.setUpMenuItem(this, contactDeveloper);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contact_developer, menu);

        contactDeveloper = menu.findItem(R.id.contact_developer);
        setUpContactDeveloper();

        return super.onCreateOptionsMenu(menu);
    }
}
