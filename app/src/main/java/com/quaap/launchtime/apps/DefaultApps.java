package com.quaap.launchtime.apps;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

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
public class DefaultApps {

    //QuickBar
    public static void checkDefaultApps(final Context context, List<AppLauncher> launchers, List<ComponentName> quickRowOrder) {
        checkDefaultApps(context,launchers,quickRowOrder,null);
    }

    public static void checkDefaultApps(final Context context, List<AppLauncher> launchers, List<ComponentName> quickRowOrder, List<String> onlyTypes) {
        if (quickRowOrder.isEmpty()) {
            Map<String, List<String>> defactivities = getDefaultActivities(context);
           // boolean addeddefault = false;
            int max = 1;
            for (List<String> tests : defactivities.values()) {
                if (tests.size() > max) max = tests.size();
            }

            AppLauncher firstapp = null;
            for (int i = 0; i < max; i++) { // try the tests in order.

                for (AppLauncher app : launchers) {
                    if (firstapp == null) firstapp = app;
                    //Log.d("Trying: ", app.getActivityName() + " " + app.getPackageName());
                    //try the app for each one of the activities
                    for (Iterator<Map.Entry<String, List<String>>> defactit = defactivities.entrySet().iterator(); defactit.hasNext(); ) {
                        Map.Entry<String, List<String>> defactent = defactit.next();

                        if (onlyTypes!=null && !onlyTypes.contains(defactent.getKey())) continue;

                        //if we have a test, search the app name
                        if (defactent.getValue().size() > i) {
                            String test = defactent.getValue().get(i);
                            if (contains(app, test)) {
                                Log.d("Using: ", app.getActivityName() + " " + app.getPackageName() + " for " + defactent.getKey());
                                quickRowOrder.add(app.getComponentName());
                                defactit.remove(); // remove this group
                               // addeddefault = true;
                                break;  //we're using this app for something
                            }
                        }
                    }
                }

            }
            if (quickRowOrder.isEmpty() && firstapp != null) { //nothing found? add first app found.
                quickRowOrder.add(firstapp.getComponentName());
            }
//            String toastmsg = null;

//            if (addeddefault) {
//                toastmsg = "Don't like the apps in your Quickbar? Long click and drag them away!";
//            } else if (quickRowOrder.size() < 3) {
//                toastmsg = "You can add more apps to your Quickrow at the bottom of the screen.";
//            }
//            if (toastmsg != null) {
//                final String toastmsgfinal = toastmsg;
//                quickRow.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(context, toastmsgfinal, Toast.LENGTH_LONG).show();
//                    }
//                }, 3000);
//            }
        }
    }

    public static boolean contains(AppLauncher app, String test) {
        return app.getActivityName().toLowerCase().contains(test.toLowerCase())
                || app.getPackageName().toLowerCase().contains(test.toLowerCase());
    }


    public static Map<String, List<String>> getDefaultActivities(Context context) {

        Map<String, List<String>> activities = new TreeMap<>();

        try {

            ComponentName browseapp = getpkg(context, Intent.ACTION_VIEW, "http://www", null);
            activities.put("browser", Arrays.asList(browseapp.getPackageName(),
                    "duckduckgo", "web.browser", "webbrowser", "opera.", "dolphin", "firefox", "mozilla", "chromium",
                    "uc.browser", "brave.browser", "TunnyBrowser", "emmx", "chrome",
                    ".browser", "browser"));

            ComponentName msgapp = getpkg(context, Intent.ACTION_MAIN, null, Intent.CATEGORY_APP_MESSAGING);
            activities.put("msg", Arrays.asList(msgapp.getClassName(), msgapp.getPackageName(), "messag", "msg", "sms", "messen", "mms", "chat", "irc" ));

            ComponentName camapp = getpkg(context, MediaStore.ACTION_IMAGE_CAPTURE, null, null);
            activities.put("camera", Arrays.asList(camapp.getPackageName(), "cameraApp", "CameraActivity", "camera.Camera", ".camera", "kamera", "camera", "kam", "cam", "photo", "foto"));

            ComponentName phoneapp = getpkg(context, Intent.ACTION_DIAL, "tel:411", null);
            activities.put("phone", Arrays.asList(phoneapp.getClassName(), phoneapp.getPackageName(), "DialtactsActivity", "dial", "phone", "fone", "contacts"));

            ComponentName emailapp = getpkg(context, Intent.ACTION_SENDTO, "mailto:", null);
            activities.put("email", Arrays.asList(emailapp.getPackageName(),
                    "k9", "inbox", "outlook", "email", "mail", "com.google.android.gm"));

            ComponentName musicapp = getpkg(context, Intent.ACTION_VIEW, "file:", "audio/*");
            activities.put("music", Arrays.asList(musicapp.getPackageName(),
                    "com.spotify.music",
                    "com.pandora.android",
                    "com.google.android.music",
                    "org.gateshipone.odyssey",
                    "org.fitchfamily.android.symphony",
                    "ch.blinkenlights.android.vanilla",
                    "com.rhapsody",
                    "musicplayer",  "music.player",
                    "mp3", "music", "tunein", "radio", "song"));


        } catch (Exception e) {
            Log.e("LaunchTime", e.getMessage(), e);
        }

        return activities;
    }

    public static ComponentName getpkg(Context context, String intentaction, String intenturi, String intentcategory) {
        return getpkg(context, intentaction, intenturi, intentcategory, null);
    }

    public static ComponentName getpkg(Context context, String intentaction, String intenturi, String intentcategory, String intenttype) {

        ComponentName cn = new ComponentName("_fakename", "_fakename");

        try {
            Intent intent;
            if (intenturi == null) {
                intent = new Intent(intentaction);
            } else {
                intent = new Intent(intentaction, Uri.parse(intenturi));
            }
            if (intentcategory != null) {
                intent.addCategory(intentcategory);
            }
            if (intenttype!=null) {
                intent.setType(intenttype);
            }

            ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

            if (resolveInfo != null) {
                Log.d("sh", resolveInfo.activityInfo.name + " " + resolveInfo.activityInfo.packageName);
                if (resolveInfo.activityInfo.name != null && !resolveInfo.activityInfo.name.equals("com.android.internal.app.ResolverActivity")) {

                    cn = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                }
            }
        } catch (Throwable t) {
            Log.e("DefApps", t.getMessage(), t);
        }
        return cn;

    }

}
