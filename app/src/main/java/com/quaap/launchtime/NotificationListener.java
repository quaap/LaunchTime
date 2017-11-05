package com.quaap.launchtime;

import android.app.Notification;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tom on 10/23/17.
 */

public class NotificationListener extends NotificationListenerService {

    private Map<String,Integer> counts = new HashMap<>();
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
            setCount(sbn, true);
            Log.d("Msg","Notification Posted: " +sbn.getPackageName() + " " + sbn.getNotification().number);
        }

    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d("Msg","Notification Posted: " +sbn.getPackageName() + " " + sbn.getNotification().number);
        setCount(sbn, true);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d("Msg","Notification Removed: " + sbn.getPackageName() + " " + sbn.getNotification().number);
        setCount(sbn,false);
    }

    private void setCount(StatusBarNotification sbn, boolean add) {
        Integer count;

        if (Build.VERSION.SDK_INT>=26) {
            count = sbn.getNotification().number;
        } else {
            count = counts.get(sbn.getPackageName());
            if (count==null) count=0;
            count = add ? count+1 : count-1;
            counts.put(sbn.getPackageName(), count);
        }
        if (count<0) count=0;
        GlobState.getBadger(this).setUnreadCount(sbn.getPackageName(), count);
    }


}
