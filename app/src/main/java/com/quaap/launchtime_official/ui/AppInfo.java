package com.quaap.launchtime_official.ui;
/*
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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.quaap.launchtime_official.MainActivity;
import com.quaap.launchtime_official.R;
import com.quaap.launchtime_official.apps.AppLauncher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

class AppInfo {

    public static PopupWindow showAppinfo(final MainActivity main, View view, AppLauncher appitem) {
        if (appitem == null) return null;

        ViewGroup item = (ViewGroup) LayoutInflater.from(main).inflate(R.layout.appinfo_view, null);

        final TextView itemDetails = item.findViewById(R.id.appinfo_data);
        TextView itemText = item.findViewById(R.id.appinfo_name);
        ImageView icon = item.findViewById(R.id.appinfo_icon);

        final String packagename = appitem.getPackageName();
        StringBuilder data = new StringBuilder(1024);
        try {

            PackageInfo pi = main.getPackageManager().getPackageInfo(packagename, PackageManager.GET_META_DATA | PackageManager.GET_PERMISSIONS | PackageManager.GET_CONFIGURATIONS);

            String name = pi.applicationInfo.loadLabel(main.getPackageManager()).toString();
            itemText.setText(name);

            icon.setImageDrawable(pi.applicationInfo.loadIcon(main.getPackageManager()));

            data
                    .append("Name: ")
                    .append(name)
                    .append("\n")
                    .append("PackageName: ")
                    .append(packagename)
                    .append("\n")
                    .append("VersionName: ")
                    .append(pi.versionName)
                    .append("\n");

            data.append("VersionCode: ");
            if (Build.VERSION.SDK_INT >= 28) {
                data.append(pi.getLongVersionCode());
            } else {
                data.append(pi.versionCode);
            }
            data.append("\n");

            data
                    .append("Enabled: ")
                    .append(pi.applicationInfo.enabled)
                    .append("\n")
                    .append("Installed: ")
                    .append(new Date(pi.firstInstallTime))
                    .append("\n")
                    .append("Updated: ")
                    .append(new Date(pi.lastUpdateTime))
                    .append("\n\n");

            if (Build.VERSION.SDK_INT >= 26) {
                data.append("Category: ");
                data.append(getCategoryFromPiCat(pi.applicationInfo));
                data.append("\n");
            }

            data.append("Flags: ");
            if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE)
                data.append("debug, ");
            if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)
                data.append("system app, ");
            if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)
                data.append("updated system app, ");
            if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_FACTORY_TEST) == ApplicationInfo.FLAG_FACTORY_TEST)
                data.append("factory test, ");
            if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_TEST_ONLY) == ApplicationInfo.FLAG_TEST_ONLY)
                data.append("test only, ");
            if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_HARDWARE_ACCELERATED) == ApplicationInfo.FLAG_HARDWARE_ACCELERATED)
                data.append("hardware accel, ");
            if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_IS_DATA_ONLY) == ApplicationInfo.FLAG_IS_DATA_ONLY)
                data.append("is data only, ");
            if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_LARGE_HEAP) == ApplicationInfo.FLAG_LARGE_HEAP)
                data.append("large heap, ");
            if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_PERSISTENT) == ApplicationInfo.FLAG_PERSISTENT)
                data.append("persistent, ");
            if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_VM_SAFE_MODE) == ApplicationInfo.FLAG_VM_SAFE_MODE)
                data.append("needs safe mode");

            if (data.charAt(data.length()-2) == ',') {
                data.deleteCharAt(data.length()-2);
            }

            data.append("\n");

            data.append("\n").append("Requested features:\n");
            int fcount = 0;
            if (pi.reqFeatures!=null) {
                for (FeatureInfo fi: pi.reqFeatures) {
                    if (fi!=null) {
                        fcount++;
                        data.append(" ");
                        if (fi.name!=null) data.append(fi.name).append(" ");
                        if ((fi.flags & FeatureInfo.FLAG_REQUIRED) == FeatureInfo.FLAG_REQUIRED) {
                            data.append("Required");
                        }

                        if (fi.reqGlEsVersion!=0) {
                            data.append(" / GL ES ver: ").append(fi.reqGlEsVersion);
                        }

                        data.append("\n");
                    }
                }
            }
            data.append(fcount).append(" total\n");

            data.append("\n").append("Granted permissions:\n");
            int gcount = 0;
            if (pi.requestedPermissions != null) {
                for (int index = 0; index < pi.requestedPermissions.length; index++) {
                    if ((pi.requestedPermissionsFlags[index] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                        String perm = pi.requestedPermissions[index];
                        if (perm != null) {
                            gcount++;
                            data.append(" ").append(perm).append("\n");
                        }
                    }
                }
            }
            data.append(gcount).append(" total\n");


            data.append("\n").append("Not granted permissions:\n");
            int dcount = 0;
            if (pi.requestedPermissions != null) {
                for (int index = 0; index < pi.requestedPermissions.length; index++) {
                    if ((pi.requestedPermissionsFlags[index] & PackageInfo.REQUESTED_PERMISSION_GRANTED) == 0) {
                        String perm = pi.requestedPermissions[index];
                        if (perm!=null) {
                            dcount++;
                            data.append(" ").append(perm).append("\n");
                        }
                    }
                }
            }

            data.append(dcount).append(" total\n");

        } catch (Exception e) {
            data.append("\n").append(e.getMessage()).append("\n");
            StringWriter stack = new StringWriter(1000);
            e.printStackTrace(new PrintWriter(stack));
            data.append(stack.toString());
        }

        itemDetails.setText(data);


        Button ok = item.findViewById(R.id.appinfo_ok);

        final PopupWindow pw = new PopupWindow(item, (int)main.getResources().getDimension(R.dimen.appinfo_width), main.mScreenDim.y - 100);
        pw.setOutsideTouchable(false);
        pw.setFocusable(true);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pw.dismiss();
            }
        });


        Button copy = item.findViewById(R.id.appinfo_copy);

        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) main.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard == null) return;
                ClipData clip = ClipData.newPlainText("Appinfo for " + packagename, itemDetails.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(main, "Copied", Toast.LENGTH_SHORT).show();
            }
        });


        Button settings = item.findViewById(R.id.appinfo_settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    //Open the specific App Info page:
                    main.openInSettings(packagename);
                    pw.dismiss();
                } catch (Throwable t) {
                    Log.e("AppInfo", t.getMessage(), t);
                }

            }
        });

        pw.showAtLocation(main.findViewById(R.id.icon_and_cat_wrap), Gravity.CENTER, 0, 25);

        return pw;
    }

    private static String getCategoryFromPiCat(ApplicationInfo appinfo) {
        String cat = null;
        if (Build.VERSION.SDK_INT >= 26) {
            int category = appinfo.category;
            switch (category) {
                case ApplicationInfo.CATEGORY_AUDIO:
                    cat = "AUDIO";
                    break;
                case ApplicationInfo.CATEGORY_GAME:
                    cat = "GAME";
                    break;
                case ApplicationInfo.CATEGORY_IMAGE:
                    cat = "IMAGE";
                    break;
                case ApplicationInfo.CATEGORY_MAPS:
                    cat = "MAPS";
                    break;
                case ApplicationInfo.CATEGORY_NEWS:
                    cat = "NEWS";
                    break;
                case ApplicationInfo.CATEGORY_PRODUCTIVITY:
                    cat = "PRODUCTIVITY";
                    break;
                case ApplicationInfo.CATEGORY_SOCIAL:
                    cat = "SOCIAL";
                    break;
                case ApplicationInfo.CATEGORY_VIDEO:
                    cat = "VIDEO";
                    break;
                case ApplicationInfo.CATEGORY_UNDEFINED:
                    cat = "UNDEFINED";
                    break;
                default:
                    cat = null;
                    break;
            }
        } else if ((appinfo.flags & ApplicationInfo.FLAG_IS_GAME) == ApplicationInfo.FLAG_IS_GAME) {
            cat = "GAMES";
        }
        return cat;
    }
}
