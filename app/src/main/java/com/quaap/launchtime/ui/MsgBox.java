package com.quaap.launchtime.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.quaap.launchtime.GlobState;
import com.quaap.launchtime.R;
import com.quaap.launchtime.components.IconsHandler;
import com.quaap.launchtime.components.Theme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

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
public class MsgBox {


    private static void show(Context context, String title, String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);

        builder.setNeutralButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void show(Context context, String title, String message, final Runnable run) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);

        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (run!=null) {
                    run.run();
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }



    public static void showNewsMessage(final Context context, SharedPreferences prefs) {


        final int newsnum = 83;
        final int news = prefs.getInt("seennews", 0);
        if (news < newsnum) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    StringWriter sw = new StringWriter();
                    BufferedReader in = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.news)));
                    try {

                        String line;
                        while ((line = in.readLine()) != null) {
                            sw.append(line);
                            sw.append("\n");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }


                    ScrollView scrll = new ScrollView(context);
                    LinearLayout viewg = new LinearLayout(context);
                    viewg.setOrientation(LinearLayout.VERTICAL);

                    Button b = new Button(context);
                    b.setText(R.string.prompt_config_features);
                    b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                    b.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            promptNewFeatures(context);
                        }
                    });


                    viewg.addView(b);
                    TextView ms = new TextView(context);
                    ms.setText(sw.toString());
                    ms.setPadding(10, 10, 10, 10);
                    viewg.addView(ms);

                    scrll.addView(viewg);

                    final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("What's new!");
                    builder.setView(scrll);

                    builder.setNeutralButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();

                }
            }, 3000);
            prefs.edit().putInt("seennews", newsnum).apply();
        }

    }

    public static void promptNewFeatures(final Context context) {


        ViewGroup content = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.new_features, null);

        final CheckBox hideCatsCheck = content.findViewById(R.id.hide_cats_check);
        TextView hideCatsText = content.findViewById(R.id.hide_cats_text);

        final CheckBox actionMenuCheck = content.findViewById(R.id.action_menu_check);
        TextView actionMenuText = content.findViewById(R.id.action_menu_text);

        final CheckBox extraMenuCheck = content.findViewById(R.id.extra_menu_check);
        TextView extraMenuText = content.findViewById(R.id.extra_menu_text);

        final CheckBox resetColorsCheck = content.findViewById(R.id.reset_colors_check);
        TextView resetColorsText = content.findViewById(R.id.reset_colors_text);


        final SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        int hiding = 0;

        if (!appPreferences.getString(context.getString(R.string.pref_key_autohide_cats_timeout), "-1").equals("-1")) {
            hideCatsCheck.setChecked(true);
            hideCatsCheck.setVisibility(View.GONE);
            hideCatsText.setVisibility(View.GONE);
            hiding++;
        }

        if (appPreferences.getBoolean(context.getString(R.string.pref_key_show_action_menus), false)) {
            actionMenuCheck.setChecked(true);
            actionMenuCheck.setVisibility(View.GONE);
            actionMenuText.setVisibility(View.GONE);
            hiding++;

            if (appPreferences.getBoolean(context.getString(R.string.pref_key_show_action_extra), false)) {
                extraMenuCheck.setVisibility(View.GONE);
                extraMenuText.setVisibility(View.GONE);
                hiding++;
            }
        }

        actionMenuCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                extraMenuCheck.setEnabled(b);
            }
        });

        extraMenuCheck.setEnabled(actionMenuCheck.isChecked());


        if (hiding<3) {

            new AlertDialog.Builder(context)
                    .setIcon(android.R.drawable.ic_menu_manage)
                    .setView(content)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor editor = appPreferences.edit();
                            if (hideCatsCheck.isChecked()) {
                                editor.putString(context.getString(R.string.pref_key_autohide_cats_timeout), "1500");
                            }
                            if (actionMenuCheck.isChecked()) {
                                editor.putBoolean(context.getString(R.string.pref_key_show_action_menus), true);
                                if (extraMenuCheck.isChecked()) {
                                    editor.putBoolean(context.getString(R.string.pref_key_show_action_extra), true);
                                }
                            }
                            if (resetColorsCheck.isChecked()) {
                                editor.putString(context.getString(R.string.pref_key_icons_pack), Theme.NEW_SYS);
                            }
                            editor.apply();

                            if (resetColorsCheck.isChecked()) {

                                IconsHandler ich = GlobState.getIconsHandler(context);
                                ich.getTheme().resetUserColors();
                            }

                        }

                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }
    }


    private static void enableFeatures(final Context context, boolean enable) {

        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        appPreferences.edit().putString(context.getString(R.string.pref_key_autohide_cats_timeout), enable?"1500":"-1").apply();
    }

}
