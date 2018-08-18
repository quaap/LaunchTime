package com.quaap.launchtime.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import com.quaap.launchtime.apps.AppLauncher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final Context mContext;

    private final SharedPreferences mPrefs;


    private final static String TAG = "Widget";
    
    private final Map<ComponentName, AppWidgetHostView> mLoadedWidgets = new HashMap<>();

    public Widget(Context context) {
        mContext = context;
        mPrefs = context.getSharedPreferences("widgets", Context.MODE_PRIVATE);
        
        for (int i=0; i<2; i++) {
            try {
                mAppWidgetManager = AppWidgetManager.getInstance(mContext);
                mAppWidgetHost = new LaunchAppWidgetHost(mContext.getApplicationContext(), WIDGET_HOST_ID);
                mAppWidgetHost.startListening();
                break;
            } catch (RuntimeException | Error e) {
                Log.e("LaunchWidget", "Couldn't start appwidgethost", e);
                if (i==1) {
                    Toast.makeText(context, "System error: widgets not available", Toast.LENGTH_LONG).show();
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    Log.e(TAG, e1.getMessage());
                }
            }
        }

        //mAppWidgetHost.deleteHost();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for (int oid: mAppWidgetHost.getAppWidgetIds()) {
                AppWidgetProviderInfo provider = mAppWidgetManager.getAppWidgetInfo(oid);
                if (provider==null) continue;
                Log.d(TAG, "Widget is allocated: " + provider.provider);
            }
        }

    }


    public AppWidgetHostView getOrCreateWidget(Activity parent, ComponentName provider) {
        AppWidgetHostView hostView = getLoadedAppWidgetHostView(provider);
        if (hostView == null) {
            int id = getWidgetId(provider);
            if (id!=-1) {
                Log.d(TAG, "loading widget from id " + provider);
                hostView =createWidgetFromId(id);
                if (hostView==null) {
                    removeWidget(provider);
                    return null;
                }
            }

            if (hostView==null) {
                Log.d(TAG, "creating new widget " + provider);
                hostView = loadWidget(parent, provider);
            }

            if (hostView==null) {
                Log.d(TAG, "AppWidgetHostView was null for " + provider);
                // db().deleteApp(app.getActivityName());
                return null;
            }
            saveLoadedWidget(provider, hostView);
        }
        return hostView;
    }

    public AppWidgetHostView getAppWidgetHostView(AppLauncher appitem) {
        return getLoadedAppWidgetHostView(appitem.getComponentName());
    }

    public AppWidgetHostView getLoadedAppWidgetHostView(ComponentName cn) {
        return mLoadedWidgets.get(cn);
    }

    private int getWidgetId(ComponentName cn) {
        return mPrefs.getInt(cn.toShortString(), -1);
    }

    private void saveWidgetId(ComponentName cn, int id) {
        mPrefs.edit().putInt(cn.toShortString(), id).apply();
    }


    public void saveLoadedWidget(ComponentName cn, AppWidgetHostView hostView) {
        saveWidgetId(cn, hostView.getAppWidgetId());
        mLoadedWidgets.put(cn, hostView);
    }


    public void removeWidget(ComponentName cn) {
        int id = getWidgetId(cn);
        mAppWidgetHost.deleteAppWidgetId(id);
        mPrefs.edit().remove(cn.toShortString()).apply();
        AppWidgetHostView widv = mLoadedWidgets.remove(cn);

        for (WidgetChangedListener wl: mWidgetChangedListeners) {
            wl.onWidgetRemoved(cn, widv);
        }

    }


    public void updateWidgetId(int oldId, int newId) {
        AppWidgetProviderInfo provider = mAppWidgetManager.getAppWidgetInfo(newId);
        AppWidgetHostView w = mLoadedWidgets.get(provider.provider);
        if (w!=null) {
            w.setAppWidget(newId, provider);
        }
        saveWidgetId(provider.provider, newId);
        mAppWidgetHost.deleteAppWidgetId(oldId);
    }

    public void done() {
        mWidgetChangedListeners.clear();
        mAppWidgetHost.stopListening();
    }

    public void delete() {
        for (ComponentName cn: new ArrayList<>(mLoadedWidgets.keySet())) {
            Log.d(TAG, "removing widget " + cn);
            removeWidget(cn);
        }
        mLoadedWidgets.clear();

        mAppWidgetHost.deleteHost();
    }

    public void popupSelectWidget(Activity parent) {
        try {
            // Allocate widget id and start widget selection activity
            int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
            Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
            pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            addEmptyData(pickIntent); // This is needed work around some weird bug.
            parent.startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
        } catch (Throwable t) {
            Log.e(TAG, t.getMessage(), t);
        }
    }


    private AppWidgetHostView createWidget(Intent data) {
        // Get the widget id
        Bundle extras = data.getExtras();
        if (extras==null) return null;
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

        return createWidgetFromId(appWidgetId);
    }

    private AppWidgetHostView createWidgetFromId(int widget_id) {
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(widget_id);

        if (appWidgetInfo==null) {
            return null;
        }
        // if (checkBindPermission(widget_id, appWidgetInfo.provider)) return null;

        // Create the host view
        AppWidgetHostView hostView = mAppWidgetHost.createView(mContext, widget_id, appWidgetInfo);
        hostView.setAppWidget(widget_id, appWidgetInfo);

        return hostView;
    }


    public AppWidgetHostView loadWidget(Activity parent, AppLauncher app) {
        //ComponentName cn = new ComponentName(app.getPackageName(), app.getActivityName());

        return loadWidget(parent, app.getComponentName());
    }

    private AppWidgetHostView loadWidget(Activity parent, ComponentName cn) {


        Log.d(TAG, "Loaded from db: " + cn.getClassName() + " - " + cn.getPackageName());
        // Check that there actually is a widget in the database
        if (cn.getPackageName().isEmpty() && cn.getClassName().isEmpty()) {
            Log.d(TAG, "DB was empty");
            return null;
        }

        final List<AppWidgetProviderInfo> infos = mAppWidgetManager.getInstalledProviders();

        // Get AppWidgetProviderInfo
        AppWidgetProviderInfo appWidgetInfo = null;

        // Iterate through all infos, trying to find the desired one
        for (final AppWidgetProviderInfo info : infos) {
            if (info.provider.equals(cn)) {
                // We found it!
                appWidgetInfo = info;
                break;
            }
        }
        if (appWidgetInfo == null) {
            Log.d(TAG, "app info was null");
            return null; // Stop here
        }

        // Allocate the hosted widget id
        int appWidgetId = mAppWidgetHost.allocateAppWidgetId();

        if (checkBindPermission(parent, appWidgetId, appWidgetInfo)) return null;

        Log.d(TAG, "Allowed to bind");
        Log.d(TAG, "creating widget");


        // Create the host view
        AppWidgetHostView hostView = mAppWidgetHost.createView(mContext, appWidgetId, appWidgetInfo);

        // Set the desired widget
        hostView.setAppWidget(appWidgetId, appWidgetInfo);

        return hostView;
    }

    private boolean checkBindPermission(final Activity parent, final int appWidgetId, final AppWidgetProviderInfo appWidgetInfo) {
        try {
            boolean allowed_to_bind = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, appWidgetInfo.provider);


            // Ask the user to allow this app to have access to their widgets
            if (!allowed_to_bind) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "asking for permission");
                        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, appWidgetInfo.provider);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, appWidgetInfo.getProfile());
                        }

                        addEmptyData(intent);
                        parent.startActivityForResult(intent, REQUEST_BIND_APPWIDGET);

                    }
                }, 500);
                return true;
            }
        } catch( Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
        return false;
    }

    private AppWidgetHostView configureWidget(Activity parent, Intent data) {
        // Get the selected widget information
        Bundle extras = data.getExtras();
        try {
            if (extras==null) return null;
            int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
            if (appWidgetInfo.configure != null) {
                // If the widget wants to be configured then start its configuration activity
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                intent.setComponent(appWidgetInfo.configure);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                parent.startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
            } else {
                // Otherwise simply create it
                return createWidget(data);

            }
        } catch (Exception | Error e) {
            Log.e(TAG, e.getMessage(), e);
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

//    public List<Integer> getAppWidgetIds() {
//        return mAppWidgetHost.getAppWidgetIds();
//    }

    public AppWidgetHostView onActivityResult(Activity parent, int requestCode, int resultCode, Intent data) {

        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + " resultCode=" + resultCode);
        // listen for widget manager response
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_APPWIDGET) {
                Log.d(TAG, "configureWidget");
                return configureWidget(parent, data);
            } else if (requestCode == REQUEST_CREATE_APPWIDGET || requestCode == REQUEST_BIND_APPWIDGET) {
                Log.d(TAG, "createWidget");
                return createWidget(data);
            } else {
                Log.d(TAG, "unknown RESULT_OK");
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.d(TAG, "RESULT_CANCELED");
            if (data!=null) {
                int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                if (appWidgetId != -1) {
                    mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                }
            }

        }
        return null;
    }


    private List<WidgetChangedListener> mWidgetChangedListeners = new ArrayList<>();

    public void addWidgetChangedListener(WidgetChangedListener listener) {
        if(!mWidgetChangedListeners.contains(listener)) {
            mWidgetChangedListeners.add(listener);
        }
    }

    public void removeWidgetChangedListener(WidgetChangedListener listener) {
        mWidgetChangedListeners.remove(listener);
    }


    public interface WidgetChangedListener {
        void onWidgetRemoved(ComponentName provider, AppWidgetHostView view);
    }

}
