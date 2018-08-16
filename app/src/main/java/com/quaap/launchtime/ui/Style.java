package com.quaap.launchtime.ui;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.quaap.launchtime.R;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

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

    private final int backgroundDefault = Color.TRANSPARENT;

    private int wallpaperColor = Color.TRANSPARENT;
    private int calculatedWallpaperColor = Color.TRANSPARENT;


    private int iconTint = Color.TRANSPARENT;

    private boolean leftHandCategories;
    private float categoryTabFontSize = 16;
    private int categoryTabPaddingHeight = 25;

    private int cattabBackgroundHighContrast;
    private int cattabSelectedBackgroundHighContrast;

    private boolean centeredIcons;

    private int launcherIconSize = 55;
    private int launcherSize = 80;
    private int launcherFontSize = 12;

    private int mAnimationDuration;

    private final SharedPreferences mAppPreferences;

    private final Context mContext;

    public Style(Context context, SharedPreferences appPreferences) {

        mContext = context;
        mAppPreferences = appPreferences;
        readPrefs();

    }

    public int getWallpaperColor() {
        return wallpaperColor;
    }


    public enum CategoryTabStyle {Default, Normal, Selected, DragHover, Tiny, Hidden, None}

    public void styleCategoryStyle(final TextView categoryTab, CategoryTabStyle catstyle, boolean highContrast) {

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)categoryTab.getLayoutParams();

        boolean isOnLeft = isLeftHandCategories();

        int padRight = 2;
        int padLeft = 6;

        if (isOnLeft) {
            padRight = 6;
            padLeft = 2;
        }
        lp.leftMargin = 2;
        lp.rightMargin = 2;


        Rect pad  = new Rect();

        int bgcolor = 0;
        categoryTab.setBackground(null);
        categoryTab.setShadowLayer(0, 0, 0, 0);

        switch (catstyle) {
            case Hidden:
                bgcolor = cattabBackground;
                if (isOnLeft) {
                    lp.rightMargin = 36;
                } else {
                    lp.leftMargin = 36;
                }

            case Tiny:
                categoryTab.setTextColor(cattabTextColor);

                if (bgcolor==0) {
                    bgcolor = highContrast ? cattabBackgroundHighContrast : cattabBackground;
                    if (isOnLeft) {
                        lp.rightMargin = 22;
                    } else {
                        lp.leftMargin = 22;
                    }
                }
                pad.set(padLeft, categoryTabPaddingHeight/5, padRight, categoryTabPaddingHeight/5);
                categoryTab.setTextSize(categoryTabFontSize-3);

                break;
            case DragHover:
                categoryTab.setTextColor(cattabTextColor);

                bgcolor = dragoverBackground;

                pad.set(padLeft, categoryTabPaddingHeight, padRight, categoryTabPaddingHeight);
                categoryTab.setTextSize(categoryTabFontSize + 1);
                categoryTab.setTextSize(categoryTabFontSize);
                if (isOnLeft) {
                    lp.rightMargin = 6;
                } else {
                    lp.leftMargin = 6;
                }
                break;
            case Selected:
                categoryTab.setTextColor(cattabSelectedText);

                bgcolor = highContrast?cattabSelectedBackgroundHighContrast:cattabSelectedBackground;

                pad.set(padLeft, categoryTabPaddingHeight+2, padRight, categoryTabPaddingHeight+2);
                categoryTab.setTextSize(categoryTabFontSize + 1);
                categoryTab.setShadowLayer(8, 4, 4, cattabTextColorInvert);

                categoryTab.clearAnimation();
                if (mAnimationDuration >0) {
                    categoryTab.animate().scaleX(1.3f).scaleY(1.3f)
                            .setInterpolator(new CycleInterpolator(1))
                            .setDuration(mAnimationDuration)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    categoryTab.setScaleX(1);
                                    categoryTab.setScaleY(1);
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {
                                    categoryTab.setScaleX(1);
                                    categoryTab.setScaleY(1);
                                }
                            }).start();
                } else {
                    categoryTab.setScaleX(1);
                    categoryTab.setScaleY(1);
                }
                lp.leftMargin = 1;
                lp.rightMargin = 1;
                break;

            case Normal:
            default:
                categoryTab.setTextColor(cattabTextColor);

                bgcolor = highContrast?cattabBackgroundHighContrast:cattabBackground;
                pad.set(padLeft, categoryTabPaddingHeight, padRight, categoryTabPaddingHeight);
                categoryTab.setTextSize(categoryTabFontSize);
                if (isOnLeft) {
                    lp.rightMargin = 22;
                } else {
                    lp.leftMargin = 22;
                }
        }

        categoryTab.setBackgroundColor(bgcolor);
        if (isRoundedTabs()) {
            categoryTab.setBackground(getBgDrawableFor(categoryTab,catstyle,highContrast));
        }
        categoryTab.setPadding(pad.left, pad.top, pad.right, pad.bottom);

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

        leftHandCategories = mAppPreferences.getString(mContext.getString(R.string.pref_key_categories_loc), "right").equals("left");

        centeredIcons  = mAppPreferences.getBoolean(mContext.getString(R.string.pref_key_center_sheet), true);

        float density = mContext.getResources().getDisplayMetrics().density / 2.0f;

        if (density<.75f) density = .75f;
        if (density>1.5f) density = 1.5f;

        int tabsizePref = Integer.parseInt(mAppPreferences.getString(mContext.getString(R.string.pref_key_tabsize), "1"));
        switch (tabsizePref) {
            case 0:  //small
                categoryTabPaddingHeight = (int)(16 * density);
                categoryTabFontSize = 14;
                break;
            case 1:  //medium
                categoryTabPaddingHeight = (int)(20 * density);
                categoryTabFontSize = 16;
                break;
            case 2:  //large
                categoryTabPaddingHeight = (int)(25 * density);
                categoryTabFontSize = 18;
                break;
            case 3: //x-large
                categoryTabPaddingHeight = (int)(25 * density);
                categoryTabFontSize = 20;
                break;
        }

        mAnimationDuration = Integer.parseInt(mAppPreferences.getString(mContext.getString(R.string.pref_key_animate_duration), "150"));


        float iconsize = mContext.getResources().getDimension(R.dimen.icon_width);
        float iconfontsize = mContext.getResources().getDimension(R.dimen.launcher_fontsize);


        int iconsizePref = Integer.parseInt(mAppPreferences.getString(mContext.getString(R.string.pref_key_iconsize), "1"));
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

        cattabBackground = mAppPreferences.getInt(mContext.getString(R.string.pref_key_cattab_background), getResColor(R.color.cattab_background));
        cattabSelectedBackground = mAppPreferences.getInt(mContext.getString(R.string.pref_key_cattabselected_background), getResColor(R.color.cattabselected_background));
        cattabSelectedText = mAppPreferences.getInt(mContext.getString(R.string.pref_key_cattabselected_text), getResColor(R.color.cattabselected_text));

        dragoverBackground = mAppPreferences.getInt(mContext.getString(R.string.pref_key_dragover_background), getResColor(R.color.dragover_background));

        cattabTextColor =  mAppPreferences.getInt(mContext.getString(R.string.pref_key_cattabtextcolor), getResColor(R.color.textcolor));
        cattabTextColorInvert = mAppPreferences.getInt(mContext.getString(R.string.pref_key_cattabtextcolorinv), getResColor(R.color.textcolorinv));

        textColor = mAppPreferences.getInt(mContext.getString(R.string.pref_key_textcolor), getResColor(R.color.textcolor));

        wallpaperColor = mAppPreferences.getInt(mContext.getString(R.string.pref_key_wallpapercolor), getResColor(R.color.wallpaper_color));

        iconTint = mAppPreferences.getInt(mContext.getString(R.string.pref_key_icon_tint), Color.TRANSPARENT);

        cattabBackgroundHighContrast = cattabBackground;

        int alpha; // = Color.alpha(cattabBackground);
        //if (alpha<80) alpha=80;
       // cattabBackgroundHighContrast = Color.argb(alpha, Color.red(cattabBackground), Color.green(cattabBackground), Color.blue(cattabBackground));

        alpha = Color.alpha(cattabBackground);
        if (alpha<80) {
            alpha=80;
            cattabBackgroundHighContrast = Color.argb(alpha, Color.red(cattabBackground)*5/6, Color.green(cattabBackground)*5/6, Color.blue(cattabBackground)*5/6);
        } else {
            cattabBackgroundHighContrast = cattabBackground;
        }

        alpha = Color.alpha(cattabSelectedBackground);
        if (alpha<80) {
            alpha=80;
            cattabSelectedBackgroundHighContrast = Color.argb(alpha, Color.red(cattabSelectedBackground)*5/6, Color.green(cattabSelectedBackground)*5/6, Color.blue(cattabSelectedBackground)*5/6);
        } else {
            cattabSelectedBackgroundHighContrast = cattabSelectedBackground;
        }

        bgDrawables.clear();

        calculateWallpaperColor();
    }

    public void calculateWallpaperColor() {
        WallpaperManager wm = (WallpaperManager)mContext.getSystemService(Context.WALLPAPER_SERVICE);
        mWallpaper = null;
        if (wm!=null) {
            WallpaperInfo wi = wm.getWallpaperInfo();
            if (wi!=null) {
                mWallpaper = wi.loadThumbnail(mContext.getPackageManager());
            }
            if (mWallpaper==null) {
                mWallpaper = wm.getDrawable();
            }

        }
        if (mWallpaper!=null) {
            Drawable wallp = mWallpaper.mutate();
            wallp.setColorFilter(wallpaperColor, PorterDuff.Mode.SRC_ATOP);
            //wallp = wallp.mutate();
            Bitmap bm = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas();
            //c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);
            c.setBitmap(bm);
            wallp.setBounds(0, 0, bm.getWidth(), bm.getHeight());
            wallp.draw(c);
            calculatedWallpaperColor = bm.getPixel(0, 0);
        } else {
            calculatedWallpaperColor = wallpaperColor;
        }

    }

    public int getCalculatedWallpaperColor() {
        return calculatedWallpaperColor;
    }

    public Drawable getWallpaperDrawable() {
        return mWallpaper;
    }

    public Drawable mWallpaper;


    private final Map<String,Drawable> bgDrawables = new WeakHashMap<>();

    public Drawable getBgDrawableFor(View view, CategoryTabStyle catstyle, boolean isHighContrast) {
        int color = -1;

        switch (catstyle) {
            case Selected:
                color = isHighContrast ? cattabSelectedBackgroundHighContrast : cattabSelectedBackground;
                break;
            case DragHover:
                color = dragoverBackground;
                break;
            case Hidden:
                color = cattabBackground;
                break;
            case Tiny:
            case Normal:
            case Default:
                color = isHighContrast ? cattabBackgroundHighContrast : cattabBackground;
                break;

            case None:
            default:

        }

        return getBgDrawableFor(view, color);
    }

    public Drawable getBgDrawableFor(View view, int color) {

        String key = view.toString() + color;

        Drawable newbg = bgDrawables.get(key);

        if (newbg==null) {

            Drawable base = mContext.getResources().getDrawable(R.drawable.rounded);
            if (base.getConstantState()==null) {
                newbg = base.mutate();  //shouldn't ever get here
                Log.d("Style", "base drawable had a null getConstantState");
            } else {
                newbg = base.getConstantState().newDrawable().mutate();
            }
            bgDrawables.put(key, newbg);
        }

        if (color!=-1) {
            newbg.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        }

        return newbg;
    }

    public boolean isRoundedTabs() {
        return mAppPreferences.getBoolean(mContext.getString(R.string.pref_key_rounded_tabs), true);
    }

    public int getMaxWCells() {
        int iconsizePref = Integer.parseInt(mAppPreferences.getString(mContext.getString(R.string.pref_key_iconsize), "1"));

        return 6-iconsizePref;

    }

//    public float getWidgetWidth(float minPixWidth) {
//
//        return minPixWidth * getRatio();
//    }

//    private float getRatio() {
//        float iconsize = mContext.getResources().getDimension(R.dimen.icon_width);
//
//        return iconsize / launcherIconSize;
//    }

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

    public boolean isCenteredIcons() {
        return centeredIcons;
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



    public void animateUpShow(View view) {
        animateShow(view,AnimateDirection.Down);
    }

    public void animateDownHide(View view) {
        animateHide(view, AnimateDirection.Down,false, true);
    }

    public enum AnimateDirection {Left, Up, Right, Down}

    private final Map<View,Long> aniHideStarted = new HashMap<>();

    public void animateHide(final View view, final AnimateDirection towards) {
        animateHide(view,towards, false, true);
    }

    public void animateHide(final View view, final AnimateDirection towards, final boolean andBack, final boolean bounce) {

//        Log.d(TAG, "animateHide " + view);
        if (mAnimationDuration==0) {
            view.clearAnimation();

            if (andBack) {
                ensureVisibleNoAni(view);

            } else {
                view.setVisibility(View.GONE);
            }
            return;
        }

        long now = System.currentTimeMillis();
        float fac = andBack?2.5f:1;
        Long then = aniHideStarted.get(view);
        if (then!=null && now - then < mAnimationDuration*fac) return;
        aniHideStarted.put(view,now);

        ViewPropertyAnimator animate = view.animate()
                .setDuration(mAnimationDuration)
                .setInterpolator(new AccelerateInterpolator())
                .alpha(0)
                .scaleY(.6f)
                .scaleX(.6f)

                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);

                        if (andBack) {
                            animateShow(view, towards, !bounce);
                        } else {
                            view.setVisibility(View.GONE);
                        }

                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        if (andBack) {
                            ensureVisibleNoAni(view);
                        } else {
                            view.setVisibility(View.GONE);
                        }
                    }
                });

        switch(towards) {
            case Down:
                animate.translationY(view.getHeight());
                break;
            case Up:
                animate.translationY(-view.getHeight());
                break;
            case Right:
                animate.translationX(view.getWidth());
                break;
            case Left:
                animate.translationX(-view.getHeight());
                break;
        }
        animate.setStartDelay(0).start();
    }

    public void ensureVisibleNoAni(View view) {
        view.setAlpha(1);
        view.setScaleX(1);
        view.setScaleY(1);
        view.setTranslationX(0);
        view.setTranslationY(0);
        view.setVisibility(View.VISIBLE);
    }

    public void animateShow(final View view, AnimateDirection from) {
        animateShow(view, from,false);
    }


    public void animateShow(final View view, AnimateDirection from, boolean reverse) {

        if (mAnimationDuration==0) {
            view.clearAnimation();
            ensureVisibleNoAni(view);
            //Log.d(TAG, "animateShow " + view);

            return;
        }

        if (reverse) {
            switch(from) {
                case Down:
                    view.setTranslationY(-view.getHeight());
                    break;
                case Up:
                    view.setTranslationY(view.getHeight());
                    break;
                case Right:
                    view.setTranslationX(-view.getWidth());
                    break;
                case Left:
                    view.setTranslationX(view.getWidth());
                    break;
            }
        }

        view.setVisibility(View.VISIBLE);

        ViewPropertyAnimator animate = view.animate()
                .setDuration(mAnimationDuration)
                .setInterpolator(new DecelerateInterpolator())
                .alpha(1)
                .scaleY(1)
                .scaleX(1)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        ensureVisibleNoAni(view);
                    }
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        ensureVisibleNoAni(view);
                    }
                });

        switch(from) {
            case Down:
            case Up:
                animate.translationY(0);
                break;
            case Right:
            case Left:
                animate.translationX(0);
                break;
        }


    }


    public void animateChangingSize(final View view, int startsize, int newsize, final Runnable before, final Runnable after) {
        final ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.height = startsize;
        view.setLayoutParams(lp);

        ValueAnimator o = ValueAnimator.ofInt(startsize,newsize);
        o.setDuration(mAnimationDuration);
        o.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                lp.height = (int)valueAnimator.getAnimatedValue();
                view.setLayoutParams(lp);
            }
        });
        o.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (before!=null) before.run();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                view.setLayoutParams(lp);
                if (after!=null) after.run();
            }
        });


        o.start();
    }


}
