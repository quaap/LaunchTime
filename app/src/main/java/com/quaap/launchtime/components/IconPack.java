package com.quaap.launchtime.components;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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

public class IconPack {

    private static final String TAG = "IconPack";

    private Map<String, String> packagesDrawables = new LinkedHashMap<>();
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


    public IconPack(Context ctx, String packageName) {

        //clear icons pack
        iconsPackPackageName = packageName;
        packagesDrawables.clear();
        backImages.clear();

        iconPackres = null;



        XmlPullParser xpp = null;

        try {
            // search appfilter.xml into icons pack apk resource folder
            iconPackres = ctx.getPackageManager().getResourcesForApplication(iconsPackPackageName);
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
        } catch (Exception | Error e) {
            Log.e(TAG, "Error parsing appfilter.xml " + e);
        }

    }

    public Map<String, Drawable> getAllIcons() {

        Map<String, Drawable> icons = new LinkedHashMap<>();

        for (String key: packagesDrawables.keySet()) {
            Drawable g = get(key);
            if (g!=null) {
                icons.put(key, g);
            }
        }

        return icons;
    }

    public Set<Drawable> getUniqueIcons() {

        Set<Drawable> icons = new LinkedHashSet<>();
        Set<String> iconNames = new LinkedHashSet<>();

        for (String key: packagesDrawables.keySet()) {
            if (!iconNames.contains(packagesDrawables.get(key))) {
                Drawable g = get(key);
                if (g!=null) {
                    icons.add(get(key));
                    iconNames.add(packagesDrawables.get(key));
                }
            }
        }

        return icons;
    }

    public Drawable get(ComponentName componentName) {
        return get(componentName.toString());
    }

    public Drawable get(String componentName) {
        String drawable = packagesDrawables.get(componentName);
        if (drawable != null) { //there is a custom icon
            try {
                int id = iconPackres.getIdentifier(drawable, "drawable", iconsPackPackageName);
                if (id > 0) {
                    //noinspection deprecation: Resources.getDrawable(int, Theme) requires SDK 21+
                    return iconPackres.getDrawable(id);
                }
            } catch (OutOfMemoryError e){
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return null;
    }


    public Drawable generateBitmap(Drawable defaultBitmap) {

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




    private Bitmap loadBitmap(String drawableName) {
        try {
            int id = iconPackres.getIdentifier(drawableName, "drawable", iconsPackPackageName);
            if (id > 0) {
                //noinspection deprecation: Resources.getDrawable(int, Theme) requires SDK 21+
                Drawable bitmap = iconPackres.getDrawable(id);
                if (bitmap instanceof BitmapDrawable) {
                    return ((BitmapDrawable) bitmap).getBitmap();
                }
            }
        } catch (OutOfMemoryError e){
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }



    private static  String [] packs = {"org.adw.launcher.THEMES", "fr.neamar.kiss.THEMES", "com.novalauncher.THEME", "com.anddoes.launcher.THEME" };

    /**
     * Scan for installed icons packs
     */
    public static Map<String, String> listAvailableIconsPacks(Context ctx) {

        PackageManager pm = ctx.getPackageManager();

        List<ResolveInfo> launcherthemes = new ArrayList<>();

        for (String pack: packs) {
            try {
                launcherthemes.addAll(pm.queryIntentActivities(new Intent(pack), PackageManager.GET_META_DATA));
            } catch (Exception e) {
                Log.e(TAG, "Unable to query theme: " + pack, e);
            }
        }



        Map<String, String> iconsPacks = new HashMap<>();

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

        return iconsPacks;
    }

}
