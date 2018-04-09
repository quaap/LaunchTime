package com.quaap.launchtime;

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

import com.quaap.launchtime.apps.Badger;
import com.quaap.launchtime.components.Categories;
import com.quaap.launchtime.components.ExceptionHandler;
import com.quaap.launchtime.components.IconsHandler;
import com.quaap.launchtime.db.DB;
import com.quaap.launchtime.ui.Style;


public class GlobState extends Application implements  DB.DBClosedListener {

    private static DB mDB;

    private IconsHandler mIconsHandler;

    private Style mStyle;

    private Badger badger;

    private LaunchReceiver packrecv;

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
            packrecv = new LaunchReceiver(); //extended from BroadcastReceiver class
            IntentFilter i = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
            i.addDataScheme("package");
            registerReceiver(packrecv, i);

            i = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
            i.addDataScheme("package");
            registerReceiver(packrecv, i);
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
