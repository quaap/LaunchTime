package com.quaap.launchtime.components;

import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.widget.GridLayout;

/**
 * Created by tom on 1/10/17.
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
public class Utils {


    public static void changeColumnCount(GridLayout gridLayout, int columnCount, int cellWidth) {
        if (gridLayout.getColumnCount() != columnCount) {
            final int viewsCount = gridLayout.getChildCount();
            for (int i = 0; i < viewsCount; i++) {
                View view = gridLayout.getChildAt(i);
                //new GridLayout.LayoutParams created with Spec.UNSPECIFIED
                //which are package visible

                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
               // lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, view.getWidth()/cellWidth+1);
                view.setLayoutParams(lp);
            }
            gridLayout.setColumnCount(columnCount);
        }
    }

    public static boolean isLandscape(Context context) {
        int orientation = context.getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

}
