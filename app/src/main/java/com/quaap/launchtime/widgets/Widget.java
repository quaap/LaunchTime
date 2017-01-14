package com.quaap.launchtime.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.quaap.launchtime.components.AppShortcut;

import java.util.ArrayList;
import java.util.List;

/**
 * Modified from Silverfish:
 * Copyright 2016 Stanislav Pintjuk
 * E-mail: stanislav.pintjuk@gmail.com
 * <p>
 * <p>
 * Copyright (C) 2017  tom kliethermes
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */


public class Widget {
    private static final int WIDGET_HOST_ID = 3455;
    final private int REQUEST_PICK_APPWIDGET = 3535;
    final private int REQUEST_CREATE_APPWIDGET = 6756;
    final private int REQUEST_BIND_APPWIDGET = 5645;
    private AppWidgetManager mAppWidgetManager;
    private LaunchAppWidgetHost mAppWidgetHost;
    private Activity mParent;


    public Widget(Activity parent) {
        mParent = parent;

        mAppWidgetManager = AppWidgetManager.getInstance(mParent);
        mAppWidgetHost = new LaunchAppWidgetHost(mParent.getApplicationContext(), WIDGET_HOST_ID);
        mAppWidgetHost.startListening();

    }

    public void popupSelectWidget() {
        // Allocate widget id and start widget selection activity
        int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        addEmptyData(pickIntent); // This is needed work around some weird bug.
        mParent.startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
    }


    private AppWidgetHostView createWidget(Intent data) {
        // Get the widget id
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

        return createWidgetFromId(appWidgetId);
    }

    private AppWidgetHostView createWidgetFromId(int widget_id) {
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(widget_id);

        // Create the host view
        AppWidgetHostView hostView = mAppWidgetHost.createView(mParent, widget_id, appWidgetInfo);
        hostView.setAppWidget(widget_id, appWidgetInfo);

        return hostView;
        // And place the widget in widget area and save.
        // placeWidget(hostView);
        //sqlHelper.updateWidget(appWidgetInfo.provider.getPackageName(), appWidgetInfo.provider.getClassName());
    }


    public AppWidgetHostView loadWidget(AppShortcut app) {
        ComponentName cn = new ComponentName(app.getPackageName(), app.getActivityName());

        Log.d("Widget creation", "Loaded from db: " + cn.getClassName() + " - " + cn.getPackageName());
        // Check that there actually is a widget in the database
        if (cn.getPackageName().isEmpty() && cn.getClassName().isEmpty()) {
            Log.d("Widget creation", "DB was empty");
            return null;
        }
        Log.d("Widget creation", "DB was not empty");


        final List<AppWidgetProviderInfo> infos = mAppWidgetManager.getInstalledProviders();

        // Get AppWidgetProviderInfo
        AppWidgetProviderInfo appWidgetInfo = null;
        // Just in case you want to see all package and class names of installed widget providers,
        // this code is useful
//        for (final AppWidgetProviderInfo info : infos) {
//            Log.d("AD3", info.provider.getPackageName() + " / "
//                    + info.provider.getClassName());
//        }
        // Iterate through all infos, trying to find the desired one
        for (final AppWidgetProviderInfo info : infos) {
            if (info.provider.getClassName().equals(cn.getClassName()) &&
                    info.provider.getPackageName().equals(cn.getPackageName())) {
                // We found it!
                appWidgetInfo = info;
                break;
            }
        }
        if (appWidgetInfo == null) {
            Log.d("Widget creation", "app info was null");
            return null; // Stop here
        }

        // Allocate the hosted widget id
        int appWidgetId = mAppWidgetHost.allocateAppWidgetId();

        boolean allowed_to_bind = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn);

        // Ask the user to allow this app to have access to their widgets
        if (!allowed_to_bind) {
            Log.d("Widget creation", "asking for permission");
            Intent i = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
            Bundle args = new Bundle();
            args.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            args.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, cn);
            if (Build.VERSION.SDK_INT >= 21) {
                args.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, null);
            }
            i.putExtras(args);
            mParent.startActivityForResult(i, REQUEST_BIND_APPWIDGET);
            return null;
        } else {

            Log.d("Widget creation", "Allowed to bind");
            Log.d("Widget creation", "creating widget");
            //Intent i = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
            //createWidgetFromId(appWidgetId);
        }
        // Create the host view
        AppWidgetHostView hostView = mAppWidgetHost.createView(mParent, appWidgetId, appWidgetInfo);

        // Set the desired widget
        hostView.setAppWidget(appWidgetId, appWidgetInfo);

        return hostView;
    }

    private AppWidgetHostView configureWidget(Intent data) {
        // Get the selected widget information
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo.configure != null) {
            // If the widget wants to be configured then start its configuration activity
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            mParent.startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            // Otherwise simply create it
            return createWidget(data);
        }
        return null;
    }


    private void addEmptyData(Intent pickIntent) {
        // This is needed work around some weird bug.
        // This will simply add some empty data to the intent.
        ArrayList customInfo = new ArrayList();
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
        ArrayList customExtras = new ArrayList();
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
    }


    public ComponentName getComponentNameFromIntent(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);


        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        return appWidgetInfo.provider;

    }

    public ComponentName onActivityResult(int requestCode, int resultCode, Intent data) {

        // listen for widget manager response
        try {
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == REQUEST_PICK_APPWIDGET || requestCode == REQUEST_CREATE_APPWIDGET || requestCode == REQUEST_BIND_APPWIDGET) {
                    return getComponentNameFromIntent(data);
                }
            }
        } finally {
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
        return null;
    }

    public static class AppShortcutWidgetHostView {

        private AppShortcut mApp;
        private AppWidgetHostView mAppWidgetHostView;

        public AppShortcutWidgetHostView(AppWidgetHostView appWidgetHostView, AppShortcut app) {
            mApp = app;
            mAppWidgetHostView = appWidgetHostView;
        }


    }

}
