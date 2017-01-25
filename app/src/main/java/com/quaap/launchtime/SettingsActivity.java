package com.quaap.launchtime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

import java.util.List;
import java.util.Stack;

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

public class SettingsActivity extends PreferenceActivity {


    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (fragmentName.startsWith("com.quaap.launchtime.SettingsActivity$")) {
            return true;
        }
        return super.isValidFragment(fragmentName);
    }


    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
        //SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        //settings.edit().clear().apply();
    }

    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public void onResume() {
            super.onResume();
        }
    }

    public static class ColorSettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.color_preferences);
        }
    }

//    @Override
//    public void onBackPressed() {
//
//        Log.d("d", "onback");
//        if (mSelected.size()>0) {
//            mSelected.pop();
//            super.onBackPressed();
//        } else {
//            Intent main = new Intent(this, MainActivity.class);
//            startActivity(main);
//            setResult(RESULT_OK);
//            finish();
//        }
//    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_HOME) {
            Intent main = new Intent(this, MainActivity.class);
            startActivity(main);
            setResult(RESULT_OK);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

}
