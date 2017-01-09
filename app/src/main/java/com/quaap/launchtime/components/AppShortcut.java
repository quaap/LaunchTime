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

    private static Map<String, String> mPrefCategories;
    private static Map<String, String[]> mCategorKeywords;

    public static void init(Context context) {
        mPrefCategories = getPredefinedCategories(context);
        mCategorKeywords = getCategoryKeywords(context);
    }

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
        mCategory = getCategoryForPackage(mPackageName);

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



    public static String getCategoryForPackage(String pkgname) {

        String category = mPrefCategories.get(pkgname);
        if (category==null) {
            OUTER: for (String cat: mCategorKeywords.keySet()) {
                for (String pkg: mCategorKeywords.get(cat)) {
                    if (pkgname.contains(pkg)) {
                        category = cat;
                        break OUTER;
                    }
                }
            }
        }
        if (category==null) {
            category = "Other";
        }

        return category;
    }

    public static Map<String, String[]> getCategoryKeywords(Context ctx)
    {
        Map<String, String[]> keywordsDict = new LinkedHashMap<>();

        keywordsDict.put("Phone", new String[]{"phone", "conv", "call", "sms", "mms", "contacts", "stk"});  // stk stands for "SIM Toolkit"
        keywordsDict.put("Games", new String[]{"game", "play"});
        keywordsDict.put("Internet", new String[]{"download", "mail", "vending", "browser", "maps", "twitter", "whatsapp", "outlook", "dropbox", "chrome", "drive"});
        keywordsDict.put("Media", new String[]{"radio", "voice", "speech", "pod", "music", "sound", "mp3", "record", "sfx", "mic"});
        keywordsDict.put("Graphics", new String[]{"pic", "pix", "gallery", "photo", "foto", "cam", "tube", "tv", "video", "draw", "graph", "gfx", "image", "img", "svg", "png"});
        keywordsDict.put("Accessories", new String[]{"editor", "calc", "calendar", "organize", "clock", "time", "viewer", "file", "manager", "memo", "note"});
        keywordsDict.put("Settings", new String[]{"settings", "config", "keyboard", "launcher", "sync", "backup"});


        return keywordsDict;
    }

    public static Map<String, String> getPredefinedCategories(Context ctx)
    {
        Map<String, String> predefCategories = new HashMap<>();

        InputStream inputStream = ctx.getResources().openRawResource(R.raw.package_category);
        String line;
        String[] lineSplit;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()){
                    lineSplit = line.split("=");
                    predefCategories.put(lineSplit[0], lineSplit[1]);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if(inputStream != null) inputStream.close();
            } catch (IOException e) { e.printStackTrace(); }
        }

        return predefCategories;
    }
}
