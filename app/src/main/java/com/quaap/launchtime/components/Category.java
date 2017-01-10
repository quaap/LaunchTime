package com.quaap.launchtime.components;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import com.quaap.launchtime.GlobState;
import com.quaap.launchtime.MainActivity;
import com.quaap.launchtime.db.DB;

import java.util.List;

/**
 * Created by tom on 1/10/17.
 * <p>
 * Copyright (C) 2017  tom
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
public class Category {

    //future work

    private String mCategory;
    private TextView mCategoryTab;
    private GridLayout mIconSheet;

    private Context mContext;

    public Category(Context context, String category) {
        mContext = context;
        mCategory = category;
        mIconSheet = new GridLayout(mContext);

    }


    public String getCategory() {
        return mCategory;
    }

    public void setCategory(String category) {
        this.mCategory = category;
    }

    public TextView getCategoryTab() {
        return mCategoryTab;
    }

    public void setCategoryTab(TextView categoryTab) {
        this.mCategoryTab = categoryTab;
    }

    public GridLayout getIconSheet() {
        return mIconSheet;
    }

    public void setIconSheet(GridLayout iconSheet) {
        this.mIconSheet = iconSheet;
    }

//
//    @NonNull
//    private GridLayout getIconSheet(String category) {
//        final GridLayout iconSheet = new GridLayout(MainActivity.this);
//        mIconSheets.put(category, iconSheet);
//        mRevCategoryMap.put(iconSheet, category);
//
//        iconSheet.setColumnCount(3);
//        iconSheet.setOnDragListener(MainActivity.this);
//
//
//        final TextView categoryTab = getCategoryTab(category, iconSheet);
//
//        mCategoryTabs.put(category, categoryTab);
//        mRevCategoryMap.put(categoryTab, category);
//        mCategoriesLayout.addView(categoryTab);
//        return iconSheet;
//    }
//
//    private void processIconSheet(final DB db, final String category, final GridLayout iconSheet, final List<AppShortcut> catapps) {
//        final List<String> apporder = db.getCategoryOrder(category);
//
//        GlobState.getGlobState(this).runAsync(new Runnable() {
//            @Override
//            public void run() {
//                //   Log.d("category--------", category);
//
//                for (String actvname: apporder) {
//                    // Log.d("apporder", pkgname);
//                    for (AppShortcut app : catapps) {
//                        if (app.getActivityName().equals(actvname)) {
//                            ViewGroup item = getShortcutView(app);
//                            iconSheet.addView(item);
//                        }
//                    }
//                }
//
//                boolean reorder = false;
//                for (AppShortcut app : catapps) {
//                    if (!apporder.contains(app.getActivityName())) {
//                        //  Log.d("no apporder", app.getPackageName());
//
//                        ViewGroup item = getShortcutView(app);
//
//                        iconSheet.addView(item);
//                        reorder = true;
//                    }
//                }
//                if (reorder) {
//                    db.setCategoryOrder(category, iconSheet);
//                }
//
//            }
//        });
//    }
}
