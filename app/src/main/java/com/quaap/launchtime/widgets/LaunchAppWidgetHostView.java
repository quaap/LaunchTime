package com.quaap.launchtime.widgets;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
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

    public boolean hasError = false;

    private float x;
    private float y;

    final int slop;

    public LaunchAppWidgetHostView(Context context) {
        this(context, 0, 0);
    }

    public LaunchAppWidgetHostView(Context context, int animationIn, int animationOut) {
        super(context, animationIn, animationOut);
        slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    protected View getErrorView() {
        hasError = true;
        return super.getErrorView();

    }

    @Override
    public void setOnLongClickListener(OnLongClickListener listener) {
        this.mLongClickListener = listener;
    }

    private boolean wasLong = false;
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mLongClickListener == null) return false;

       // Log.d("Widget", ev.getY() + " " + ev + " ");

        switch (ev.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                mLongClickStarted = System.currentTimeMillis();
                final long starttime = mLongClickStarted;
                x=ev.getX();
                y=ev.getY();

                if (y>getPaddingTop()*2 && x>getPaddingLeft()*2 && y<getHeight()-getPaddingBottom()*2 && x<getWidth()-getPaddingRight()*2) {
                    this.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mLongClickListener != null && mLongClickStarted != -1 && mLongClickStarted == starttime) {
                                wasLong = true;
                                mLongClickListener.onLongClick(LaunchAppWidgetHostView.this);
                                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                            }
                        }
                    }, ViewConfiguration.getLongPressTimeout());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(x - ev.getX()) > slop || Math.abs(y - ev.getY()) > slop) { //moved too much, not a longclick
                    mLongClickStarted = -1;
                    wasLong = false;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_HOVER_EXIT:
            case MotionEvent.ACTION_OUTSIDE:
                wasLong = false;
            case MotionEvent.ACTION_UP:
                mLongClickStarted = -1;
                if (wasLong) {                        // setSource is a hack to let us know to ignore that cancel.
                    ev.setSource(InputDevice.SOURCE_ANY); //returning true means target will get ACTION_CANCEL.
                    wasLong = false;
                    return true;
                }
                break;
            default:
                Log.d("Widget", ev + " " + ev.getActionMasked() + " " + ev.getDownTime());
        }


        return false;
    }
}
