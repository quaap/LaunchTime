package com.quaap.launchtime.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.widget.TextView;

import com.quaap.launchtime.MainActivity;
import com.quaap.launchtime.R;

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

public class Style {
    private int cattabTextColor;
    private int cattabTextColorInvert;
    private int cattabBackground;
    private int cattabSelectedBackground;


    private int cattabSelectedText;
    private int dragoverBackground;
    private int textColor;
    private int backgroundDefault = Color.TRANSPARENT;
    private boolean leftHandCategories;
    private float categoryTabFontSize = 16;
    private int categoryTabPaddingHeight = 16;

    private int launcherIconSize = 55;
    private int launcherSize = 80;
    private int launcherFontSize = 12;

    private SharedPreferences mAppPreferences;

    private Context mContext;

    public Style(Context context, SharedPreferences appPreferences) {

        mContext = context;
        mAppPreferences = appPreferences;
        readPrefs();

    }

    public enum CategoryTabStyle {Default, Normal, Selected, DragHover, Tiny}

    public void styleCategoryStyle(TextView categoryTab, CategoryTabStyle catstyle) {


        switch (catstyle) {
            case Tiny:
                categoryTab.setPadding(6, categoryTabPaddingHeight/6, 2, categoryTabPaddingHeight/6);
                categoryTab.setTextColor(cattabTextColor);
                categoryTab.setBackgroundColor(cattabBackground);
                categoryTab.setTextSize(categoryTabFontSize-3);
                categoryTab.setShadowLayer(0, 0, 0, 0);
                break;
            case DragHover:
                categoryTab.setPadding(6, categoryTabPaddingHeight, 2, categoryTabPaddingHeight);
                categoryTab.setTextColor(cattabTextColor);
                categoryTab.setBackgroundColor(dragoverBackground);
                categoryTab.setTextSize(categoryTabFontSize);
                categoryTab.setShadowLayer(0, 0, 0, 0);
                break;
            case Selected:
                categoryTab.setPadding(6, categoryTabPaddingHeight, 2, categoryTabPaddingHeight);
                categoryTab.setTextColor(cattabSelectedText);
                categoryTab.setBackgroundColor(cattabSelectedBackground);
                categoryTab.setTextSize(categoryTabFontSize);
                categoryTab.setShadowLayer(8, 4, 4, cattabTextColorInvert);
                break;
            case Normal:
            default:
                categoryTab.setPadding(6, categoryTabPaddingHeight, 2, categoryTabPaddingHeight);
                categoryTab.setTextColor(cattabTextColor);
                categoryTab.setBackgroundColor(cattabBackground);
                categoryTab.setTextSize(categoryTabFontSize);
                categoryTab.setShadowLayer(0, 0, 0, 0);
        }
    }

    public int getLauncherIconSize() {
        return launcherIconSize;
    }

    public int getLauncherSize() {
        return launcherSize;
    }

    public int getLauncherFontSize() {
        return launcherFontSize;
    }

    public void readPrefs() {
        //Checks application preferences and adjust accordingly

        leftHandCategories = mAppPreferences.getString("pref_categories_loc", "right").equals("left");

        int tabsizePref = Integer.parseInt(mAppPreferences.getString("preference_tabsize", "1"));
        switch (tabsizePref) {
            case 0:  //small
                categoryTabPaddingHeight = 12;
                categoryTabFontSize = 14;
                break;
            case 1:  //medium
                categoryTabPaddingHeight = 16;
                categoryTabFontSize = 16;
                break;
            case 2:  //large
                categoryTabPaddingHeight = 20;
                categoryTabFontSize = 18;
                break;
            case 3: //x-large
                categoryTabPaddingHeight = 24;
                categoryTabFontSize = 20;
                break;
        }


        int iconsizePref = Integer.parseInt(mAppPreferences.getString("preference_iconsize", "1"));
        switch (iconsizePref) {
            case 0:  //small
                launcherIconSize = 40;
                launcherFontSize = 11;
                break;
            case 1:  //medium
                launcherIconSize = 55;
                launcherFontSize = 13;
                break;
            case 2:  //large
                launcherIconSize = 70;
                launcherFontSize = 14;
                break;
            case 3: //x-large
                launcherIconSize = 90;
                launcherFontSize = 15;
                break;
        }
        launcherSize = (int)(launcherIconSize*1.3);

        cattabBackground = mAppPreferences.getInt("cattab_background", getResColor(R.color.cattab_background));
        cattabSelectedBackground = mAppPreferences.getInt("cattabselected_background", getResColor(R.color.cattabselected_background));
        cattabSelectedText = mAppPreferences.getInt("cattabselected_text", getResColor(R.color.cattabselected_text));

        dragoverBackground = mAppPreferences.getInt("dragover_background", getResColor(R.color.dragover_background));

        cattabTextColor =  mAppPreferences.getInt("cattabtextcolor", getResColor(R.color.textcolor));
        cattabTextColorInvert = mAppPreferences.getInt("cattabtextcolorinv", getResColor(R.color.textcolorinv));

        textColor = mAppPreferences.getInt("textcolor", getResColor(R.color.textcolor));
    }

    private int getResColor(int res) {
        if (Build.VERSION.SDK_INT >= 23) {
            return mContext.getColor(res);
        } else {
            return mContext.getResources().getColor(res);
        }
    }
    public int getCattabTextColor() {
        return cattabTextColor;
    }

    public int getCattabTextColorInvert() {
        return cattabTextColorInvert;
    }

    public int getCattabBackground() {
        return cattabBackground;
    }

    public int getCattabSelectedBackground() {
        return cattabSelectedBackground;
    }

    public int getCattabSelectedText() {
        return cattabSelectedText;
    }

    public int getDragoverBackground() {
        return dragoverBackground;
    }

    public int getTextColor() {
        return textColor;
    }

    public int getBackgroundDefault() {
        return backgroundDefault;
    }

    public boolean isLeftHandCategories() {
        return leftHandCategories;
    }

    public float getCategoryTabFontSize() {
        return categoryTabFontSize;
    }

    public int getCategoryTabPaddingHeight() {
        return categoryTabPaddingHeight;
    }


}
