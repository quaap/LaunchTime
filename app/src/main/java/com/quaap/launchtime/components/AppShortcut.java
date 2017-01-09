package com.quaap.launchtime.components;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.quaap.launchtime.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by tom on 1/8/17.
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
public class AppShortcut implements Comparable<AppShortcut>{
    private String mPackageName;
    private String mLabel;
    private String mCategory;
    private volatile Drawable mIconDrawable;
    private volatile ImageView mIconImage;


    public AppShortcut(String packageName, String label, String category) {
        mPackageName = packageName;
        mLabel = label;
        mCategory = category;
    }


    public AppShortcut(AppShortcut shortcut) {
        mPackageName = shortcut.getPackageName();
        mLabel = shortcut.getLabel();
        mCategory = shortcut.getCategory();
        mIconDrawable = shortcut.mIconDrawable;

    }

//    public AppShortcut(PackageManager pm, String packageName) throws PackageManager.NameNotFoundException {
//        mPackageName = packageName;
//        ApplicationInfo info = pm.getApplicationInfo(mPackageName, PackageManager.GET_META_DATA);
//        mLabel = pm.getApplicationLabel(info).toString();
//        loadAppIconAsync(pm);
//    }

    public AppShortcut(PackageManager pm, ResolveInfo ri) {
        mPackageName = ri.activityInfo.packageName;
        mLabel = ri.loadLabel(pm).toString();
        mCategory = Categories.getCategoryForPackage(mPackageName);

        Log.d("LaunchTime", mPackageName  + ", " +  ri.activityInfo.name + ", " + mLabel);


        loadAppIconAsync(pm);
    }

    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        mLabel = label;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }

    public String getCategory() {
        return mCategory;
    }

    public void setCategory(String category) {
        mCategory = category;
    }

    public ImageView getIconImage() {
        return mIconImage;
    }

    public void setIconImage(ImageView iconImage) {
        mIconImage = iconImage;
        if (mIconDrawable!=null) {
            mIconImage.setImageDrawable(mIconDrawable);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AppShortcut) {
            return mPackageName.equals(((AppShortcut)obj).mPackageName);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return mPackageName.hashCode();
    }

    @Override
    public int compareTo(AppShortcut appShortcut) {
        return this.mLabel.toLowerCase(Locale.getDefault()).compareTo(appShortcut.mLabel.toLowerCase(Locale.getDefault()));
    }


    public void loadAppIconAsync(final PackageManager pm){

        // Create an async task
        AsyncTask<Void,Void,Drawable> loadAppIconTask = new AsyncTask<Void, Void, Drawable>() {

            // Keep track of all the exceptions
            private Exception exception = null;


            @Override
            protected Drawable doInBackground(Void... voids) {
                // load the icon
                Drawable app_icon = null;
                try {
                    app_icon = pm.getApplicationIcon(mPackageName);

                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    exception = e;
                }

                return app_icon;
            }

            @Override
            protected void onPostExecute(Drawable app_icon){
                if (exception == null) {
                    mIconDrawable = app_icon;
                    if (mIconImage!=null) {
                        mIconImage.setImageDrawable(mIconDrawable);
                    }
                } else {
                    Log.d("loadAppIconAsync", "ERROR Could not load app icon.");

                }
            }
        };

        loadAppIconTask.execute(null,null,null);
    }

}
