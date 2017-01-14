package com.quaap.launchtime.components;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.util.Locale;

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
public class AppShortcut implements Comparable<AppShortcut> {
    private String mPackageName;
    private String mActivityName;
    private String mLabel;
    private String mCategory;
    private boolean mWidget;
    private volatile Drawable mIconDrawable;
    private volatile ImageView mIconImage;


    public AppShortcut(String activityName, String packageName, String label, String category, boolean isWidget) {
        mActivityName = activityName;
        mPackageName = packageName;
        mLabel = label;
        mCategory = category;
        mWidget = isWidget;
    }


    public AppShortcut(AppShortcut shortcut) {
        mActivityName = shortcut.getActivityName();
        mPackageName = shortcut.getPackageName();
        mLabel = shortcut.getLabel();
        mCategory = shortcut.getCategory();
        mIconDrawable = shortcut.mIconDrawable;
        mWidget = shortcut.mWidget;

    }

//    public AppShortcut(PackageManager pm, String packageName) throws PackageManager.NameNotFoundException {
//        mPackageName = packageName;
//        ApplicationInfo info = pm.getApplicationInfo(mPackageName, PackageManager.GET_META_DATA);
//        mActivityName = pm.resolveActivity()
//        mLabel = pm.getApplicationLabel(info).toString();
//        mCategory = Categories.getCategoryForPackage(mPackageName);
//        loadAppIconAsync(pm);
//        mWidget = false;
//    }

    public AppShortcut(PackageManager pm, ResolveInfo ri) {
        mActivityName = ri.activityInfo.name;
        mPackageName = ri.activityInfo.packageName;
        mLabel = ri.loadLabel(pm).toString();
        mCategory = Categories.getCategoryForPackage(mPackageName);
        mWidget = false;

        Log.d("LaunchTime", mPackageName + ", " + ri.activityInfo.name + ", " + mLabel);


        loadAppIconAsync(pm);
    }

    public String getLabel() {
        return mLabel;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public String getActivityName() {
        return mActivityName;
    }

    public String getCategory() {
        return mCategory;
    }

    public boolean isWidget() {
        return mWidget;
    }

    public boolean iconLoaded() {
        return mIconDrawable != null;
    }

    public void setIconImage(ImageView iconImage) {
        mIconImage = iconImage;
        if (mIconDrawable != null) {
            mIconImage.setImageDrawable(mIconDrawable);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AppShortcut) {
            return mActivityName.equals(((AppShortcut) obj).mActivityName);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return mActivityName.hashCode();
    }

    @Override
    public int compareTo(AppShortcut appShortcut) {
        return this.mLabel.toLowerCase(Locale.getDefault()).compareTo(appShortcut.mLabel.toLowerCase(Locale.getDefault()));
    }


    public void loadAppIconAsync(final PackageManager pm) {
        if (iconLoaded() || isWidget()) return;
        // Create an async task
        AsyncTask<Void, Void, Drawable> loadAppIconTask = new AsyncTask<Void, Void, Drawable>() {

            // Keep track of all the exceptions
            private Exception exception = null;


            @Override
            protected Drawable doInBackground(Void... voids) {
                // load the icon
                Drawable app_icon = null;
                try {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setClassName(mPackageName, mActivityName);
                    app_icon = pm.getActivityIcon(intent);
                    //app_icon = pm.getApplicationIcon(mPackageName);

                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    exception = e;
                }

                return app_icon;
            }

            @Override
            protected void onPostExecute(Drawable app_icon) {
                if (exception == null) {
                    mIconDrawable = app_icon;
                    if (mIconImage != null) {
                        mIconImage.setImageDrawable(mIconDrawable);
                    }
                } else {
                    Log.d("loadAppIconAsync", "ERROR Could not load app icon.");

                }
            }
        };

        loadAppIconTask.execute(null, null, null);
    }

}
