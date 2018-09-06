package com.quaap.launchtime_official.widgets;
/*
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

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.util.Log;

import com.quaap.launchtime_official.GlobState;

public class WidgetsRestoredReceiver extends BroadcastReceiver {
    private static final String TAG = "WidgetsRestored";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            try {
                if (AppWidgetManager.ACTION_APPWIDGET_HOST_RESTORED.equals(intent.getAction())) {
                    int[] oldIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_OLD_IDS);
                    int[] newIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                    if (oldIds.length == newIds.length) {

                        Widget widgetHelper = GlobState.getWidgetHelper(context);

                        for (int i = 0; i < newIds.length; i++) {
                            widgetHelper.updateWidgetId(oldIds[i], newIds[i]);
                        }

                    } else {
                        Log.e(TAG, "Invalid host restored received " + intent);
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, t.getMessage(), t);
            }
        }
    }

}
