package com.quaap.launchtime.components;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;

import com.quaap.launchtime.GlobState;
import com.quaap.launchtime.MainActivity;
import com.quaap.launchtime.MainHelper;
import com.quaap.launchtime.R;
import com.quaap.launchtime.db.DB;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tom on 8/10/17.
 */

public class QuickRow {

    public static final String QUICK_ROW_CAT = "QuickRow";
    private GridLayout mQuickRow;
    private HorizontalScrollView mQuickRowScroller;

    private MainActivity mMainActivity;

    public QuickRow(final View.OnDragListener dragListener, MainActivity mainActivity) {

        mMainActivity = mainActivity;

        mQuickRow = (GridLayout) mMainActivity.findViewById(R.id.layout_quickrow);

        mQuickRowScroller = (HorizontalScrollView) mMainActivity.findViewById(R.id.layout_quickrow_scroll);

        mQuickRow.setOnDragListener(dragListener);
        mQuickRowScroller.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                return dragListener.onDrag(mQuickRow, dragEvent);
            }
        });
    }

    public GridLayout getGridLayout() {
        return mQuickRow;
    }

    public HorizontalScrollView getScroller() {
        return mQuickRowScroller;
    }

    public int getScrollPos() {
        return mQuickRowScroller.getScrollX();
    }

    public void scrollToStart() {
        mQuickRowScroller.smoothScrollTo(0, 0);
    }



    public void processQuickApps(List<AppShortcut> shortcuts, PackageManager packageMan) {
        List<AppShortcut> quickRowApps = new ArrayList<>();
        final List<ComponentName> quickRowOrder = db().getAppCategoryOrder(QUICK_ROW_CAT);

        MainHelper.checkDefaultApps(mQuickRow.getContext(), shortcuts, quickRowOrder, mQuickRow);


        for (AppShortcut app : shortcuts) {

            if (quickRowOrder.contains(app.getComponentName())) {
                AppShortcut qapp = AppShortcut.createAppShortcut(app);
                qapp.loadAppIconAsync(mQuickRow.getContext(), packageMan);
                quickRowApps.add(qapp);
            }
        }


        mQuickRow.removeAllViews();
        for (ComponentName actvname : quickRowOrder) {
            for (AppShortcut app : quickRowApps) {
                if (app.getComponentName().equals(actvname)) {
                    ViewGroup item = mMainActivity.getShortcutView(app, true);
                    if (item!=null) {
                        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.TOP);
                        mQuickRow.addView(item, lp);
                    }
                }
            }
        }

    }

    public void removeFromQuickApps(ComponentName actvname) {
        for (int i = mQuickRow.getChildCount()-1; i>=0; i--) {
            AppShortcut app = (AppShortcut) mQuickRow.getChildAt(i).getTag();
            if (app != null && actvname.equals(app.getComponentName())) {
                mQuickRow.removeView(mQuickRow.getChildAt(i));
            }
        }
    }

    private DB db() {
        return GlobState.getGlobState(mQuickRow.getContext()).getDB();
    }
}
