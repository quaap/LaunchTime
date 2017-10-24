package com.quaap.launchtime_official;

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
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.widget.TextView;


public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);


        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;

            TextView txtappname = (TextView) findViewById(R.id.txtappname);
            txtappname.setText(getString(R.string.app_name) + " " + version);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        TextView txtAbout = (TextView) findViewById(R.id.txtAbout);
        txtAbout.setMovementMethod(LinkMovementMethod.getInstance());
        if (Build.VERSION.SDK_INT >= 24) {
            txtAbout.setText(Html.fromHtml(getString(R.string.about_app), 0));
        } else {
            txtAbout.setText(Html.fromHtml(getString(R.string.about_app)));

        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        finish();
        //super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_HOME) {
            Intent home = new Intent(this, MainActivity.class);
            startActivity(home);
            finish();
        } else if (keyCode==KeyEvent.KEYCODE_MENU) {
            Intent sett = new Intent(this, SettingsActivity.class);
            startActivity(sett);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

}
