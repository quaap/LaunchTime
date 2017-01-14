package com.quaap.launchtime.components;

import android.content.Context;

import com.quaap.launchtime.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by tom on 1/9/17.
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
public class Categories {


    //Don't change these values here.  Change them in strings.xml
    public static final String CAT_SEARCH = "Search";
    public static final String CAT_TALK = "Communicate";
    public static final String CAT_GAMES = "Games";
    public static final String CAT_INTERNET = "Internet";
    public static final String CAT_MEDIA = "Media";
    public static final String CAT_GRAPHICS = "Graphics";
    public static final String CAT_ACCESSORIES = "Accessories";
    public static final String CAT_OTHER = "Other";
    public static final String CAT_SETTINGS = "Settings";
    public static final String CAT_HIDDEN = "Hidden";
    public static final String[] CAT_TINY = {CAT_OTHER, CAT_SETTINGS, CAT_HIDDEN};
    public static final String[] DefCategoryOrder = {
            CAT_SEARCH,
            CAT_TALK,
            CAT_GAMES,
            CAT_INTERNET,
            CAT_MEDIA,
            CAT_GRAPHICS,
            CAT_ACCESSORIES,
            CAT_SETTINGS,
            CAT_OTHER,
            CAT_HIDDEN
    };
    private static Map<String, String> mPrefCategories;
    private static Map<String, String[]> mCategorKeywords;

    public static void init(Context context) {
        mPrefCategories = getPredefinedCategories(context);
        mCategorKeywords = getCategoryKeywords(context);
    }

    public static String getCategoryForPackage(String pkgname) {

        String category = mPrefCategories.get(pkgname);
        if (category == null) {
            OUTER:
            for (String cat : mCategorKeywords.keySet()) {
                for (String pkg : mCategorKeywords.get(cat)) {
                    if (pkgname.contains(pkg)) {
                        category = cat;
                        break OUTER;
                    }
                }
            }
        }
        if (category == null) {
            category = CAT_OTHER;
        }

        return category;
    }

    public static boolean isTinyCategory(String category) {
        return Arrays.asList(Categories.CAT_TINY).contains(category);
    }

    public static boolean isSpeacialCategory(String category) {
        return Arrays.asList(CAT_OTHER, CAT_SETTINGS, CAT_HIDDEN, CAT_SEARCH).contains(category);
    }

    public static String getCatLabel(Context context, String category) {
        Map<String, Integer> catmap = new HashMap<>();
        catmap.put(CAT_SEARCH, R.string.category_Search);
        catmap.put(CAT_TALK, R.string.category_Talk);
        catmap.put(CAT_GAMES, R.string.category_Games);
        catmap.put(CAT_INTERNET, R.string.category_Internet);
        catmap.put(CAT_MEDIA, R.string.category_Media);
        catmap.put(CAT_GRAPHICS, R.string.category_Graphics);
        catmap.put(CAT_ACCESSORIES, R.string.category_Accessories);
        catmap.put(CAT_OTHER, R.string.category_Other);
        catmap.put(CAT_SETTINGS, R.string.category_Settings);
        catmap.put(CAT_HIDDEN, R.string.category_Hidden);
        return context.getString(catmap.get(category));
    }

    public static String getCatFullLabel(Context context, String category) {
        Map<String, Integer> catmap = new HashMap<>();
        catmap.put(CAT_SEARCH, R.string.category_Search_full);
        catmap.put(CAT_TALK, R.string.category_Talk_full);
        catmap.put(CAT_GAMES, R.string.category_Games_full);
        catmap.put(CAT_INTERNET, R.string.category_Internet_full);
        catmap.put(CAT_MEDIA, R.string.category_Media_full);
        catmap.put(CAT_GRAPHICS, R.string.category_Graphics_full);
        catmap.put(CAT_ACCESSORIES, R.string.category_Accessories_full);
        catmap.put(CAT_OTHER, R.string.category_Other_full);
        catmap.put(CAT_SETTINGS, R.string.category_Settings_full);
        catmap.put(CAT_HIDDEN, R.string.category_Hidden_full);
        return context.getString(catmap.get(category));
    }

    public static Map<String, String[]> getCategoryKeywords(Context ctx) {
        Map<String, String[]> keywordsDict = new LinkedHashMap<>();

        keywordsDict.put(CAT_TALK, new String[]{"phone", "conv", "call", "sms", "mms", "contacts", "stk", "mail", "twitter", "whatsapp", "outlook", "talk", "facebook", "social", "chat"});  // stk stands for "SIM Toolkit"
        keywordsDict.put(CAT_GAMES, new String[]{"game", "play", "puzz", "com.ea", "com.king", "com.halfbrick"});
        keywordsDict.put(CAT_INTERNET, new String[]{"download", "vending", "browser", "maps", "dropbox", "chrome", "drive"});
        keywordsDict.put(CAT_MEDIA, new String[]{"radio", "voice", "audio", "speech", "pod", "music", "sound", "mp3", "record", "sfx", "mic"});
        keywordsDict.put(CAT_GRAPHICS, new String[]{"pic", "pix", "gallery", "photo", "foto", "cam", "tube", "vid", "video", "draw", "graph", "gfx", "image", "img", "svg", "png"});
        keywordsDict.put(CAT_ACCESSORIES, new String[]{"editor", "calc", "calendar", "organize", "clock", "time", "viewer", "file", "manager", "memo", "note"});
        keywordsDict.put(CAT_SETTINGS, new String[]{"setting", "config", "keyboard", "launch", "sync", "backup", "prefer", "prefs"});


        return keywordsDict;
    }

    public static Map<String, String> getPredefinedCategories(Context ctx) {
        Map<String, String> predefCategories = new HashMap<>();

        InputStream inputStream = ctx.getResources().openRawResource(R.raw.package_category);
        String line;
        String[] lineSplit;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    lineSplit = line.split("=");
                    predefCategories.put(lineSplit[0], lineSplit[1]);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return predefCategories;
    }
}
