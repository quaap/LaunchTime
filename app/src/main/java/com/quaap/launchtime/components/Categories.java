package com.quaap.launchtime.components;

import android.content.Context;

import com.quaap.launchtime.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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


    private static Map<String, String> mPrefCategories;
    private static Map<String, String[]> mCategorKeywords;

    public static void init(Context context) {
        mPrefCategories = getPredefinedCategories(context);
        mCategorKeywords = getCategoryKeywords(context);
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


    public static final String CAT_SETTINGS = "Settings";
    public static final String CAT_HIDDEN = "Hidden";

    public static final String [] DefCategoryOrder = {
            "Phone",
            "Games",
            "Internet",
            "Media",
            "Graphics",
            "Accessories",
            "Other",
            CAT_SETTINGS,
            CAT_HIDDEN
    };

    public static Map<String, String[]> getCategoryKeywords(Context ctx)
    {
        Map<String, String[]> keywordsDict = new LinkedHashMap<>();

        keywordsDict.put("Phone", new String[]{"phone", "conv", "call", "sms", "mms", "contacts", "stk"});  // stk stands for "SIM Toolkit"
        keywordsDict.put("Games", new String[]{"game", "play"});
        keywordsDict.put("Internet", new String[]{"download", "mail", "vending", "browser", "maps", "twitter", "whatsapp", "outlook", "dropbox", "chrome", "drive"});
        keywordsDict.put("Media", new String[]{"radio", "voice", "audio", "speech", "pod", "music", "sound", "mp3", "record", "sfx", "mic"});
        keywordsDict.put("Graphics", new String[]{"pic", "pix", "gallery", "photo", "foto", "cam", "tube", "tv", "video", "draw", "graph", "gfx", "image", "img", "svg", "png"});
        keywordsDict.put("Accessories", new String[]{"editor", "calc", "calendar", "organize", "clock", "time", "viewer", "file", "manager", "memo", "note"});
        keywordsDict.put(CAT_SETTINGS, new String[]{"settings", "config", "keyboard", "launcher", "sync", "backup"});


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
