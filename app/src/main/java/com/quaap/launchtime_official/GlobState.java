package com.quaap.launchtime_official;

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

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.preference.PreferenceManager;

import com.quaap.launchtime_official.apps.Badger;
import com.quaap.launchtime_official.components.Categories;
import com.quaap.launchtime_official.components.ExceptionHandler;
import com.quaap.launchtime_official.components.IconsHandler;
import com.quaap.launchtime_official.db.DB;
import com.quaap.launchtime_official.ui.Style;


public class GlobState extends Application implements  DB.DBClosedListener {

    private static DB mDB;

    private IconsHandler mIconsHandler;

    private Style mStyle;

    private Badger badger;

    private LaunchReceiver packrecv;
    private UnreadReceiver unreadrecv;
    private ShortcutReceiver shortcutrecv;

    public static GlobState getGlobState(Context context) {
        return (GlobState) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!BuildConfig.DEBUG) Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

        Categories.init(this);

        badger = new Badger(this);

        //this.deleteDatabase(DB.DATABASE_NAME);
        mIconsHandler = new IconsHandler(this);

        mStyle = new Style(this, PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        if (Build.VERSION.SDK_INT >= 26) {
            {
                packrecv = new LaunchReceiver(); //extended from BroadcastReceiver class
                IntentFilter i = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
                i.addDataScheme("package");
                registerReceiver(packrecv, i);

                i = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
                i.addDataScheme("package");
                registerReceiver(packrecv, i);

            }
            {
                unreadrecv = new UnreadReceiver(); //extended from BroadcastReceiver class

                IntentFilter iu = new IntentFilter(UnreadReceiver.DEFAULT_ACTION);
                registerReceiver(unreadrecv, iu);

                iu = new IntentFilter(UnreadReceiver.APEX_ACTION);
                registerReceiver(unreadrecv, iu);

                iu = new IntentFilter(UnreadReceiver.SONY_ACTION);
                registerReceiver(unreadrecv, iu);

                iu = new IntentFilter(UnreadReceiver.ADW_ACTION);
                registerReceiver(unreadrecv, iu);
            }

            {
                shortcutrecv = new ShortcutReceiver();

                IntentFilter i = new IntentFilter("com.android.launcher.action.INSTALL_SHORTCUT");
                registerReceiver(shortcutrecv, i);

            }

        }

    }

    public static Style getStyle(Context context) {
        return ((GlobState) context.getApplicationContext()).mStyle;
    }

    public static IconsHandler getIconsHandler(Context context) {
        return ((GlobState) context.getApplicationContext()).mIconsHandler;
    }


    public static Badger getBadger(Context context) {
        return ((GlobState) context.getApplicationContext()).badger;
    }


    public static ShortcutReceiver getShortcutReceiver(Context context) {
        return ((GlobState) context.getApplicationContext()).shortcutrecv;

    }

    public synchronized DB getDB() {
        if (mDB==null) {
            mDB = DB.openDB(this, this);
        }
        return mDB;
    }

    @Override
    public void onTerminate() {

        if (packrecv!=null) {
            this.unregisterReceiver(packrecv);
        }
        if (shortcutrecv !=null) {
            this.unregisterReceiver(shortcutrecv);
        }
        if (unreadrecv!=null) {
            this.unregisterReceiver(unreadrecv);
        }

        if (mDB != null) {
            mDB.close();
        }

        super.onTerminate();
    }

    @Override
    public void onDBClosed() {
        mDB = null;
    }
}
