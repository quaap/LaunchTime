package com.quaap.launchtime;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.quaap.launchtime.apps.AppLauncher;
import com.quaap.launchtime.components.ExceptionHandler;
import com.quaap.launchtime.db.DB;

/**
 * Copyright (C) 2017   Tom Kliethermes
 *
 * This file is part of LaunchTime and is is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
public class LaunchReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (GlobState.enableCrashReporter && !BuildConfig.DEBUG) Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(context));

        try {
            String action = intent.getAction();

            Uri data = intent.getData();
            if (data==null) return;
            String packageName = data.getEncodedSchemeSpecificPart();

            boolean wasACTION_PACKAGE_CHANGED = false;
            if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
                wasACTION_PACKAGE_CHANGED = true;
                try {
                    PackageManager pm = context.getPackageManager();

                    Intent packageIntent = pm.getLaunchIntentForPackage(packageName);
                    if (packageIntent == null) {
                        action = Intent.ACTION_PACKAGE_REMOVED;
                    } else {
                        action = Intent.ACTION_PACKAGE_ADDED;
                    }
                    Log.d("Launchy", "packageIntent " + packageIntent);
                } catch (Exception e) {
                    Log.e("Launchy", e.getMessage(),e);
                }
            }

            if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {

                Log.i("InstallCatch", "The installed package is: " + packageName);
                //Log.i("InstallCatch", "The data is: " + data);

                try {
                    DB db = ((GlobState) context.getApplicationContext()).getDB();

                    PackageManager pm = context.getPackageManager();

                    Intent packageIntent = pm.getLaunchIntentForPackage(packageName);
                    if (packageIntent==null) {
                        return;
                    }
                    ResolveInfo ri = context.getPackageManager().resolveActivity(packageIntent, 0);
                    String activityName = ri.activityInfo.name;
                    ComponentName cn = new ComponentName(ri.activityInfo.packageName, activityName);
                    String category = db.getAppCategory(cn);

                    if (category != null && db.getCategoryDisplay(category) == null) {
                        category = null;
                    }

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

                    AppLauncher.removeAppLauncher(cn);
                    AppLauncher app = AppLauncher.createAppLauncher(context, context.getPackageManager(), ri, category, prefs.getBoolean("prefs_autocat", true));


                    if (db.addApp(app)) {
                        db.addAppCategoryOrder(app.getCategory(), app.getComponentName());
                        if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) && !wasACTION_PACKAGE_CHANGED) {
                            Toast.makeText(context, app.getLabel() + " was installed into " + db.getCategoryDisplay(app.getCategory()), Toast.LENGTH_LONG).show();
                        }

                    }
                } catch (Exception e) {
                    Log.e("LaunchReceiver", "Could not get " + packageName, e);
                }

            } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                Log.i("RemoveCatch", "The uninstalled package is: " + packageName);
                if (!packageName.equals(context.getPackageName()) && !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) { //upgrade
                    DB db = ((GlobState) context.getApplicationContext()).getDB();
                    db.deleteApp(null, packageName);
                }

            }
        } catch (Exception e) {
            Log.e("LaunchReceiver", e.getMessage(), e);
        }

    }
}
