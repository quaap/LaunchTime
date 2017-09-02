package com.quaap.launchtime.components;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;

import com.quaap.launchtime.R;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by tom on 9/2/17.
 */

public class Theme {

    private Context ctx;


    private Map<String, BuiltinIconTheme> builtinThemes = new LinkedHashMap<>();

    private IconsHandler iconsHandler;

    public Theme(Context ctx, IconsHandler ich) {
        this.ctx = ctx;
        iconsHandler = ich;
        initBuiltinIconThemes();
    }



    private void initBuiltinIconThemes() {
        builtinThemes.put(IconsHandler.DEFAULT_PACK, new DefaultIconTheme(IconsHandler.DEFAULT_PACK, ctx.getString(R.string.icons_pack_default_name)));

        BuiltinIconTheme bw = new MonochromeIconTheme("bw", ctx.getString(R.string.theme_bw))
                .setColor(Thing.Mask, Color.WHITE)
                .setColor(Thing.Text, Color.WHITE)
                .setColor(Thing.AltText, Color.WHITE)
                .setColor(Thing.Background, Color.BLACK)
                .setColor(Thing.AltBackground, Color.parseColor("#ff222222"));

        builtinThemes.put(bw.getPackKey(), bw);


        BuiltinIconTheme termcap = new MonochromeIconTheme("termcap", ctx.getString(R.string.theme_termcap))
                .setColor(Thing.Mask, Color.parseColor("#dd22ff22"))
                .setColor(Thing.Text, Color.parseColor("#dd22ff22"))
                .setColor(Thing.AltText, Color.parseColor("#dd22ff22"))
                .setColor(Thing.Background, Color.BLACK)
                .setColor(Thing.AltBackground, Color.parseColor("#dd112211"));

        builtinThemes.put(termcap.getPackKey(), termcap);


        BuiltinIconTheme coolblue = new MonochromeIconTheme("coolblue", ctx.getString(R.string.theme_coolblue))
                .setColor(Thing.Mask, Color.parseColor("#ff1111ff"))
                .setColor(Thing.Text, Color.parseColor("#eeffffff"))
                .setColor(Thing.AltText, Color.parseColor("#eeffffff"))
                .setColor(Thing.Background, Color.parseColor("#88000077"))
                .setColor(Thing.AltBackground, Color.parseColor("#881111ff"));

        builtinThemes.put(coolblue.getPackKey(), coolblue);

        BuiltinIconTheme redplanet = new MonochromeIconTheme("redplanet", ctx.getString(R.string.theme_redplanet))
                .setColor(Thing.Mask, Color.parseColor("#ffff2222"))
                .setColor(Thing.Text, Color.parseColor("#eeff2222"))
                .setColor(Thing.AltText, Color.parseColor("#eeff2222"))
                .setColor(Thing.Background, Color.parseColor("#99aa1111"))
                .setColor(Thing.AltBackground, Color.parseColor("#22121111"));

        builtinThemes.put(redplanet.getPackKey(), redplanet);

        BuiltinIconTheme ladypink = new MonochromeIconTheme("ladypink", ctx.getString(R.string.theme_ladypink))
                .setColor(Thing.Mask, Color.parseColor("#ffff1493"))
                .setColor(Thing.Text, Color.parseColor("#eeffffff"))
                .setColor(Thing.AltText, Color.parseColor("#eeffc0cb"))
                .setColor(Thing.Background, Color.parseColor("#ffff69b4"))
                .setColor(Thing.AltBackground, Color.parseColor("#ffff1493"));

        builtinThemes.put(ladypink.getPackKey(), ladypink);
    }

    public  Map<String, BuiltinIconTheme> getBuiltinIconThemes() {
        return builtinThemes;
    }


    public boolean isBuiltinTheme(String packagename) {
        return builtinThemes.containsKey(packagename);
    }

    public BuiltinIconTheme getBuiltinTheme(String packagename) {
        return builtinThemes.get(packagename);
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
        BuiltinIconTheme theme = builtinThemes.get(iconsHandler.getIconsPackPackageName());
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


    public void saveUserColors() {

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


    public boolean restoreUserColors() {

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
        return themeprefs.contains(getThemePrefName(COLOR_PREFS[0]));
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
            return iconsHandler.getDefaultAppDrawable(componentName, uristr);
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

            Drawable app_icon = iconsHandler.getDefaultAppDrawable(componentName, uristr);

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

            Drawable app_icon = iconsHandler.getDefaultAppDrawable(componentName, uristr);

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
