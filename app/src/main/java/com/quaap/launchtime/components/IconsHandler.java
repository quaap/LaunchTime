package com.quaap.launchtime.components;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 *  Portions of this file are dereived from KISS and is licensed under the GPL v3.
 *  https://github.com/Neamar/KISS
 *
 *  Modified by Tom Kliethermes. 2017
 */

/*
 * Inspired from http://stackoverflow.com/questions/31490630/how-to-load-icon-from-icon-pack
 */

/**
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

public class IconsHandler {

    private static final String TAG = "IconsHandler";

    private IconPack iconPack;

    private volatile String iconsPackPackageName;

    // map with available icons packs
    private Map<String, String> iconsPacks = new HashMap<>();

    private PackageManager pm;
    private Context ctx;

    public static final String DEFAULT_PACK = "default";

    private Theme theme;

    public IconsHandler(Context ctx) {
        super();
        this.ctx = ctx;
        this.pm = ctx.getPackageManager();
        theme = new Theme(ctx, this);
        loadAvailableIconsPacks();
        loadIconsPack();
    }


    /**
     * Load configured icons pack
     */
    private void loadIconsPack() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
        loadIconsPack(prefs.getString("icons-pack", DEFAULT_PACK));

    }

    /**
     * Parse icons pack metadata
     *
     * @param packageName Android package ID of the package to parse
     */
    public void loadIconsPack(String packageName) {

        theme.saveUserColors();

        iconsPackPackageName = packageName;

        cacheClear();

        boolean hasusercolors = theme.restoreUserColors();

        // inbuilt theme icons, nothing to do
        if (theme.isBuiltinTheme(iconsPackPackageName)) {
            if (!hasusercolors) theme.getBuiltinTheme(iconsPackPackageName).applyTheme();
            return;
        }
        iconPack = new IconPack(ctx,iconsPackPackageName);
    }

    public boolean isIconTintable() {
        return theme.isBuiltinThemeIconTintable(iconsPackPackageName);
    }

    public boolean isIconTintable(String packageName) {
        return theme.isBuiltinThemeIconTintable(packageName);
    }

    public Drawable getCustomIcon(ComponentName componentName, String uristr) {
        Drawable app_icon = null;
        Bitmap custombitmap = SpecialIconStore.loadBitmap(ctx, componentName, SpecialIconStore.IconType.Custom);
        if (custombitmap != null) {
            app_icon = new BitmapDrawable(ctx.getResources(), custombitmap);
        }
        return  app_icon;
    }

    public Drawable getDefaultAppDrawable(ComponentName componentName, String uristr) {
        return getDefaultAppDrawable(componentName, uristr, false);
    }

    public Drawable getDefaultAppDrawable(ComponentName componentName, String uristr, boolean nodefault) {

        Drawable app_icon = null;

        try {
            app_icon = getCustomIcon(componentName, uristr);

            if (app_icon == null) {

                Intent intent;
                if (uristr != null) {
                    if (uristr.equals("")) {
                        intent = new Intent(componentName.getClassName());
                    } else {
                        intent = new Intent(componentName.getClassName(), Uri.parse(uristr));
                    }

                } else {
                    intent = new Intent(Intent.ACTION_MAIN);
                    intent.setClassName(componentName.getPackageName(), componentName.getClassName());
                }

                try {
                    app_icon = pm.getActivityIcon(intent);
                } catch (Exception | OutOfMemoryError e) {
                    Log.e("IconLookup", "Couldn't get icon for " + componentName.getClassName(), e);
                }
            }

            if (app_icon == null) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        LauncherApps launcher = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                        LauncherActivityInfo info = launcher.getActivityList(componentName.getPackageName(), android.os.Process.myUserHandle()).get(0);
                        app_icon = info.getBadgedIcon(0);
                    } else {
                        app_icon = pm.getActivityIcon(componentName);
                    }
                } catch (NameNotFoundException | IndexOutOfBoundsException e) {
                    Log.e(TAG, "Unable to found component " + componentName.toString() + e);
                    return null;
                }
            }

            if (app_icon == null && !nodefault) {
                app_icon = pm.getDefaultActivityIcon();
            }

            Bitmap bitmap = SpecialIconStore.loadBitmap(ctx, componentName, SpecialIconStore.IconType.Shortcut);

            if (bitmap != null && app_icon!=null) {
                Log.d(TAG, "Got special icon for " + componentName.getClassName());
                try {
                    Bitmap newbm = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
                    Canvas canvas = new Canvas(newbm);
                    canvas.drawBitmap(bitmap, 0, 0, null);
                    app_icon.setBounds(canvas.getWidth() / 2, 0, canvas.getWidth(), canvas.getHeight() / 2);
                    app_icon.draw(canvas);
                    app_icon = new BitmapDrawable(ctx.getResources(), newbm);
                    //Log.d("loadAppIconAsync", " yo");
                } catch (Exception | OutOfMemoryError e) {
                    Log.e("loadAppIconAsync", "couldn't make special icon", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception getting app icon for " + componentName , e);
            if (app_icon == null && !nodefault)  {
                app_icon = pm.getDefaultActivityIcon();
            }
        }

        return app_icon;
    }


    /**
     * Get or generate icon for an app
     */
    public Drawable getDrawableIconForPackage(ComponentName componentName, String uristr) {
//        // system icons, nothing to do
//        if (iconsPackPackageName.equalsIgnoreCase(DEFAULT_PACK)) {
//           // Log.d(TAG, "getDrawableIconForPackage called for " + componentName);
//            return getDefaultAppDrawable(componentName, uristr);
//        }

        for (String key: theme.getBuiltinIconThemes().keySet()) {

            if (iconsPackPackageName.equalsIgnoreCase(key)) {
                return theme.getBuiltinTheme(key).getDrawable(componentName, uristr);
            }
        }

        Drawable icon = getCustomIcon(componentName, uristr);

        if (icon!=null) return  icon;

        icon = iconPack.get(componentName);
        if (icon!=null) return  icon;

        //search first in cache
        Drawable systemIcon = cacheGetDrawable(componentName.toString());
        if (systemIcon != null)
            return systemIcon;

        systemIcon = this.getDefaultAppDrawable(componentName, uristr);
        if (systemIcon instanceof BitmapDrawable) {
            Drawable generated = iconPack.generateBitmap(systemIcon);
            cacheStoreDrawable(componentName.toString(), generated);
            return generated;
        }
        return systemIcon;
    }


    public String getIconsPackPackageName() {
        return iconsPackPackageName;
    }


    public Theme getTheme() {
        return theme;
    }


    /**
     * Scan for installed icons packs
     */
    public void loadAvailableIconsPacks() {

        iconsPacks = IconPack.listAvailableIconsPacks(ctx);

    }



    private Map<String, String> getIconsPacks() {
        return iconsPacks;
    }

    public Map<String, String> getAllIconsThemes() {
        Map<String, String> iconsThemes = new LinkedHashMap<>();

        iconsThemes.put(DEFAULT_PACK, theme.getBuiltinTheme(DEFAULT_PACK).getPackName());

        iconsThemes.putAll(getIconsPacks());

        for (Theme.BuiltinTheme ic: theme.getBuiltinIconThemes().values()) {
            if ( !ic.getPackKey().equals(DEFAULT_PACK)) {
                iconsThemes.put(ic.getPackKey(), ic.getPackName() + " (sys)");
            }
        }

        return iconsThemes;
    }

    private boolean isDrawableInCache(String key) {
        File drawableFile = cacheGetFileName(key);
        return drawableFile.isFile();
    }

    private boolean cacheStoreDrawable(String key, Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            File drawableFile = cacheGetFileName(key);
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(drawableFile);
                ((BitmapDrawable) drawable).getBitmap().compress(CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Unable to store drawable in cache " + e);
            }
        }
        return false;
    }

    private Drawable cacheGetDrawable(String key) {

        if (!isDrawableInCache(key)) {
            return null;
        }

        FileInputStream fis;
        try {
            fis = new FileInputStream(cacheGetFileName(key));
            BitmapDrawable drawable =
                    new BitmapDrawable(this.ctx.getResources(), BitmapFactory.decodeStream(fis));
            fis.close();
            return drawable;
        } catch (Exception e) {
            Log.e(TAG, "Unable to get drawable from cache " + e);
        }

        return null;
    }

    /**
     * create path for icons cache like this
     * {cacheDir}/icons/{icons_pack_package_name}_{key_hash}.png
     */
    private File cacheGetFileName(String key) {
        return new File(getIconsCacheDir() + iconsPackPackageName + "_" + key.hashCode() + ".png");
    }

    private File getIconsCacheDir() {
        return new File(this.ctx.getCacheDir().getPath() + "/icons/");
    }

    /**
     * Clear cache
     */
    private void cacheClear() {
        File cacheDir = this.getIconsCacheDir();

        if (!cacheDir.isDirectory())
            return;

        for (File item : cacheDir.listFiles()) {
            if (!item.delete()) {
                Log.w(TAG, "Failed to delete file: " + item.getAbsolutePath());
            }
        }
    }


}