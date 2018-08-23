package com.quaap.launchtime.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.quaap.launchtime.GlobState;
import com.quaap.launchtime.R;
import com.quaap.launchtime.apps.AppLauncher;

import java.util.HashMap;
import java.util.LinkedHashMap;
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

@SuppressLint("ApplySharedPref")
public class Theme {


    //public final static String NEW_SYS = "newish";
    public static final String PREFS_UPDATE_KEY = "prefsUpdate";

    private enum Thing {Mask, Text, AltText, Background, AltBackground, Wallpaper}

    private final int [] COLOR_PREFS_REFS = {R.string.pref_key_icon_tint, R.string.pref_key_cattab_background,
            R.string.pref_key_cattabselected_background, R.string.pref_key_cattabselected_text,
            R.string.pref_key_cattabtextcolor, R.string.pref_key_cattabtextcolorinv,
            R.string.pref_key_wallpapercolor,  R.string.pref_key_textcolor};

    private final String [] COLOR_PREFS;

    private final Thing [] THING_MAP = {Thing.Mask, Thing.Background, Thing.AltBackground, Thing.AltText, Thing.Text, Thing.Background, Thing.Wallpaper, Thing.Text};


//    private int [] getColorDefaultsClassic()  {
//        return new int [] {getResColor(R.color.icon_tint), getResColor(R.color.cattab_background), getResColor(R.color.cattabselected_background),
//                getResColor(R.color.cattabselected_text),  getResColor(R.color.textcolor), getResColor(R.color.textcolorinv),
//                Color.TRANSPARENT,  getResColor(R.color.textcolor)};
//    }

    private int [] getColorDefaults()  {
        return new int [] {getResColor(R.color.icon_tint), getResColor(R.color.cattab_background), getResColor(R.color.cattabselected_background),
                getResColor(R.color.cattabselected_text),  getResColor(R.color.textcolor), getResColor(R.color.textcolorinv),
                getResColor(R.color.wallpaper_color),  getResColor(R.color.textcolor)};
    }


    private final Context ctx;


    private final Map<String, BuiltinTheme> builtinThemes = new LinkedHashMap<>();

    private final IconsHandler iconsHandler;

    private final SharedPreferences prefs;

    Theme(Context ctx, IconsHandler ich) {
        this.ctx = ctx;

        COLOR_PREFS = new String[COLOR_PREFS_REFS.length];
        for (int i=0; i<COLOR_PREFS_REFS.length; i++) {
            COLOR_PREFS[i] = ctx.getString(COLOR_PREFS_REFS[i]);
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
        iconsHandler = ich;
        initBuiltinIconThemes();

    }



    //TODO: load these from a file / other package

    private void initBuiltinIconThemes() {
        //builtinThemes.put(IconsHandler.DEFAULT_PACK, new DefaultTheme(IconsHandler.DEFAULT_PACK, ctx.getString(R.string.icons_pack_default_name)));

        int[] defcolors = getColorDefaults();
        BuiltinTheme newish = new MonochromeTheme(IconsHandler.DEFAULT_PACK, ctx.getString(R.string.icons_pack_default_name))
                .setColor(Thing.Mask, defcolors[0])
                .setColor(Thing.Text, defcolors[4])
                .setColor(Thing.AltText, defcolors[3])
                .setColor(Thing.Background, defcolors[1])
                .setColor(Thing.Wallpaper, defcolors[6])
                .setColor(Thing.AltBackground, defcolors[2]);

        builtinThemes.put(newish.getPackKey(), newish);


        BuiltinTheme classic = new MonochromeTheme("classic", ctx.getString(R.string.theme_classic))
                .setColor(Thing.Mask, Color.TRANSPARENT)
                .setColor(Thing.Text, getResColor(R.color.textcolor_classic))
                .setColor(Thing.AltText, Color.WHITE)
                .setColor(Thing.Background, getResColor(R.color.cattab_background_classic))
                .setColor(Thing.Wallpaper, Color.TRANSPARENT)
                .setColor(Thing.AltBackground, getResColor(R.color.cattabselected_background_classic));

        builtinThemes.put(classic.getPackKey(), classic);


        BuiltinTheme crystal1= new MonochromeTheme("crystal1", ctx.getString(R.string.theme_crystal))
                .setColor(Thing.Mask, Color.parseColor("#CC888888"))
                .setColor(Thing.Text, Color.argb(255,240,240,240))
                .setColor(Thing.AltText, Color.WHITE)
                .setColor(Thing.Wallpaper, Color.parseColor("#772955A8"))
                .setColor(Thing.Background, Color.argb(20,250,250,255))
                .setColor(Thing.AltBackground, Color.argb(90,250,250,255));

        builtinThemes.put(crystal1.getPackKey(), crystal1);


        BuiltinTheme charcoal = new MonochromeTheme("charcoal", ctx.getString(R.string.theme_charcoal))
                .setColor(Thing.Mask, Color.parseColor("#4F1D1D1D"))
                .setColor(Thing.Text, Color.parseColor("#EFBBBBBB"))
                .setColor(Thing.AltText, Color.LTGRAY)
                .setColor(Thing.Wallpaper, Color.parseColor("#C2505050"))
                .setColor(Thing.Background, Color.parseColor("#DD434343"))
                .setColor(Thing.AltBackground, Color.parseColor("#e3353535"));

        builtinThemes.put(charcoal.getPackKey(), charcoal);

        BuiltinTheme paper= new MonochromeTheme("whitepaper", ctx.getString(R.string.theme_paper))
                .setColor(Thing.Mask, Color.parseColor("#CB404040"))
                .setColor(Thing.Text, Color.argb(255,20,20,20))
                .setColor(Thing.AltText, Color.DKGRAY)
                .setColor(Thing.Wallpaper, Color.parseColor("#DBF2F2F2"))
                .setColor(Thing.Background, Color.parseColor("#C3F7F7F7"))
                .setColor(Thing.AltBackground, Color.parseColor("#9FEFF2F2"));

        builtinThemes.put(paper.getPackKey(), paper);

        BuiltinTheme transp = new MonochromeTheme("transparent", ctx.getString(R.string.theme_transp))
                .setColor(Thing.Mask, Color.TRANSPARENT)
                .setColor(Thing.Text, Color.argb(255,240,240,240))
                .setColor(Thing.AltText, Color.WHITE)
                .setColor(Thing.Background, Color.TRANSPARENT)
                .setColor(Thing.Wallpaper, Color.parseColor("#77132951"))
                .setColor(Thing.AltBackground, Color.TRANSPARENT);

        builtinThemes.put(transp.getPackKey(), transp);



        int [] ucolors = {Color.argb(127,50,50,50), Color.argb(127,10,10,160), Color.argb(127,170,10,10)};
        int [] ubcolors = {Color.BLACK, Color.argb(127,0,0,60), Color.argb(127,60,0,6)};
        for (int i=1; i<=3; i++) {
            BuiltinTheme u = new MonochromeTheme("user" + i, ctx.getString(R.string.user_theme, i))
                    .setColor(Thing.Mask, Color.TRANSPARENT)
                    .setColor(Thing.Text, Color.argb(255,230,230,230))
                    .setColor(Thing.AltText, Color.WHITE)
                    .setColor(Thing.Background, ubcolors[i-1])
                    .setColor(Thing.Wallpaper, ubcolors[i-1])
                    .setColor(Thing.AltBackground, ucolors[i-1]);

            builtinThemes.put(u.getPackKey(), u);
        }


        BuiltinTheme bwicon = new MonochromeTheme("bwicon", ctx.getString(R.string.theme_bw))
                .setColor(Thing.Mask, Color.WHITE)
                .setColor(Thing.Text, Color.argb(255,220,220,220))
                .setColor(Thing.AltText, Color.WHITE)
                .setColor(Thing.Background, Color.BLACK)
                .setColor(Thing.Wallpaper, Color.BLACK)
                .setColor(Thing.AltBackground, Color.parseColor("#ff222222"));

        builtinThemes.put(bwicon.getPackKey(), bwicon);


        BuiltinTheme termcap = new MonochromeTheme("termcap", ctx.getString(R.string.theme_termcap))
                .setColor(Thing.Mask, Color.parseColor("#dd22ff22"))
                .setColor(Thing.Text, Color.parseColor("#dd22ff22"))
                .setColor(Thing.AltText, Color.parseColor("#dd22ff22"))
                .setColor(Thing.Background, Color.BLACK)
                .setColor(Thing.Wallpaper, Color.BLACK)
                .setColor(Thing.AltBackground, Color.parseColor("#dd112211"));

        builtinThemes.put(termcap.getPackKey(), termcap);


        BuiltinTheme coolblue = new MonochromeTheme("coolblue", ctx.getString(R.string.theme_coolblue))
                .setColor(Thing.Mask, Color.parseColor("#ff1111ff"))
                .setColor(Thing.Text, Color.parseColor("#eeffffff"))
                .setColor(Thing.AltText, Color.parseColor("#eeffffff"))
                .setColor(Thing.Background, Color.parseColor("#88000077"))
                .setColor(Thing.Wallpaper, Color.parseColor("#55000077"))
                .setColor(Thing.AltBackground, Color.parseColor("#881111ff"));

        builtinThemes.put(coolblue.getPackKey(), coolblue);

        BuiltinTheme redplanet = new MonochromeTheme("redplanet", ctx.getString(R.string.theme_redplanet))
                .setColor(Thing.Mask, Color.parseColor("#ffff2222"))
                .setColor(Thing.Text, Color.parseColor("#eeff2222"))
                .setColor(Thing.AltText, Color.parseColor("#eeff2222"))
                .setColor(Thing.Background, Color.parseColor("#77550000"))
                .setColor(Thing.Wallpaper, Color.parseColor("#33550000"))
                .setColor(Thing.AltBackground, Color.parseColor("#22121111"));

        builtinThemes.put(redplanet.getPackKey(), redplanet);

        BuiltinTheme ladypink = new MonochromeTheme("ladypink", ctx.getString(R.string.theme_ladypink))
                .setColor(Thing.Mask, Color.parseColor("#ffff1493"))
                .setColor(Thing.Text, Color.parseColor("#eeffffff"))
                .setColor(Thing.AltText, Color.parseColor("#eeffc0cb"))
                .setColor(Thing.Background, Color.parseColor("#ffff69b4"))
                .setColor(Thing.Wallpaper, Color.parseColor("#88ff69b4"))
                .setColor(Thing.AltBackground, Color.parseColor("#ffff1493"));

        builtinThemes.put(ladypink.getPackKey(), ladypink);
    }

    public  Map<String, BuiltinTheme> getBuiltinIconThemes() {
        return builtinThemes;
    }


    public boolean isBuiltinTheme(String packagename) {
        return builtinThemes.containsKey(packagename);
    }

    public BuiltinTheme getBuiltinTheme(String packagename) {
        return builtinThemes.get(packagename);
    }


    public boolean isBuiltinThemeIconTintable(String packagename) {
        return isBuiltinTheme(packagename) && (builtinThemes.get(packagename) instanceof MonochromeTheme);
    }



    private int getResColor(int res) {
        if (Build.VERSION.SDK_INT >= 23) {
            return ctx.getColor(res);
        } else {
            return ctx.getResources().getColor(res);
        }
    }


    private int getCurrentThemeColor(String pref) {
        BuiltinTheme theme = builtinThemes.get(iconsHandler.getIconsPackPackageName());
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
        return "theme_" + iconsHandler.getIconsPackPackageName() + "_" + pref;
    }



    public void resetUserColors() {


        SharedPreferences.Editor themeedit = ctx.getSharedPreferences("theme", Context.MODE_PRIVATE).edit();

        prefs.edit().putBoolean(Theme.PREFS_UPDATE_KEY, true).apply();
        SharedPreferences.Editor appedit = prefs.edit();

        try {

            int max = COLOR_PREFS.length;
            for (String COLOR_PREF : COLOR_PREFS) {
                appedit.putInt(COLOR_PREF, getCurrentThemeColor(COLOR_PREF));
                //themeedit.putInt(getThemePrefName(COLOR_PREFS[i]),  getCurrentThemeColor(COLOR_PREFS[i]));
                themeedit.remove(getThemePrefName(COLOR_PREF));
            }

        } finally {
            appedit.apply();
            prefs.edit().remove(PREFS_UPDATE_KEY).apply();
            themeedit.apply();
        }
    }


    public void saveUserColors() {

        SharedPreferences appprefs = prefs;
        SharedPreferences.Editor themeedit = ctx.getSharedPreferences("theme",Context.MODE_PRIVATE).edit();

        try {

            int max = COLOR_PREFS.length;
            for (String COLOR_PREF : COLOR_PREFS) {
                themeedit.putInt(getThemePrefName(COLOR_PREF), appprefs.getInt(COLOR_PREF, getCurrentThemeColor(COLOR_PREF)));
            }

        } finally {
            themeedit.apply();
        }
    }


    public boolean restoreUserColors() {
        Log.d("Theme", "restoreUserColors");
        SharedPreferences themeprefs = ctx.getSharedPreferences("theme",Context.MODE_PRIVATE);

        prefs.edit().putBoolean(PREFS_UPDATE_KEY, true).apply();

        SharedPreferences.Editor appedit = prefs.edit();

        try {

            int max = COLOR_PREFS.length;
            for (String COLOR_PREF : COLOR_PREFS) {
                appedit.putInt(COLOR_PREF, themeprefs.getInt(getThemePrefName(COLOR_PREF), getCurrentThemeColor(COLOR_PREF)));
            }
        } finally {
            appedit.apply();
            prefs.edit().remove(PREFS_UPDATE_KEY).apply();
        }
        return themeprefs.contains(getThemePrefName(COLOR_PREFS[0]));
    }



    public Map<String,String> getUserSetts() {

        Map<String,String> setts = new LinkedHashMap<>();

        for (Map.Entry<String,?> ent : prefs.getAll().entrySet()) {
            setts.put(ent.getKey(), ent.getValue()==null?"":ent.getValue().toString());
        }
        return setts;
    }




    abstract class BuiltinTheme {

        private final String mKey;
        private final String mName;

        private final Map<Thing,Integer> mColors = new HashMap<>();

        BuiltinTheme(String key, String name) {
            this(key, name, null);
        }

        BuiltinTheme(String key, String name, Map<Thing, Integer> colors) {
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

        public abstract Drawable getDrawable(AppLauncher app);

        boolean hasColors() {
            return mColors.size()>0;
        }

        BuiltinTheme setColor(Thing thing, int color) {
            mColors.put(thing, color);
            return this;
        }

        Integer getColor(Thing thing) {
            Integer val = mColors.get(thing);
            if (val == null) val = Color.BLACK;
            return val;
        }



        void applyTheme() {

            //SharedPreferences themeprefs = ctx.getSharedPreferences("theme",Context.MODE_PRIVATE);
            Log.d("Theme", "applyTheme");

            SharedPreferences appprefs = prefs;
            prefs.edit().putBoolean(PREFS_UPDATE_KEY, true).apply();

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
                prefs.edit().remove(PREFS_UPDATE_KEY).apply();
            }

        }

    }


    private class DefaultTheme extends BuiltinTheme {

        DefaultTheme(String key, String name) {
            super(key, name);
        }

//        public DefaultTheme(String key, String name, Map<Thing, Integer> colors) {
//            super(key, name, colors);
//        }
        @Override
        public Drawable getDrawable(AppLauncher app) {
            return iconsHandler.getDefaultAppDrawable(app);
        }

    }



    private class MonochromeTheme extends BuiltinTheme {
        MonochromeTheme(String key, String name) {
            super(key, name);
        }

//        public MonochromeTheme(String key, String name, Map<Thing, Integer> colors) {
//            super(key, name, colors);
//        }

        @Override
        public Drawable getDrawable(AppLauncher app) {


            //Log.d(TAG, "getDrawable called for " + componentName.getPackageName());

            Drawable app_icon = iconsHandler.getDefaultAppDrawable(app);


            int mask_color = prefs.getInt(ctx.getString(R.string.pref_key_icon_tint), getColor(Thing.Mask));

           // Log.d("iconi", mask_color + " mask");

            IconsHandler.applyIconTint(app_icon, mask_color);


            return app_icon;
        }


    }


//    public class PolychromeTheme extends BuiltinTheme {
//        private final int [] mFGColors;
//        private final int mBGColor;
//
//        public PolychromeTheme(String key, String name, int [] color, int bgcolor) {
//            super(key, name);
//            mFGColors = Arrays.copyOf(color, color.length);
//            mBGColor = bgcolor;
//        }
//
//        @Override
//        public Drawable getDrawable(AppLauncher app) {
//
//            //Log.d(TAG, "getDrawable called for " + componentName.getPackageName());
//
//            Drawable app_icon = iconsHandler.getDefaultAppDrawable(app);
//
//            app_icon = app_icon.mutate();
//
//
//            PorterDuff.Mode mode = PorterDuff.Mode.MULTIPLY;
//
//            int color = Math.abs(app.getComponentName().getPackageName().hashCode()) % mFGColors.length;
//            app_icon.setColorFilter(mFGColors[color], mode);
//
//            return app_icon;
//        }
//
//
//    }

}
