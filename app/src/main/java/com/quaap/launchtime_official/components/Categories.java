package com.quaap.launchtime_official.components;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;

import com.quaap.launchtime_official.GlobState;
import com.quaap.launchtime_official.R;
import com.quaap.launchtime_official.db.DB;


import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Some portions modified from Silverfish:
 * Copyright 2016 Stanislav Pintjuk
 * E-mail: stanislav.pintjuk@gmail.com
 *
 *
 * Additional work Copyright (C) 2017   Tom Kliethermes
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
public class Categories {

    private static Resources resources;

    //Don't change these values here.  Change their displays in strings.xml
    public static final String CAT_SEARCH = "Search";
    public static final String CAT_TALK = "Communicate";
    public static final String CAT_GAMES = "Games";
    public static final String CAT_INTERNET = "Internet";
    public static final String CAT_MEDIA = "Media";
    public static final String CAT_GRAPHICS = "Graphics";
    public static final String CAT_Utilities = "Utilities";
    public static final String CAT_OTHER = "Other";
    public static final String CAT_SETTINGS = "Settings";
    public static final String CAT_HIDDEN = "Hidden";
    public static final String[] CAT_TINY = {CAT_OTHER, CAT_SETTINGS};
    public static final String[] CAT_HIDDENS = {CAT_HIDDEN};
    public static final String[] CAT_SPECIALS = {CAT_OTHER, CAT_TALK, CAT_HIDDEN, CAT_SEARCH};

    public static final String[] DefCategoryOrder = {
            CAT_TALK,
            CAT_SEARCH,
            CAT_GAMES,
            CAT_INTERNET,
            CAT_MEDIA,
            CAT_GRAPHICS,
            CAT_Utilities,
            CAT_SETTINGS,
            CAT_OTHER,
            CAT_HIDDEN
    };

    private static Map<String, String[]> mCategorKeywords;

    public static void init(Context context) {
        resources = context.getResources();

        mCategorKeywords = getCategoryKeywords();
    }

    public static String getCategoryForAction(Context context, String action) {
        String category = null;
        if (action.contains("CALL") || action.contains("SEND")
                || action.contains("DIAL") || action.contains("CONTACT")
                || action.contains("MAIL") || action.contains("MESSAG")) {
            category = CAT_TALK;
        } else if (action.contains("MUSIC")) {
            category = CAT_MEDIA;
        } else if (action.contains("WEB")) {
            category = CAT_INTERNET;
        }

        return checkCat(context, category);
    }

    public static String getCategoryForUri(Context context, String uri) {
        String category = null;
        if (uri.contains("sms") || uri.contains("call") || uri.contains("tel:") || uri.contains("contact")) {
            category = CAT_TALK;
        } else if (uri.contains("aud") || uri.contains("aud")) {
            category = CAT_MEDIA;
        } else if (uri.contains("http")) {
            category = CAT_INTERNET;
        }

        return checkCat(context, category);
    }


    public static String getCategoryForPackage(Context context, String pkgname, boolean guess) {
        DB db = GlobState.getGlobState(context).getDB();

        String category = db.getCategoryForPackage(pkgname);
        if (guess && (category == null || category.equals(CAT_OTHER))) {
            category = guessCategoryForPackage(context,pkgname);
        }
        return checkCat(context, category);
    }

    public static String getCategoryForActivity(Context context, String actvname, boolean guess) {
        DB db = GlobState.getGlobState(context).getDB();

        String category = db.getCategoryForActivity(actvname);
        if (guess && (category == null || category.equals(CAT_OTHER))) {
            category = guessCategoryForPackage(context,actvname);
        }
        return checkCat(context, category);
    }

    public static String getCategoryForComponent(Context context, ComponentName activity, boolean guess) {
        return getCategoryForComponent(context, activity.getClassName(), activity.getPackageName(), guess);
    }

    public static String getCategoryForComponent(Context context, String actvname, String pkgname, boolean guess) {

        String catact = getCategoryForActivity(context, actvname, false);
        String catpack = getCategoryForPackage(context, pkgname, false);

        String category = catact;
        if (category==null || category.equals(CAT_OTHER)) category = catpack;

        if (guess && (category == null || category.equals(CAT_OTHER))) {
            category = guessCategoryForPackage(context,pkgname);
        }


        return checkCat(context, category);

    }



    public static String guessCategoryForPackage(Context context, String pkgname) {
        String category = null;
        OUTER:
        for (String cat : mCategorKeywords.keySet()) {
            for (String pkg : mCategorKeywords.get(cat)) {
                if (pkgname.contains(pkg)) {
                    category = cat;
                    break OUTER;
                }
            }
        }

        return checkCat(context, category);
    }



    private static String checkCat(Context context, String category) {
        if (category == null) {
            category = CAT_OTHER;
        } else if (!isSpeacialCategory(category)){
            DB db = GlobState.getGlobState(context).getDB();
            String dbcat = db.getCategoryDisplay(category);
            if (dbcat == null) {
                category = Categories.CAT_OTHER;  //the user deleted the category
            }
        }

        return category;
    }

    public static boolean isTinyCategory(String category) {
        return Arrays.asList(Categories.CAT_TINY).contains(category);
    }

    public static boolean isHiddenCategory(String category) {
        return Arrays.asList(Categories.CAT_HIDDENS).contains(category);
    }

    public static boolean isSpeacialCategory(String category) {
        return Arrays.asList(Categories.CAT_SPECIALS).contains(category);
    }

    public static String getCatLabel(Context context, String category) {
        Map<String, Integer> catmap = new HashMap<>();
        catmap.put(CAT_SEARCH, R.string.category_Search);
        catmap.put(CAT_TALK, R.string.category_Talk);
        catmap.put(CAT_GAMES, R.string.category_Games);
        catmap.put(CAT_INTERNET, R.string.category_Internet);
        catmap.put(CAT_MEDIA, R.string.category_Media);
        catmap.put(CAT_GRAPHICS, R.string.category_Graphics);
        catmap.put(CAT_Utilities, R.string.category_Utilities);
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
        catmap.put(CAT_Utilities, R.string.category_Utilities_full);
        catmap.put(CAT_OTHER, R.string.category_Other_full);
        catmap.put(CAT_SETTINGS, R.string.category_Settings_full);
        catmap.put(CAT_HIDDEN, R.string.category_Hidden_full);
        return context.getString(catmap.get(category));
    }

    public static Map<String, String[]> getCategoryKeywords() {
        Map<String, String[]> keywordsDict = new LinkedHashMap<>();
        
        keywordsDict.put(CAT_TALK, resources.getStringArray(R.array.CAT_TALK));
        keywordsDict.put(CAT_GAMES, resources.getStringArray(R.array.CAT_GAMES));
        keywordsDict.put(CAT_INTERNET, resources.getStringArray(R.array.CAT_INTERNET));
        keywordsDict.put(CAT_MEDIA, resources.getStringArray(R.array.CAT_MEDIA));
        keywordsDict.put(CAT_GRAPHICS, resources.getStringArray(R.array.CAT_GRAPHICS));
        keywordsDict.put(CAT_Utilities, resources.getStringArray(R.array.CAT_ACCESSORIES));
        keywordsDict.put(CAT_SETTINGS, resources.getStringArray(R.array.CAT_SETTINGS));


        return keywordsDict;
    }


}
