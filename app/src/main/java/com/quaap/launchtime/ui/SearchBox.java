package com.quaap.launchtime.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.quaap.launchtime.GlobState;
import com.quaap.launchtime.MainActivity;
import com.quaap.launchtime.R;
import com.quaap.launchtime.apps.AppCursorAdapter;
import com.quaap.launchtime.apps.InteractiveScrollView;
import com.quaap.launchtime.apps.StaticListView;
import com.quaap.launchtime.components.MsgBox;
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

public class SearchBox {
    private final ViewGroup mSearchView;

    private int mSearchRememberScrollPos;

    private final AppCursorAdapter mSearchAdapter;
    private final EditText mSearchbox;

    private final MainActivity mMainActivity;
    private final InteractiveScrollView mIconSheetScroller;

    public SearchBox(final MainActivity mainActivity, InteractiveScrollView iconSheetScroller) {

        mMainActivity = mainActivity;
        mIconSheetScroller = iconSheetScroller;


        mSearchView = (ViewGroup) LayoutInflater.from(mainActivity).inflate(R.layout.search_layout, null);


        ImageView quickasett = mSearchView.findViewById(R.id.quick_settings_android);
        quickasett.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchAction("android.settings.SETTINGS", "settings");
            }
        });

        ImageView quicklsett = mSearchView.findViewById(R.id.quick_settings_launchtime);
        quicklsett.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.openSettings(mainActivity);
            }
        });


        ImageView quickbsett = mSearchView.findViewById(R.id.quick_settings_bt);
        quickbsett.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchActivity("com.android.settings", "com.android.settings.bluetooth.BluetoothSettings", "BluetoothSettings");
            }
        });


        ImageView quickvsett = mSearchView.findViewById(R.id.quick_settings_vol);
        quickvsett.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchActivity("com.android.settings", "com.android.settings.SoundSettings", "BluetoothSettings");
            }
        });

        ImageView quickwisett = mSearchView.findViewById(R.id.quick_settings_wifi);
        quickwisett.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchActivity("com.android.settings", "com.android.settings.wifi.WifiSettings", "WifiSettings");
            }
        });

        ImageView quickdevsett = mSearchView.findViewById(R.id.quick_settings_dev);
        quickdevsett.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MsgBox.show(mMainActivity, mMainActivity.getString(android.R.string.dialog_alert_title),
                        mMainActivity.getString(R.string.devops_warn), new Runnable() {
                    @Override
                    public void run() {
                        launchActivity("com.android.settings", "com.android.settings.DevelopmentSettings", "DevOps");
                    }
                });
            }
        });

        mSearchbox = mSearchView.findViewById(R.id.search_box);

        mSearchAdapter = new AppCursorAdapter(mainActivity, mSearchbox, R.layout.search_item, 0);
        StaticListView list = mSearchView.findViewById(R.id.search_dropdownarea);

        list.setAdapter(mSearchAdapter);
        list.setOnItemClickListener(mSearchAdapter);

        list.setOnLoadCompleteListener(new StaticListView.OnLoadCompleteListener() {
            @Override
            public void loadComplete() {
                mIconSheetScroller.scrollTo(0, mSearchRememberScrollPos);
                mIconSheetScroller.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mIconSheetScroller.scrollTo(0, mSearchRememberScrollPos);

                    }
                }, 100);
            }
        });


        mSearchView.findViewById(R.id.btn_clear_searchbox).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSearchbox.setText("");
                mSearchRememberScrollPos = 0;
            }
        });


        mSearchView.findViewById(R.id.btn_clear_recents).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity)
                        .setTitle(R.string.clear_recent)
                        .setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                db().deleteAppLaunchedRecords();
                                mMainActivity.populateRecentApps();
                            }
                        }).setNegativeButton(R.string.cancel, null);
                builder.show();

            }
        });

        refreshSearch(false);

    }

    private void launchActivity(String packageName, String classname, String name) {
        try {
            Intent l = new Intent(Intent.ACTION_VIEW);
            l.setClassName(packageName, classname);
            l.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mMainActivity.startActivity(l);
        } catch (Throwable t) {
            Log.e("short", t.getMessage(), t);
            Toast.makeText(mMainActivity, "Couldn't start setting "  + name, Toast.LENGTH_SHORT).show();
        }
    }

    private void launchAction(String action, String name) {
        try {
            Intent l = new Intent(action);
            l.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mMainActivity.startActivity(l);
        } catch (Throwable t) {
            Log.e("short", t.getMessage(), t);
            Toast.makeText(mMainActivity, "Couldn't start setting "  + name, Toast.LENGTH_SHORT).show();
        }
    }


    public void refreshSearch(boolean rememberPos) {
        if (rememberPos) mSearchRememberScrollPos = mIconSheetScroller.getScrollY();
        if (mSearchAdapter!=null) {
            mSearchAdapter.refreshCursor();
        }
    }

    public void closeSeachAdapter() {
        //close our search cursor, if needed
        if (mSearchAdapter!=null){
            mSearchAdapter.close();
        }
    }


    public ViewGroup getSearchView() {
        return mSearchView;
    }

    public String getSeachText() {
        return mSearchbox.getText().toString();
    }

    public void setSearchText(CharSequence text) {
        mSearchbox.setText(text);
    }

    private DB db() {
        return GlobState.getGlobState(mMainActivity).getDB();
    }
}
