package com.quaap.launchtime.components;

import android.content.Context;
import android.database.Cursor;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ProgressBar;
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
public class AppCursorAdapter extends ResourceCursorAdapter implements StaticListView.OnItemClickListener {
    private MainActivity mMain;

    private EditText mTextHolder;


    private DB mDB;

    public AppCursorAdapter(final MainActivity main, EditText textHolder, int layout, int flags) {
        super(main, layout, null, flags);
        mMain = main;
        mDB = GlobState.getGlobState(main).getDB();
        mTextHolder = textHolder;

        mTextHolder.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId==EditorInfo.IME_ACTION_SEARCH) {
                    mTextHolder.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            refreshCursor();
                        }
                    },10);
                }
                return false;
            }
        });

        mTextHolder.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mTextHolder.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshCursor();
                    }
                },10);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mTextHolder.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshCursor();
            }
        },10);

    }

    public void refreshCursor() {
        String text = mTextHolder.getText().toString().trim();
        if (text.length()==0) {
            text = "xxXXXXX";
        }

        text = text.replace(".", "_");

        changeCursor(mDB.getAppCursor("%" + text + "%"));

        //Log.d("gghh", mTextHolder.getText().toString());

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
    public void onItemClick(Object item, View itemView, int position, long id) {
        Cursor cursor = (Cursor) item;
        String activityName = cursor.getString(0);
        String label = cursor.getString(1);

       // mTextHolder.setText(label);

        mMain.launchApp(activityName);

    }


}
