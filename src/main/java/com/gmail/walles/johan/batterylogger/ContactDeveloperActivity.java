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
import android.widget.TextView;

/**
 * Show collected system logs and offer user to compose an e-mail to the developer.
 */
public class ContactDeveloperActivity extends ActionBarActivity {
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
                ContactDeveloperUtil.sendMail(ContactDeveloperActivity.this,
                        logView.getText());

                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }
}
