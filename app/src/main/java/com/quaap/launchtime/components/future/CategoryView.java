package com.quaap.launchtime.components.future;

import android.content.Context;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.quaap.launchtime.GlobState;
import com.quaap.launchtime.R;
import com.quaap.launchtime.components.AppShortcut;
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
public class CategoryView {

    //future work

    private String mCategory;
    private TextView mCategoryTab;
    private GridLayout mIconSheet;
    private CategoryListener mCategoryListener;

    private Context mContext;

    public CategoryView(Context context, String category, CategoryListener categoryListener) {
        mCategoryListener = categoryListener;
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


}
