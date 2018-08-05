package com.quaap.launchtime.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import com.quaap.launchtime.apps.AppLauncher;

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
    private final Activity mParent;


    public Widget(Activity parent) {
        mParent = parent;

        for (int i=0; i<2; i++) {
            try {
                mAppWidgetManager = AppWidgetManager.getInstance(mParent);
                mAppWidgetHost = new LaunchAppWidgetHost(mParent.getApplicationContext(), WIDGET_HOST_ID);
                mAppWidgetHost.startListening();
                break;
            } catch (RuntimeException | Error e) {
                Log.e("LaunchWidget", "Couldn't start appwidgethost", e);
                if (i==1) {
                    Toast.makeText(parent, "System error: widgets not available", Toast.LENGTH_LONG).show();
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    Log.e("Wodget", e1.getMessage());
                }
            }
        }

    }

    public void done() {
        mAppWidgetHost.stopListening();
    }

    public void popupSelectWidget() {
        try {
            // Allocate widget id and start widget selection activity
            int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
            Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
            pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            addEmptyData(pickIntent); // This is needed work around some weird bug.
            mParent.startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
        } catch (Throwable t) {
            Log.e("Widget", t.getMessage(), t);
        }
    }


    private AppWidgetHostView createWidget(Intent data) {
        // Get the widget id
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

        return createWidgetFromId(appWidgetId);
    }

    private AppWidgetHostView createWidgetFromId(int widget_id) {
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(widget_id);

       // if (checkBindPermission(widget_id, appWidgetInfo.provider)) return null;

        // Create the host view
        AppWidgetHostView hostView = mAppWidgetHost.createView(mParent, widget_id, appWidgetInfo);
        hostView.setAppWidget(widget_id, appWidgetInfo);

        return hostView;
    }


    public AppWidgetHostView loadWidget(AppLauncher app) {
        ComponentName cn = new ComponentName(app.getPackageName(), app.getActivityName());

        Log.d("LaunchWidgeth", "Loaded from db: " + cn.getClassName() + " - " + cn.getPackageName());
        // Check that there actually is a widget in the database
        if (cn.getPackageName().isEmpty() && cn.getClassName().isEmpty()) {
            Log.d("LaunchWidgeth", "DB was empty");
            return null;
        }

        final List<AppWidgetProviderInfo> infos = mAppWidgetManager.getInstalledProviders();

        // Get AppWidgetProviderInfo
        AppWidgetProviderInfo appWidgetInfo = null;

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
            Log.d("LaunchWidgeth", "app info was null");
            return null; // Stop here
        }

        // Allocate the hosted widget id
        int appWidgetId = mAppWidgetHost.allocateAppWidgetId();

        if (checkBindPermission(appWidgetId, cn)) return null;

        Log.d("LaunchWidgeth", "Allowed to bind");
        Log.d("LaunchWidgeth", "creating widget");


        // Create the host view
        AppWidgetHostView hostView = mAppWidgetHost.createView(mParent, appWidgetId, appWidgetInfo);

        // Set the desired widget
        hostView.setAppWidget(appWidgetId, appWidgetInfo);

        return hostView;
    }

    private boolean checkBindPermission(final int appWidgetId, final ComponentName cn) {
        try {
            boolean allowed_to_bind = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn);


            // Ask the user to allow this app to have access to their widgets
            if (!allowed_to_bind) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("LaunchWidgeth", "asking for permission");
                        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, cn);

                        addEmptyData(intent);
                        mParent.startActivityForResult(intent, REQUEST_BIND_APPWIDGET);

                    }
                }, 500);
                return true;
            }
        } catch( Exception e) {
            Log.e("LaunchTime", e.getMessage());
            return false;
        }
        return false;
    }

    private AppWidgetHostView configureWidget(Intent data) {
        // Get the selected widget information
        Bundle extras = data.getExtras();
        try {
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
        } catch (Exception | Error e) {
            Log.e("LaunchWidgeth", e.getMessage(), e);
        }
        return null;
    }


    private void addEmptyData(Intent pickIntent) {
        // This is needed work around some weird bug.
        // This will simply add some empty data to the intent.
        ArrayList<Parcelable> customInfo = new ArrayList<>();
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
        ArrayList<Parcelable> customExtras = new ArrayList<>();
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
    }


    public ComponentName getComponentNameFromIntent(Intent data) {
        if (data!=null) {
            Bundle extras = data.getExtras();
            if (extras!=null) {
                int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

                if (appWidgetId != -1) {

                    AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
                    if (appWidgetInfo != null) {
                        return appWidgetInfo.provider;
                    }
                }
            }
        }
        return null;
    }

    public void widgetRemoved(int appWidgetId) {

        mAppWidgetHost.deleteAppWidgetId(appWidgetId);

    }

//    public List<Integer> getAppWidgetIds() {
//        return mAppWidgetHost.getAppWidgetIds();
//    }

    public AppWidgetHostView onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("LaunchWidgeth", "onActivityResult: requestCode=" + requestCode + " resultCode=" + resultCode);
        // listen for widget manager response
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_APPWIDGET) {
                Log.d("LaunchWidgeth", "configureWidget");
                return configureWidget(data);
            } else if (requestCode == REQUEST_CREATE_APPWIDGET || requestCode == REQUEST_BIND_APPWIDGET) {
                Log.d("LaunchWidgeth", "createWidget");
                return createWidget(data);
            } else {
                Log.d("LaunchWidgeth", "unknown RESULT_OK");
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.d("LaunchWidgeth", "RESULT_CANCELED");
            if (data!=null) {
                int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                if (appWidgetId != -1) {
                    mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                }
            }

        }
        return null;
    }


}
