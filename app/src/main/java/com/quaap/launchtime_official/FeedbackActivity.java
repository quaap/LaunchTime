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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.quaap.launchtime_official.apps.AppLauncher;
import com.quaap.launchtime_official.components.ExceptionHandler;
import com.quaap.launchtime_official.components.HttpUtils;
import com.quaap.launchtime_official.ui.QuickRow;
import com.quaap.launchtime_official.db.DB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FeedbackActivity extends Activity {

    private final LinkedHashMap<String,String> scrubbednames = new LinkedHashMap<>();
    private final Map<String,Boolean> includes = new HashMap<>();
    private final List<AppLauncher> apps = new ArrayList<>();
    private final Map<String,AppLauncher> appMap = new HashMap<>();
    private String version;
    private String appname;
    private ListView itemsList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (GlobState.enableCrashReporter && !BuildConfig.DEBUG) Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        
        setContentView(R.layout.activity_feedback);

        itemsList = findViewById(R.id.info_data_items);

        appname = getString(R.string.app_name);
        version = "0";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            loadData();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        Button sendIt = findViewById(R.id.info_send);

        sendIt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int count = 0;
                for (String key: includes.keySet()) {
                    if (includes.get(key)) count++;
                }

                if (count>0) {
                    AsyncTask<Void, Void, String> sendItAsync = new AsyncTask<Void, Void, String>() {

                        @Override
                        protected String doInBackground(Void... voids) {
                            return sendData();
                        }

                        @Override
                        protected void onPostExecute(String message) {
                            Toast.makeText(FeedbackActivity.this, message, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    };
                    sendItAsync.execute();
                } else {
                    Toast.makeText(FeedbackActivity.this, "Must select something, otherwise there's no point in sending!", Toast.LENGTH_LONG).show();
                }
            }
        });

        ImageView selectNone = findViewById(R.id.info_select_none);
        selectNone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (String key: includes.keySet()) {
                    includes.put(key, false);
                }

                Log.d("Feedback", "selectNone");
                populateItems();

            }
        });

        ImageView selectAll = findViewById(R.id.info_select_all);
        selectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (String key: includes.keySet()) {
                    includes.put(key, true);
                }

                Log.d("Feedback", "selectAll");
                populateItems();
            }
        });

    }

    @Override
    public void onBackPressed() {
        finish();
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

    private String makeCompname(ComponentName componentName) {
        return componentName.getPackageName() + "/" + componentName.getClassName();
    }

    private void loadData() {


        TextView txtappname = findViewById(R.id.info_app_name);
        txtappname.setText(appname);

        TextView txtappver = findViewById(R.id.info_app_version);
        txtappver.setText(version);


        DB db = GlobState.getGlobState(this).getDB();

        List<ComponentName> actnames = db.getAppNames();
        Collections.sort(actnames);

        for (ComponentName componentName: actnames) {
            AppLauncher app = db.getApp(componentName);
            if (app==null) continue;
            apps.add(app);

            String activityname = makeCompname(componentName);

            appMap.put(activityname,app);

            int count = db.getAppLaunchedCount(componentName);

            String scrubbed;
            if (app.isActionLink() || app.isLink()) {
                String uri = app.getLinkUri();
                if (uri!=null && uri.length()>6) {
                    uri = uri.substring(0,6) + "." + uri.hashCode();
                }
                scrubbed = app.getLinkBaseActivityName() + "." + count + "/" + uri;
                //activityname = app.getLinkBaseActivityName() + "/" + uri;
            } else {
                scrubbed = activityname + "." + count;
            }
            
            scrubbednames.put(activityname, scrubbed);
            includes.put(scrubbed, true);

        }

        for (String catid: db.getCategories()) {
            String cat = "cat." + catid + "." + db.getCategoryDisplay(catid) + "." + db.isTinyCategory(catid);
            scrubbednames.put(cat, cat);
            includes.put(cat, true);
        }

        for (ComponentName componentName: db.getAppCategoryOrder(QuickRow.QUICK_ROW_CAT)) {
            String name = "qr." + scrubbednames.get(makeCompname(componentName));
            scrubbednames.put(name, name);
            includes.put(name, true);
        }

        try {
            for (Map.Entry<String, String> ent : GlobState.getIconsHandler(this).getTheme().getUserSetts().entrySet()) {
                String name = "set." + ent.getKey() + "." + ent.getValue();
                scrubbednames.put(name, name);
                includes.put(name, true);
            }

            String sdk = "SDK." + Build.VERSION.SDK_INT;
            scrubbednames.put(sdk, sdk);
            includes.put(sdk, true);

            String rel = "RELEASE." + Build.VERSION.RELEASE;
            scrubbednames.put(rel, rel);
            includes.put(rel, true);

        } catch (Exception e) {
            Log.e("Feedback", e.getMessage(), e);
        }


        populateItems();

    }

    private void populateItems() {
        itemsList.setAdapter(new PackageAdapter(this, new ArrayList<>(scrubbednames.keySet())));
    }


    private String sendData() {
        StringBuffer sb = buildSendData();
        //Log.d("SendData", sb.toString());

        String requestURL =  getString(R.string.feedback_url);
        HashMap<String, String> postDataParams = new HashMap<>();

        postDataParams.put("app",appname);
        postDataParams.put("data", sb.toString());

        String response = HttpUtils.sendPostData(requestURL, postDataParams);

        Log.d("SendData", response);

        String message;
        if (response.trim().equals("1")) {
            message = getString(R.string.sent_success);
        } else {
            message = getString(R.string.sent_failed) + response;
        }
        return message;
    }

    @NonNull
    private StringBuffer buildSendData() {
        StringBuffer sb = new StringBuffer(16000);
        sb.append(appname);
        sb.append(": ");
        sb.append(version);
        sb.append("\n");
        String comment = ((EditText)findViewById(R.id.info_user_message)).getText().toString();
        if (comment.length()>0) {
            sb.append("comment:");
            sb.append(Base64.encodeToString(comment.getBytes(),Base64.DEFAULT));
            //sb.append(comment);
            sb.append("\n");
        }
        sb.append("BEGIN APP DATA\n");
        for (String actvname: scrubbednames.keySet()) {
            String scrubbed = scrubbednames.get(actvname);
            Boolean checked = includes.get(scrubbed);
            if (checked==null || checked){
                sb.append(scrubbed);
                AppLauncher app = appMap.get(actvname);
                if (app!=null) {
                    sb.append(":");
                    sb.append(app.getCategory());
                }
                sb.append("\n");
            }
        }
        sb.append("END APP DATA\n");
        return sb;
    }




    class PackageAdapter extends ArrayAdapter<String> {

        PackageAdapter(Context context, List<String> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            // Get the data item for this position
            String activityname = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.line_item, parent, false);
            }

            CheckBox includeit = convertView.findViewById(R.id.info_include);
            TextView pcknameview = convertView.findViewById(R.id.item_text);
            TextView catnameview = convertView.findViewById(R.id.item_cat);
            final AppLauncher app = appMap.get(activityname);
            if (app!=null) {
                catnameview.setText(app.getCategory());
            } else {
                catnameview.setText("na");
            }

            String scrubbed = scrubbednames.get(activityname);

            pcknameview.setText(scrubbed);


            includeit.setOnCheckedChangeListener(null);
            Boolean checked = includes.get(scrubbed);

            includeit.setChecked(checked);

            final String activityname2 = scrubbed;
            includeit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    includes.put(activityname2, b);
                }
            });

            return convertView;
        }
    }
}
