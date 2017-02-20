package com.quaap.launchtime.components;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
public class AppShortcut implements Comparable<AppShortcut> {
    private String mPackageName;
    private String mActivityName;
    private String mLabel;
    private String mCategory;
    private boolean mWidget;

    private volatile Drawable mIconDrawable;
    private volatile ImageView mIconImage;

    public static final String LINK_SEP = ":IS_APP_LINK:";
    public static final String ACTION_PACKAGE = "ACTION.PACKAGE";


    private static Map<ComponentName,AppShortcut> mAppShortcuts = new HashMap<>();


    public static AppShortcut createAppShortcut(String activityName, String packageName, String label, String category, boolean isWidget) {
        AppShortcut app = mAppShortcuts.get(activityName);
        if (app == null) {
            app = new AppShortcut(activityName, packageName, label, category, isWidget);
            mAppShortcuts.put(app.getComponentName(), app);
        }
        return app;
    }

    public static AppShortcut createAppShortcut(Context context, PackageManager pm, ResolveInfo ri) {
        return createAppShortcut(context, pm, ri, null, true);
    }

    public static AppShortcut createAppShortcut(Context context, PackageManager pm, ResolveInfo ri, String category, boolean autocat) {
        String activityName = ri.activityInfo.name;
        AppShortcut app = mAppShortcuts.get(activityName);
        if (app == null) {
            app = new AppShortcut(context, pm, ri, category, autocat);
            mAppShortcuts.put(app.getComponentName(), app);
        }
        return app;
    }

    public static AppShortcut createAppShortcut(AppShortcut shortcut) {
        return new AppShortcut(shortcut);
    }


    public static AppShortcut createActionLink(String activityName, Uri linkUri, String packageName, String label, String category) {
        activityName = makeLink(activityName, linkUri);
        AppShortcut app = mAppShortcuts.get(activityName);
        if (app == null) {
            app = new AppShortcut(activityName, packageName, label, category, false);
            mAppShortcuts.put(app.getComponentName(), app);
        }
        return app;
    }

    public static AppShortcut createActionLink(String actionName, Uri linkUri, String label, String category) {

        actionName = makeLink(actionName, linkUri);
        AppShortcut app = mAppShortcuts.get(actionName);
        if (app == null) {
            app = new AppShortcut(actionName, ACTION_PACKAGE, label, category, false);
            mAppShortcuts.put(app.getComponentName(), app);
        }
        return app;
    }

    public static AppShortcut getAppShortcut(ComponentName activityName) {
        return mAppShortcuts.get(activityName);
    }

    public static AppShortcut removeAppShortcut(ComponentName activityName) {
        return mAppShortcuts.remove(activityName);
    }


    private AppShortcut(String activityName, String packageName, String label, String category, boolean isWidget) {
        mActivityName = activityName;
        mPackageName = packageName;
        mLabel = label;
        mCategory = category;
        mWidget = isWidget;
//        if (mCategory==null) {
//            mCategory = Categories.getCategoryForPackage(mPackageName);
//        }
    }


    private AppShortcut(AppShortcut shortcut) {
        mActivityName = shortcut.getActivityName();
        mPackageName = shortcut.getPackageName();
        mLabel = shortcut.getLabel();
        mCategory = shortcut.getCategory();
        mIconDrawable = shortcut.mIconDrawable;
        mWidget = shortcut.mWidget;

    }


    private AppShortcut(Context context, PackageManager pm, ResolveInfo ri, String category, boolean autocat) {
        mActivityName = ri.activityInfo.name;
        mPackageName = ri.activityInfo.packageName;
        mLabel = ri.loadLabel(pm).toString();
        if (category!=null) {
            mCategory = category;
            //Log.d("LaunchTime", mPackageName + ", " + ri.activityInfo.name + ", " + mLabel + "  cat " + category);
        } else if (autocat) {
            mCategory = Categories.getCategoryForPackage(context, mPackageName);
            if (mCategory.equals(Categories.CAT_OTHER)) {
                mCategory = Categories.getCategoryForPackage(context, mActivityName);
            }
            //Log.d("LaunchTime", mPackageName + ", " + ri.activityInfo.name + ", " + mLabel + "  auto " + category);
        } else {
            mCategory = Categories.CAT_OTHER;
            //Log.d("LaunchTime", mPackageName + ", " + ri.activityInfo.name + ", " + mLabel + "  plain " + category);
        }
        mWidget = false;



        loadAppIconAsync(context, pm);
    }


    private static String makeLink(String activityName, Uri uri) {
        if (!activityName.contains(LINK_SEP)) {
            return activityName + LINK_SEP + uri;
        }
        Log.e("Link", "Activity is already a link"+ activityName, new Throwable("Activity is already a link"+ activityName));
        return activityName;
    }


    public ComponentName getComponentName() {
        return new ComponentName(mPackageName, mActivityName);
    }

    public boolean isLink() {
        return mActivityName.contains(LINK_SEP);
    }

    public String getLinkBaseActivityName() {
       return mActivityName.split(LINK_SEP,2)[0];
    }

    public String getLinkUri() {
        String [] parts = mActivityName.split(LINK_SEP,2);
        if (parts.length==2) {
            return parts[1];
        }
        return null;
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

    public boolean isActionLink() {
        return mPackageName.equals(ACTION_PACKAGE);
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
    public int compareTo(@NonNull AppShortcut appShortcut) {
        return this.mLabel.toLowerCase(Locale.getDefault()).compareTo(appShortcut.mLabel.toLowerCase(Locale.getDefault()));
    }


    public void loadAppIconAsync(final Context context, final PackageManager pm) {
        if (iconLoaded() || isWidget()) return;
        // Create an async task
        AsyncTask<Void, Void, Drawable> loadAppIconTask = new AsyncTask<Void, Void, Drawable>() {

            // Keep track of all the exceptions
            private Exception exception = null;


            @Override
            protected Drawable doInBackground(Void... voids) {
                // load the icon
                Drawable app_icon = null;

                Intent intent;
                if (isActionLink()) {
                    String uristr = getLinkUri();
                    if (uristr==null) {
                        intent = new Intent(getLinkBaseActivityName());
                    } else {
                        intent = new Intent(getLinkBaseActivityName(), Uri.parse(uristr));
                    }

                } else {
                    intent = new Intent(Intent.ACTION_MAIN);
                    intent.setClassName(mPackageName, getLinkBaseActivityName());
                }
                try {
                    app_icon = pm.getActivityIcon(intent);
                } catch (Exception | OutOfMemoryError e) {
                    Log.e("IconLookup", "Couldn't get icon for" + getLinkBaseActivityName(), e);
                }
                if (app_icon == null) {
                    app_icon = pm.getDefaultActivityIcon();
                }

                Bitmap bitmap = IconCache.loadBitmap(context, mActivityName);

                if (bitmap!=null) {
                    Log.d("loadAppIconAsync", "Got special icon for " + mActivityName);
                    try {
                        Bitmap newbm = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
                        Canvas canvas = new Canvas(newbm);
                        canvas.drawBitmap(bitmap, 0, 0, null);
                        app_icon.setBounds(canvas.getWidth() / 2, 0, canvas.getWidth(), canvas.getHeight()/2);
                        app_icon.draw(canvas);
                        app_icon = new BitmapDrawable(context.getResources(), newbm);
                        //Log.d("loadAppIconAsync", " yo");
                    } catch (Exception | OutOfMemoryError e) {
                        Log.e("loadAppIconAsync", "couldn't make special icon", e);
                    }
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
