package com.quaap.launchtime.ui;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.view.animation.CycleInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
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

    private int cattabBackgroundHighContrast;
    private int cattabSelectedBackgroundHighContrast;


    private int launcherIconSize = 55;
    private int launcherSize = 80;
    private int launcherFontSize = 12;

    private int aniDuration;

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

    public void styleCategoryStyle(final TextView categoryTab, CategoryTabStyle catstyle, boolean highContrast) {

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)categoryTab.getLayoutParams();

//        ShapeDrawable back = new ShapeDrawable(new RoundRectShape(new float[]{20,20,20,20,20,20,20,20}, null, null));
//        categoryTab.setBackground(back);
        //lp.setMargins(2, 4, 2, 3);

        boolean isOnLeft = isLeftHandCategories();

        int padRight = 2;
        int padLeft = 6;

        if (isOnLeft) {
            padRight = 6;
            padLeft = 2;
        }
        lp.leftMargin = 2;
        lp.rightMargin = 2;
        switch (catstyle) {
            case Tiny:
                categoryTab.setPadding(padLeft, categoryTabPaddingHeight/5, padRight, categoryTabPaddingHeight/5);
                categoryTab.setTextColor(cattabTextColor);
                categoryTab.setBackgroundColor(highContrast?cattabBackgroundHighContrast:cattabBackground);
                categoryTab.setTextSize(categoryTabFontSize-3);
                categoryTab.setShadowLayer(0, 0, 0, 0);
                if (isOnLeft) {
                    lp.rightMargin = 22;
                } else {
                    lp.leftMargin = 22;
                }
                break;
            case DragHover:
                categoryTab.setPadding(padLeft, categoryTabPaddingHeight, padRight, categoryTabPaddingHeight);
                categoryTab.setTextColor(cattabTextColor);
                categoryTab.setBackgroundColor(dragoverBackground);
                categoryTab.setTextSize(categoryTabFontSize + 1);
                categoryTab.setTextSize(categoryTabFontSize);
                categoryTab.setShadowLayer(0, 0, 0, 0);
                if (isOnLeft) {
                    lp.rightMargin = 6;
                } else {
                    lp.leftMargin = 6;
                }
                break;
            case Selected:
                categoryTab.setPadding(padLeft, categoryTabPaddingHeight+2, padRight, categoryTabPaddingHeight+2);
                categoryTab.setTextColor(cattabSelectedText);
                categoryTab.setBackgroundColor(highContrast?cattabSelectedBackgroundHighContrast:cattabSelectedBackground);
                categoryTab.setTextSize(categoryTabFontSize + 1);
                categoryTab.setShadowLayer(8, 4, 4, cattabTextColorInvert);

                if (aniDuration>0) {
                    categoryTab.animate().scaleX(1.3f).scaleY(1.3f)
                            .setInterpolator(new CycleInterpolator(1))
                            .setDuration(aniDuration)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    categoryTab.animate().scaleX(1).scaleY(1).setStartDelay(50)
                                            .setDuration(aniDuration)
                                            .setInterpolator(new LinearInterpolator());
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {
                                    super.onAnimationCancel(animation);
                                    categoryTab.animate().scaleX(1).scaleY(1).setStartDelay(50)
                                            .setDuration(aniDuration)
                                            .setInterpolator(new LinearInterpolator());
                                }
                            }).start();
                }
                lp.leftMargin = 1;
                lp.rightMargin = 1;
                break;
            case Normal:
            default:
                categoryTab.setPadding(padLeft, categoryTabPaddingHeight, padRight, categoryTabPaddingHeight);
                categoryTab.setTextColor(cattabTextColor);
                categoryTab.setBackgroundColor(highContrast?cattabBackgroundHighContrast:cattabBackground);
                categoryTab.setTextSize(categoryTabFontSize);
                categoryTab.setShadowLayer(0, 0, 0, 0);
                if (isOnLeft) {
                    lp.rightMargin = 22;
                } else {
                    lp.leftMargin = 22;
                }
        }

        if (highContrast) {
            if (categoryTab.getAlpha()<.89f) {
                categoryTab.setAlpha(.9f);
            }
            lp.topMargin=2;
            lp.bottomMargin=2;
        } else {
            lp.topMargin=4;
            lp.bottomMargin=3;
        }

        categoryTab.setLayoutParams(lp);
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

        aniDuration = Integer.parseInt(mAppPreferences.getString("pref_animate_duration", "250"));


        float iconsize = mContext.getResources().getDimension(R.dimen.icon_width);
        float iconfontsize = mContext.getResources().getDimension(R.dimen.launcher_fontsize);


        int iconsizePref = Integer.parseInt(mAppPreferences.getString("preference_iconsize", "1"));
        switch (iconsizePref) {
            case 0:  //small
                launcherIconSize = (int)(iconsize*.74);
                launcherFontSize = (int)(iconfontsize*.87);
                launcherSize = (int)(launcherIconSize*1.3);
                break;
            case 1:  //medium
                launcherIconSize = (int)(iconsize*.95);
                launcherFontSize = (int)iconfontsize;
                launcherSize = (int)(launcherIconSize*1.32);
                break;
            case 2:  //large
                launcherIconSize = (int)(iconsize*1.3);
                launcherFontSize = (int)(iconfontsize*1.3);
                launcherSize = (int)(launcherIconSize*1.33);
                break;
            case 3: //x-large
                launcherIconSize = (int)(iconsize*1.8);
                launcherFontSize = (int)(iconfontsize*1.7);
                launcherSize = (int)(launcherIconSize*1.3);
                break;
        }
        //Log.d("style", "launcherFontSize = " + launcherFontSize);

        cattabBackground = mAppPreferences.getInt("cattab_background", getResColor(R.color.cattab_background));
        cattabSelectedBackground = mAppPreferences.getInt("cattabselected_background", getResColor(R.color.cattabselected_background));
        cattabSelectedText = mAppPreferences.getInt("cattabselected_text", getResColor(R.color.cattabselected_text));

        dragoverBackground = mAppPreferences.getInt("dragover_background", getResColor(R.color.dragover_background));

        cattabTextColor =  mAppPreferences.getInt("cattabtextcolor", getResColor(R.color.textcolor));
        cattabTextColorInvert = mAppPreferences.getInt("cattabtextcolorinv", getResColor(R.color.textcolorinv));

        textColor = mAppPreferences.getInt("textcolor", getResColor(R.color.textcolor));

        wallpaperColor = mAppPreferences.getInt("wallpapercolor", getResColor(R.color.wallpaper_color));

        iconTint = mAppPreferences.getInt("icon_tint", Color.TRANSPARENT);

        cattabBackgroundHighContrast = cattabBackground;

        int alpha = Color.alpha(cattabBackground);
        if (alpha<220) alpha=220;
        cattabBackgroundHighContrast = Color.argb(alpha, Color.red(cattabBackground), Color.green(cattabBackground), Color.blue(cattabBackground));

        alpha = Color.alpha(cattabSelectedBackground);
        if (alpha<220) alpha=220;
        cattabSelectedBackgroundHighContrast = Color.argb(alpha, Color.red(cattabSelectedBackground), Color.green(cattabSelectedBackground), Color.blue(cattabSelectedBackground));



    }


    public int getMaxWCells() {
        int iconsizePref = Integer.parseInt(mAppPreferences.getString("preference_iconsize", "1"));

        return 6-iconsizePref;

    }

    public float getWidgetWidth(float minPixWidth) {

        return minPixWidth * getRatio();
    }

    public float getRatio() {
        float iconsize = mContext.getResources().getDimension(R.dimen.icon_width);

        return iconsize / launcherIconSize;
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
