package com.quaap.launchtime_official.apps;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

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

    private BadgerCountChangeListener badgerCountChangeListener;

    private final Map<ComponentName,Integer> unreadCount = new HashMap<>();

    private final SharedPreferences prefs;

    public Badger(Context context) {
        prefs = context.getSharedPreferences("badges", Context.MODE_PRIVATE);
        for (String cname: prefs.getAll().keySet()) {
            ComponentName cn = ComponentName.unflattenFromString(cname);
            if (cn!=null) unreadCount.put(cn, (prefs.getInt(cname, 0)));
        }
    }

    public void setUnreadCount(String activityName, String packageName, int count) {
        ComponentName compName = new ComponentName(packageName, activityName);
        setUnreadCount(compName,count);
    }

    private void setUnreadCount(ComponentName compName, int count) {
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
