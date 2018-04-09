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
        if (!BuildConfig.DEBUG) Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(context));

        Log.i("InstallCatch", intent.toString());

        try {
            String action = intent.getAction();
            if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                Uri data = intent.getData();
                if (data==null) return;
                String packageName = data.getEncodedSchemeSpecificPart();
                Log.i("InstallCatch", "The installed package is: " + packageName);

                try {
                    DB db = ((GlobState) context.getApplicationContext()).getDB();

                    PackageManager pm = context.getPackageManager();

                    Intent packageIntent = pm.getLaunchIntentForPackage(packageName);
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
                        if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                            Toast.makeText(context, app.getLabel() + " was installed into " + db.getCategoryDisplay(app.getCategory()), Toast.LENGTH_LONG).show();
                        }
//                        else {
//                            String label = ri.loadLabel(context.getPackageManager()).toString();
//                            if (app.getLabel()==null || !app.getLabel().equals(label)) {
//
//                                db.updateAppLabel(ri.activityInfo.packageName, activityName, label);
//                                app.setLabel(label);
//                            }
//                        }
                    }
                } catch (Exception e) {
                    Log.e("LaunchReceiver", "Could not get " + packageName, e);
                }

            } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                Uri data = intent.getData();
                if (data==null) return;
                String packageName = data.getEncodedSchemeSpecificPart();
                Log.i("RemoveCatch", "The uninstalled package is: " + packageName);
                if (!packageName.equals(context.getPackageName()) && !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) { //upgrade
                    DB db = ((GlobState) context.getApplicationContext()).getDB();
                    db.deleteApp(null, packageName);
                }
//        } else if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
//            Log.d("ShortcutCatch", "intent received");
//            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
//                Log.d("ShortcutCatch", "Shortcut ID: " + intent.getIntExtra(Intent.EXTRA_TEXT, 0));
//                Log.d("ShortcutCatch", "Shortcut name: " + intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME));
//            }

            }
        } catch (Exception e) {
            Log.e("LaunchReceiver", e.getMessage(), e);
        }

    }
}
