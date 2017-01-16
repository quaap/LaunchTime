package com.quaap.launchtime;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.quaap.launchtime.components.AppShortcut;
import com.quaap.launchtime.components.IconCache;
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
public class ShortcutReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("com.android.launcher.action.INSTALL_SHORTCUT".equals(action)) {
            Log.d("ShortcutCatch", "intent received");
            Bundle extras = intent.getExtras();
            if (extras != null) {

                try {
                    Log.d("ShortcutCatch", "Shortcut name: " + intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME));
                    Intent intent2 = (Intent) extras.get(Intent.EXTRA_SHORTCUT_INTENT);
                    Bitmap receivedicon = (Bitmap) extras.get(Intent.EXTRA_SHORTCUT_ICON);
                    if (intent2 != null) {
                        Uri data = intent2.getData();
                        ComponentName cn = intent2.getComponent();
                        String shortcutLabel = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

                        Log.d("ShortcutCatch", "uri=" + data);
                        if (cn != null) {
                            Log.d("ShortcutCatch", "cn2package=" + cn.getPackageName() + ", cn2classname=" + cn.getClassName());


                            addShortcut(context, shortcutLabel, data, cn, receivedicon);
                        }
                    }
                } catch (Exception e) {
                    Log.e("ShortcutCatch", "Can't make shortcutlink", e);
                }

            }


        } else {
            Log.d("ShortcutCatch", "unknown intent received: " + action);
        }

    }

    private void addShortcut(Context context, String label, Uri uri, ComponentName cn, Bitmap bitmap) {

        AppShortcut appshortcut = AppShortcut.createAppShortcut(cn.getClassName(), uri, cn.getPackageName(),label, null,false);
        DB db = ((GlobState)context.getApplicationContext()).getDB();
        db.addApp(appshortcut);

        if (bitmap!=null) {
            IconCache.saveBitmap(context, appshortcut.getActivityName(), bitmap);
        }


    }

    //            for (String key: extras.keySet()) {
    //                Log.d("ShortcutCatch", " extra: " + key + " = " + extras.get(key));
    //            }
    //Intent.EXTRA_SHORTCUT_INTENT
    //Intent.EXTRA_SHORTCUT_NAME
    //Intent.EXTRA_SHORTCUT_ICON
    //  D/ShortcutCatch:  extra: android.intent.extra.shortcut.INTENT = Intent { dat=http://www.cracked.com/... cmp=acr.browser.lightning/.activity.MainActivity }
    //  D/ShortcutCatch:  extra: android.intent.extra.shortcut.ICON = android.graphics.Bitmap@dc57caf
    //  D/ShortcutCatch:  extra: android.intent.extra.shortcut.NAME = Cracked.com - America's Only Humor Site | Cracked.com
    //  D/ShortcutCatch: Shortcut name: Cracked.com - America's Only Humor Site | Cracked.com
}
