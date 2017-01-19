package com.quaap.launchtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.quaap.launchtime.components.AppShortcut;
import com.quaap.launchtime.db.DB;

/**
 * Created by tom on 1/15/17.
 * <p>
 * Copyright (C) 2017  tom
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
public class LaunchReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            Uri data = intent.getData();
            String packageName = data.getEncodedSchemeSpecificPart();
            Log.i("InstallCatch", "The installed package is: " + packageName);

            try {
                PackageManager pm = context.getPackageManager();

                Intent packageIntent = pm.getLaunchIntentForPackage(packageName);
                ResolveInfo ri = context.getPackageManager().resolveActivity(packageIntent, 0);

                AppShortcut app = AppShortcut.createAppShortcut(context, context.getPackageManager(), ri);
                DB db = ((GlobState)context.getApplicationContext()).getDB();
                if (db.addApp(app)) {
                    db.addAppCategoryOrder(app.getCategory(), app.getActivityName());
                    Toast.makeText(context, app.getLabel() + " was installed into " + db.getCategoryDisplay(app.getCategory()), Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e("InstallCatch", "Could not get " + packageName, e);
            }

        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            Uri data = intent.getData();
            String packageName = data.getEncodedSchemeSpecificPart();
            Log.i("RemoveCatch", "The uninstalled package is: " + packageName);

            DB db = ((GlobState)context.getApplicationContext()).getDB();
            db.deleteApp(packageName,true);
        } else if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
            Log.d("ShortcutCatch", "intent received");
            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                Log.d("ShortcutCatch", "Shortcut ID: " + intent.getIntExtra(Intent.EXTRA_TEXT, 0));
                Log.d("ShortcutCatch", "Shortcut name: " + intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME));
            }

        }

    }
}
