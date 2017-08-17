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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.quaap.launchtime_official.components.HttpUtils;

import java.util.HashMap;

public class CrashReportActivity extends Activity {
    String error;
    String appname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_report);


        appname = getString(R.string.app_name);

        TextView errText = (TextView)findViewById(R.id.err_report_text);

        error = getIntent().getStringExtra("error");


        String appname = getString(R.string.app_name);
        String version = "-";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        error = appname + " " + version + "\n" + error;

        errText.setText(error);

        final Button sendIt =(Button)findViewById(R.id.err_report_btn);

        sendIt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendIt.setEnabled(false);

                AsyncTask<Void, Void, String> sendItAsync = new AsyncTask<Void, Void, String>() {

                    @Override
                    protected String doInBackground(Void... voids) {
                        return sendData();
                    }

                    @Override
                    protected void onPostExecute(String message) {
                        Toast.makeText(CrashReportActivity.this, "Thanks!", Toast.LENGTH_SHORT).show();
                        endItAll();
                    }
                };
                sendItAsync.execute();
            }
        });

        Button cancelIt =(Button)findViewById(R.id.err_cancel_btn);
        cancelIt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                endItAll();
            }
        });

        findViewById(R.id.btn_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent settings = new Intent(CrashReportActivity.this, SettingsActivity.class);
                settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                settings.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                startActivity(settings);
                finish();
            }
        });
    }

    private void endItAll() {
        final Button sendIt =(Button)findViewById(R.id.err_report_btn);
        sendIt.postDelayed(new Runnable() {
            @Override
            public void run() {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        }, 2000);
    }

    private String sendData() {
        String requestURL =  getString(R.string.feedback_url);
        HashMap<String, String> postDataParams = new HashMap<>();

        String comment = ((EditText)findViewById(R.id.err_user_comment)).getText().toString();
        if (comment.length()>0) {
            comment = "\ncomment:" +  Base64.encodeToString(comment.getBytes(),Base64.DEFAULT) + "\n";
        }
        postDataParams.put("app",appname);
        postDataParams.put("data", error + comment);

        String response = HttpUtils.sendPostData(requestURL, postDataParams);

        Log.d("err-report" , response);

        return response;
    }
}
