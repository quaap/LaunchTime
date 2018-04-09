
package com.quaap.launchtime_official;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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

public class UnreadReceiver extends BroadcastReceiver{

    //This works for Google Messages, at least
    private static final String DEFAULT_ACTION = "android.intent.action.BADGE_COUNT_UPDATE";
    private static final String DEFAULT_BADGE_COUNT = "badge_count";
    private static final String DEFAULT_BADGE_PACKAGENAME = "badge_count_package_name";
    private static final String DEFAULT_BADGE_ACTIVITY_NAME = "badge_count_class_name";

    //I'm not sure if these below will ever work because of permissions
    private static final String SONY_ACTION = "com.sonyericsson.home.action.UPDATE_BADGE";
    private static final String SONY_BADGE_COUNT = "com.sonyericsson.home.intent.extra.badge.MESSAGE";
    private static final String SONY_BADGE_SHOW = "com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE";
    private static final String SONY_BADGE_PACKAGENAME = "com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME";
    private static final String SONY_BADGE_ACTIVITY_NAME = "com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME";

    private static final String APEX_ACTION = "com.anddoes.launcher.COUNTER_CHANGED";
    private static final String APEX_BADGE_PACKAGENAME = "package";
    private static final String APEX_BADGE_ACTIVITY_NAME = "class";
    private static final String APEX_BADGE_COUNT = "count";

    private static final String ADW_ACTION = "org.adw.launcher.counter.SEND";
    private static final String ADW_BADGE_PACKAGENAME = "PNAME";
    private static final String ADW_BADGE_ACTIVITY_NAME = "CNAME";
    private static final String ADW_BADGE_COUNT = "COUNT";


    private String lastCountAction;
    private String lastCountActivity;
    private String lastCountPackage;
    private long lastCountTime;
    private int lastCount = -1;

    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            String action = intent.getAction();

            if (action==null) return;

            int badgeCount = 0;
            String badgePackage = null;
            String badgeActivity = null;


            if (action.equals(DEFAULT_ACTION)) {
                badgeCount = intent.getIntExtra(DEFAULT_BADGE_COUNT, 0);
                badgeActivity = intent.getStringExtra(DEFAULT_BADGE_ACTIVITY_NAME);
                badgePackage = intent.getStringExtra(DEFAULT_BADGE_PACKAGENAME);

            } else if (action.equals(APEX_ACTION)) {
                badgeCount = intent.getIntExtra(APEX_BADGE_COUNT, 0);
                badgeActivity = intent.getStringExtra(APEX_BADGE_ACTIVITY_NAME);
                badgePackage = intent.getStringExtra(APEX_BADGE_PACKAGENAME);

            } else if (action.equals(ADW_ACTION)) {
                badgeCount = intent.getIntExtra(ADW_BADGE_COUNT, 0);
                badgeActivity = intent.getStringExtra(ADW_BADGE_ACTIVITY_NAME);
                badgePackage = intent.getStringExtra(ADW_BADGE_PACKAGENAME);

            } else if (action.equals(SONY_ACTION)) {
                if (intent.getBooleanExtra(SONY_BADGE_SHOW, false)) {
                    String bc = intent.getStringExtra(SONY_BADGE_COUNT);
                    if (bc!=null) {
                        try {
                            badgeCount = Integer.parseInt(bc);
                        } catch (NumberFormatException e) {
                            badgeCount = 0;
                        }
                    }
                }
                badgeActivity = intent.getStringExtra(SONY_BADGE_ACTIVITY_NAME);
                badgePackage = intent.getStringExtra(SONY_BADGE_PACKAGENAME);

            } else {
                Log.e("UnreadReceiver", "Unknown badge action '" + action + "' " + intent.toString());
            }


            Log.d("BADGE", action + " " + badgeCount + " " + badgeActivity + " " + badgePackage);

            if (badgeActivity != null && badgePackage != null) {
                if (badgeCount>999) badgeCount = 999;

                //don't update badge if this app just previously got an update,
                // in case an app tries multiple broadcast types
                if (lastCountActivity!=null && badgeActivity.equals(lastCountActivity) &&
                        lastCountPackage!=null && badgePackage.equals(lastCountPackage) &&
                        System.currentTimeMillis() - lastCountTime<500 &&
                        lastCount==badgeCount) {
                    Log.d("UnreadReceiver", "app " + badgeActivity + " " + badgePackage + " using action " + action + " after using " + lastCountAction);

                } else if (badgeCount>=0) {
                    GlobState.getBadger(context).setUnreadCount(badgeActivity, badgePackage, badgeCount);
                }


                lastCountAction = action;
                lastCountTime = System.currentTimeMillis();
                lastCountActivity = badgeActivity;
                lastCountPackage = badgePackage;
                lastCount = badgeCount;

            }


        } catch (Exception | Error e) {
            Log.e("UnreadReceiver", e.getMessage(),e);
        }


    }
}
