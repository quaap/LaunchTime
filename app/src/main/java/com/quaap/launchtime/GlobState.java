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
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.quaap.launchtime.apps.Badger;
import com.quaap.launchtime.components.Categories;
import com.quaap.launchtime.components.ExceptionHandler;
import com.quaap.launchtime.components.IconsHandler;
import com.quaap.launchtime.db.DB;
import com.quaap.launchtime.ui.Style;
import com.quaap.launchtime.widgets.Widget;
import com.quaap.launchtime.widgets.WidgetsRestoredReceiver;

import java.util.concurrent.Executor;

public class GlobState extends Application implements  DB.DBClosedListener {

    private static DB mDB;

    private IconsHandler mIconsHandler;

    private Style mStyle;

    private Badger badger;

    private LaunchReceiver packrecv;
    private UnreadReceiver unreadrecv;
    private ShortcutReceiver shortcutrecv;
    private WidgetsRestoredReceiver widgetrecv;

    private Widget mWidgetHelper;

    private Executor mThreadPool;

    public static final boolean enableCrashReporter
            = GlobState.class.getPackage().getName().equals("com.quaap.launch"+"time");

    public static GlobState getGlobState(Context context) {
        return (GlobState) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (enableCrashReporter && !BuildConfig.DEBUG) Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

        //might as well use the Async pool.
        mThreadPool = AsyncTask.THREAD_POOL_EXECUTOR; //new ThreadPoolExecutor(0,3,30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                Log.d("Global", "Threadpool pre-started");
            }
        });

        Categories.init(this);

        badger = new Badger(this);

        mWidgetHelper = new Widget(this);

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

                i = new IntentFilter(Intent.ACTION_PACKAGE_CHANGED);
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

            {
                widgetrecv = new WidgetsRestoredReceiver();

                IntentFilter i = new IntentFilter("android.appwidget.action.APPWIDGET_HOST_RESTORED");
                registerReceiver(widgetrecv, i);

            }


        }

    }

    public static void execute(Context context, Runnable runnable) {
        ((GlobState) context.getApplicationContext()).mThreadPool.execute(runnable);
    }

    public static Executor getExecutor(Context context) {
        return ((GlobState) context.getApplicationContext()).mThreadPool;
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

    public static Widget getWidgetHelper(Context context) {
        return ((GlobState) context.getApplicationContext()).mWidgetHelper;
    }

    public static Widget deleteAndGetWidgetHelper(Context context) {
        GlobState g = (GlobState) context.getApplicationContext();
        if (g.mWidgetHelper!=null) {
            g.mWidgetHelper.delete();
            g.mWidgetHelper.done();
        }
        g.mWidgetHelper = new Widget(g);
        return g.mWidgetHelper;
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
        //mThreadPool.shutdown();

        if (packrecv!=null) {
            this.unregisterReceiver(packrecv);
        }
        if (shortcutrecv !=null) {
            this.unregisterReceiver(shortcutrecv);
        }
        if (unreadrecv!=null) {
            this.unregisterReceiver(unreadrecv);
        }

        if (widgetrecv!=null) {
            this.unregisterReceiver(widgetrecv);
        }


        if (mDB != null) {
            mDB.close();
        }

        //mThreadPool.shutdownNow();
        super.onTerminate();
    }

    @Override
    public void onDBClosed() {
        mDB = null;
    }
}
