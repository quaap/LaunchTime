package com.quaap.launchtime.apps;

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
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ImageView;

import com.quaap.launchtime.R;
import com.quaap.launchtime.components.Categories;
import com.quaap.launchtime.components.IconCache;

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
public class AppLauncher implements Comparable<AppLauncher> {


    private static Map<ComponentName,AppLauncher> mAppLaunchers = new HashMap<>();
    public static final String LINK_SEP = ":IS_APP_LINK:";
    public static final String ACTION_PACKAGE = "ACTION.PACKAGE";


    public static AppLauncher createAppLauncher(String activityName, String packageName, String label, String category, boolean isWidget) {
        AppLauncher app = mAppLaunchers.get(new ComponentName(packageName, activityName));
        if (app == null) {
            app = new AppLauncher(activityName, packageName, label, category, isWidget);
            mAppLaunchers.put(app.getComponentName(), app);
        }
        return app;
    }

    public static AppLauncher createAppLauncher(Context context, PackageManager pm, ResolveInfo ri) {
        return createAppLauncher(context, pm, ri, null, true);
    }

    public static AppLauncher createAppLauncher(Context context, PackageManager pm, ResolveInfo ri, String category, boolean autocat) {
        String activityName = ri.activityInfo.name;
        AppLauncher app = mAppLaunchers.get(new ComponentName(ri.activityInfo.packageName, activityName));
        if (app == null) {
            app = new AppLauncher(context, pm, ri, category, autocat);
            mAppLaunchers.put(app.getComponentName(), app);
        }
        return app;
    }

    public static AppLauncher createAppLauncher(AppLauncher launcher) {
        return createAppLauncher(launcher, false);
    }

    public static AppLauncher createAppLauncher(AppLauncher launcher, boolean copyOrig) {
        return new AppLauncher(launcher, copyOrig);
    }


    public static AppLauncher createActionLink(String activityName, Uri linkUri, String packageName, String label, String category) {
        activityName = makeLink(activityName, linkUri);
        AppLauncher app = mAppLaunchers.get(new ComponentName(packageName, activityName));
        if (app == null) {
            app = new AppLauncher(activityName, packageName, label, category, false);
            mAppLaunchers.put(app.getComponentName(), app);
        }
        return app;
    }

    public static AppLauncher createActionLink(String actionName, Uri linkUri, String label, String category) {

        actionName = makeLink(actionName, linkUri);
        AppLauncher app = mAppLaunchers.get(new ComponentName(ACTION_PACKAGE, actionName));
        if (app == null) {
            app = new AppLauncher(actionName, ACTION_PACKAGE, label, category, false);
            mAppLaunchers.put(app.getComponentName(), app);
        }
        return app;
    }

    public static AppLauncher getAppLauncher(ComponentName activityName) {
        return mAppLaunchers.get(activityName);
    }

    public static AppLauncher removeAppLauncher(ComponentName activityName) {
        return mAppLaunchers.remove(activityName);
    }



    private String mPackageName;
    private String mActivityName;
    private String mLabel;


    private String mCategory;
    private boolean mWidget;

    private volatile Drawable mIconDrawable;
    private volatile ImageView mIconImage;



    private AppLauncher(String activityName, String packageName, String label, String category, boolean isWidget) {
        mActivityName = activityName;
        mPackageName = packageName;
        mLabel = label;
        mCategory = category;
        mWidget = isWidget;
//        if (mCategory==null) {
//            mCategory = Categories.getCategoryForPackage(mPackageName);
//        }
    }


    private AppLauncher(AppLauncher launcher, boolean copyOrig) {
        mActivityName = copyOrig ? launcher.getLinkBaseActivityName() : launcher.getActivityName();
        mPackageName = launcher.getPackageName();
        mLabel = launcher.getLabel();
        mCategory = launcher.getCategory();
        if (!copyOrig) {
            mIconDrawable = launcher.mIconDrawable;
        }
        mWidget = launcher.mWidget;

    }


    private AppLauncher(Context context, PackageManager pm, ResolveInfo ri, String category, boolean autocat) {
        mActivityName = ri.activityInfo.name;
        mPackageName = ri.activityInfo.packageName;
        mLabel = ri.loadLabel(pm).toString();
        if (category!=null) {
            mCategory = category;
            //Log.d("LaunchTime", mPackageName + ", " + ri.activityInfo.name + ", " + mLabel + "  cat " + category);
        } else if (autocat) {
            mCategory = Categories.getCategoryForComponent(context,mActivityName, mPackageName, true);

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

    private static final String linkuri = "_link";

    public AppLauncher makeAppLink() {
        return AppLauncher.createActionLink(getLinkBaseActivityName(), new Uri.Builder().scheme(linkuri).path(linkuri + Math.random()).build(), getPackageName(), getLabel(), getCategory());
    }

    public boolean isAppLink() {
        String uristr = getLinkUri();
        return uristr!=null && uristr.startsWith(linkuri);
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

    public void setCategory(String category) {
        mCategory = category;
    }

    public boolean isWidget() {
        return mWidget;
    }

    public boolean isActionLink() {
        return mPackageName.equals(ACTION_PACKAGE);
    }

    public boolean isNormalApp() {
        return !(isWidget() || isLink() || isActionLink() || isAppLink());
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

    public Drawable getIconDrawable() {
        return mIconDrawable;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AppLauncher) {
            return getComponentName().equals(((AppLauncher) obj).getComponentName());
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return getComponentName().hashCode();
    }

    @Override
    public int compareTo(@NonNull AppLauncher appLauncher) {
        return this.mLabel.toLowerCase(Locale.getDefault()).compareTo(appLauncher.mLabel.toLowerCase(Locale.getDefault()));
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

                if (isLink()) {
                    app_icon = drawLinkSymbol(app_icon, context);
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

        loadAppIconTask.execute();
    }

    private Drawable drawLinkSymbol(Drawable app_icon, Context context) {
        try {
            Bitmap newbm = Bitmap.createBitmap(app_icon.getIntrinsicWidth(), app_icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(newbm);
            app_icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            app_icon.draw(canvas);

            Drawable link;
            if (Build.VERSION.SDK_INT >= 21) {
                link = context.getDrawable(R.drawable.link);
            } else {
                link = context.getResources().getDrawable(R.drawable.link);
            }
            if (link!=null) {
                link.setBounds(canvas.getWidth() * 2 / 3, canvas.getHeight() * 2 / 3, canvas.getWidth(), canvas.getHeight());
                link.draw(canvas);
            }

            app_icon = new BitmapDrawable(context.getResources(), newbm);
            //Log.d("loadAppIconAsync", " yo");
        } catch (Exception | OutOfMemoryError e) {
            Log.e("loadAppIconAsync", "couldn't make link icon", e);
        }
        return app_icon;
    }

}
