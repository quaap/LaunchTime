package com.quaap.launchtime;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.quaap.launchtime.components.AppShortcut;
import com.quaap.launchtime.db.DB;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {


    private ScrollView mIconSheetScroller;

    private Map<String,GridLayout> mIconSheets;
    private Map<String,TextView> mCategoryTabs;

    private String mCategory;

    private GridLayout mQuickRow;

    LinearLayout mCategoriesLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        AppShortcut.init(this);

        mCategoriesLayout = (LinearLayout)findViewById(R.id.layout_categories);
        mIconSheetScroller = (ScrollView)findViewById(R.id.layout_icons_scroller);
//        mIconSheet = (GridLayout) findViewById(R.id.layout_icons);
//        mIconSheet.setColumnCount(3);

        mIconSheets = new TreeMap<>();
        mCategoryTabs = new TreeMap<>();

        loadApplications();

        switchCategory(mCategory);
    }


    private void switchCategory(String category) {
        for(TextView cat: mCategoryTabs.values()) {
            cat.setBackgroundColor(Color.TRANSPARENT);
        }

        mIconSheetScroller.removeAllViews();
        mIconSheetScroller.addView(mIconSheets.get(category));
        mCategoryTabs.get(category).setBackgroundColor(Color.argb(127,127,127,250));
    }

    protected DB getDB() {
        return ((GlobState)this.getApplicationContext()).getDB();
    }

    private void loadApplications() {
        DB db = getDB();

        List<String> dbpkgnames = db.getAppPkgNames();

        final PackageManager pm = getApplicationContext().getPackageManager();

        // Set MAIN and LAUNCHER filters, so we only get activities with that defined on their manifest
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        // Get all activities that have those filters
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);

        Map<String, List<AppShortcut>> shortcuts = new LinkedHashMap<>();

        List<String> pmpkgnames = new ArrayList<>();

        for (int i = 0; i < activities.size(); i++) {

            AppShortcut app;

            ResolveInfo ri = activities.get(i);
            String pkgname = ri.activityInfo.packageName;
            pmpkgnames.add(pkgname);

            if (dbpkgnames.contains(pkgname)) {
                app = db.getApp(pkgname);
                app.loadAppIconAsync(pm);
            } else {
                app = new AppShortcut(pm, ri);
                db.addApp(app);
            }
            List<AppShortcut> catapps= shortcuts.get(app.getCategory());
            if (catapps==null) {
                catapps = new ArrayList<>();
                shortcuts.put(app.getCategory(), catapps);
            }
            catapps.add(app);

        }

        //remove shortcuts if they are not in the system
        for (Iterator<String> it=dbpkgnames.iterator(); it.hasNext();) {
            String dbpkg = it.next();
            if (!pmpkgnames.contains(dbpkg)) {
                it.remove();
                //db.remove(dbpkg);
            }
        }


        for (final String category: db.getCategories()) {

            if (mCategory==null) mCategory = category;

            GridLayout iconSheet = new GridLayout(this);
            mIconSheets.put(category, iconSheet);

            iconSheet.setColumnCount(3);

            TextView categoryTab = new TextView(this);
            categoryTab.setText(category);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.weight = 1;
            lp.gravity = Gravity.CENTER;
            categoryTab.setLayoutParams(lp);

            categoryTab.setTextSize(16);
            categoryTab.setPadding(6,24,2,24);

            categoryTab.setClickable(true);
            categoryTab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switchCategory(category);
                }
            });

            mCategoryTabs.put(category, categoryTab);
            mCategoriesLayout.addView(categoryTab);

            List<AppShortcut> catapps = shortcuts.get(category);
            Collections.sort(catapps);

            for (final AppShortcut app : catapps) {


                ViewGroup item = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.shortcut_icon, (ViewGroup) null);

                item.setClickable(true);

                item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent i = pm.getLaunchIntentForPackage(app.getPackageName());
                        MainActivity.this.startActivity(i);
                    }
                });


                ImageView iconImage = (ImageView) item.findViewById(R.id.shortcut_icon);

                app.setIconImage(iconImage);

                TextView iconLabel = (TextView) item.findViewById(R.id.shortcut_text);
                iconLabel.setText(app.getLabel());

                iconSheet.addView(item);
            }
        }
    }




}
