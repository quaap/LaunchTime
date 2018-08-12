package com.quaap.launchtime.apps;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ImageView;

import com.quaap.launchtime.GlobState;
import com.quaap.launchtime.R;
import com.quaap.launchtime.components.Categories;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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


    private static final Map<ComponentName,AppLauncher> mAppLaunchers = Collections.synchronizedMap(new HashMap<ComponentName,AppLauncher>());
    private static final String LINK_SEP = ":IS_APP_LINK:";
    public static final String ACTION_PACKAGE = "ACTION.PACKAGE";
    private static final String OREOSHORTCUT = "OREOSHORTCUT:";
    public static final String OLDSHORTCUT = "OLDSHORTCUT:";


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


//    public static AppLauncher createActionLink(String activityName, Uri linkUri, String packageName, String label, String category) {
//        activityName = makeLink(activityName, linkUri);
//        AppLauncher app = mAppLaunchers.get(new ComponentName(packageName, activityName));
//        if (app == null) {
//            app = new AppLauncher(activityName, packageName, label, category, false);
//            mAppLaunchers.put(app.getComponentName(), app);
//        }
//        return app;
//    }
//
//    public static AppLauncher createActionLink(String actionName, Uri linkUri, String label, String category) {
//
//        actionName = makeLink(actionName, linkUri);
//        AppLauncher app = mAppLaunchers.get(new ComponentName(ACTION_PACKAGE, actionName));
//        if (app == null) {
//            app = new AppLauncher(actionName, ACTION_PACKAGE, label, category, false);
//            mAppLaunchers.put(app.getComponentName(), app);
//        }
//        return app;
//    }

    public static AppLauncher createActionShortcut(Intent launchintent, String label, String category) {
        launchintent.putExtra("_LT__LINK_",  Math.random());  //so we can have multiple shortcuts
        String actionName = makeLink(OLDSHORTCUT, launchintent.toUri(0));

        AppLauncher app = mAppLaunchers.get(new ComponentName(ACTION_PACKAGE, actionName));
        if (app == null) {
            app = new AppLauncher(actionName, ACTION_PACKAGE, label, category, false);
            mAppLaunchers.put(app.getComponentName(), app);
        }
        return app;
    }


    public static AppLauncher createShortcut(Intent launchintent, String packageName, String label, String category) {
        launchintent.putExtra("_LT__LINK_",  Math.random());  //so we can have multiple shortcuts
        String actionName = makeLink(OLDSHORTCUT, launchintent.toUri(0));

        AppLauncher app = mAppLaunchers.get(new ComponentName(packageName, actionName));
        if (app == null) {
            app = new AppLauncher(actionName, packageName, label, category, false);
            mAppLaunchers.put(app.getComponentName(), app);
        }
        return app;
    }


    public static AppLauncher createOreoShortcut(String shortcutid, String packageName, String label, String category) {

        String actionName = makeLink(OREOSHORTCUT, shortcutid);
        AppLauncher app = mAppLaunchers.get(new ComponentName(packageName, actionName));
        if (app == null) {
            app = new AppLauncher(actionName, packageName, label, category, false);
            mAppLaunchers.put(app.getComponentName(), app);
        }
        return app;
    }

    public static AppLauncher getAppLauncher(ComponentName activityName) {
        return mAppLaunchers.get(activityName);
    }

    public static void removeAppLauncher(ComponentName activityName) {
        mAppLaunchers.remove(activityName);
    }

    public static void removeAppLauncher(String activityName, String packageName) {
        try {
            if (activityName==null) activityName = "";
            mAppLaunchers.remove(new ComponentName(packageName, activityName));
        } catch (Exception e) {
            Log.e("AppLauncher", e.getMessage(), e);
        }
    }

    public static void clearIcons() {
        for (AppLauncher app: mAppLaunchers.values()) {
            app.clearDrawable();
        }
        mAppLaunchers.clear();
    }


    private final String mPackageName;
    private final String mActivityName;
    private String mLabel;


    private String mCategory;
    private final boolean mWidget;

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
            mCategory = Categories.getCategoryFromPiCat(ri.activityInfo.applicationInfo);
            if (mCategory==null) {
                mCategory = Categories.getCategoryForComponent(context, mActivityName, mPackageName, true, ri.activityInfo.applicationInfo);
            }

            //Log.d("LaunchTime", mPackageName + ", " + ri.activityInfo.name + ", " + mLabel + "  auto " + category);
        } else {
            mCategory = Categories.CAT_OTHER;
            //Log.d("LaunchTime", mPackageName + ", " + ri.activityInfo.name + ", " + mLabel + "  plain " + category);
        }
        mWidget = false;



        loadAppIconAsync(context);
    }


    private static String makeLink(String activityName, Uri uri) {
        String uristr = "";
        if (uri != null) uristr = uri.toString();
        return makeLink(activityName, uristr);
    }

    private static String makeLink(String activityName, String uri) {
        if (!activityName.contains(LINK_SEP)) {
            return activityName + LINK_SEP + uri;
        }
        Log.e("Link", "Activity is already a link"+ activityName, new Throwable("Activity is already a link"+ activityName));
        return activityName;
    }

    private static final String linkuri = "_link";

    public AppLauncher makeAppLink() {
        //return AppLauncher.createActionLink(getLinkBaseActivityName(), new Uri.Builder().scheme(linkuri).path(linkuri + Math.random()).build(), getPackageName(), getLabel(), getCategory());
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(getPackageName(), getLinkBaseActivityName());
        intent.setPackage(getPackageName());

        return AppLauncher.createShortcut(intent, intent.getPackage(), getLabel(), null);
    }

    public boolean isAppLink() {
        String uristr = getLinkUri();
        return uristr!=null && uristr.startsWith(linkuri);
    }

    public ComponentName getComponentName() {
        return new ComponentName(mPackageName, mActivityName);
    }

    public ComponentName getBaseComponentName() {
        return new ComponentName(mPackageName, getLinkBaseActivityName());
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

    public boolean isOreoShortcut() {
        return mActivityName.contains(OREOSHORTCUT);
    }
    public boolean isShortcut() {
        return mActivityName.contains(OLDSHORTCUT);
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
        return !(isWidget() || isLink() || isActionLink() || isAppLink() || isShortcut() || isOreoShortcut());
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

    public void clearDrawable() {
        mIconDrawable = null;
        mIconImage = null;
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


    private Drawable drawLinkSymbol(Drawable app_icon, Context context) {
        try {
            Bitmap newbm = Bitmap.createBitmap(app_icon.getIntrinsicWidth(), app_icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(newbm);
            app_icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            app_icon.draw(canvas);

            Drawable link;
            if (Build.VERSION.SDK_INT >= 21) {
                link = context.getResources().getDrawable(R.drawable.link, context.getTheme());
            } else {
                link = context.getResources().getDrawable(R.drawable.link);
            }
            if (link!=null) {
                int tint = GlobState.getStyle(context).getIconTint();
                if (Color.alpha(tint)>10) {
                    link.setColorFilter(tint, PorterDuff.Mode.MULTIPLY);
                }
                link.setBounds(canvas.getWidth() * 3 / 4, canvas.getHeight() * 3 / 4, canvas.getWidth(), canvas.getHeight());
                link.draw(canvas);
            }

            app_icon = new BitmapDrawable(context.getResources(), newbm);
            //Log.d("loadAppIconAsync", " yo");
        } catch (Exception | OutOfMemoryError e) {
            Log.e("loadAppIconAsync", "couldn't make link icon", e);
        }
        return app_icon;
    }

    private static final BlockingQueue<AppLauncher> iconQueue = new LinkedBlockingQueue<>();
    private static final Object iconLoaderSync = new Object();
    private static IconLoaderTask iconLoader;
    private static Handler handler;

    public void loadAppIconAsync(final Context context) {
        if (iconLoaded() || isWidget()) return;
        // Create an async task
        //new IconLoaderTask(this, pm).execute(context);
        queueIconLoad(this);

        synchronized (iconLoaderSync) {
            if (iconLoader==null || !iconLoader.isrunning || !iconLoader.isAlive()) {
                if (handler==null) handler = new Handler(Looper.getMainLooper());
                iconLoader = new IconLoaderTask(context, handler);
                iconLoader.start();
            }
        }

    }

    void queueIconLoad(AppLauncher app) {
        iconQueue.offer(app);
    }


    private static class IconLoaderTask extends Thread {

        boolean isrunning = true;
        private WeakReference<Context> mContextRef;
        private WeakReference<Handler> mHandlerRef;

        private int processed=0;


        IconLoaderTask(Context context, Handler handler) {
            mContextRef = new WeakReference<>(context);
            mHandlerRef = new WeakReference<>(handler);
        }

        @Override
        public void run() {
            Log.d("IconLoaderTask", "Starting IconLoad task");
            try {
                do {
                    Context context = mContextRef.get();
                    Handler handler = mHandlerRef.get();
                    if (context == null || handler == null) {
                        Log.d("IconLoaderTask", context + " " + handler);
                        return;
                    }
                    try {
                        final AppLauncher inst = iconQueue.poll(5, TimeUnit.SECONDS);
                        if (inst == null) {
                            isrunning = false;
                        } else if (!inst.iconLoaded()) {
                            processed++;
                            Drawable app_icon = null;
                            try {

                                String uristr = null;
                                if (inst.isActionLink()) {
                                    uristr = inst.getLinkUri();
                                    if (uristr == null) uristr = "";
                                }

                                app_icon = GlobState.getIconsHandler(context).getDrawableIconForPackage(inst);

                                if (app_icon == null) {
                                    app_icon = context.getPackageManager().getDefaultActivityIcon();
                                }
                                if (inst.isLink()) {
                                    app_icon = inst.drawLinkSymbol(app_icon, context);
                                }


                            } catch (Exception | Error e) {
                                Log.d("loadAppIconAsync", e.getMessage(), e);
                                if (app_icon == null) {
                                    app_icon = context.getPackageManager().getDefaultActivityIcon();
                                }
                            }

                            inst.mIconDrawable = app_icon;

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (inst.mIconImage != null) {
                                        inst.mIconImage.setImageDrawable(inst.mIconDrawable);
                                    }
                                }
                            });

                        }

                    } catch (InterruptedException e) {
                        Log.d("loadAppIconAsync", e.getMessage(), e);
                        isrunning = false;
                    }
                } while (isrunning);

            } finally {
                isrunning = false;
                Log.d("IconLoaderTask", "Completing IconLoad task. Processed " + processed);

                synchronized (iconLoaderSync) {
                    iconLoader = null;
                    handler = null;
                }
            }
        }

    }

//
//    private static class IconLoaderTask extends AsyncTask<Context, Void, Drawable> {
//
//        // Keep track of all the exceptions
//        private final Exception exception = null;
//
//        private final WeakReference<AppLauncher> instref;
//
//        private final PackageManager pm;
//
//        IconLoaderTask(AppLauncher inst, PackageManager pm) {
//            super();
//            instref = new WeakReference<>(inst);
//
//            this.pm = pm;
//        }
//        @Override
//        protected Drawable doInBackground(Context... contexts) {
//
//            AppLauncher inst = instref.get();
//            if (inst==null) return null;
//
//            // load the icon
//            Drawable app_icon = null;
//            try {
//
//                String uristr = null;
//                if (inst.isActionLink()) {
//                    uristr = inst.getLinkUri();
//                    if (uristr == null) uristr = "";
//                }
//
//                if (contexts.length==0 || contexts[0]==null) {
//                    app_icon = pm.getDefaultActivityIcon();
//                } else {
//
//                    app_icon = GlobState.getIconsHandler(contexts[0]).getDrawableIconForPackage(inst);
//
//                    if (app_icon == null) {
//                        app_icon = pm.getDefaultActivityIcon();
//                    }
//                    if (inst.isLink()) {
//                        app_icon = inst.drawLinkSymbol(app_icon, contexts[0]);
//                    }
//                }
//
//            } catch (Exception | Error e) {
//                Log.d("loadAppIconAsync", e.getMessage(), e);
//                if (app_icon == null) {
//                    app_icon = pm.getDefaultActivityIcon();
//                }
//            }
//
//            return app_icon;
//        }
//
//        @Override
//        protected void onPostExecute(Drawable app_icon) {
//            if (exception == null) {
//                AppLauncher inst = instref.get();
//                if (inst==null) return;
//                inst.mIconDrawable = app_icon;
//                if (inst.mIconImage != null) {
//                    inst.mIconImage.setImageDrawable(inst.mIconDrawable);
//                }
//            } else {
//                Log.d("loadAppIconAsync", "ERROR Could not load app icon.");
//
//            }
//        }
//    }
}
