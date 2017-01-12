package com.quaap.launchtime.components.future;

import android.content.Context;
import android.view.ViewGroup;

import com.quaap.launchtime.db.DB;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tom on 1/12/17.
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
public class CategoryBar {
    Map<CategoryTab,CategoryView> mContents;
    private Context mContext;
    private ViewGroup mTabLayout;
    private DB mDb;

    public CategoryBar(Context context, ViewGroup tabLayout, DB db) {
        mContext = context;
        mTabLayout = tabLayout;
        mDb = db;
        mContents = new HashMap<>();
    }

    public void loadCategories() {

    }
}
