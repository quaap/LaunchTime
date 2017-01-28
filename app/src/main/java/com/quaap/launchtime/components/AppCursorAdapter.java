package com.quaap.launchtime.components;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.quaap.launchtime.GlobState;
import com.quaap.launchtime.MainActivity;
import com.quaap.launchtime.R;
import com.quaap.launchtime.db.DB;

/**
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
public class AppCursorAdapter extends ResourceCursorAdapter implements AdapterView.OnItemClickListener {
    private MainActivity mMain;

    private TextView mTextHolder;

    private DB mDB;

    public AppCursorAdapter(MainActivity main, TextView textHolder, int layout, Cursor cursor, int flags) {
        super(main, layout, cursor, flags);
        mMain = main;
        mDB = GlobState.getGlobState(main).getDB();
        mTextHolder = textHolder;

    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        if (getFilterQueryProvider() != null) {
            return getFilterQueryProvider().runQuery(constraint);
        }

        Cursor cursor = mDB.getAppCursor("%" + (constraint == null ? "XXXXXX" : constraint.toString()) + "%");

        return cursor;
    }

    // The bindView method is used to bind all data to a given view
    // such as setting the text on a TextView.
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        String activityName = cursor.getString(0);

        ViewGroup appholder = (ViewGroup) view.findViewById(R.id.icontarget);
        appholder.removeAllViews();

        AppShortcut app = mDB.getApp(activityName);
        if (app != null) {
            app.loadAppIconAsync(context, context.getPackageManager());
            View v = mMain.getShortcutView(app, false, false);
            if (v!=null) {
                appholder.addView(v);
            } else {
                appholder.addView(new TextView(context));
            }
        }

        String label = cursor.getString(1);
        TextView labelView = (TextView) view.findViewById(R.id.label);
        labelView.setText(label);
    }

    public void close() {
        try {
            changeCursor(null);
        } catch(Exception e)  {
            Log.d("Appcursor", "Exception on 'close()'", e);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        String activityName = cursor.getString(0);
        String label = cursor.getString(1);

        mTextHolder.setText(label);

        mMain.launchApp(activityName);
    }


}
