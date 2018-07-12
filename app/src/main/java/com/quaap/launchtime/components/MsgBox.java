package com.quaap.launchtime.components;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;

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


    public static void show(Context context, String title, String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);

        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    public static void showNewsMessage(final Context context, SharedPreferences prefs) {
        final int newsnum = 80;
        final int news = prefs.getInt("seennews", 0);
        if (news<newsnum) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    String msg = "";

                    msg += "" +
                            "In 0.8.0:\n" +
                            " * Autohide the menu!\n" +
                            " * Better menu appearence.\n" +
                            " * Animated transitions.\n" +
                            " * Turn off/on unread badges.\n" +
                            " * Better default apps in Quickbar.\n" +
                            " * Many appearance tweaks.\n" +
                            " * Some speed-ups.\n" +
                            " * Fix Oreo crash loading iconpacks.\n" +
                            " * Fix a few bugs and crashes.\n";

                    msg += "\n" +
                            "In 0.7.6:\n" +
                            " * Enable pinned shortcuts on Oreo (Android 8).\n" +
                            " * Fix unread counts on Oreo (Android 8).\n" +
                            " * Prevent a few rare crashes.\n" +
                            " * More apps categorized.\n";

                    msg += "\n" +
                            "In 0.7.5:\n" +
                            " * More apps categorized.\n" +
                            " * Updated German translations.\n" +
                            " * Recognize new apps installed on Oreo.\n";

                    msg += "\n" +
                            "In 0.7.4:\n" +
                            " * Fix crashes and other bugs.\n";

                    msg += "\n" +
                            "In 0.7.3:\n" +
                            " * Better widgets support.\n" +
                            " * Swipe left and right to switch categories.\n" +
                            " * Better large screen/tablet support.\n" +
                            " * Fixes and speedups.\n" +
                            "";

                    msg += "\n" +
                            "In 0.7.2:\n" +
                            " * Beta \"unread\" badges on certain apps.\n" +
                            " * Minor fixes.\n" +
                            "";

                    msg += "\n" +
                            "In 0.7.1:\n" +
                            " * Updated French translations.\n" +
                            " * Fixes for a few rare crashes.\n";

                    msg += "\n" +
                            "In 0.7.0:\n" +
                            " * Icon packs.\n" +
                            " * Customize icons and labels.\n" +
                            " * Built-in themes.\n" +
                            " * Backups save customization.\n" +
                            " * Better app shortcuts.\n" +
                            " * Better color selector.\n" +
                            " * Android 7.1 shortcut actions.\n" +
                            " * Machine translations for German, French, Spanish, and others (expert translations wanted!).";

                    msg += "\n\n" + "Go to Settings->Help for links to submit feature requests, bugs, and pull requests.";
                    show(context, "What's new!", msg);

                }
            }, 3500);
            prefs.edit().putInt("seennews", newsnum).apply();
        }

    }
}
