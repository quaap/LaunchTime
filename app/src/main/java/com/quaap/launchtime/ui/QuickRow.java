package com.quaap.launchtime.ui;

import android.content.ComponentName;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;

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
    private final GridLayout mQuickRow;
    private final HorizontalScrollView mQuickRowScroller;

    private final MainActivity mMainActivity;

    private final Style mStyle;

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


    public void setCenterIcons(final boolean center) {
        mQuickRow.postDelayed(new Runnable() {
            @Override
            public void run() {
                float num = mMainActivity.getScreenDimensions().x / (float)mStyle.getLauncherSize() - 1;

                //Log.d("Quick", "cols = " + num);
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)mQuickRow.getLayoutParams();
                if (center && mQuickRow.getChildCount()<=num) {
                    lp.gravity = Gravity.CENTER_HORIZONTAL;
                } else {
                    lp.gravity = Gravity.START;
                }
                mQuickRow.setLayoutParams(lp);

            }
        },1000);

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


    public void processQuickApps(List<AppLauncher> launchers) {
        List<AppLauncher> quickRowApps = new ArrayList<>();
        final List<ComponentName> quickRowOrder = db().getAppCategoryOrder(QUICK_ROW_CAT);

        boolean newstuff = DefaultApps.checkDefaultApps(mQuickRow.getContext(), launchers, quickRowOrder);

        for (ComponentName compname: quickRowOrder) {

            //Log.d("Quick", compname.flattenToString());
            AppLauncher app = null;
            for (AppLauncher a: launchers) {
                if (compname.equals(a.getComponentName())) {
                    app = a;
                    break;
                }
            }
            if (app==null) {
                //Log.d("Quick", "Not found: " + compname.flattenToString());
              newstuff = true; //app could have been uninstalled
            } else if (compname.equals(app.getComponentName())) {
                //Log.d("Quick", "Found: " + compname.flattenToString());
               // AppLauncher qapp = AppLauncher.createAppLauncher(app, true);
                quickRowApps.add(app);

            }

        }
        if (newstuff) db().setAppCategoryOrder(QUICK_ROW_CAT, quickRowApps);
    }


//    public void fillQuickApps(List<AppLauncher> quickRowApps, PackageManager packageMan) {
//
//        final List<ComponentName> quickRowOrder = db().getAppCategoryOrder(QUICK_ROW_CAT);
//
//
//        mQuickRow.removeAllViews();
//        for (ComponentName actvname : quickRowOrder) {
//            for (AppLauncher app : quickRowApps) {
//                if (app.getComponentName().equals(actvname)) {
//                    ViewGroup item = mMainActivity.getLauncherView(app, true);
//                    if (item!=null) {
//                        app.loadAppIconAsync(mQuickRow.getContext(), packageMan);
//                        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
//                        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.TOP);
//                        mQuickRow.addView(item, lp);
//                    }
//                }
//            }
//        }
//
//    }

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
