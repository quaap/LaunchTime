package com.quaap.launchtime.apps;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

public class Badger {

    public static final String ANY_ACT = "ANY";
    private BadgerCountChangeListener badgerCountChangeListener;

    private Map<ComponentName,Integer> unreadCount = new HashMap<>();

    private SharedPreferences prefs;

    public Badger(Context context) {
        prefs = context.getSharedPreferences("badges", Context.MODE_PRIVATE);
        for (String cname: prefs.getAll().keySet()) {
            unreadCount.put(ComponentName.unflattenFromString(cname), (prefs.getInt(cname, 0)));
        }
    }
    public void setUnreadCount(String packageName, int count) {
        setUnreadCount(null, packageName, count);
    }

    public void setUnreadCount(String activityName, String packageName, int count) {
        if (activityName==null) {
            activityName= ANY_ACT;
        }
        ComponentName compName = new ComponentName(packageName, activityName);
        setUnreadCount(compName,count);
    }

    public void setUnreadCount(ComponentName compName, int count) {
        if (count==0) {
            unreadCount.remove(compName);
            prefs.edit().remove(compName.flattenToString()).apply();
        } else {
            unreadCount.put(compName, count);
            prefs.edit().putInt(compName.flattenToString(), count).apply();
        }
        if (badgerCountChangeListener!=null) {
            if (compName.getClassName().equals(ANY_ACT)) {
                if (!containsFullName(compName.getPackageName())) {
                    badgerCountChangeListener.badgerCountChanged(compName.getPackageName(), count);
                }
            } else {
                badgerCountChangeListener.badgerCountChanged(compName, count);
            }

            Log.d("unread count", compName.toString() + " " + count);
        }
    }

    public boolean containsFullName(String packageName) {
        for (ComponentName cn: unreadCount.keySet()) {
            if (cn.getPackageName().equals(packageName) && !cn.getClassName().equals(ANY_ACT)) {
                return true;
            }
        }
        return false;
    }

    public int getUnreadCount(ComponentName compname) {
        Integer count = unreadCount.get(compname);
        if (count==null) {
            count = unreadCount.get(new ComponentName(compname.getPackageName(), ANY_ACT));
            if (count==null) {
                return 0;
            }
        }
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
        void badgerCountChanged(String packagename, int count);
    }
}
