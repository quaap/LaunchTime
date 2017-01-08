package com.quaap.launchtime;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.quaap.launchtime.components.AppShortcut;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    GridLayout mIconSheet;
    GridLayout mQuickRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        mIconSheet = (GridLayout) findViewById(R.id.layout_icons);
        mIconSheet.setColumnCount(3);
        loadApplications();
    }


    private void loadApplications() {


        PackageManager pm = getApplicationContext().getPackageManager();

        // Set MAIN and LAUNCHER filters, so we only get activities with that defined on their manifest
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        // Get all activities that have those filters
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);

        List<AppShortcut> shortcuts = new ArrayList<>();

        for (int i = 0; i < activities.size(); i++) {
            ResolveInfo ri = activities.get(i);
            String pkgname = ri.activityInfo.packageName;


            shortcuts.add(new AppShortcut(pm, ri));

        }

        Collections.sort(shortcuts);


        for (AppShortcut app: shortcuts) {


            ViewGroup item = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.shortcut_icon, (ViewGroup) null);

            item.setTag(app.getPackageName());

            ImageView iconImage = (ImageView)item.findViewById(R.id.shortcut_icon);

            app.setIconImage(iconImage);

            TextView iconLabel = (TextView)item.findViewById(R.id.shortcut_text);
            iconLabel.setText(app.getLabel());

            mIconSheet.addView(item);
        }
    }


    public static void loadAppIconAsync(final PackageManager pm, final String pkgname, final ImageView im ){

        // Create an async task
        AsyncTask<Void,Void,Drawable> loadAppIconTask = new AsyncTask<Void, Void, Drawable>() {

            // Keep track of all the exceptions
            private Exception exception = null;


            @Override
            protected Drawable doInBackground(Void... voids) {
                // load the icon
                Drawable app_icon = null;
                try {
                    app_icon = pm.getApplicationIcon(pkgname);

                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    exception = e;
                }

                return app_icon;
            }

            @Override
            protected void onPostExecute(Drawable app_icon){
                if (exception == null) {
                    im.setImageDrawable(app_icon);

                } else {
                    Log.d("loadAppIconAsync", "ERROR Could not load app icon.");

                }
            }
        };

        loadAppIconTask.execute(null,null,null);
    }
}
