package com.quaap.launchtime;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import android.view.KeyEvent;
import android.widget.Toast;

import com.quaap.launchtime.components.IconsHandler;
import com.quaap.launchtime.ui.MsgBox;

import java.util.Map;

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
 *
 *  Some portions derived from KISS.
 *  https://github.com/Neamar/KISS
 *
 */


public class SettingsActivity extends PreferenceActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.DKGRAY));
        getListView().setBackgroundColor(Color.DKGRAY);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (fragmentName.startsWith("com.quaap.launchtime.SettingsActivity$")) {
            return true;
        }
        return super.isValidFragment(fragmentName);
    }

    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            Preference newFeatures = findPreference(getString(R.string.pref_key_conf_new_features));
            newFeatures.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    MsgBox.promptNewFeatures(getActivity(), false, new Runnable() {
                        @Override
                        public void run() {
                            getActivity().finish();
                        }
                    });

                    return true;
                }
            });


        }

        @Override
        public void onResume() {
            super.onResume();

        }
    }


    @Override
    public void onBackPressed() {

        Intent main = new Intent(this, MainActivity.class);
        //setResult(RESULT_OK);
        finish();
        startActivity(main);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_HOME || keyCode==KeyEvent.KEYCODE_MENU) {
            Intent main = new Intent(this, MainActivity.class);
            startActivity(main);
            setResult(RESULT_OK);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

}
