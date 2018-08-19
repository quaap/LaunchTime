package com.quaap.launchtime.color;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.quaap.launchtime.GlobState;
import com.quaap.launchtime.R;
import com.quaap.launchtime.components.Theme;
import com.quaap.launchtime.ui.Style;

public class ColorDemo extends Preference {
    private ViewGroup thisview;
    private final FrameLayout body;
    private final FrameLayout bg;
    private final LinearLayout menu;
    private final LinearLayout iconarea;
    private final TextView cat1;
    private final TextView catsel;
    private final TextView icon1;
    private final TextView icon2;
    private final Drawable icond1;
    private final Drawable icond2;

    private final Style style;

    public ColorDemo(Context context, AttributeSet attrs) {
        super(context, attrs);
        style = GlobState.getStyle(getContext());
        body = (FrameLayout) LayoutInflater.from(getContext()).inflate(R.layout.color_demo, null);
        body.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        bg = body.findViewById(R.id.demo_bg);

        menu = body.findViewById(R.id.demo_cattabbg);
        iconarea = body.findViewById(R.id.demo_iconarea);

        cat1 = body.findViewById(R.id.demo_category_tab);
        catsel = body.findViewById(R.id.demo_category_tab_sel);

        icon1 = body.findViewById(R.id.demo_launcher);
        icon2 = body.findViewById(R.id.demo_launcher2);
        icond1 = icon1.getCompoundDrawables()[1];
        icond2 = icon2.getCompoundDrawables()[1];

    }


    @SuppressLint("RtlHardcoded")
    public void applyStyle() {

        style.calculateWallpaperColor();
        Drawable wpd = style.getWallpaperDrawable();

        Bitmap bm = Bitmap.createBitmap(400, 240, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas();
        //c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);
        c.setBitmap(bm);
        wpd.setBounds(0, 0, bm.getWidth(), bm.getHeight());
        wpd.draw(c);

        body.setBackground(new BitmapDrawable(getContext().getResources(), bm));
        //bm.recycle();

        bg.setBackgroundColor(style.getWallpaperColor());

        int cattabwidth = getContext().getResources().getDimensionPixelSize(R.dimen.cattabbar_width);
        FrameLayout.LayoutParams ilp = (FrameLayout.LayoutParams)iconarea.getLayoutParams();
        FrameLayout.LayoutParams mlp = (FrameLayout.LayoutParams)menu.getLayoutParams();
        if (style.isLeftHandCategories()) {
            mlp.gravity = Gravity.LEFT;
            ilp.leftMargin = cattabwidth;
            ilp.rightMargin = cattabwidth;
        } else {
            mlp.gravity = Gravity.RIGHT;
            ilp.leftMargin=10;
            ilp.rightMargin=50;
        }

        if (style.isCenteredIcons()) {
            ilp.gravity = Gravity.CENTER_HORIZONTAL;
        } else {
            ilp.gravity = Gravity.LEFT;
        }
        iconarea.setLayoutParams(ilp);
        menu.setLayoutParams(mlp);

//        cat1.setBackgroundColor(style.getCattabBackground());
//        cat1.setTextColor(style.getCattabTextColor());
//
//        catsel.setBackgroundColor(style.getCattabSelectedBackground());
//        catsel.setTextColor(style.getCattabSelectedText());

        style.styleCategoryStyle(cat1, Style.CategoryTabStyle.Normal, false);
        style.styleCategoryStyle(catsel, Style.CategoryTabStyle.Selected, false);

        icon1.setTextSize(TypedValue.COMPLEX_UNIT_PX, style.getLauncherFontSize()+1);
        icon1.setTextColor(style.getTextColor());

        Drawable icond1b = getNewIf(icond1, getContext().getResources());
        icond1b.setBounds(0,0,style.getLauncherIconSize(),style.getLauncherIconSize());
        icon1.setCompoundDrawables(null, Theme.applyIconTint(icond1b, style.getIconTint()),null,null);


        icon2.setTextSize(TypedValue.COMPLEX_UNIT_PX, style.getLauncherFontSize()+1);
        icon2.setTextColor(style.getTextColor());

        Drawable icond2b = getNewIf(icond2, getContext().getResources());
        icond2b.setBounds(0,0,style.getLauncherIconSize(),style.getLauncherIconSize());
        icon2.setCompoundDrawables(null, Theme.applyIconTint(icond2b, style.getIconTint()),null,null);

    }

    private static Drawable getNewIf(Drawable d, Resources res) {
        Drawable.ConstantState cd = d.getConstantState();
        if (cd!=null) {
            d = cd.newDrawable(res);
        } else {
            d.mutate();
        }
        return d;
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        thisview = (ViewGroup)super.onCreateView(parent);

        if (body.getParent()!=null) {
            ((ViewGroup)body.getParent()).removeView(body);
        }
        thisview.addView(body);
        applyStyle();
        return thisview;
    }

}
