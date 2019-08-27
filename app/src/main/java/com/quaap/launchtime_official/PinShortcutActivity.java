package com.quaap.launchtime_official;
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.quaap.launchtime_official.components.IconsHandler;

public class PinShortcutActivity extends Activity {


    @TargetApi(Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Pinshort", "onCreate");

        Intent intent = getIntent();
        //Log.d("Pinshort", "Intent " + intent);
        if (intent == null) {
            finish();
            return;
        }
        LauncherApps launcherApps =  this.getSystemService(LauncherApps.class);
        if (launcherApps==null) {
            finish();
            return;
        }

        //private Widget mWidgetHelper;
        LauncherApps.PinItemRequest mRequest = launcherApps.getPinItemRequest(intent);

        if (mRequest == null) {
            finish();
            return;
        }

        if (mRequest.getRequestType() == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            acceptShortcut(launcherApps, mRequest);
            finish();
            return;
        }

//        if (request.getRequestType() == LauncherApps.PinItemRequest.REQUEST_TYPE_APPWIDGET) {
//
//        }

//        mWidgetHelper = GlobState.getWidgetHelper(this);
//
//        AppWidgetProviderInfo pinfo = mRequest.getAppWidgetProviderInfo(this);
//        if (pinfo==null) {
//            finish();
//            return;
//        }
//
//        //mWidgetHelper.loadWidget(this, pinfo.provider);
//
        finish();

    }

    @TargetApi(Build.VERSION_CODES.O)
    private void acceptShortcut(LauncherApps launcherApps, LauncherApps.PinItemRequest request) {
        ShortcutReceiver shrecv = GlobState.getShortcutReceiver(this);
        if (shrecv == null) {
            return;
        }

        ShortcutInfo si = request.getShortcutInfo();
        if (si == null) {
            return;
        }
        Drawable iconDrawable = launcherApps.getShortcutIconDrawable(si, 0);

        Bitmap icon = null;

        if (iconDrawable != null) {
            icon = IconsHandler.drawableToBitmap(iconDrawable);
        }

        String label = null;
        if (si.getShortLabel() != null) {
            label = si.getShortLabel().toString();

            CharSequence longlabel = si.getLongLabel();
            if (longlabel != null) {
                if (longlabel.toString().startsWith(label)) {
                    label = longlabel.toString();
                } else {
                    label += " " + longlabel;
                }
            }


        }

        shrecv.addOreoLink(this, si.getId(), si.getPackage(), label, icon);

        request.accept();
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//
//        mWidgetHelper.onActivityResult(this, requestCode, resultCode,data);
//
//        super.onActivityResult(requestCode, resultCode, data);
//
//        finish();
//    }
}
