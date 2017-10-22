package com.quaap.launchtime.apps;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by tom on 10/21/17.
 */

public class Badger {

    private BadgerCountChangeListener badgerCountChangeListener;

    private Map<ComponentName,Integer> unreadCount = new HashMap<>();

    private SharedPreferences prefs;

    public Badger(Context context) {
        prefs = context.getSharedPreferences("badges", Context.MODE_PRIVATE);
        for (String cname: prefs.getAll().keySet()) {
            unreadCount.put(ComponentName.unflattenFromString(cname), (prefs.getInt(cname, 0)));
        }
    }

    public void setUnreadCount(String activityName, String packageName, int count) {
        ComponentName compName = new ComponentName(packageName, activityName);
        setUnreadCount(compName,count);
    }

    public void setUnreadCount(ComponentName compName, int count) {
        unreadCount.put(compName, count);
        if (count==0) {
            prefs.edit().remove(compName.flattenToString()).apply();
        } else {
            prefs.edit().putInt(compName.flattenToString(), count).apply();
        }
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

    public void clearAll() {
        Set<ComponentName> comps = new HashSet<>(unreadCount.keySet());
        unreadCount.clear();
        for (ComponentName compName: comps) {
            setUnreadCount(compName, 0);
        }

    }

    public interface BadgerCountChangeListener {
        void badgerCountChanged(ComponentName compname, int count);
    }
}
