package com.quaap.launchtime;

import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.quaap.launchtime.apps.Badger;

import java.util.HashMap;
import java.util.Map;

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

public class NotificationListener extends NotificationListenerService {

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Msg","Notification service started");
    }


    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d("Msg","onListenerConnected");
        pollNotifyCounts();
    }

    private void pollNotifyCounts() {
        pollNotifyCounts(null);
    }

    private void pollNotifyCounts(StatusBarNotification del) {
        try {
            Map<String, Integer> counts = new HashMap<>();

            if (del != null) {
                counts.put(del.getPackageName(), 0);
            }

            for (StatusBarNotification sbn : getActiveNotifications()) {
                Integer count;

                int number = sbn.getNotification().number;
                if (Build.VERSION.SDK_INT >= 26 || number > 0) {
                    count = number;
                } else {
                    count = counts.get(sbn.getPackageName());
                    if (count == null) count = 0;
                    count++;
                }
                counts.put(sbn.getPackageName(), count);
            }

            if (counts.size() > 0) {
                Badger badger = GlobState.getBadger(this);
                for (String packageName : counts.keySet()) {
                    badger.setUnreadCount(packageName, counts.get(packageName));
                }
            }
        } catch (Exception | Error e) {
            Log.d("NotifyListener", e.getMessage(),e);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d("Msg","Notification Posted: " +sbn.getPackageName() + " " + sbn.getNotification().number);
        pollNotifyCounts();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d("Msg","Notification Removed: " + sbn.getPackageName() + " " + sbn.getNotification().number);
        pollNotifyCounts(sbn);
    }


}
