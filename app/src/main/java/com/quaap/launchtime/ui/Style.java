package com.quaap.launchtime.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.widget.TextView;

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

    private int wallpaperColor = Color.TRANSPARENT;

    private int iconTint = Color.TRANSPARENT;

    private boolean leftHandCategories;
    private float categoryTabFontSize = 16;
    private int categoryTabPaddingHeight = 25;

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

    public int getWallpaperColor() {
        return wallpaperColor;
    }


    public enum CategoryTabStyle {Default, Normal, Selected, DragHover, Tiny}

    public void styleCategoryStyle(TextView categoryTab, CategoryTabStyle catstyle) {


        switch (catstyle) {
            case Tiny:
                categoryTab.setPadding(6, categoryTabPaddingHeight/5, 2, categoryTabPaddingHeight/5);
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
                categoryTab.setPadding(6, categoryTabPaddingHeight+2, 2, categoryTabPaddingHeight+2);
                categoryTab.setTextColor(cattabSelectedText);
                categoryTab.setBackgroundColor(cattabSelectedBackground);
                categoryTab.setTextSize(categoryTabFontSize + 1);
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
                categoryTabPaddingHeight = 16;
                categoryTabFontSize = 14;
                break;
            case 1:  //medium
                categoryTabPaddingHeight = 20;
                categoryTabFontSize = 16;
                break;
            case 2:  //large
                categoryTabPaddingHeight = 25;
                categoryTabFontSize = 18;
                break;
            case 3: //x-large
                categoryTabPaddingHeight = 30;
                categoryTabFontSize = 20;
                break;
        }


        float iconsize = mContext.getResources().getDimension(R.dimen.icon_width);
        float iconfontsize = mContext.getResources().getDimension(R.dimen.launcher_fontsize);


        int iconsizePref = Integer.parseInt(mAppPreferences.getString("preference_iconsize", "1"));
        switch (iconsizePref) {
            case 0:  //small
                launcherIconSize = (int)(iconsize*.74);
                launcherFontSize = (int)(iconfontsize*.87);
                break;
            case 1:  //medium
                launcherIconSize = (int)(iconsize*.95);
                launcherFontSize = (int)iconfontsize;
                break;
            case 2:  //large
                launcherIconSize = (int)(iconsize*1.3);
                launcherFontSize = (int)(iconfontsize*1.3);
                break;
            case 3: //x-large
                launcherIconSize = (int)(iconsize*1.8);
                launcherFontSize = (int)(iconfontsize*1.7);
                break;
        }
        //Log.d("style", "launcherFontSize = " + launcherFontSize);
        launcherSize = (int)(launcherIconSize*1.3);

        cattabBackground = mAppPreferences.getInt("cattab_background", getResColor(R.color.cattab_background));
        cattabSelectedBackground = mAppPreferences.getInt("cattabselected_background", getResColor(R.color.cattabselected_background));
        cattabSelectedText = mAppPreferences.getInt("cattabselected_text", getResColor(R.color.cattabselected_text));

        dragoverBackground = mAppPreferences.getInt("dragover_background", getResColor(R.color.dragover_background));

        cattabTextColor =  mAppPreferences.getInt("cattabtextcolor", getResColor(R.color.textcolor));
        cattabTextColorInvert = mAppPreferences.getInt("cattabtextcolorinv", getResColor(R.color.textcolorinv));

        textColor = mAppPreferences.getInt("textcolor", getResColor(R.color.textcolor));

        wallpaperColor = mAppPreferences.getInt("wallpapercolor", getResColor(R.color.wallpaper_color));

        iconTint = mAppPreferences.getInt("icon_tint", Color.TRANSPARENT);
    }


    public int getMaxWCells() {
        int iconsizePref = Integer.parseInt(mAppPreferences.getString("preference_iconsize", "1"));

        return 6-iconsizePref;

    }

    public float getWidgetWidth(float minPixWidth) {
        float iconsize = mContext.getResources().getDimension(R.dimen.icon_width);

        return launcherIconSize * (minPixWidth/iconsize);
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

    public int getIconTint() {
        return iconTint;
    }
}
