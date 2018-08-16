package com.quaap.launchtime.color;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.quaap.launchtime.GlobState;
import com.quaap.launchtime.R;
import com.quaap.launchtime.components.Theme;
import com.quaap.launchtime.ui.Style;

public class ColorDemo extends Preference {
    FrameLayout body;
    TextView cat1;
    TextView catsel;
    TextView icon;

    Style style;

    public ColorDemo(Context context, AttributeSet attrs) {
        super(context, attrs);
        style = GlobState.getStyle(getContext());
        body = (FrameLayout) LayoutInflater.from(getContext()).inflate(R.layout.color_demo, null);
        body.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        cat1 = body.findViewById(R.id.category_tab);
        catsel = body.findViewById(R.id.category_tab_sel);

        icon = body.findViewById(R.id.launcher);
    }


    public void applyStyle() {
        style.calculateWallpaperColor();
        body.setBackgroundColor(style.getCalculatedWallpaperColor());
        cat1.setBackgroundColor(style.getCattabBackground());
        cat1.setTextColor(style.getCattabTextColor());
        catsel.setBackgroundColor(style.getCattabSelectedBackground());
        catsel.setTextColor(style.getCattabSelectedText());
        icon.setTextColor(style.getTextColor());

        Drawable d = icon.getCompoundDrawables()[1];
        icon.setCompoundDrawables(null, Theme.applyIconTint(d, style.getIconTint()),null,null);

    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        ViewGroup view = (ViewGroup)super.onCreateView(parent);


        if (body.getParent()!=null) {
            ((ViewGroup)body.getParent()).removeView(body);
        }
        view.addView(body);
        applyStyle();
        return view;
    }
}
