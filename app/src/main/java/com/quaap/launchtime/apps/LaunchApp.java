package com.quaap.launchtime.apps;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.net.Uri;
import android.os.Build;
import android.os.UserHandle;
import android.util.Log;
import android.widget.Toast;

import com.quaap.launchtime.GlobState;
import com.quaap.launchtime.db.DB;

import java.util.Collections;
import java.util.List;

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

public class LaunchApp {


    private static final String TAG = "LT LaunchApp";

    private final Activity activity;

    public LaunchApp(Activity activity) {
        this.activity = activity;
    }

    private DB db() {
        return GlobState.getGlobState(activity).getDB();
    }
    //Run/open the thing that was clicked
    public void launchApp(String activityname, String pkgname) {
        try {
            launchApp(db().getApp(new ComponentName(pkgname, activityname)));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void launchApp(final AppLauncher app) {
        String activityname = app.getLinkBaseActivityName();

        try {

            if (app.isOreoShortcut()) {
                launchOreoShortcut(app);
            } else if (app.isShortcut()) {
                launchShortcut(app);
            } else {
                Intent intent = getAppIntent(app);

                Log.d(TAG, "Launching " + app.getComponentName());
                if (isValidActivity(intent)) {
                    // actually start it
                    activity.startActivity(intent);
                } else {
                    Toast.makeText(activity, "Could not launch item", Toast.LENGTH_LONG).show();
                }
            }

            //log the launch
            if (app.isAppLink()) {
                if (app.isActionLink()) {
                    db().appLaunched(new ComponentName(app.getPackageName(), app.getLinkBaseActivityName()));
                } else {
                    db().appLaunched(app.getBaseComponentName());
                }
            } else {
                db().appLaunched(app.getComponentName());
            }
        } catch (Exception e) {
            Log.d(TAG, "Could not launch " + activityname, e);
            Toast.makeText(activity, "Could not launch item: " + e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
        }
        //showButtonBar(false, true);
    }


    private void launchShortcut(final AppLauncher app) {

        try {
            Intent launchIntent = Intent.parseUri(app.getLinkUri(),0);
            Log.d(TAG, "Launching " + app.getComponentName());
            if (isValidActivity(launchIntent)) {
                // actually start it
                activity.startActivity(launchIntent);
            } else {
                Toast.makeText(activity, "Could not launch item", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    //Thanks to KISS: https://github.com/Neamar/KISS/blob/0e654be99665294f601c58991dd20ae8595572bd/app/src/main/java/fr/neamar/kiss/result/ShortcutsResult.java
    private void launchOreoShortcut(final AppLauncher app) {
        if (Build.VERSION.SDK_INT >= 26) {
            final LauncherApps launcherApps = activity.getSystemService(LauncherApps.class);
            // Only the default launcher is allowed to start shortcuts
            if (launcherApps==null || !launcherApps.hasShortcutHostPermission()) {
                Toast.makeText(activity, "Must be default launcher", Toast.LENGTH_LONG).show();
                return;
            }

            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
            query.setPackage(app.getPackageName());
            query.setShortcutIds(Collections.singletonList(app.getLinkUri()));
            query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);

            List<UserHandle> userHandles = launcherApps.getProfiles();

            Log.d(TAG, "launching " + app.getLinkUri());

            // Find the correct UserHandle, and launch the shortcut.
            boolean disabled = false;
            for (UserHandle userHandle : userHandles) {
                List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(query, userHandle);
                if (shortcuts != null) {
                    for (ShortcutInfo s: shortcuts) {
                        if (s!=null && s.isEnabled()) {
                            launcherApps.startShortcut(s, null, null);
                            return;
                        } else {
                            disabled = true;
                        }
                    }
                }
            }
            Log.d(TAG, "failed to launch " + app.getLinkUri());
            if (app.getLinkUri().matches("^https?://.*")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(app.getLinkUri()));
                //intent.setPackage(app.getPackageName());
                activity.startActivity(intent);
            } else {
                Toast.makeText(activity, "Failed to launch shortcut. " + (disabled?"Shortcut seems to be disabled.": "May be expired."), Toast.LENGTH_LONG).show();
            }

        }
    }

    public static Intent getAppIntent(final AppLauncher app) {
        String activityname = app.getLinkBaseActivityName();
        String packagename = app.getPackageName();

        Intent intent;
        try {
            String uristr = app.getLinkUri();
            Uri uri = null;
            if (uristr != null && !uristr.equals("")) {
                uri = Uri.parse(uristr);
            }

            if (app.isShortcut()) {
                intent = Intent.parseUri(app.getLinkUri(), 0);
            } else if (app.isActionLink()) {
                //Change "CALL" to "DIAL" so we can avoid needing the
                // android.permission.CALL_PHONE permission
                if (activityname.startsWith("android.intent.action.CALL")) {
                    activityname = "android.intent.action.DIAL";
                }

                if (uri == null) {
                    intent = new Intent(activityname);
                } else {
                    intent = new Intent(activityname, uri);
                }


            } else {
                intent = new Intent();
                intent.setClassName(packagename, activityname);
                if (uri != null) {
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setData(uri);
                } else {
                    intent.setAction(Intent.ACTION_MAIN);
                }
            }
            // Log.d("launch", activityname + "  " + uristr);
            //needed to place in the open apps list
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } catch (Exception e) {
            Log.d(TAG, "Could not launch " + activityname, e);
            intent = new Intent();
        }
        return intent;

    }

    public boolean isValidActivity(AppLauncher app) {
        return isValidActivity(getAppIntent(app));
    }

    private boolean isValidActivity(Intent intent) {
        List<ResolveInfo> list = activity.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

}
