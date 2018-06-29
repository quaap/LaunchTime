package com.quaap.launchtime_official.apps;

/**
 * Triggers a event when scrolling reaches bottom.
 * <p>
 * Created by martinsandstrom on 2010-05-12.
 * Updated by martinsandstrom on 2014-07-22.
 */

/**
 * Additional work
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
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ScrollView;



public class InteractiveScrollView extends ScrollView {
    OnPositionChangedListener mListener;
    OnSwipeHorizontalListener mHSwipeListener;

    public InteractiveScrollView(Context context, AttributeSet attrs,
                                 int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public InteractiveScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public InteractiveScrollView(Context context) {
        super(context);
        init(context);
    }

    private static final String TAG = "SV";

    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        swipelen = ViewConfiguration.get(context).getScaledPagingTouchSlop()*density;
        clickSlop = ViewConfiguration.get(context).getScaledTouchSlop()*density;
        Log.d(TAG, "swipelen=" + swipelen );
        Log.d(TAG, "clickSlop=" + clickSlop );
    }

    private float x, y;
    private long startTime;

    private float swipelen = 150;
    private float clickSlop = 10;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        //Log.d(TAG, "onInterceptTouchEvent");
        boolean isLRSwipe = shouldSwipe(motionEvent);

        return isLRSwipe || super.onInterceptTouchEvent(motionEvent);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        //Log.d(TAG, "onTouchEvent");

        boolean isLRSwipe = shouldSwipe(motionEvent);

        return isLRSwipe || super.onTouchEvent(motionEvent);
    }

    private boolean shouldSwipe(MotionEvent motionEvent) {
        if (mHSwipeListener!=null) {
            int action = motionEvent.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    x = motionEvent.getX();
                    y = motionEvent.getY();
                    startTime = System.currentTimeMillis();
                    //Log.d(TAG, "ACTION_DOWN=" + x);
                    break;


                case MotionEvent.ACTION_MOVE:
                    //Log.d(TAG, "ACTION_MOVE=" + motionEvent.getX());
                    break;

                case MotionEvent.ACTION_UP:
                    //Log.d(TAG, "ACTION_UP=" + motionEvent.getX() + " " + x);
                    if (System.currentTimeMillis() - startTime < 200) {
                        float xdiff = motionEvent.getX() - x;
                        float ydiff = motionEvent.getY() - y;
                        //Log.d(TAG, " xdiff==" + xdiff + " " + ydiff);
                        if (Math.abs(xdiff)>Math.abs(ydiff*3/2)) {
                            if (xdiff > swipelen) {
                               // Log.d(TAG, "xdiff=" + xdiff);
                                mHSwipeListener.onLeftSwipe(xdiff);
                                return true;
                            } else if (xdiff < -swipelen) {
                               // Log.d(TAG, "xdiff=" + xdiff);
                                mHSwipeListener.onRightSwipe(xdiff);
                                return true;
                            }
                        }
                        if (Math.abs(xdiff) <= clickSlop && Math.abs(xdiff) <= clickSlop) {
                            performClick();
                        }
                    }

            }
        }
        return false;
    }


    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        if (mListener != null) {
            View view = getChildAt(getChildCount() - 1);

            float bottom = (float) view.getBottom();

            if (bottom == 0) return;

            int diff = (int) (bottom - (getHeight() + getScrollY()));

            float percentDown = (getHeight() + getScrollY()) / bottom;
            float percentUp = (bottom - getScrollY()) / bottom;

            mListener.onPositionChanged(percentUp, percentDown, getScrollY(), diff);

        }

        super.onScrollChanged(l, t, oldl, oldt);
    }


    public OnPositionChangedListener getOnPositionChangedListener() {
        return mListener;
    }

    public void setOnPositionChangedListener(
            OnPositionChangedListener onPositionChangedListener) {
        mListener = onPositionChangedListener;
    }


    public OnSwipeHorizontalListener getHSwipeListener() {
        return mHSwipeListener;
    }

    public void setHSwipeListener(OnSwipeHorizontalListener mHSwipeListener) {
        this.mHSwipeListener = mHSwipeListener;
    }

    // public enum InteractiveScrollViewPosition {Top, Body, Bottom}

    /**
     * Event listener.
     */
    public interface OnPositionChangedListener {
        void onPositionChanged(float percentUp, float percentDown, int distFromTop, int distFromBottom);
    }

    public interface OnSwipeHorizontalListener {
        void onLeftSwipe(float absDist);
        void onRightSwipe(float absDist);

    }
}