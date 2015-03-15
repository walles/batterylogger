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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.FileNotFoundException;

/**
 * Inspired by http://stephendnicholas.com/archives/974
 */
public class LogProvider extends ContentProvider {
    public static final String AUTHORITY = "com.gmail.walles.johan.batterylogger";

    private static final String TAG = "LogProvider";

    private static final int URI_CODE = 1;

    /** UriMatcher used to match against incoming requests */
    private UriMatcher uriMatcher;

    @Override
    public boolean onCreate() {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // Add a URI to the matcher which will match against the form
        // 'content://com.gmail.walles.johan.batterylogger/logs.txt'
        // and return 1 in the case that the incoming Uri matches this pattern
        uriMatcher.addURI(AUTHORITY, "attachment.txt", URI_CODE);

        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String ignoredMode)
            throws FileNotFoundException
    {
        Log.v(TAG, "Called with URI: '" + uri);

        if (uriMatcher.match(uri) != URI_CODE) {
            Log.v(TAG, "Unsupported uri: '" + uri + "'.");
            throw new FileNotFoundException("Unsupported uri: " + uri);
        }

        ParcelFileDescriptor pfd =
                ParcelFileDescriptor.open(
                        ContactDeveloperUtil.getAttachmentFile(getContext()),
                        ParcelFileDescriptor.MODE_READ_ONLY);
        return pfd;
    }

    @Override
    public int update(Uri uri, ContentValues contentvalues, String s,
                      String[] as)
    {
        return 0;
    }

    @Override
    public int delete(Uri uri, String s, String[] as) {
        return 0;
    }

    @Override
    @Nullable
    public Uri insert(Uri uri, ContentValues contentvalues) {
        return null;
    }

    @Override
    @Nullable
    public String getType(Uri uri) {
        return null;
    }

    @Override
    @Nullable
    public Cursor query(Uri uri, String[] projection, String s, String[] as1,
                        String s1)
    {
        return null;
    }
}
