package com.quaap.launchtime.components;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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


    //private DB mDB;

    public AppCursorAdapter(final MainActivity main, EditText textHolder, int layout, int flags) {
        super(main, layout, null, flags);
        mMain = main;

        mTextHolder = textHolder;

//        mTextHolder.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
//                if (actionId==EditorInfo.IME_ACTION_SEARCH) {
//                    mTextHolder.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            refreshCursor();
//                        }
//                    },10);
//                }
//                return false;
//            }
//        });

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


//        mTextHolder.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                refreshCursor();
//            }
//        },10);

    }

    private DB db() {
        return GlobState.getGlobState(mMain).getDB();
    }

    public void refreshCursor() {
        String text = mTextHolder.getText().toString().trim();
        if (text.length()==0) {
            text = "XXXXXXXXXXXX";
        } else {

            text = text.replace(".", "_");
            text = "%" + text + "%";
        }

       // Log.d("refreshCursor", text);

        getFilter().filter(text);

    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
       // Log.d("BackThread", constraint.toString());
        return db().getAppCursor(constraint.toString());
    }

    static class ViewHolder {
        ViewGroup appholder;
        TextView labelView;
    }

    // The bindView method is used to bind all data to a given view
    // such as setting the text on a TextView.
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {

        final String activityName;
        final String label;
        try {
            activityName = cursor.getString(0);
            label = cursor.getString(1);
        } catch (CursorIndexOutOfBoundsException e) {
            Log.e("LaunchTime", "Bad cursor");
            return;
        } catch (Exception e) {
            Log.e("LaunchTime", "Bad cursor", e);
            return;
        }

        ViewHolder holder = (ViewHolder)view.getTag();
        if (holder==null) {
            holder = new ViewHolder();

            holder.appholder = (ViewGroup) view.findViewById(R.id.icontarget);
            holder.labelView = (TextView) view.findViewById(R.id.label);
            view.setTag(holder);
        }

        final ViewHolder viewholder = holder;
        new AsyncTask<Void,Void,AppShortcut>() {

            @Override
            protected AppShortcut doInBackground(Void... voids) {

                return db().getApp(activityName);
            }

            @Override
            protected void onPostExecute(AppShortcut app) {
                super.onPostExecute(app);
                if (app != null) {
                    app.loadAppIconAsync(context, context.getPackageManager());
                    View v = mMain.getShortcutView(app, false, false);

                    viewholder.appholder.removeAllViews();
                    if (v!=null) {
                        viewholder.appholder.addView(v);
                    } else {
                        viewholder.appholder.addView(new TextView(context));
                    }
                }

                viewholder.labelView.setText(label);
            }
        }.execute();



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
        //String label = cursor.getString(1);

       // mTextHolder.setText(label);

        mMain.launchApp(activityName);

    }


}
