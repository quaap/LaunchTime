package com.quaap.launchtime.ui;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.quaap.launchtime.GlobState;
import com.quaap.launchtime.MainActivity;
import com.quaap.launchtime.apps.AppLauncher;
import com.quaap.launchtime.apps.DefaultApps;
import com.quaap.launchtime.R;
import com.quaap.launchtime.db.DB;

import java.util.ArrayList;
import java.util.List;

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

public class QuickRow {

    public static final String QUICK_ROW_CAT = "QuickRow";
    private GridLayout mQuickRow;
    private HorizontalScrollView mQuickRowScroller;

    private MainActivity mMainActivity;

    private Style mStyle;

    public QuickRow(final View.OnDragListener dragListener, MainActivity mainActivity) {

        mMainActivity = mainActivity;
        mStyle = GlobState.getStyle(mMainActivity);

        mQuickRow = mMainActivity.findViewById(R.id.layout_quickrow);

        mQuickRowScroller = mMainActivity.findViewById(R.id.layout_quickrow_scroll);

        mQuickRow.setOnDragListener(dragListener);
        mQuickRowScroller.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                return dragListener.onDrag(mQuickRow, dragEvent);
            }
        });
    }

    public GridLayout getGridLayout() {
        return mQuickRow;
    }

    public HorizontalScrollView getScroller() {
        return mQuickRowScroller;
    }

    public int getScrollPos() {
        return mQuickRowScroller.getScrollX();
    }

    public void scrollToStart() {
        mQuickRowScroller.smoothScrollTo(0, 0);
    }


    public boolean appAlreadyHere(AppLauncher app) {
        //prevent copies of the same app on the quickrow
        for (int i = 0; i < mQuickRow.getChildCount(); i++) {

            AppLauncher inbar = (AppLauncher) mQuickRow.getChildAt(i).getTag();
            if (app.getLinkBaseActivityName().equals(inbar.getLinkBaseActivityName())) {
                return true;
            }
        }
        return false;
    }

    public void clearIcons() {
        for (int i = 0; i < mQuickRow.getChildCount(); i++) {

            AppLauncher inbar = (AppLauncher) mQuickRow.getChildAt(i).getTag();
            inbar.clearDrawable();
        }
    }


    public void setCenterIcons(boolean center) {
        float num = mMainActivity.getScreenDimensions().x / mStyle.getLauncherSize();

        //Log.d("Quick", "cols = " + num);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)mQuickRow.getLayoutParams();
        if (center && mQuickRow.getChildCount()<=num) {
            lp.gravity = Gravity.CENTER_HORIZONTAL;
        } else {
            lp.gravity = Gravity.START;
        }
        mQuickRow.setLayoutParams(lp);
    }

    public boolean isSelf(View other) {
        return other == mQuickRow;
    }

    public void repopulate() {
        final List<ComponentName> quickRowOrder = db().getAppCategoryOrder(QUICK_ROW_CAT);

        mQuickRow.postDelayed(new Runnable() {
            @Override
            public void run() {
                mQuickRow.removeAllViews();
                for (ComponentName actvname : quickRowOrder) {
                    AppLauncher app = db().getApp(actvname);
                    if (!appAlreadyHere(app)) {
                        ViewGroup item = mMainActivity.getLauncherView(app, true);
                        if (item != null) {
                            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.TOP);
                            mQuickRow.addView(item, lp);
                        }
                    }
                }

            }
        }, 400);

    }

    public void processQuickApps(List<AppLauncher> launchers, PackageManager packageMan) {
        List<AppLauncher> quickRowApps = new ArrayList<>();
        final List<ComponentName> quickRowOrder = db().getAppCategoryOrder(QUICK_ROW_CAT);

        DefaultApps.checkDefaultApps(mQuickRow.getContext(), launchers, quickRowOrder, mQuickRow);


        for (AppLauncher app : launchers) {

            if (quickRowOrder.contains(app.getComponentName())) {
                AppLauncher qapp = AppLauncher.createAppLauncher(app);
                qapp.loadAppIconAsync(mQuickRow.getContext(), packageMan);
                quickRowApps.add(qapp);
            }
        }


        mQuickRow.removeAllViews();
        for (ComponentName actvname : quickRowOrder) {
            for (AppLauncher app : quickRowApps) {
                if (app.getComponentName().equals(actvname)) {
                    ViewGroup item = mMainActivity.getLauncherView(app, true);
                    if (item!=null) {
                        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.TOP);
                        mQuickRow.addView(item, lp);
                    }
                }
            }
        }

    }

    public void removeFromQuickApps(ComponentName actvname) {
        for (int i = mQuickRow.getChildCount()-1; i>=0; i--) {
            AppLauncher app = (AppLauncher) mQuickRow.getChildAt(i).getTag();
            if (app != null && actvname.equals(app.getComponentName())) {
                mQuickRow.removeView(mQuickRow.getChildAt(i));
            }
        }
    }

    private DB db() {
        return GlobState.getGlobState(mQuickRow.getContext()).getDB();
    }
}
