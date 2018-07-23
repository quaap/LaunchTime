package com.quaap.launchtime.widgets;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
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

    private float x;
    private float y;


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
        if (mLongClickListener == null) return false;

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                x=ev.getX();
                y=ev.getY();
                mLongClickStarted = System.currentTimeMillis();
                final long starttime = mLongClickStarted;
                this.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mLongClickListener != null && mLongClickStarted == starttime) {
                            mLongClickListener.onLongClick(LaunchAppWidgetHostView.this);
                            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        }
                    }
                }, ViewConfiguration.getLongPressTimeout());
                break;
            case MotionEvent.ACTION_MOVE:
                final int slop  = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                if (Math.abs(x - ev.getX()) > slop || Math.abs(y - ev.getY()) > slop) { //moved too much, not a longclick
                    mLongClickStarted = -1;
                }
                break;
            case MotionEvent.ACTION_UP:
                mLongClickStarted = -1;
        }


        return false;
    }
}
