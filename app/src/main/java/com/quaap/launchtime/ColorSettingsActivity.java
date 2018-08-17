package com.quaap.launchtime;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.widget.Toast;

import com.quaap.launchtime.color.ColorDemo;
import com.quaap.launchtime.components.IconsHandler;

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


public class ColorSettingsActivity extends PreferenceActivity {

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
        if (fragmentName.startsWith("com.quaap.launchtime.ColorSettingsActivity$")) {
            return true;
        }
        return super.isValidFragment(fragmentName);
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        ColorDemo colorDemo;
        SharedPreferences prefs;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.color_preferences);


            colorDemo = (ColorDemo)findPreference("colordemo");

            colorDemo.applyStyle();

            Preference colorbutton = findPreference(getString(R.string.pref_key_reset_colors));
            colorbutton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    new AlertDialog.Builder(getActivity())

                            .setTitle(R.string.reset_colors_title)
                            .setMessage(R.string.confirm_reset_colors)

                            .setPositiveButton(R.string.reset_colors_title, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    IconsHandler ich = GlobState.getIconsHandler(getActivity());
                                    ich.getTheme().resetUserColors();
                                    Toast.makeText(getActivity(), R.string.colors_reset_default,Toast.LENGTH_SHORT).show();
                                    //getActivity().finish();
                                    Intent intent = getActivity().getIntent();
                                    getActivity().overridePendingTransition(0, 0);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                    getActivity().finish();
                                    getActivity().overridePendingTransition(0, 0);
                                    getActivity().startActivity(intent);
                                    dialog.dismiss();
                                }

                            })
                            .setNegativeButton(R.string.cancel,  new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            })
                            .create().show();


                    return true;
                }
            });

            prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            prefs.registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onPause() {
            prefs.unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }


        @Override
        public void onResume() {
            super.onResume();

            IconsHandler iph = GlobState.getIconsHandler(this.getActivity());
            findPreference(getString(R.string.pref_key_icon_tint)).setEnabled(iph.isIconTintable());

            ListPreference iconsPack = (ListPreference)findPreference(getString(R.string.pref_key_icons_pack));

            setListPreferenceIconsPacksData(iconsPack, this.getActivity());
//            iconsPack.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//                @Override
//                public boolean onPreferenceChange(Preference preference, Object newValue) {
//                    getActivity().finish();
//                    return true;
//                }
//            });


        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            colorDemo.applyStyle();
        }

    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //Intent main = new Intent(this, MainActivity.class);
        ////setResult(RESULT_OK);
        //finish();
        //startActivity(main);

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

    private static void setListPreferenceIconsPacksData(ListPreference lp, Context context) {
        IconsHandler iph = GlobState.getIconsHandler(context);

        iph.loadAvailableIconsPacks();

        Map<String, String> iconsPacks = iph.getAllIconsThemes();

        CharSequence[] entries = new CharSequence[iconsPacks.size()];
        CharSequence[] entryValues = new CharSequence[iconsPacks.size()];

        int i = 0;
        for (String packageIconsPack : iconsPacks.keySet()) {
            entries[i] = iconsPacks.get(packageIconsPack);
            entryValues[i] = packageIconsPack;
            i++;
        }

        lp.setEntries(entries);
        lp.setDefaultValue(IconsHandler.DEFAULT_PACK);
        lp.setEntryValues(entryValues);
    }
}
