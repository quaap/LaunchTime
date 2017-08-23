package com.quaap.launchtime.components;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import android.preference.PreferenceManager;
import android.os.Build;
import android.util.Log;

import com.quaap.launchtime.R;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/*
 *  This file is part of KISS and is licensed under the GPL v3.
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
    // map with available icons packs
    private Map<String, String> iconsPacks = new HashMap<>();
    // map with available drawable for an icons pack
    private Map<String, String> packagesDrawables = new HashMap<>();
    // instance of a resource object of an icon pack
    private Resources iconPackres;
    // package name of the icons pack
    private volatile String iconsPackPackageName;
    // list of back images available on an icons pack
    private List<Bitmap> backImages = new ArrayList<>();
    // bitmap mask of an icons pack
    private Bitmap maskImage = null;
    // front image of an icons pack
    private Bitmap frontImage = null;
    // scale factor of an icons pack
    private float factor = 1.0f;
    private PackageManager pm;
    private Context ctx;

    public static final String DEFAULT_PACK = "default";

    private Map<String, BuiltinIconTheme> builtinThemes = new LinkedHashMap<>();

    public IconsHandler(Context ctx) {
        super();
        this.ctx = ctx;
        this.pm = ctx.getPackageManager();
        initBuiltinIconThemes();
        loadAvailableIconsPacks();
        loadIconsPack();
    }

    private void initBuiltinIconThemes() {
        builtinThemes.put(DEFAULT_PACK, new DefaultIconTheme(DEFAULT_PACK, ctx.getString(R.string.icons_pack_default_name)));

        BuiltinIconTheme bw = new MonochromeIconTheme("bw", "BlackWhite (sys)")
                .setColor(Thing.Mask, Color.WHITE)
                .setColor(Thing.Text, Color.WHITE)
                .setColor(Thing.AltText, Color.WHITE)
                .setColor(Thing.Background, Color.BLACK)
                .setColor(Thing.AltBackground, Color.parseColor("#ff222222"));

        builtinThemes.put(bw.getPackKey(), bw);


        BuiltinIconTheme termcap = new MonochromeIconTheme("termcap", "Termcap (sys)")
                .setColor(Thing.Mask, Color.parseColor("#dd22ff22"))
                .setColor(Thing.Text, Color.parseColor("#dd22ff22"))
                .setColor(Thing.AltText, Color.parseColor("#dd22ff22"))
                .setColor(Thing.Background, Color.BLACK)
                .setColor(Thing.AltBackground, Color.parseColor("#dd112211"));

        builtinThemes.put(termcap.getPackKey(), termcap);


        BuiltinIconTheme coolblue = new MonochromeIconTheme("coolblue", "Coolblue (sys)")
                .setColor(Thing.Mask, Color.parseColor("#ff1111ff"))
                .setColor(Thing.Text, Color.parseColor("#ee2222ff"))
                .setColor(Thing.AltText, Color.parseColor("#ee2222ff"))
                .setColor(Thing.Background, Color.parseColor("#ff000001"))
                .setColor(Thing.AltBackground, Color.parseColor("#ff111112"));

        builtinThemes.put(coolblue.getPackKey(), coolblue);

        BuiltinIconTheme vulcan = new MonochromeIconTheme("vulcan", "Vulcan (sys)")
                .setColor(Thing.Mask, Color.parseColor("#ffff2222"))
                .setColor(Thing.Text, Color.parseColor("#eeff2222"))
                .setColor(Thing.AltText, Color.parseColor("#eeff2222"))
                .setColor(Thing.Background, Color.parseColor("#ff010000"))
                .setColor(Thing.AltBackground, Color.parseColor("#ff121111"));

        builtinThemes.put(vulcan.getPackKey(), vulcan);

    }

    private Collection<BuiltinIconTheme> getBuiltinIconThemes() {
        return builtinThemes.values();
    }



    /**
     * Load configured icons pack
     */
    private void loadIconsPack() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        loadIconsPack(prefs.getString("icons-pack", DEFAULT_PACK));

    }

    /**
     * Parse icons pack metadata
     *
     * @param packageName Android package ID of the package to parse
     */
    public void loadIconsPack(String packageName) {

        //setDefaultColors();

        saveUserColors();

        //clear icons pack
        iconsPackPackageName = packageName;
        packagesDrawables.clear();
        backImages.clear();
        cacheClear();
        iconPackres = null;

        restoreUserColors();

//        // system icons, nothing to do
//        if (iconsPackPackageName.equalsIgnoreCase(DEFAULT_PACK)) {
//            return;
//        }

        // inbuilt theme icons, nothing to do
        if (builtinThemes.keySet().contains(iconsPackPackageName)) {
            return;
        }

        XmlPullParser xpp = null;

        try {
            // search appfilter.xml into icons pack apk resource folder
            iconPackres = pm.getResourcesForApplication(iconsPackPackageName);
            int appfilterid = iconPackres.getIdentifier("appfilter", "xml", iconsPackPackageName);
            if (appfilterid > 0) {
                xpp = iconPackres.getXml(appfilterid);
            }

            if (xpp != null) {
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        //parse <iconback> xml tags used as backgroud of generated icons
                        if (xpp.getName().equals("iconback")) {
                            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                if (xpp.getAttributeName(i).startsWith("img")) {
                                    String drawableName = xpp.getAttributeValue(i);
                                    Bitmap iconback = loadBitmap(drawableName);
                                    if (iconback != null) {
                                        backImages.add(iconback);
                                    }
                                }
                            }
                        }
                        //parse <iconmask> xml tags used as mask of generated icons
                        else if (xpp.getName().equals("iconmask")) {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                                String drawableName = xpp.getAttributeValue(0);
                                maskImage = loadBitmap(drawableName);
                            }
                        }
                        //parse <iconupon> xml tags used as front image of generated icons
                        else if (xpp.getName().equals("iconupon")) {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                                String drawableName = xpp.getAttributeValue(0);
                                frontImage = loadBitmap(drawableName);
                            }
                        }
                        //parse <scale> xml tags used as scale factor of original bitmap icon
                        else if (xpp.getName().equals("scale")) {
                            if (xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("factor")) {
                                factor = Float.valueOf(xpp.getAttributeValue(0));
                            }
                        }
                        //parse <item> xml tags for custom icons
                        if (xpp.getName().equals("item")) {
                            String componentName = null;
                            String drawableName = null;

                            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                if (xpp.getAttributeName(i).equals("component")) {
                                    componentName = xpp.getAttributeValue(i);
                                } else if (xpp.getAttributeName(i).equals("drawable")) {
                                    drawableName = xpp.getAttributeValue(i);
                                }
                            }
                            if (!packagesDrawables.containsKey(componentName)) {
                                packagesDrawables.put(componentName, drawableName);
                            }
                        }
                    }
                    eventType = xpp.next();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing appfilter.xml " + e);
        }

    }

    private Bitmap loadBitmap(String drawableName) {
        int id = iconPackres.getIdentifier(drawableName, "drawable", iconsPackPackageName);
        if (id > 0) {
            //noinspection deprecation: Resources.getDrawable(int, Theme) requires SDK 21+
            Drawable bitmap = iconPackres.getDrawable(id);
            if (bitmap instanceof BitmapDrawable) {
                return ((BitmapDrawable) bitmap).getBitmap();
            }
        }
        return null;
    }


    private Drawable getDefaultAppDrawable(ComponentName componentName, String uristr) {

        Drawable app_icon = null;

        Intent intent;
        if (uristr!=null) {
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
            Log.e("IconLookup", "Couldn't get icon for" + componentName.getClassName(), e);
        }

        if (app_icon==null) {
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

        if (app_icon == null) {
            app_icon = pm.getDefaultActivityIcon();
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

        for (String key: builtinThemes.keySet()) {

            if (iconsPackPackageName.equalsIgnoreCase(key)) {
                return builtinThemes.get(key).getDrawable(componentName, uristr);
            }
        }

        String drawable = packagesDrawables.get(componentName.toString());
        if (drawable != null) { //there is a custom icon
            int id = iconPackres.getIdentifier(drawable, "drawable", iconsPackPackageName);
            if (id > 0) {
                //noinspection deprecation: Resources.getDrawable(int, Theme) requires SDK 21+
                return iconPackres.getDrawable(id);
            }
        }

        //search first in cache
        Drawable systemIcon = cacheGetDrawable(componentName.toString());
        if (systemIcon != null)
            return systemIcon;

        systemIcon = this.getDefaultAppDrawable(componentName, uristr);
        if (systemIcon instanceof BitmapDrawable) {
            Drawable generated = generateBitmap(systemIcon);
            cacheStoreDrawable(componentName.toString(), generated);
            return generated;
        }
        return systemIcon;
    }

    private Drawable generateBitmap(Drawable defaultBitmap) {

        // if no support images in the icon pack return the bitmap itself
        if (backImages.size() == 0) {
            return defaultBitmap;
        }

        // select a random background image
        Random r = new Random();
        int backImageInd = r.nextInt(backImages.size());
        Bitmap backImage = backImages.get(backImageInd);
        int w = backImage.getWidth();
        int h = backImage.getHeight();

        // create a bitmap for the result
        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        // draw the background first
        canvas.drawBitmap(backImage, 0, 0, null);

        // scale original icon
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(((BitmapDrawable) defaultBitmap).getBitmap(), (int) (w * factor), (int) (h * factor), false);

        if (maskImage != null) {
            // draw the scaled bitmap with mask
            Bitmap mutableMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas maskCanvas = new Canvas(mutableMask);
            maskCanvas.drawBitmap(maskImage, 0, 0, new Paint());

            // paint the bitmap with mask into the result
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            canvas.drawBitmap(scaledBitmap, (w - scaledBitmap.getWidth()) / 2, (h - scaledBitmap.getHeight()) / 2, null);
            canvas.drawBitmap(mutableMask, 0, 0, paint);
            paint.setXfermode(null);
        } else { // draw the scaled bitmap without mask
            canvas.drawBitmap(scaledBitmap, (w - scaledBitmap.getWidth()) / 2, (h - scaledBitmap.getHeight()) / 2, null);
        }

        // paint the front
        if (frontImage != null) {
            canvas.drawBitmap(frontImage, 0, 0, null);
        }

        return new BitmapDrawable(iconPackres, result);
    }


    private String [] packs = {"org.adw.launcher.THEMES", "fr.neamar.kiss.THEMES", "com.novalauncher.THEME", "com.anddoes.launcher.THEME" };

    /**
     * Scan for installed icons packs
     */
    public void loadAvailableIconsPacks() {

        List<ResolveInfo> launcherthemes = new ArrayList<>();

        for (String pack: packs) {
            try {
                launcherthemes.addAll(pm.queryIntentActivities(new Intent(pack), PackageManager.GET_META_DATA));
            } catch (Exception e) {
                Log.e(TAG, "Unable to query theme: " + pack, e);
            }
        }



        iconsPacks.clear();

        for (ResolveInfo ri : launcherthemes) {

            String packageName = ri.activityInfo.packageName;
            if (!iconsPacks.containsKey(packageName)) {
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                    String name = pm.getApplicationLabel(ai).toString();
                    iconsPacks.put(packageName, name);
                } catch (PackageManager.NameNotFoundException e) {
                    // shouldn't happen
                    Log.e(TAG, "Unable to found package " + packageName + e);
                }
            }
        }
    }

    public String getIconsPackName() {
        return iconsPackPackageName;
    }


    private Map<String, String> getIconsPacks() {
        return iconsPacks;
    }

    public Map<String, String> getAllIconsThemes() {
        Map<String, String> iconsThemes = new LinkedHashMap<>();

        iconsThemes.put(DEFAULT_PACK, builtinThemes.get(DEFAULT_PACK).getPackName());

        iconsThemes.putAll(getIconsPacks());

        for (IconsHandler.BuiltinIconTheme ic: getBuiltinIconThemes()) {
            if ( !ic.getPackKey().equals(DEFAULT_PACK)) {
                iconsThemes.put(ic.getPackKey(), ic.getPackName());
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


    private final String [] COLOR_PREFS = {"cattab_background", "cattabselected_background", "cattabselected_text",  "cattabtextcolor", "cattabtextcolorinv",
            "wallpapercolor",  "textcolor"};

    private Thing [] THING_MAP = {Thing.AltBackground, Thing.AltBackground, Thing.AltText, Thing.Text, Thing.Background, Thing.Background, Thing.Text};


    private int [] getColorDefaults()  {
        return new int [] {getResColor(R.color.cattab_background), getResColor(R.color.cattabselected_background),
            getResColor(R.color.cattabselected_text),  getResColor(R.color.textcolor), getResColor(R.color.textcolorinv),
            Color.TRANSPARENT,  getResColor(R.color.textcolor)};
    };


    private int getResColor(int res) {
        if (Build.VERSION.SDK_INT >= 23) {
            return ctx.getColor(res);
        } else {
            return ctx.getResources().getColor(res);
        }
    }


    private int getCurrentThemeColor(String pref) {
        BuiltinIconTheme theme = builtinThemes.get(iconsPackPackageName);
        if (theme!=null && theme.hasColors()) {
            int max = COLOR_PREFS.length;
            for (int i=0; i<max; i++) {
                if (pref.equals(COLOR_PREFS[i])) {
                    return theme.getColor(THING_MAP[i]);
                }
            }
        }

        int [] colorDefaults = getColorDefaults();
        int max = COLOR_PREFS.length;
        for (int i=0; i<max; i++) {
            if (pref.equals(COLOR_PREFS[i])) {
                return colorDefaults[i];
            }
        }
        throw new IllegalArgumentException("No such preference '" + pref + "'");
    }


    private String getThemePrefName(String pref) {
        return "theme_" + iconsPackPackageName + "_" + pref;
    }


    public void resetUserColors() {


        SharedPreferences.Editor themeedit = ctx.getSharedPreferences("theme",Context.MODE_PRIVATE).edit();
        SharedPreferences.Editor appedit = PreferenceManager.getDefaultSharedPreferences(ctx).edit();

        try {

            int max = COLOR_PREFS.length;
            for (int i=0; i<max; i++) {
                appedit.putInt(COLOR_PREFS[i],  getCurrentThemeColor(COLOR_PREFS[i]));
                //themeedit.putInt(getThemePrefName(COLOR_PREFS[i]),  getCurrentThemeColor(COLOR_PREFS[i]));
                themeedit.remove(getThemePrefName(COLOR_PREFS[i]));
            }

        } finally {
            appedit.apply();
            themeedit.apply();
        }
    }


    private void saveUserColors() {

        SharedPreferences appprefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor themeedit = ctx.getSharedPreferences("theme",Context.MODE_PRIVATE).edit();

        try {

            int max = COLOR_PREFS.length;
            for (int i=0; i<max; i++) {
                themeedit.putInt(getThemePrefName(COLOR_PREFS[i]),  appprefs.getInt(COLOR_PREFS[i], getCurrentThemeColor(COLOR_PREFS[i])));
            }

        } finally {
            themeedit.apply();
        }
    }


    private void restoreUserColors() {

        SharedPreferences themeprefs = ctx.getSharedPreferences("theme",Context.MODE_PRIVATE);
        SharedPreferences.Editor appedit = PreferenceManager.getDefaultSharedPreferences(ctx).edit();

        try {

            int max = COLOR_PREFS.length;
            for (int i=0; i<max; i++) {
                appedit.putInt(COLOR_PREFS[i],  themeprefs.getInt(getThemePrefName(COLOR_PREFS[i]), getCurrentThemeColor(COLOR_PREFS[i])));
            }

        } finally {
            appedit.apply();
        }
    }


    private enum Thing {Mask, Text, AltText, Background, AltBackground}


    abstract class BuiltinIconTheme {

        private String mKey;
        private String mName;

        private Map<Thing,Integer> mColors = new HashMap<>();

        BuiltinIconTheme(String key, String name) {
            this(key, name, null);
        }

        BuiltinIconTheme(String key, String name, Map<Thing, Integer> colors) {
            mKey = key;
            mName = name;
            if (colors != null) {
                mColors.putAll(colors);
            }

        }

        String getPackKey() {
            return mKey;
        }

        String getPackName() {
            return mName;
        }

        public abstract Drawable getDrawable(ComponentName componentName, String uristr);

        boolean hasColors() {
            return mColors.size()>0;
        }

        BuiltinIconTheme setColor(Thing thing, int color) {
            mColors.put(thing, color);
            return this;
        }

        Integer getColor(Thing thing) {
            return mColors.get(thing);
        }



        void applyTheme() {

            //SharedPreferences themeprefs = ctx.getSharedPreferences("theme",Context.MODE_PRIVATE);

            SharedPreferences appprefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            SharedPreferences.Editor appedit = appprefs.edit();
            try {

                int[] colorDefaults = getColorDefaults();
                int max = COLOR_PREFS.length;
                for (int i = 0; i < max; i++) {
                    if (hasColors()) {
                        appedit.putInt(COLOR_PREFS[i], getColor(THING_MAP[i]));
                    } else {
                        appedit.putInt(COLOR_PREFS[i], colorDefaults[i]);
                    }
                }
            } finally {
                appedit.apply();
            }

        }


    }


    private class DefaultIconTheme extends BuiltinIconTheme {


        DefaultIconTheme(String key, String name) {
            super(key, name);
        }

        @Override
        public Drawable getDrawable(ComponentName componentName, String uristr) {
            return getDefaultAppDrawable(componentName, uristr);
        }


    }

    private class MonochromeIconTheme extends BuiltinIconTheme {
        MonochromeIconTheme(String key, String name) {
            super(key, name);
        }

        public MonochromeIconTheme(String key, String name, Map<Thing, Integer> colors) {
            super(key, name, colors);
        }

        @Override
        public Drawable getDrawable(ComponentName componentName, String uristr) {

            //Log.d(TAG, "getDrawable called for " + componentName.getPackageName());

            Drawable app_icon = getDefaultAppDrawable(componentName, uristr);

            app_icon = app_icon.mutate();

            if (getColor(Thing.Mask) == Color.WHITE) {
                app_icon = convertToGrayscale(app_icon);
            } else {
                PorterDuff.Mode mode = PorterDuff.Mode.MULTIPLY;
                app_icon.setColorFilter(getColor(Thing.Mask), mode);
            }


            return app_icon;
        }


    }

    public class PolychromeIconTheme extends BuiltinIconTheme {
        private int [] mFGColors;
        private int mBGColor;

        public PolychromeIconTheme(String key, String name, int [] color, int bgcolor) {
            super(key, name);
            mFGColors = Arrays.copyOf(color, color.length);
            mBGColor = bgcolor;
        }

        @Override
        public Drawable getDrawable(ComponentName componentName, String uristr) {

            //Log.d(TAG, "getDrawable called for " + componentName.getPackageName());

            Drawable app_icon = getDefaultAppDrawable(componentName, uristr);

            app_icon = app_icon.mutate();


            PorterDuff.Mode mode = PorterDuff.Mode.MULTIPLY;

            int color = Math.abs(componentName.getPackageName().hashCode()) % mFGColors.length;
            app_icon.setColorFilter(mFGColors[color], mode);

            return app_icon;
        }


    }

    private Drawable convertToGrayscale(Drawable drawable)
    {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);

        drawable.setColorFilter(filter);

        return drawable;
    }
}