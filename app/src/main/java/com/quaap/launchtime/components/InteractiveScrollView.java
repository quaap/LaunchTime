package com.quaap.launchtime.components;

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
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;



public class InteractiveScrollView extends ScrollView {
    OnPositionChangedListener mListener;

    public InteractiveScrollView(Context context, AttributeSet attrs,
                                 int defStyle) {
        super(context, attrs, defStyle);
    }

    public InteractiveScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InteractiveScrollView(Context context) {
        super(context);

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
            //
            //        if (diff <40 && mListener != null) {
            //            mListener.onPositionChanged(InteractiveScrollViewPosition.Bottom);
            //        } else if (getScrollY()<40) {
            //            mListener.onPositionChanged(InteractiveScrollViewPosition.Top);
            //        } else {
            //            mListener.onPositionChanged(InteractiveScrollViewPosition.Body);
            //        }

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


    // public enum InteractiveScrollViewPosition {Top, Body, Bottom}

    /**
     * Event listener.
     */
    public interface OnPositionChangedListener {
        void onPositionChanged(float percentUp, float percentDown, int distFromTop, int distFromBottom);
    }

}