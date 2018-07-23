package com.quaap.launchtime;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.quaap.launchtime.apps.AppLauncher;
import com.quaap.launchtime.components.Categories;
import com.quaap.launchtime.components.ExceptionHandler;
import com.quaap.launchtime.components.SpecialIconStore;
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


public class ShortcutReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        if (GlobState.enableCrashReporter && !BuildConfig.DEBUG) Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(context));
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


                        String intentaction = intent2.getAction();
                        String shortcutLabel = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
//                        for (String key: intent2.getExtras().keySet()) {
//                            Log.d("ShortcutCatch", " extra2: " + key + " = " + intent2.getExtras().get(key));
//                        }

                        Log.d("ShortcutCatch", "intent2.action=" + intent2.getAction());
                        Log.d("ShortcutCatch", "uri=" + data);
                        if (cn != null) {
                            Log.d("ShortcutCatch", "cn2package=" + cn.getPackageName() + ", cn2classname=" + cn.getClassName());

                            addLink(context, shortcutLabel, data, cn, receivedicon);
                        } else if (intentaction!=null){
                            Log.d("ShortcutCatch", "intentaction=" + intentaction + " uri=" + data);
                            addLink(context, intentaction, shortcutLabel, data, receivedicon);
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

    public void addLink(Context context, String action, String label, Uri uri, Bitmap bitmap) {
        DB db = GlobState.getGlobState(context).getDB();

        String catID = MainActivity.getLatestCategory();
        if (catID==null || catID.isEmpty()) {
            catID = Categories.getCategoryForAction(context, action);
            if (catID.equals(Categories.CAT_OTHER)) {
                catID = Categories.getCategoryForUri(context, uri.toString());
            }
        }

        AppLauncher appLauncher = AppLauncher.createActionLink(action, uri,label, catID);
        db.addApp(appLauncher);

        if (bitmap!=null) {
            SpecialIconStore.saveBitmap(context, appLauncher.getComponentName(), bitmap, SpecialIconStore.IconType.Shortcut);
        }

    }

    public void addLink(Context context, String label, Uri uri, ComponentName cn, Bitmap bitmap) {
        DB db = GlobState.getGlobState(context).getDB();

        ComponentName latest = db.getLatestInstall();
        if (latest!=null && System.currentTimeMillis() - db.getLatestInstallTime()<4000) {
            if (latest.equals(cn)) {
                Log.i("ShortcutCatch", "ignoring cn2package=" + cn.getPackageName() + ", cn2classname=" + cn.getClassName());
                return;
            }
        }

        String catID = MainActivity.getLatestCategory();
        if (catID==null || catID.isEmpty()) {
            catID = Categories.getCategoryForComponent(context, cn, true, null);
        }

        AppLauncher appLauncher = AppLauncher.createActionLink(cn.getClassName(), uri, cn.getPackageName(),label, catID);
        db.addApp(appLauncher);

        if (bitmap!=null) {
            SpecialIconStore.saveBitmap(context, appLauncher.getComponentName(), bitmap, SpecialIconStore.IconType.Shortcut);
        }

    }

    public void addOreoLink(Context context, String shortcutid, String packageName, String label, Bitmap bitmap) {

        Log.d("ShortcutOreo", "shortcutid: " + shortcutid);
        Log.d("ShortcutOreo", "packageName: " + packageName);
        Log.d("ShortcutOreo", "label: " + label);

        DB db = GlobState.getGlobState(context).getDB();
        String catID = MainActivity.getLatestCategory();
        if (catID==null || catID.isEmpty()) {
            catID = Categories.getCategoryForPackage(context, packageName, true);
        }
        Log.d("ShortcutCatch", "catID: " + catID);

        AppLauncher appLauncher = AppLauncher.createOreoShortcut(shortcutid, packageName,label,catID);
        db.addApp(appLauncher);

        if (bitmap!=null) {
            SpecialIconStore.saveBitmap(context, appLauncher.getComponentName(), bitmap, SpecialIconStore.IconType.Shortcut);
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
