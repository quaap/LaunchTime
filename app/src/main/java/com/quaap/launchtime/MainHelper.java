package com.quaap.launchtime;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.quaap.launchtime.components.AppShortcut;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
public class MainHelper {

    //QuickBar

    public static void checkDefaultApps(final Context context, List<AppShortcut> shortcuts, List<ComponentName> quickRowOrder, View quickRow) {
        if (quickRowOrder.isEmpty()) {
            Map<String, List<String>> defactivities = getDefaultActivities(context);
            boolean addeddefault = false;
            int max = 1;
            for (List<String> tests : defactivities.values()) {
                if (tests.size() > max) max = tests.size();
            }

            AppShortcut firstapp = null;
            for (int i = 0; i < max; i++) { // try the tests in order.

                for (AppShortcut app : shortcuts) {
                    if (firstapp == null) firstapp = app;
                    //Log.d("Trying: ", app.getActivityName() + " " + app.getPackageName());
                    //try the app for each one of the activities
                    for (Iterator<Map.Entry<String, List<String>>> defactit = defactivities.entrySet().iterator(); defactit.hasNext(); ) {
                        Map.Entry<String, List<String>> defactent = defactit.next();

                        //if we have a test, search the app name
                        if (defactent.getValue().size() > i) {
                            String test = defactent.getValue().get(i);
                            if (contains(app, test)) {
                                Log.d("Using: ", app.getActivityName() + " " + app.getPackageName() + " for " + defactent.getKey());
                                quickRowOrder.add(app.getComponentName());
                                defactit.remove(); // remove this group
                                addeddefault = true;
                                break;  //we're using this app for something
                            }
                        }
                    }
                }

            }
            if (quickRowOrder.isEmpty() && firstapp != null) { //nothing found? add first app found.
                quickRowOrder.add(firstapp.getComponentName());
            }
            String toastmsg = null;

            if (addeddefault) {
                toastmsg = "Don't like the apps in your Quickbar? Long click and drag them away!";
            } else if (quickRowOrder.size() < 3) {
                toastmsg = "You can add more apps to your Quickrow at the bottom of the screen.";
            }
            if (toastmsg != null) {
                final String toastmsgfinal = toastmsg;
                quickRow.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, toastmsgfinal, Toast.LENGTH_LONG).show();
                    }
                }, 3000);
            }
        }
    }

    public static boolean contains(AppShortcut app, String test) {
        return app.getActivityName().toLowerCase().contains(test.toLowerCase())
                || app.getPackageName().toLowerCase().contains(test.toLowerCase());
    }


    public static Map<String, List<String>> getDefaultActivities(Context context) {

        Map<String, List<String>> activities = new TreeMap<>();


        ComponentName browseapp = getpkg(context, Intent.ACTION_VIEW, "http://", null);
        activities.put("browser", Arrays.asList(
                browseapp.getClassName(), browseapp.getPackageName(),
                "opera", "dolphin", "firefox", "mozilla", "chromium",
                "uc.browser", "brave.browser", "TunnyBrowser", "chrome",
                ".browser", "browser"));

        ComponentName msgapp = getpkg(context, Intent.ACTION_MAIN, null, Intent.CATEGORY_APP_MESSAGING);
        activities.put("msg", Arrays.asList(msgapp.getClassName(), msgapp.getPackageName(), "messag", "msg", "sms"));

        activities.put("camera", Arrays.asList("cameraApp", "CameraActivity", ".camera", "camera", "cam", "photo", "foto"));
        activities.put("phone", Arrays.asList("DialtactsActivity", "dial", "phone", "contacts"));

        activities.put("music", Arrays.asList("music", "mp3", "media", "player"));
        activities.put("email", Arrays.asList("k9", "inbox", "outlook", "mail"));


        return activities;
    }

    public static ComponentName getpkg(Context context, String intentaction, String intenturi, String intentcategory) {

        ComponentName cn = new ComponentName("_fakename", "_fakename");

        Intent intent;
        if (intenturi == null) {
            intent = new Intent(intentaction);
        } else {
            intent = new Intent(intentaction, Uri.parse(intenturi));
        }
        if (intentcategory != null) {
            intent.addCategory(intentcategory);
        }
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, 0);

        if (resolveInfo != null) {
            Log.d("sh", resolveInfo.activityInfo.name + " " + resolveInfo.activityInfo.packageName);

            cn = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
        }
        return cn;

    }

}
