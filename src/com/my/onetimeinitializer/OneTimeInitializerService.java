/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.my.onetimeinitializer;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.net.URISyntaxException;
import java.util.Set;

import android.content.res.Resources;

/**
 * A class that performs one-time initialization after installation.
 *
 * <p>Android doesn't offer any mechanism to trigger an app right after installation, so we use the
 * BOOT_COMPLETED broadcast intent instead.  This means, when the app is upgraded, the
 * initialization code here won't run until the device reboots.
 */
public class OneTimeInitializerService extends IntentService {

    // class name is too long
    //private static final String TAG = OneTimeInitializerService.class.getSimpleName()
    //        .substring(0, 22);

    private static final String TAG = "MyOneTimeInitializer";

    // Name of the shared preferences file.
    private static final String SHARED_PREFS_FILE = "myoti";

    // Name of the preference containing the mapping version.
    private static final String MAPPING_VERSION_PREF = "mapping_version";

    // This is the content uri for Launcher content provider. See
    // LauncherSettings and LauncherProvider in the Launcher app for details.
    private static final Uri LAUNCHER_CONTENT_URI =
            Uri.parse("content://com.android.launcher2.settings/favorites?notify=true");

    private static final String LAUNCHER_ID_COLUMN = "_id";
    private static final String LAUNCHER_INTENT_COLUMN = "intent";

    private SharedPreferences mPreferences;

    public OneTimeInitializerService() {
        super("OneTimeInitializer Service");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPreferences = getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "OneTimeInitializerService.onHandleIntent");
        }

        final int currentVersion = getMappingVersion();
        int newVersion = currentVersion;
        if (currentVersion < 1) {
            if (Log.isLoggable(TAG, Log.INFO)) {
                Log.i(TAG, "Updating to version 1.");
            }
            updateLauncherDB();

            newVersion = 1;
        }

        updateMappingVersion(newVersion);
    }

    private int getMappingVersion() {
        return mPreferences.getInt(MAPPING_VERSION_PREF, 0);
    }

    private void updateMappingVersion(int version) {
        SharedPreferences.Editor ed = mPreferences.edit();
        ed.putInt(MAPPING_VERSION_PREF, version);
        ed.commit();
    }

    private void updateLauncherDB() {
        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(LAUNCHER_CONTENT_URI,
                new String[]{LAUNCHER_ID_COLUMN, LAUNCHER_INTENT_COLUMN}, null, null, null);
        if (c == null) {
            return;
        }

        Resources res = getResources();
        String[] oldComponents = res.getStringArray(R.array.old_components);
        String[] newComponents = res.getStringArray(R.array.new_components);

        if (oldComponents == null || oldComponents.length == 0 ||
            newComponents == null || newComponents.length == 0) {
            return;
        }

        try {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Total launcher icons: " + c.getCount());
            }

            while (c.moveToNext()) {
                long favoriteId = c.getLong(0);
                final String intentUri = c.getString(1);
                if (intentUri != null) {
                    try {
                        final Intent intent = Intent.parseUri(intentUri, 0);
                        final ComponentName componentName = intent.getComponent();
                        final Set<String> categories = intent.getCategories();

                        for (int i = 0; i < oldComponents.length; i++) {
                            String[] strs = oldComponents[i].split("/");
                            String oldPkgName = strs[0];
                            String oldClassName = strs[1];
                            if (oldClassName.startsWith("."))
                                oldClassName = oldPkgName + oldClassName;

                            if (Intent.ACTION_MAIN.equals(intent.getAction()) &&
                                componentName != null && categories != null &&
                                categories.contains(Intent.CATEGORY_LAUNCHER) &&
                                oldPkgName.equals(componentName.getPackageName()) &&
                                oldClassName.equals(componentName.getClassName())) {

                                if (newComponents[i].length() == 0) {  // new component == null, remove old component in database
                                    Log.v(TAG, "need to remove: " + oldComponents[i]);

                                    String deleteWhere = LAUNCHER_ID_COLUMN + "=" + favoriteId;
                                    cr.delete(LAUNCHER_CONTENT_URI, deleteWhere, null);
                                } else {
                                    Log.v(TAG, oldComponents[i] + " -> " + newComponents[i]);

                                    strs = newComponents[i].split("/");
                                    String newPkgName = strs[0];
                                    String newClassName = strs[1];

                                    if (newClassName.startsWith("."))
                                        newClassName = newPkgName + newClassName;

                                    Log.v(TAG, "oldPkgName = " + oldPkgName);
                                    Log.v(TAG, "oldClassName = " + oldClassName);
                                    Log.v(TAG, "newPkgName = " + newPkgName);
                                    Log.v(TAG, "newClassName = " + newClassName);
                                
                                    final ComponentName newName = new ComponentName(newPkgName, newClassName);
                                    intent.setComponent(newName);
                                    final ContentValues values = new ContentValues();
                                    values.put(LAUNCHER_INTENT_COLUMN, intent.toUri(0));

                                    String updateWhere = LAUNCHER_ID_COLUMN + "=" + favoriteId;
                                    cr.update(LAUNCHER_CONTENT_URI, values, updateWhere, null);
                                }
                            }
                        }
                    } catch (RuntimeException ex) {
                        Log.e(TAG, "Problem moving Dialtacts activity", ex);
                    } catch (URISyntaxException e) {
                        Log.e(TAG, "Problem moving Dialtacts activity", e);
                    }
                }
            }

        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
}
