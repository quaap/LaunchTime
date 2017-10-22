package com.quaap.launchtime.apps;

import android.content.ComponentName;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tom on 10/21/17.
 */

public class Badger {

    private BadgerCountChangeListener badgerCountChangeListener;

    private Map<ComponentName,Integer> unreadCount = new HashMap<>();

    public void setUnreadCount(String activityName, String packageName, int count) {
        ComponentName compName = new ComponentName(packageName,activityName);
        unreadCount.put(compName, count);
        if (badgerCountChangeListener!=null) badgerCountChangeListener.badgerCountChanged(compName, count);
    }

    public int getUnreadCount(String activityName, String packageName) {
        return getUnreadCount(new ComponentName(packageName,activityName));
    }

    public int getUnreadCount(ComponentName compname) {
        Integer count = unreadCount.get(compname);
        if (count==null) return 0;
        return count;
    }

    public void setBadgerCountChangeListener(BadgerCountChangeListener badgerCountChangeListener) {
        this.badgerCountChangeListener = badgerCountChangeListener;
    }

    public interface BadgerCountChangeListener {
        void badgerCountChanged(ComponentName compname, int count);
    }
}
