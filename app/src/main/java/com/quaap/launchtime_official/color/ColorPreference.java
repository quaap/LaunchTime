package com.quaap.launchtime_official.color;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
* Created by tom on 1/24/17.
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
public class ColorPreference extends DialogPreference {

    private Context mContext;
    private int color=Color.BLACK;
    private ColorChooser picker=null;



    public ColorPreference(Context ctxt, AttributeSet attrs) {
        super(ctxt, attrs);

        setPositiveButtonText("Set");
        setNegativeButtonText("Cancel");
    }

    TextView tc;
    @Override
    protected View onCreateView(ViewGroup parent) {
        ViewGroup view = (ViewGroup)super.onCreateView(parent);
        tc = new TextView(getContext());
        tc.setText("    ");
        tc.setBackgroundColor(color);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(8);
        lp.setMarginStart(16);
        view.addView(tc);
        return view;
    }

    @Override
    protected View onCreateDialogView() {
        picker=new ColorChooser(getContext());

        return(picker);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        picker.setColor(color);

    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            color=picker.getSelectedColor();
            if (tc!=null) {
                tc.setBackgroundColor(color);
            }

            if (callChangeListener(color)) {
                persistInt(color);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return(a.getInt(index, Color.BLACK));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {


        if (restoreValue) {
            color=getPersistedInt(Color.BLACK);
        }
        else {
            color=(int)defaultValue;
        }


    }

    @Override
    public Drawable getIcon() {
        return new ColorDrawable(color);
    }
}
