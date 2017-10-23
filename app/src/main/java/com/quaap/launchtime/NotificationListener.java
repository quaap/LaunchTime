package com.quaap.launchtime;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * Created by tom on 10/23/17.
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

        for (StatusBarNotification sbn: getActiveNotifications()) {
            //Notification n = sbn.getNotification();
            Log.d("Msg","Notification Posted: " + sbn.getNotification().number);
        }

    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d("Msg","Notification Posted: " + sbn.getNotification().number);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d("Msg","Notification Removed");
    }


}
