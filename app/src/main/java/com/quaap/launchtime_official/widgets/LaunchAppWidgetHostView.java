package com.quaap.launchtime_official.widgets;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Created by tom on 1/14/17.
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

public class LaunchAppWidgetHostView extends AppWidgetHostView {

    private OnLongClickListener mLongClickListener;
    private long mLongClickStarted = -1;


    public LaunchAppWidgetHostView(Context context) {
        super(context);
    }

    public LaunchAppWidgetHostView(Context context, int animationIn, int animationOut) {
        super(context, animationIn, animationOut);
    }


    @Override
    public void setOnLongClickListener(OnLongClickListener listener) {
        this.mLongClickListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (MotionEventCompat.getActionMasked(ev)) {
            case MotionEvent.ACTION_DOWN:
                mLongClickStarted = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_MOVE:
                boolean upVal = System.currentTimeMillis() - mLongClickStarted > ViewConfiguration.getLongPressTimeout();
                if (upVal && mLongClickListener != null) {
                    mLongClickListener.onLongClick(LaunchAppWidgetHostView.this);
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                }
                break;
            case MotionEvent.ACTION_UP:
                mLongClickStarted = -1;
        }

        return false;
    }
}
