package com.quaap.launchtime;

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
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.quaap.launchtime.components.AppCursorAdapter;
import com.quaap.launchtime.components.AppShortcut;
import com.quaap.launchtime.components.Categories;
import com.quaap.launchtime.components.ExceptionHandler;
import com.quaap.launchtime.components.InteractiveScrollView;
import com.quaap.launchtime.components.StaticListView;
import com.quaap.launchtime.db.DB;
import com.quaap.launchtime.widgets.Widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MainActivity extends Activity implements
        View.OnLongClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    //TODO: everything needs a major refactor.
    // custom views or fragments?

    public static final String QUICK_ROW_CAT = "QuickRow";
    private static final int UNINSTALL_RESULT = 3454;

    private FrameLayout mIconSheetTopFrame;
    private InteractiveScrollView mIconSheetScroller;
    private ViewGroup mIconSheetBottomFrame;
    private ViewGroup mIconSheetHolder;
    private Map<String, GridLayout> mIconSheets;
    private GridLayout mIconSheet;
    private ScrollView mCategoriesScroller;
    private Map<String, TextView> mCategoryTabs;
    private Map<View, String> mRevCategoryMap;
    private volatile String mCategory;
    private GridLayout mQuickRow;
    private HorizontalScrollView mQuickRowScroller;
    private ImageView mShowButtons;


    private View mSortCategoryButton;
    private View mAddCategoryButton;
    private View mRenameCategoryButton;
    private View mEditWidgetsButton;
    private View mOpenPrefsButton;


    private LinearLayout mCategoriesLayout;
    private TextView mRemoveAppText;
    private FrameLayout mRemoveDropzone;
    private FrameLayout mLinkDropzone;
    private FrameLayout mLinkDropzonePeek;
    private PackageManager mPackageMan;
    private AppShortcut mBeingDragged;
    private volatile ViewGroup mDragDropSource;
    private SharedPreferences mPrefs;
    private View mBeingUninstalled;
    private Widget mWidgetHelper;
    private ViewGroup mSearchView;

    private int cattabTextColor;
    private int cattabTextColorInvert;
    private int cattabBackground;
    private int cattabSelectedBackground;
    private int dragoverBackground;
    private int textColor;
    private int backgroundDefault = Color.TRANSPARENT;
    private Animation itemClickedAnim;

    private float categoryTabFontSize = 16;
    private int categoryTabPaddingHeight = 16;
    private int mColumns = 3;

    private boolean leftHandCategories;

    private Point mScreenDim;

    private SharedPreferences mAppPreferences;

    private Map<String, AppWidgetHostView> mLoadedWidgets = new HashMap<>();
    public Map<AppShortcut,ViewGroup> mAppShortcutViews = new HashMap<>();

    private boolean mChildLock;
    private boolean mChildLockSetup;

    //private DB db();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("LaunchTime", "onCreate");

        if (!BuildConfig.DEBUG) Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

        setContentView(R.layout.activity_main);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // test this here in case db is reopened by something later.
        boolean isFirstRun = GlobState.getGlobState(this).getDB().isFirstRun();

        //Setup some of our globals utils

        mPackageMan = getApplicationContext().getPackageManager();
        mAppPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mAppPreferences.registerOnSharedPreferenceChangeListener(this);
        mWidgetHelper = new Widget(this);


        mScreenDim = getScreenDimensions();

        //Load resources and init the form members
        setColors();
        initUI();


        mQuickRow.setOnDragListener(mMainDragListener);
        mQuickRowScroller.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                return mMainDragListener.onDrag(mQuickRow, dragEvent);
            }
        });
        mIconSheetHolder.setOnDragListener(iconSheetDropRedirector);

        findViewById(R.id.iconarea_wrap).setOnDragListener(iconSheetDropRedirector);


        mSearchView = getSearchView();
        mPrefs = getSharedPreferences("default", MODE_PRIVATE);
        mCategory = mPrefs.getString("category", getTopCategory());

        readPrefs();

        // get all the apps installed and process them
        loadApplications();

    }



    //All db access is routed through here.
    // We don't store the connection, because the connection might end up closed.
    private DB db() {
        return GlobState.getGlobState(this).getDB();
    }

    private View.OnDragListener iconSheetDropRedirector = new View.OnDragListener() {
        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            return mMainDragListener.onDrag(mIconSheet, dragEvent);
        }
    };

    //screen rotation, etc.
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mScreenDim = getScreenDimensions();
        checkConfig();
    }

    @Override
    protected void onPause() {
        Log.d("LaunchTime", "onPause");
        mBackPressedSessionCount=0;
        checkChildLock();

        //save a few items
        mPrefs.edit()
                .putInt("scrollpos" + mCategory, mIconSheetScroller.getScrollY())
                .putString("category", mCategory)
                .putLong("pausetime", System.currentTimeMillis())
                .apply();

        //close our search cursor, if needed
        if (mSearchAdapter!=null){
            mSearchAdapter.close();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("LaunchTime", "onResume");

        //Check how long we've been gone
        long pausetime = mPrefs.getLong("pausetime", -1);
        int homesetting = Integer.parseInt(mAppPreferences.getString("pref_return_home", "9999999"));

        //We go "home" if it's been longer than the timeout
        boolean skiphome = false;
        if (pausetime>-1 && System.currentTimeMillis() - pausetime > homesetting*1000 && !mChildLock) {
            mCategory = getTopCategory();
            skiphome = true;
            mQuickRowScroller.smoothScrollTo(0, 0);
        } else {
            mCategory = mPrefs.getString("category", getTopCategory());
        }

        // If the category has been deleted, pick a known-good category
        if (mCategory==null || db().getCategoryDisplay(mCategory)==null) {
            mCategory = Categories.CAT_TALK;
        }
        switchCategory(mCategory);

        if (!skiphome) {
            //move the page to the right scroll position
            mIconSheetScroller.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mIconSheetScroller.scrollTo(0, mPrefs.getInt("scrollpos" + mCategory, 0));

                }
            }, 100);
        }

        //rerun our query if needed
        if (mCategory.equals(Categories.CAT_SEARCH)) {
            refreshSearch(false);
        }
        hideRemoveDropzone();

        //lock things up if it was in toddler mode
        checkChildLock();
    }

    private String getTopCategory() {
        String category = db().getCategories().get(0);
        // If the category has been deleted, pick a known-good category
        if (category==null || db().getCategoryDisplay(category)==null) {
            category = Categories.CAT_TALK;
        }
        return category;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("debug", "A preference has been changed: " +  key);

        if (key!=null) {
            //Delete our icon cache so the labels can be regenerated.
            if (key.equals("textcolor")) {
                mAppShortcutViews.clear();
            }
            checkConfig();
            switchCategory(mCategory);

            if (key.equals("prefs_toddler_lock")) {
                mChildLock = sharedPreferences.getBoolean("prefs_toddler_lock", false);
                if (mChildLock) mChildLockSetup = false;
                checkChildLock();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        checkChildLock();
    }

    @Override
    public void onDestroy() {
        mAppPreferences.unregisterOnSharedPreferenceChangeListener(this);
        mWidgetHelper.done();
        super.onDestroy();
    }



    private synchronized void switchCategory(String category) {
        try {
            if (category == null) return;
            mCategory = category;

            //make sure selected category is in the database.
            if (db().getCategoryDisplay(mCategory) == null) {
                mCategory = getTopCategory();
            }

            //switch all category tabs to their default style and text
            for (TextView catTab : mCategoryTabs.values()) {
                styleCategorySpecial(catTab, CategoryTabStyle.Default);
                catTab.setText(db().getCategoryDisplay(mRevCategoryMap.get(catTab)));
            }

            //change the selected tab to the full label name
            TextView catTab = mCategoryTabs.get(mCategory);
            catTab.setText(db().getCategoryDisplayFull(mCategory));
            catTab.setVisibility(View.VISIBLE);

            mIconSheet = mIconSheets.get(mCategory);

            //Check the screen rotation and changes column count, if needed
            checkConfig();

            //refresh icons on page
            repopulateIconSheet(mCategory);

            //the top frame holds the search zone, but only on the search page.
            mIconSheetTopFrame.removeAllViews();
            if (mCategory.equals(Categories.CAT_SEARCH)) {

                mIconSheetTopFrame.addView(mSearchView);

                //Show recent apps
                populateRecentApps();

                //load our cursor
                refreshSearch(true);

            } else {

                // not the search page: close the cursor
                if (mSearchAdapter != null) {
                    mSearchAdapter.close();
                }
            }

            //Actually switch the icon sheet.
            mIconSheetHolder.removeAllViews();
            mIconSheetHolder.addView(mIconSheet);

            showButtonBar(false, true);
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "SwitchCat", e);
        }
    }


    private int mBackPressedSessionCount;
    @Override
    public void onBackPressed() {
        //back does nothign if in toddler mode
        if (mChildLock) {
            if (++mBackPressedSessionCount==17) {
                deactivateChildLock();
            }
            return;
        }



        String topCat = getTopCategory();
        if (mIconSheetBottomFrame.getVisibility()==View.VISIBLE) {
            showButtonBar(false, true);
        } else if (mQuickRowScroller.getScrollX()>0) {
            mQuickRowScroller.smoothScrollTo(0, 0);
        } else if (mIconSheetScroller.getScrollY()>0) {
            //Otherwise, scroll to top
            mIconSheetScroller.smoothScrollTo(0, 0);
        } else if (mCategory.equals(Categories.CAT_SEARCH) && mSearchbox!=null && mSearchbox.getText().length()!=0) {
            //If search is open, clear the searchbox
            mSearchbox.setText("");
        } else if (!mCategory.equals(topCat)){
            //Otherwise, switch to known-good category
            switchCategory(topCat);
            mCategoriesScroller.smoothScrollTo(0, 0);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

       // Log.d("LaunchTime", keyCode + "");
        if (!mChildLock &&keyCode == KeyEvent.KEYCODE_MENU) {
            openSettings();
        }
        return super.onKeyDown(keyCode, event);
    }


    //Catch home key press
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (Intent.ACTION_MAIN.equals(intent.getAction())) {

            final boolean alreadyOnHome =
                    ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                            != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
           // Log.d("LaunchTime", " new intent " + alreadyOnHome);
            if (alreadyOnHome && !mChildLock) {

                // If we are on home screen, reset most things and go to top category.
                mQuickRowScroller.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (mSearchbox!=null) mSearchbox.setText("");
                            showButtonBar(false, true);
                            mQuickRowScroller.smoothScrollTo(0, 0);
                            switchCategory(getTopCategory());
                            mCategoriesScroller.smoothScrollTo(0, 0);
                            mIconSheetScroller.smoothScrollTo(0, 0);
                        } catch (Exception e) {
                            Log.e("LaunchTime", e.getMessage(), e);
                        }

                    }
                }, 100);
            }

        }
    }

    private void readPrefs() {

        //Checks application preferences and adjust accordingly
        try {

            leftHandCategories = mAppPreferences.getString("pref_categories_loc", "right").equals("left");
            mChildLock = mAppPreferences.getBoolean("prefs_toddler_lock", false);

            int tabsizePref = Integer.parseInt(mAppPreferences.getString("preference_tabsize", "1"));
            switch (tabsizePref) {
                case 0:  //small
                    categoryTabPaddingHeight = 12;
                    categoryTabFontSize = 14;
                    break;
                case 1:  //medium
                    categoryTabPaddingHeight = 16;
                    categoryTabFontSize = 16;
                    break;
                case 2:  //large
                    categoryTabPaddingHeight = 20;
                    categoryTabFontSize = 18;
                    break;
                case 3: //x-large
                    categoryTabPaddingHeight = 24;
                    categoryTabFontSize = 20;
                    break;
            }

            int orientationPref = Integer.parseInt(mAppPreferences.getString("preference_orientation", "0"));

            switch (orientationPref) {
                case 0:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    break;
                case 1:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    break;
                case 2:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    break;
                case 3:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    break;

            }

        } catch (Exception e) {
            Log.e("Launch", e.getMessage(), e);
        }
    }


    //This is run on switchcategory and screen rotation, etc.
    // Checks global preferences and
    //  if we need to change the column count, etc
    private void checkConfig() {
        readPrefs();
        setColors();
        try {

            mScreenDim = getScreenDimensions();
            float shortcutw = getResources().getDimension(R.dimen.shortcut_width);
            float catwidth = getResources().getDimension(R.dimen.cattabbar_width);

            mColumns = (int)((mScreenDim.x - catwidth)/(shortcutw + 2));

            if (mIconSheet.getColumnCount() != mColumns) {
                changeColumnCount(mIconSheet, mColumns);
            }

            mShowButtons.setBackgroundColor(cattabBackground);
            mShowButtons.setMinimumHeight(categoryTabPaddingHeight*3);
            //mShowButtons.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, categoryTabPaddingHeight*3));
            //mShowButtons.setPadding(2,categoryTabPaddingHeight,2,4);


            //Switch the menu left/right
            ViewGroup wrap = (ViewGroup)findViewById(R.id.icon_and_cat_wrap);
            View cats = findViewById(R.id.category_tabs_wrap);
            boolean isleft = wrap.getChildAt(0) == cats;
            if (leftHandCategories) {
                if (!isleft) {
                    wrap.removeView(cats);
                    wrap.addView(cats, 0);
                }
            } else {
                if (isleft) {
                    wrap.removeView(cats);
                    wrap.addView(cats);
                }
            }

        } catch (Exception e) {
            Log.e("Launch", e.getMessage(), e);
        }
    }



    //Run/open the thing that was clicked
    public void launchApp(String activityname, String pkgname) {
        launchApp(db().getApp(new ComponentName(pkgname, activityname)));
    }

    public void launchApp(final AppShortcut app) {
        String activityname = app.getLinkBaseActivityName();
        String packagename = app.getPackageName();
        String uristr = null;
        Uri uri = null;
        boolean isapplink = false;
        try {
            if (app.isLink()) {
                uristr = app.getLinkUri();
                if (uristr!=null) {
                    uri = Uri.parse(uristr);
                    if (app.isAppLink()) {
                        uri = null;
                        isapplink = true;
                    }
                }

            }
            Log.d("Launch", app.getActivityName() + " " + app.getPackageName() + " " + uristr);
            Intent intent;
            // is Link is a shortcut?
            if (app.isActionLink()) {
                //Change "CALL" to "DIAL" so we can avoid needing the
                // android.permission.CALL_PHONE permission
                if (activityname.startsWith("android.intent.action.CALL")) {
                    activityname = "android.intent.action.DIAL";
                }
                //build an activity-specific intent with the uri
                intent = new Intent(activityname, uri);
            } else {
                //regualt activity, start with MAIN
                if (uristr == null) {
                    intent = new Intent(Intent.ACTION_MAIN);
                } else {
                    intent = new Intent(Intent.ACTION_MAIN, uri);
                }
                intent.setClassName(packagename, activityname);
            }

            //needed to place in the open apps list
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // actually start it
            startActivity(intent);
            //log the launch
            if (isapplink) {
                db().appLaunched(new ComponentName(app.getPackageName(), app.getLinkBaseActivityName()));
            } else {
                db().appLaunched(app.getComponentName());
            }
        } catch (Exception e) {
            Log.d("Launch", "Could not launch " + activityname, e);
            Toast.makeText(this, "Could not launch item: " + e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
        }
        showButtonBar(false, true);

    }


    // runs at create time to read all apps and add them to our db, if not there already
    private void loadApplications() {

        //create the grids for each existing category.

        for (final String category : db().getCategories()) {

            createIconSheet(category);
        }

        if (!db().isFirstRun()) {
            //Make sure the displayed icons load first
            //Load the quickrow icons first
            for (ComponentName actvname : db().getAppCategoryOrder(QUICK_ROW_CAT)) {
                if (db().isAppInstalled(actvname)) {
                    AppShortcut app = db().getApp(actvname);
                    if (app != null) {
                        app.loadAppIconAsync(this, mPackageMan);
                    }
                }
            }

            //Load the selected category icons
            for (ComponentName actvname : db().getAppCategoryOrder(mCategory)) {
                if (db().isAppInstalled(actvname)) {
                    AppShortcut app = db().getApp(actvname);
                    if (app != null) {
                        app.loadAppIconAsync(this, mPackageMan);
                    }
                }
            }
        }

        //Look for new apps
        //final List<AppShortcut> shortcuts = processActivities();

        AsyncTask<Void, Void, List<AppShortcut>> task = new AsyncTask<Void, Void, List<AppShortcut>>() {
            @Override
            protected List<AppShortcut> doInBackground(Void... params) {
                return processActivities();
            }

            @Override
            protected void onPostExecute(List<AppShortcut> appShortcuts) {
                processQuickApps(appShortcuts);
                firstRunPostApps();
                if (mCategory.equals(Categories.CAT_SEARCH)) {
                    populateRecentApps();
                } else {
                    repopulateIconSheet(mCategory);
                }
            }
        };

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        //loads the quickrow or adds default apps if it is empty
       // processQuickApps(shortcuts);


    }


    private void firstRunPostApps() {
        if (db().isFirstRun()) {
            String selfAct = this.getPackageName() + "." + this.getClass().getSimpleName();
            Log.d(this.getClass().getSimpleName(), "My name is " + selfAct);

            //Move self icon to hidden
            db().updateAppCategory(selfAct, Categories.CAT_HIDDEN);

            //Take a backup no that things are pre-sorted.
            db().backup("After install");

            //Show the help screen on very first run.
            mQuickRow.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent help = new Intent(MainActivity.this, AboutActivity.class);
                    help.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(help);
                }
            }, 5000);
        }
    }


    @NonNull
    private GridLayout createIconSheet(String category) {
        final GridLayout iconSheet = new GridLayout(MainActivity.this);
        mIconSheets.put(category, iconSheet);
        mRevCategoryMap.put(iconSheet, category);
        iconSheet.setColumnCount(mColumns);
        iconSheet.setOnDragListener(mMainDragListener);

        final TextView categoryTab = createCategoryTab(category, iconSheet);


        mCategoryTabs.put(category, categoryTab);
        mRevCategoryMap.put(categoryTab, category);
        return iconSheet;
    }

    private void populateRecentApps() {

        GridLayout iconSheet = mIconSheets.get(Categories.CAT_SEARCH);

        iconSheet.removeAllViews();

        int i=0;
        for (ComponentName actvname : db().getAppLaunchedList()) {
            if (db().isAppInstalled(actvname)) {
                AppShortcut app = db().getApp(actvname);
                //Log.d("Recent", "Trying " + actvname + " " + app);

                addAppToIconSheet(iconSheet, app, false);
                if (i++ > 60) break;
            }
        }
    }

    private void repopulateIconSheet(String category) {
        GridLayout iconSheet = mIconSheets.get(category);

        iconSheet.removeAllViews();

        final List<ComponentName> apporder = db().getAppCategoryOrder(category);
        List<AppShortcut> apps = db().getApps(category);

        for (ComponentName actvname : apporder) {
            //Log.d("app", "repopulateIconSheet " + category + " " +  actvname.getClassName() + " " +  actvname.getPackageName());
            for (Iterator<AppShortcut> it = apps.iterator(); it.hasNext(); ) {
                AppShortcut app = it.next();
                if (actvname.equals(app.getComponentName())) {
                    addAppToIconSheet(iconSheet, app, true);
                    it.remove();
                }
            }
        }

        for (AppShortcut app : apps) {
            addAppToIconSheet(iconSheet, app, true);
           // Log.d("order", app.getActivityName());
        }

        if (apps.size()>0) {
            db().setAppCategoryOrder(category, iconSheet);
        }
    }

    private boolean addAppToIconSheet(GridLayout iconSheet, AppShortcut app, boolean reuse) {
        return addAppToIconSheet(iconSheet, app, -1, reuse);
    }

    private boolean addAppToIconSheet(GridLayout iconSheet, AppShortcut app, int pos, boolean reuse) {
        if (app != null) {
            try {
                if (isAppInstalled(app.getPackageName())) {
                    ViewGroup item = getShortcutView(app, false, reuse);
                    if (item != null) {
                        if (!app.iconLoaded()) {
                            app.loadAppIconAsync(this, mPackageMan);
                        }
                        ViewGroup parent = (ViewGroup) item.getParent();
                        if (parent != null) parent.removeView(item);
                        GridLayout.LayoutParams lp = getAppShortcutLayoutParams(iconSheet, app);
                        iconSheet.addView(item, pos, lp);
                        return true;
                    }
                } //else {
                //Log.d("LaunchTime", "Not showing recent " + app.getPackageName() + " " + app.getActivityName() + ": Not installed.");
                //}
            } catch (Exception e) {
                Log.e("LaunchTime", "exception adding icon to sheet", e);
                Toast.makeText(this,"Couldn't place icon: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d("LaunchTime", "Not showing recent: Null.");
        }
        return false;
    }

    @NonNull
    private GridLayout.LayoutParams getAppShortcutLayoutParams(GridLayout grid, AppShortcut app) {

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();

        int w = getShortCutWidth(app);
        int h = getShortCutHeight(app);

        if (w>0 || h>0) {
            float sw = getResources().getDimension(R.dimen.shortcut_width);
            //float sh = getResources().getDimension(R.dimen.shortcut_height);

            //int width = (int)(sw + 20) * mColumns;

            float cellwidth = sw;
            float cellheight = cellwidth + 5;  // ~square cells


            int wcells = (int) Math.ceil(w / cellwidth);
            if (wcells > 1) {
                int start = GridLayout.UNDEFINED;
                if (wcells > grid.getColumnCount()) {
                    wcells = grid.getColumnCount();
                }
                if (wcells > 1) start = 0;
                lp.columnSpec = GridLayout.spec(start, wcells, GridLayout.FILL);



                Log.d("widcol", "w=" + w + " wcells=" + wcells + " start=" + start + " cellwidth=" + cellwidth + " r=" + cellwidth * wcells);
            }
            int hcells = (int) Math.ceil(h / cellheight);
            if (hcells > 1) {
                lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, hcells, GridLayout.FILL);
            }

            final AppWidgetHostView appwid = mLoadedWidgets.get(app.getActivityName());

            if (appwid != null) {



                //appwid.updateAppWidgetSize(null, (int) sw, (int) sh, (int) (cellwidth * wcells), (int) (cellheight * hcells));
                final int wDp = pxToDip(cellwidth*wcells);
                final int hDp = pxToDip(cellheight*hcells);
                lp.width = (int)(cellwidth*wcells);
                lp.height = (int)(cellheight*hcells);

                Log.d("widcol2", "w=" + w + " wcells=" + wcells  + " cellwidth=" + cellwidth + " r=" + cellwidth * wcells);
                appwid.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        appwid.updateAppWidgetSize(null, wDp, hDp, wDp, hDp);
                        if (appwid.getParent()!=null) {
                            appwid.getParent().requestLayout();
                        }
                        appwid.requestLayout();
                    }
                }, 1000);

            }
        }

        return lp;
    }

    public int pxToDip(float pixel){
        float scale = getResources().getDisplayMetrics().density;
        return (int)((pixel - 0.5f)/scale);
    }

    public void changeColumnCount(GridLayout gridLayout, int columnCount) {
        if (gridLayout.getColumnCount() != columnCount) {

            List<View> childViews = new ArrayList<>();

            for (int i = gridLayout.getChildCount()-1; i >=0 ; i--) {
                View view = gridLayout.getChildAt(i);
                if (view == null) {
                    Log.d("gridrelayout", "null child at " + i);
                    continue;
                }
                childViews.add(view);
                gridLayout.removeView(view);
            }


            gridLayout.setColumnCount(columnCount);
            Collections.reverse(childViews);

            for (View view: childViews) {
                GridLayout.LayoutParams lp;
                if (view.getTag() instanceof AppShortcut) {
                    AppShortcut app = (AppShortcut) view.getTag();

                    lp = getAppShortcutLayoutParams(gridLayout, app);
                } else {
                    lp = new GridLayout.LayoutParams();
                }
                view.setLayoutParams(lp);

                gridLayout.addView(view);
            }
        }
    }

//
//    private void listAll() {
//        Intent intent = new Intent(Intent.ACTION_MAIN, null);
//
//
//        // Get all activities that have those filters
//        List<ResolveInfo> activities = mPackageMan.queryIntentActivities(intent, 0);
//
//
//        for (int i = 0; i < activities.size(); i++) {
//
//            AppShortcut app;
//
//            ResolveInfo resolveInfo = activities.get(i);
//            ActivityInfo activityInfo = resolveInfo.activityInfo;
//            String name = activityInfo.name;
//            String packageName = activityInfo.applicationInfo.packageName;
//            String className = activityInfo.applicationInfo.className;
//            String label = resolveInfo.loadLabel(mPackageMan).toString();
//            boolean enabled = activityInfo.enabled;
//            boolean exported = activityInfo.exported;
//
//        }
//    }

    private List<AppShortcut> processActivities() {
        final List<AppShortcut> shortcuts = new ArrayList<>();

        List<ComponentName> dbactvnames = db().getAppNames();

        Set<ComponentName> pmactvnames = new HashSet<>();
        List<AppShortcut> newapps = new ArrayList<>();

        // Set MAIN and LAUNCHER filters, so we only get activities with that defined on their manifest
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        // Get all activities that have those filters
        List<ResolveInfo> activities = mPackageMan.queryIntentActivities(intent, 0);


        for (int i = 0; i < activities.size(); i++) {

            AppShortcut app;

            ResolveInfo ri = activities.get(i);
            String actvname = ri.activityInfo.name;
            ComponentName appcn = new ComponentName(ri.activityInfo.packageName, actvname);

            if (!pmactvnames.contains(appcn)) {
                pmactvnames.add(appcn);

                app = db().getApp(appcn);

                if (dbactvnames.contains(appcn) && app != null) {
                    app.loadAppIconAsync(this, mPackageMan);
                  //  Log.d("app", "app was in db " + actvname + " " +  ri.activityInfo.packageName);
                } else {
                  //  Log.d("app", "app was not in db " + actvname + " " +  ri.activityInfo.packageName);
                    app = AppShortcut.createAppShortcut(this, mPackageMan, ri);
                    newapps.add(app);
                }

                shortcuts.add(app);


            }

        }

        //remove shortcuts if they are not in the system
        for (Iterator<ComponentName> it = dbactvnames.iterator(); it.hasNext(); ) {
            ComponentName dbactv = it.next();
            if (!pmactvnames.contains(dbactv)) {
                AppShortcut app = db().getApp(dbactv);
                if (!isAppInstalled(app.getPackageName())) {  //might be a widget, check packagename
                    Log.d("Launch", "Removing " + dbactv);
                    it.remove();
                    db().deleteApp(dbactv);
                   // removeFromQuickApps(dbactv);
                }
            }
        }

        db().addApps(newapps);

        return shortcuts;
    }

    private void processQuickApps(List<AppShortcut> shortcuts) {
        List<AppShortcut> quickRowApps = new ArrayList<>();
        final List<ComponentName> quickRowOrder = db().getAppCategoryOrder(QUICK_ROW_CAT);

        MainHelper.checkDefaultApps(this, shortcuts, quickRowOrder, mQuickRow);


        for (AppShortcut app : shortcuts) {

            if (quickRowOrder.contains(app.getComponentName())) {
                AppShortcut qapp = AppShortcut.createAppShortcut(app);
                qapp.loadAppIconAsync(this, mPackageMan);
                quickRowApps.add(qapp);
            }
        }


        mQuickRow.removeAllViews();
        for (ComponentName actvname : quickRowOrder) {
            for (AppShortcut app : quickRowApps) {
                if (app.getComponentName().equals(actvname)) {
                    ViewGroup item = getShortcutView(app, true);
                    if (item!=null) {
                        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.TOP);
                        mQuickRow.addView(item, lp);
                    }
                }
            }
        }
        db().setAppCategoryOrder(mRevCategoryMap.get(mQuickRow), mQuickRow);

    }

    private void removeFromQuickApps(ComponentName actvname) {
        for (int i = mQuickRow.getChildCount()-1; i>=0; i--) {
            AppShortcut app = (AppShortcut) mQuickRow.getChildAt(i).getTag();
            if (app != null && actvname.equals(app.getComponentName())) {
                mQuickRow.removeView(mQuickRow.getChildAt(i));
            }
        }
    }


    public ViewGroup getShortcutView(final AppShortcut app, boolean smallIcon) {
        return getShortcutView(app, smallIcon, true);
    }



    public ViewGroup getShortcutView(final AppShortcut app, boolean smallIcon, boolean reuse) {


        if (smallIcon) reuse = false;
        ViewGroup item = mAppShortcutViews.get(app);
        if (reuse) {
            if (item!=null) return item;
        }

        if (app.isWidget()) {
            item = new FrameLayout(this);

            AppWidgetHostView appwid = mLoadedWidgets.get(app.getActivityName());
            if (appwid == null) {
                appwid = mWidgetHelper.loadWidget(app);
                if (appwid==null) {
                    Log.d("Widget2", "AppWidgetHostView was null for " + app.getActivityName() + " " + app.getPackageName());
                   // db().deleteApp(app.getActivityName());
                    return null;
                }
            }

            mLoadedWidgets.put(app.getActivityName(), appwid);
            AppWidgetProviderInfo pinfo = appwid.getAppWidgetInfo();
            Log.d("widsize", "Min: " + pinfo.minWidth + "," + pinfo.minHeight);
            Log.d("widsize", "MinResize: " + pinfo.minResizeWidth + "," + pinfo.minResizeHeight);
            Log.d("widsize", "Resizemode: " + pinfo.resizeMode);

            storeShortCutDimen(app, pinfo.minWidth, pinfo.minHeight);

            ViewGroup parent = (ViewGroup) appwid.getParent();
            if (parent != null) {
                parent.removeView(appwid);
            }
            item.addView(appwid);
            final View wrap = item;
            appwid.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (mChildLock) return false;
                    return MainActivity.this.onLongClick(wrap);
                }
            });
            appwid.setOnDragListener(new View.OnDragListener() {
                @Override
                public boolean onDrag(View view, DragEvent dragEvent) {
                    if (mChildLock) return false;
                    return mMainDragListener.onDrag(wrap, dragEvent);
                }
            });



        } else {


            item = (ViewGroup) LayoutInflater.from(this).inflate(smallIcon ? R.layout.shortcut_small_icon : R.layout.shortcut_icon, (ViewGroup) null);

            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    view.startAnimation(itemClickedAnim);
                    launchApp(app);
                }
            });

            ImageView iconImage = (ImageView) item.findViewById(R.id.shortcut_icon);
            app.setIconImage(iconImage);
            app.loadAppIconAsync(this,mPackageMan);

            if (!smallIcon) {
                TextView iconLabel = (TextView) item.findViewById(R.id.shortcut_text);
                iconLabel.setTextColor(textColor);
                iconLabel.setText(app.getLabel());
            }

        }
        item.setTag(app);
        item.setClickable(true);
        item.setOnLongClickListener(this);
        item.setOnDragListener(mMainDragListener);

        if (reuse) {
            mAppShortcutViews.put(app, item);
        }
        return item;
    }

    private void setupWidget() {
        mWidgetHelper.popupSelectWidget();
    }

    private void addWidget(AppWidgetHostView appwid) {
        if (mChildLock) return;

        ComponentName cn = appwid.getAppWidgetInfo().provider;
        String actvname = cn.getClassName();
        String pkgname = cn.getPackageName();

        String catId = db().getAppCategory(cn);
        if (catId == null || catId.equals(mCategory)) {

            Log.d("Widget", actvname + " " + pkgname);

            mLoadedWidgets.put(actvname, appwid);

            String label = pkgname;

            AppShortcut.removeAppShortcut(cn);
            AppShortcut app = AppShortcut.createAppShortcut(actvname, pkgname, label, mCategory, true);

            db().addApp(app);
            db().addAppCategoryOrder(mCategory, app.getComponentName());
        } else {
            Toast.makeText(this, getString(R.string.widget_alreay,db().getCategoryDisplay(catId)), Toast.LENGTH_LONG).show();
        }


    }

    private void storeShortCutDimen(AppShortcut app, int width, int height) {
        SharedPreferences.Editor ePrefs = mPrefs.edit();

        ePrefs.putInt(app.getActivityName() + "_width", width);

        ePrefs.putInt(app.getActivityName() + "_height", height);

        ePrefs.apply();

    }

    private int getShortCutWidth(AppShortcut app) {
        return mPrefs.getInt(app.getActivityName() + "_width", 0);
    }

    private int getShortCutHeight(AppShortcut app) {
        return mPrefs.getInt(app.getActivityName() + "_height", 0);
    }

    private int mSearchRememberScrollPos;

    AppCursorAdapter mSearchAdapter;
    EditText mSearchbox;
    private ViewGroup getSearchView() {
        ViewGroup searchView = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.search_layout, (ViewGroup) null);

        mSearchbox = (EditText) searchView.findViewById(R.id.search_box);

        mSearchAdapter = new AppCursorAdapter(this, mSearchbox, R.layout.search_item, 0);
        StaticListView list = (StaticListView) searchView.findViewById(R.id.search_dropdownarea);

        list.setAdapter(mSearchAdapter);
        list.setOnItemClickListener(mSearchAdapter);

        list.setOnLoadCompleteListener(new StaticListView.OnLoadCompleteListener() {
            @Override
            public void loadComplete() {
                mIconSheetScroller.scrollTo(0, mSearchRememberScrollPos);
                mIconSheetScroller.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mIconSheetScroller.scrollTo(0, mSearchRememberScrollPos);

                    }
                }, 100);
            }
        });



        searchView.findViewById(R.id.btn_clear_searchbox).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSearchbox.setText("");
                mSearchRememberScrollPos = 0;
            }
        });


        searchView.findViewById(R.id.btn_clear_recents).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.clear_recent)
                        .setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                db().deleteAppLaunchedRecords();
                                populateRecentApps();
                            }
                        }).setNegativeButton(R.string.cancel, null);
                builder.show();

            }
        });

        refreshSearch(false);
        return searchView;
    }

    private void refreshSearch(boolean rememberPos) {
        if (rememberPos) mSearchRememberScrollPos = mIconSheetScroller.getScrollY();
        if (mSearchAdapter!=null) {
            mSearchAdapter.refreshCursor();
        }
    }

    private CategoryTabStyle getDefaultCategoryStyle(String category) {
        CategoryTabStyle catstyle = CategoryTabStyle.Normal;

        if (category.equals(mCategory)) {
            catstyle = CategoryTabStyle.Selected;
        } else if (db().isTinyCategory(category)) {
            catstyle = CategoryTabStyle.Tiny;
        }
        return catstyle;
    }

    private void styleCategorySpecial(TextView categoryTab, CategoryTabStyle catstyle) {
        styleCategorySpecial(categoryTab, catstyle, mRevCategoryMap.get(categoryTab));
    }


    private void styleCategorySpecial(TextView categoryTab, CategoryTabStyle catstyle, String category) {

        if (catstyle == CategoryTabStyle.Default) {
            catstyle = getDefaultCategoryStyle(category);
        }

        switch (catstyle) {
            case Tiny:
                categoryTab.setPadding(6, categoryTabPaddingHeight/6, 2, categoryTabPaddingHeight/6);
                categoryTab.setBackgroundColor(cattabBackground);
                categoryTab.setTextSize(categoryTabFontSize-3);
                categoryTab.setShadowLayer(0, 0, 0, 0);
                break;
            case DragHover:
                categoryTab.setPadding(6, categoryTabPaddingHeight, 2, categoryTabPaddingHeight);
                categoryTab.setBackgroundColor(dragoverBackground);
                categoryTab.setTextSize(categoryTabFontSize);
                categoryTab.setShadowLayer(0, 0, 0, 0);
                break;
            case Selected:
                categoryTab.setPadding(6, categoryTabPaddingHeight, 2, categoryTabPaddingHeight);
                categoryTab.setBackgroundColor(cattabSelectedBackground);
                categoryTab.setTextSize(categoryTabFontSize);
                categoryTab.setShadowLayer(8, 4, 4, cattabTextColorInvert);
                break;
            case Normal:
            default:
                categoryTab.setPadding(6, categoryTabPaddingHeight, 2, categoryTabPaddingHeight);
                categoryTab.setBackgroundColor(cattabBackground);
                categoryTab.setTextSize(categoryTabFontSize);
                categoryTab.setShadowLayer(0, 0, 0, 0);
        }
    }

    private TextView createCategoryTab(final String category, final GridLayout iconSheet) {
        final TextView categoryTab = new TextView(this);
        categoryTab.setText(db().getCategoryDisplay(category));
        categoryTab.setTag(category);
        // categoryTab.setWidth((int)Utils.dpToPx(this,categoryTabWidth));

        categoryTab.setTextColor(cattabTextColor);
        categoryTab.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        final CategoryTabStyle catstyle = getDefaultCategoryStyle(category);

        if (Categories.isHiddenCategory(category)) {
            categoryTab.setVisibility(View.GONE);
        }

        if (catstyle == CategoryTabStyle.Normal) {
            lp.weight = 1;
        }
        styleCategorySpecial(categoryTab, CategoryTabStyle.Default, category);
        lp.gravity = Gravity.CENTER;
        lp.setMargins(2, 3, 2, 3);
        categoryTab.setLayoutParams(lp);

        categoryTab.setGravity(Gravity.CENTER);


        categoryTab.setClickable(true);
        categoryTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mChildLock) return;
               // view.startAnimation(itemClickedAnim);
                switchCategory(category);

            }
        });
        categoryTab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mChildLock) return true;
                ClipData data = ClipData.newPlainText(category, category);
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                //view.startDrag(data, shadowBuilder, view, 0);

                boolean dragstarted;
                if (Build.VERSION.SDK_INT>=24) {
                    dragstarted = view.startDragAndDrop(data, shadowBuilder, view, 0);
                } else {
                    dragstarted = view.startDrag(data, shadowBuilder, view, 0);
                }

                if (dragstarted) {
                    mDragDropSource = mCategoriesLayout;
                    if (!Categories.isSpeacialCategory(category)) {
                        showRemoveDropzone();
                    }
                    showHiddenCategories();
                }




                return true;
            }
        });

        categoryTab.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View overView, final DragEvent event) {
                if (mChildLock) return true;
                View dragObj = (View) event.getLocalState();
                boolean isAppShortcut = dragObj.getTag() instanceof AppShortcut;
                boolean isSearch = category.equals(Categories.CAT_SEARCH);
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_ENTERED:
                        if (catstyle == CategoryTabStyle.Tiny || (!isAppShortcut || !isSearch)) {
                            styleCategorySpecial(categoryTab, CategoryTabStyle.DragHover);
                        }
                       // Log.d("LaunchTime", "DRAG_ENTERED: " + ((AppShortcut)dragObj.getTag()).getActivityName());

                        break;

                    case DragEvent.ACTION_DRAG_LOCATION:

                        scrollOnDrag(overView, event, mCategoriesScroller);
                        break;
                    case DragEvent.ACTION_DRAG_ENDED:
                        mBeingDragged = null;
                        hideRemoveDropzone();
                        hideHiddenCategories();
                    case DragEvent.ACTION_DRAG_EXITED:

                        styleCategorySpecial(categoryTab, CategoryTabStyle.Default);
                        break;

                    case DragEvent.ACTION_DROP:
                        if (isAppShortcut) {
                            if (!isSearch) {
                                mBeingDragged.setCategory(category);
                                db().updateAppCategory(mBeingDragged.getActivityName(), category);
                                mMainDragListener.onDrag(iconSheet, event);
                            }
                        } else {
                            ViewGroup container1 = (ViewGroup) overView.getParent();
                            ViewGroup container2 = (ViewGroup) dragObj.getParent();


                            int index = -1;
                            for (int i = 0; i < container1.getChildCount(); i++) {
                                if (container1.getChildAt(i) == overView) {
                                    index = i;
                                }
                            }
                            container2.removeView(dragObj);
                            if (index == -1) {
                                container1.addView(dragObj);
                            } else {
                                container1.addView(dragObj, index);
                            }
                            db().setCategoryOrder(container1);
                        }
                        break;
                }
                return true;
            }
        });
        mCategoriesLayout.addView(categoryTab);

        return categoryTab;
    }

    private long mDropZoneHover=0;
    private View.OnDragListener mMainDragListener = new View.OnDragListener() {
        @Override
        public boolean onDrag(View droppedOn, DragEvent event) {
            if (mChildLock) return false;

            View dragObj = (View) event.getLocalState();
            boolean isShortcut = true;
            boolean isSpecial = false;
            boolean isApplink = false;
            if (dragObj.getTag() == null || !(dragObj.getTag() instanceof AppShortcut )) {
                isShortcut = false;
            } else  {
                AppShortcut app = (AppShortcut)dragObj.getTag();
                isSpecial = app.isLink() || app.isWidget();
                isApplink = app.isAppLink();
            }

            if (mCategory.equals(Categories.CAT_SEARCH) && isAncestor(mIconSheet, droppedOn)) return false;

            boolean nocolor = droppedOn instanceof GridLayout || droppedOn == mRemoveDropzone
                    || droppedOn == mLinkDropzone || !isShortcut || mQuickRow == mDragDropSource
                    || isAncestor(mSearchView, droppedOn);

            //prevent dropping categories anywhere but category area and trash
            if (mDragDropSource==mCategoriesLayout && !(droppedOn==mCategoriesLayout || droppedOn==mRemoveDropzone )) {
                return false;
            }

            if ((isSpecial && !isApplink) && (droppedOn==mQuickRow || isAncestor(mQuickRow, droppedOn))) return false;

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // do nothing
                    break;

                case DragEvent.ACTION_DRAG_LOCATION:
                    //scroll the scrollview

                    if (isShortcut) {
                        scrollOnDrag(droppedOn, event, mIconSheetScroller);
                        hscrollOnDrag(droppedOn, event, mQuickRowScroller);

                        if (!isSpecial && !mCategory.equals(Categories.CAT_SEARCH) && mLinkDropzone.getVisibility()!=View.VISIBLE && (droppedOn==mRemoveDropzone || droppedOn==mLinkDropzonePeek) && System.currentTimeMillis()-mDropZoneHover > 400) {
                            mLinkDropzone.setVisibility(View.VISIBLE);
                            mLinkDropzonePeek.setVisibility(View.GONE);
                           // Log.d("LaunchTime", "mLinkDropzone.setVisibility(View.VISIBLE)");
                        }
                    }
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    if (!nocolor ) {
                        droppedOn.setBackgroundColor(dragoverBackground);
                    }
                    if (droppedOn==mRemoveDropzone || droppedOn==mLinkDropzonePeek) {
                        mDropZoneHover = System.currentTimeMillis();
                        //Log.d("LaunchTime", "DRAG_ENTERED: " + ((AppShortcut)dragObj.getTag()).getActivityName());
                    }
                    //Log.d("LaunchTime", "DRAG_ENTERED: " + ((AppShortcut)dragObj.getTag()).getActivityName());
                    break;
                case DragEvent.ACTION_DRAG_EXITED:

                    if (!nocolor) droppedOn.setBackgroundColor(backgroundDefault);
                    break;
                case DragEvent.ACTION_DROP:

                    //Log.d("dropon", droppedOn.toString());

                    if (!nocolor) droppedOn.setBackgroundColor(backgroundDefault);
                    // Dropped, reassign View to ViewGroup

                    if (dragObj == droppedOn) {
                        // Log.d("sort", "self drop");
                        break;
                    }

                    try {
                        if (handleDrop(droppedOn, dragObj, isShortcut)) return true;
                    } catch (Exception e) {
                        Log.e("LaunchTime", e.getMessage(), e);
                    }
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    if (!nocolor) droppedOn.setBackgroundColor(backgroundDefault);
                    mBeingDragged = null;
                    hideRemoveDropzone();
                    hideHiddenCategories();
                    break;
                default:
                    break;
            }
            return true;
        }

        private boolean handleDrop(View droppedOn, View dragObj, boolean isShortcut) {
            if (mChildLock) return false;

            ViewGroup target;
            Object droppedOnTag = droppedOn.getTag();
            if (droppedOn == mRemoveDropzone) {  // need to delete the dropped thing
                //Stuff to be deleted
                if (mQuickRow == mDragDropSource) {
                    removeDroppedItem(dragObj);
                } else if (mDragDropSource == mIconSheets.get(Categories.CAT_SEARCH)) {
                    removeDroppedRecentItem(dragObj);
                } else if (mBeingDragged != null && (mBeingDragged.isWidget() || mBeingDragged.isLink())) {
                    removeDroppedItem(dragObj);
                    refreshSearch(true);

                } else if (mDragDropSource == mCategoriesLayout && !isShortcut) {
                    //delete category tab
                    promptDeleteCategory((String) dragObj.getTag());

                } else {
                    //uninstall app
                    if (mBeingDragged!=null) {
                        mBeingUninstalled = dragObj;
                        launchUninstallIntent(mBeingDragged.getPackageName());
                    }
                }
                return true;
            } else if (droppedOn == mLinkDropzone) {
                if (isShortcut) {
                    AppShortcut app = (AppShortcut)dragObj.getTag();
                    Log.d("LaunchLink", "Making link: " + app.getActivityName() + " " + app.getPackageName());
                    AppShortcut appshortcut = app.makeAppLink();
                    appshortcut.setCategory(mCategory);
                    db().addApp(appshortcut);
                    repopulateIconSheet(mCategory);
                } else {
                    Log.d("LaunchLink", "non-shortcut dropped on linker: " + dragObj + " tag=" + dragObj.getTag());
                }
                return true;
            } else if (droppedOn instanceof GridLayout) {
                target = (GridLayout) droppedOn;
            } else if (droppedOn instanceof FrameLayout) {
                target = (FrameLayout) droppedOn;
            } else if (droppedOn.getParent() instanceof GridLayout){
                target = (GridLayout) droppedOn.getParent();
            } else {
                return true;
            }

            if (droppedOnTag!=null && droppedOnTag instanceof AppShortcut) {
                if (((AppShortcut)droppedOnTag).isWidget()) {
                    target = (GridLayout) droppedOn.getParent();
                }
            }


            //Find the drop position
            int index = -1;
            for (int i = 0; i < target.getChildCount(); i++) {
                if (target.getChildAt(i) == droppedOn) {
                    index = i;
                }
            }


            //remove icon from source?
            boolean remove = false;
            if (mDragDropSource == mQuickRow && mQuickRow == target) remove = true;

            if (!mCategory.equals(Categories.CAT_SEARCH)) {
                if (mQuickRow != mDragDropSource && mQuickRow != target) remove = true;
            }

            if (remove) {
                mDragDropSource.removeView(dragObj);
            } else {
                if (target == mQuickRow) {
                    if (mQuickRow != mDragDropSource) {
                        //prevent copies of the same app on the quickrow
                        for (int i = 0; i < mQuickRow.getChildCount(); i++) {
                            AppShortcut dragging = (AppShortcut) dragObj.getTag();
                            AppShortcut inbar = (AppShortcut) mQuickRow.getChildAt(i).getTag();
                            if (dragging.getLinkBaseActivityName().equals(inbar.getLinkBaseActivityName())) {
                                return true;
                            }
                        }
                    }
                    //make a copy of the shortcut to put on the quickbar
                    dragObj = getShortcutView(AppShortcut.createAppShortcut((AppShortcut) dragObj.getTag(), true), true);

                } else {
                    dragObj = getShortcutView(AppShortcut.createAppShortcut((AppShortcut) dragObj.getTag()), false, false);
                }
            }


            if (!(target != mQuickRow && mQuickRow == mDragDropSource)) {
                try {
                    ViewParent parent = dragObj.getParent();
                    if (parent!=null) {
                        Log.e("LaunchTime", "dragObj " + dragObj + " still has parent " + parent, new Throwable() );
                        ((ViewGroup)parent).removeView(dragObj);
                    }

                    ViewGroup.LayoutParams lp = null;
                    if (target instanceof GridLayout && dragObj.getTag() instanceof AppShortcut) {
                        lp = getAppShortcutLayoutParams((GridLayout)target, (AppShortcut)dragObj.getTag());
                    }

                    if (index == -1) {
                        target.addView(dragObj, lp);
                    } else {
                        target.addView(dragObj, index, lp);
                    }
                } catch (Exception e) {
                    Log.e("LaunchTime", "exception adding icon to sheet", e);
                    Toast.makeText(MainActivity.this,"Couldn't place icon: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            //save the new order
            db().setAppCategoryOrder(mRevCategoryMap.get(target), target);
            if (!target.equals(mDragDropSource)) {
                db().setAppCategoryOrder(mRevCategoryMap.get(mDragDropSource), mDragDropSource);
            }

            if (mCategory.equals(Categories.CAT_SEARCH)) {
                refreshSearch(true);
            }
            return false;
        }

        private void removeDroppedRecentItem(View dragObj) {
            if (mChildLock) return;

            try {
                db().deleteAppLaunchedRecord(mBeingDragged.getComponentName());
                mDragDropSource.removeView(dragObj);
            } catch (Exception e) {
                Log.e("LaunchTime", e.getMessage(), e);
            }
        }

        private void removeDroppedItem(View dragObj) {
            if (mChildLock) return;

            try {
                mDragDropSource.removeView(dragObj);
                db().setAppCategoryOrder(mRevCategoryMap.get(mDragDropSource), mDragDropSource);

                if (mBeingDragged.isLink()) {
                    db().deleteApp(mBeingDragged.getComponentName());
                }

                if (mBeingDragged.isWidget()) {

                    db().deleteApp(mBeingDragged.getComponentName());
                    AppWidgetHostView wid = mLoadedWidgets.remove(mBeingDragged.getActivityName());
                    if (wid != null) {
                        mWidgetHelper.widgetRemoved(wid.getAppWidgetId());
                    }
                }
            } catch (Exception e) {
                Log.e("LaunchTime", e.getMessage(), e);
            }
        }

    };

    private boolean isAncestor(ViewGroup potentialParent, View potentialChild) {

        if (potentialParent==potentialChild) return true; //self;

        ViewParent parent = potentialChild.getParent();

         do {

            if (parent == potentialParent) {
                return true;
            }
        } while (parent!=null && (parent = parent.getParent()) != null);
        return false;

    }

    private void scrollOnDrag(View view, DragEvent event, ScrollView scrollView) {
        float ty = view.getTop() + event.getY();

        if (isAncestor(scrollView, view)) {

            int thresh = scrollView.getHeight() / 6;

            if (ty < scrollView.getScrollY() + thresh) {
                scrollView.smoothScrollBy(0, -10);
            } else if (ty > scrollView.getScrollY() + scrollView.getHeight() - thresh) {
                scrollView.smoothScrollBy(0, 10);
            }
        }
    }

    private void hscrollOnDrag(View view, DragEvent event, HorizontalScrollView scrollView) {
        float tx = view.getLeft() + event.getX();

        if (isAncestor(scrollView, view)) {

            int thresh = scrollView.getWidth() / 6;

            if (tx < scrollView.getScrollX() + thresh) {
                scrollView.smoothScrollBy(-10, 0);
            } else if (tx > scrollView.getScrollX() + scrollView.getWidth() - thresh) {
                scrollView.smoothScrollBy(10,0);
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (mChildLock) return false;

        AppShortcut dragitem = (AppShortcut) view.getTag();
        String label = dragitem.getLabel();
        ClipData data = ClipData.newPlainText(label, label);
        View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);

        boolean dragstarted;
        if (Build.VERSION.SDK_INT>=24) {
            dragstarted = view.startDragAndDrop(data, shadowBuilder, view, 0);
        } else {
            dragstarted = view.startDrag(data, shadowBuilder, view, 0);
        }

        if (dragstarted) {
            mBeingDragged = dragitem;
            mDragDropSource = (ViewGroup) view.getParent();
            Log.d("LaunchTime", "Drag started: " + dragitem.getActivityName() +  ", source = " + mDragDropSource);
            showHiddenCategories();

           // Log.d("LaunchTime", "source = " + mDragDropSource);
            //if (mDragDropSource.getId()!=R.id.icontarget) {
                showRemoveDropzone();
            //}
            return true;
        }


        return false;
    }



    private void showHiddenCategories() {
        for (String cat: db().getCategories()) {
            mCategoryTabs.get(cat).setVisibility(View.VISIBLE);
        }
    }
    private void hideHiddenCategories() {

        if (mAppPreferences.getBoolean("pref_hide_empty_cat", false)) {
            for (String cat : db().getCategories()) {
                if (!cat.equals(Categories.CAT_SEARCH) && !mCategory.equals(cat) && db().getAppCount(cat) == 0) {
                    mCategoryTabs.get(cat).setVisibility(View.GONE);
                }
            }
        }
        for (String cat: Categories.CAT_HIDDENS) {
            if (mCategory.equals(cat) ) {
                mCategoryTabs.get(cat).setVisibility(View.VISIBLE);
            } else {
                mCategoryTabs.get(cat).setVisibility(View.GONE);
            }
        }
    }

    private void showRemoveDropzone() {
        if (mChildLock) return;

        showButtonBar(false, false);
        mRemoveDropzone.setVisibility(View.VISIBLE);
        mShowButtons.setVisibility(View.GONE);

        if (mDragDropSource == mQuickRow
            || mDragDropSource == mCategoriesLayout
            || mDragDropSource == mIconSheets.get(Categories.CAT_SEARCH)
            || (mBeingDragged!=null && (mBeingDragged.isWidget() || mBeingDragged.isLink())
        ) ) {
            mRemoveDropzone.setBackgroundColor(Color.YELLOW);
            //mRemoveAppText.setText(getString(R.string.remove_shortcut) + "\n\u267B");
            mRemoveAppText.setText(R.string.remove_shortcut);
            mRemoveAppText.setCompoundDrawablesWithIntrinsicBounds(0,0,0,R.drawable.recycle);
            mRemoveAppText.setTextColor(Color.BLACK);
            mLinkDropzonePeek.setVisibility(View.GONE);
        } else {
            mRemoveDropzone.setBackgroundColor(Color.RED);
           // mRemoveAppText.setText(getString(R.string.uninstall_app) + "\n" + new String(Character.toChars(0x1F5D1)));
            mRemoveAppText.setText(R.string.uninstall_app);
            mRemoveAppText.setCompoundDrawablesWithIntrinsicBounds(0,0,0,R.drawable.trash);
            //mRemoveAppText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.showy, 0,0,R.drawable.trash);
            mRemoveAppText.setTextColor(Color.WHITE);
            mLinkDropzonePeek.setVisibility(View.VISIBLE);
        }
    }

    private void hideRemoveDropzone() {
        mRemoveDropzone.setVisibility(View.GONE);
        mLinkDropzone.setVisibility(View.GONE);
        mLinkDropzonePeek.setVisibility(View.GONE);
        mShowButtons.setVisibility(View.VISIBLE);
    }

    private void launchUninstallIntent(String packageName) {
        if (mChildLock) return;

        Log.d("Launch", "Uninstalling " + packageName);
        Uri packageUri = Uri.parse("package:" + packageName);
        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
        uninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        startActivityForResult(uninstallIntent, UNINSTALL_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == UNINSTALL_RESULT) {
                switch (resultCode) {
                    case RESULT_OK:
                        ComponentName actvname = ((AppShortcut) mBeingUninstalled.getTag()).getComponentName();
                        db().deleteApp(actvname);
                        removeFromQuickApps(actvname);
                        AppShortcut.removeAppShortcut(actvname);
                        refreshSearch(true);
                        mDragDropSource.removeView(mBeingUninstalled);
                        db().setAppCategoryOrder(mRevCategoryMap.get(mDragDropSource), mDragDropSource);
                        Toast.makeText(this, R.string.app_was_uninstalled, Toast.LENGTH_SHORT).show();
                        break;
                    case RESULT_CANCELED:
                        Toast.makeText(this, R.string.uninstall_canceled, Toast.LENGTH_LONG).show();
                        break;
                    default:
                        Toast.makeText(this, R.string.could_not_uninstall, Toast.LENGTH_LONG).show();

                }
            } else {
                AppWidgetHostView appwid = mWidgetHelper.onActivityResult(requestCode, resultCode, data);
                if (appwid == null) {
                    Log.d("LaunchWidget2", "appwid is null.");
                    ComponentName cn = mWidgetHelper.getComponentNameFromIntent(data);
                    if (cn != null) {
                        Log.d("LaunchWidget2", "classname is " + cn.getClassName());
                        db().deleteApp(cn);
                    } else {
                        super.onActivityResult(requestCode, resultCode, data);
                    }
                } else {
                    addWidget(appwid);
                }
            }
        } catch (Exception e) {
            Log.e("LaunchTime", e.getMessage(), e);
        }
    }

    private boolean populateDeletedCategorySpinner(Spinner catDeletedSpinner, final EditText shortname, final EditText fullname) {
        final List<String> deldCats = new ArrayList<>();
        deldCats.add("");
        for (String cat: Categories.DefCategoryOrder) {
            String displayName = db().getCategoryDisplay(cat);
            if (displayName==null) {
                deldCats.add(cat);
            }
        }
        catDeletedSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,deldCats));
        catDeletedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i>0) {
                    shortname.setText(Categories.getCatLabel(MainActivity.this,deldCats.get(i)));
                    fullname.setText(Categories.getCatFullLabel(MainActivity.this,deldCats.get(i)));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        return deldCats.size()>1;
    }

    private void promptGetCategoryName(String title, String message, final String category, String defName,
                                       String defFullName, boolean defIsTiny, final CategoryChangerListener categoryChangerListener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        ViewGroup view = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.category_name, (ViewGroup) null);

        TextView messageView = (TextView) view.findViewById(R.id.message_txt);

        final TextView catDeletedLabel = (TextView) view.findViewById(R.id.cat_deleted_label);
        final Spinner catDeletedSpinner = (Spinner) view.findViewById(R.id.cat_deleted_spinner);

        final EditText shortname = (EditText) view.findViewById(R.id.shortname);
        final EditText fullname = (EditText) view.findViewById(R.id.fullname);
        final CheckBox isTiny = (CheckBox) view.findViewById(R.id.istiny_checkbox);

        int vis = View.GONE;
        if (category.length()==0 && populateDeletedCategorySpinner(catDeletedSpinner, shortname, fullname)) {
            vis = View.VISIBLE;
        }
        catDeletedLabel.setVisibility(vis);
        catDeletedSpinner.setVisibility(vis);


        shortname.setSelectAllOnFocus(true);
        fullname.setSelectAllOnFocus(true);

        messageView.setText(message);
        shortname.setText(defName);
        fullname.setText(defFullName);
        isTiny.setChecked(defIsTiny);

        builder.setView(view);

        builder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String delcat = (String)catDeletedSpinner.getSelectedItem();
                categoryChangerListener.onClick(dialog, which, (delcat!=null && delcat.length()>0?delcat:category), shortname.getText().toString(), fullname.getText().toString(), isTiny.isChecked());
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(shortname.getWindowToken(), 0);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(shortname.getWindowToken(), 0);
            }
        });


        builder.show();
    }


    ///////////
    // Category manipulation

    private void promptRenameCategory(final String category) {

        promptGetCategoryName(getString(R.string.rename_cat),
                getString(Categories.isSpeacialCategory(category)?R.string.rename_cat3:R.string.rename_cat2),
                category,
                db().getCategoryDisplay(category),
                db().getCategoryDisplayFull(category),
                db().isTinyCategory(category),
                new CategoryChangerListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, String category, String newDisplayName, String newDisplayFullName, boolean isTiny) {
                        try {
                            renameCategory(category, newDisplayName, newDisplayFullName, isTiny);
                        } catch (IllegalArgumentException e) {

                            Toast.makeText(MainActivity.this, R.string.need_name, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void renameCategory(String category, String newDisplayName, String newDisplayFullName, boolean isTiny) {
        newDisplayName = newDisplayName.trim();
        newDisplayFullName = newDisplayFullName.trim();

        if (newDisplayFullName.length() == 0) {
            newDisplayFullName = newDisplayName;
        }

        if (newDisplayName.length() < 1) {
            throw new IllegalArgumentException("Must give a name");
        }

        if (db().updateCategory(category, newDisplayName, newDisplayFullName, isTiny)) {

            TextView categoryTab = mCategoryTabs.get(category);
            if (category.equals(mCategory)) {
                categoryTab.setText(newDisplayFullName);
            } else {
                categoryTab.setText(newDisplayName);
            }
        } else {
            Toast.makeText(MainActivity.this, R.string.no_rename, Toast.LENGTH_SHORT).show();
        }

    }

    private void promptAddCategory() {

        promptGetCategoryName(getString(R.string.add_cat),
                getString(R.string.add_cat2),
                "",
                "",
                "",
                false,
                new CategoryChangerListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, String category, String newDisplayName, String newDisplayFullName, boolean isTiny) {
                        try {
                            addCategory(category, newDisplayName, newDisplayFullName, isTiny);
                        } catch (IllegalArgumentException e) {

                            Toast.makeText(MainActivity.this, R.string.need_name, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void addCategory(String category, String newDisplayName, String newDisplayFullName,  boolean isTiny) {
        category = category.trim();
        newDisplayName = newDisplayName.trim();
        newDisplayFullName = newDisplayFullName.trim();

        if (category.length() == 0) {
            category = newDisplayName;
        }
        if (newDisplayName.length() == 0) {
            category = newDisplayName;
        }

        if (newDisplayFullName.length() == 0) {
            newDisplayFullName = newDisplayName;
        }

        if (newDisplayName.length() < 1) {
            throw new IllegalArgumentException("Must give a name");
        }
        Log.d("AddCat", category +", " + newDisplayName +", " +  newDisplayFullName +", " +  isTiny);
        if (db().addCategory(category, newDisplayName, newDisplayFullName, isTiny)) {
            createIconSheet(category);

            switchCategory(category);
        } else {
            Toast.makeText(MainActivity.this, R.string.no_add_cat, Toast.LENGTH_SHORT).show();
        }
    }

    private void promptDeleteCategory(final String category) {

        final String message = getString(R.string.cat_deleted, db().getCategoryDisplay(Categories.CAT_OTHER));
        new AlertDialog.Builder(MainActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.delete_cat)
                .setMessage(R.string.delete_cat_prompt)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteCategory(category);
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    }

                })
                .setNegativeButton(R.string.cancel, null)
                .show();

    }

    private void deleteCategory(final String category) {
        TextView categoryTab = mCategoryTabs.get(category);

        if (db().deleteCategory(category)) {

            View iconSheet = mIconSheets.get(category);
            mRevCategoryMap.remove(iconSheet);


            mCategoryTabs.remove(category);
            mRevCategoryMap.remove(categoryTab);

            mCategoriesLayout.removeView(categoryTab);

            repopulateIconSheet(Categories.CAT_OTHER);
            //String newcat = mCategoryTabs.keySet().iterator().next();

            if (category.equals(mCategory)) {
                switchCategory(Categories.CAT_OTHER);
            }
            mCategoryTabs.get(Categories.CAT_OTHER).setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(MainActivity.this, R.string.no_delete_cat, Toast.LENGTH_SHORT).show();
        }
    }


    private static final int APPSORT_NONE = -1;
    private static final int APPSORT_LABEL = 0;
    private static final int APPSORT_USAGE = 1;
    private static final int APPSORT_INSTALL_REV = 2;
    private static final int APPSORT_INSTALL = 3;
    private static final int APPSORT_PACKAGE = 4;

    private void promptSortCategory(String category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.sort_prompt_title);

        builder.setItems(R.array.sort_strings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                sortCategory(mCategory, i);
                repopulateIconSheet(mCategory);

            }
        });

        builder.setNegativeButton(R.string.cancel, null);

        builder.show();
    }

    private void sortCategory(String category, final int sortby) {

        final List<ComponentName> recents = db().getAppLaunchedList();
        if (sortby != APPSORT_NONE) {
            List<AppShortcut> apps = db().getApps(category);
            Collections.sort(apps, new Comparator<AppShortcut>() {
                @Override
                public int compare(AppShortcut appfirst, AppShortcut appsecond) {
                    switch (sortby) {
                        case APPSORT_LABEL:
                            return appfirst.getLabel().compareToIgnoreCase(appsecond.getLabel());
                        case APPSORT_INSTALL_REV:
                            return getInstallTime(appsecond.getPackageName()).compareTo(getInstallTime(appfirst.getPackageName()));
                        case APPSORT_INSTALL:
                            return getInstallTime(appfirst.getPackageName()).compareTo(getInstallTime(appsecond.getPackageName()));
                        case APPSORT_USAGE:
                            int p1 = recents.indexOf(appfirst.getComponentName());
                            int p2 = recents.indexOf(appsecond.getComponentName());
                            if (p1==-1) p1 = Integer.MAX_VALUE;
                            if (p2==-1) p2 = Integer.MAX_VALUE;
                            int comp = ((Integer)p1).compareTo(p2);
                            if (comp==0) {
                                return appfirst.getLabel().compareToIgnoreCase(appsecond.getLabel());
                            }
                            return comp;
                        case APPSORT_PACKAGE:
                            return appfirst.getPackageName().compareToIgnoreCase(appsecond.getPackageName());

                    }
                    return 0;
                }
            });

            db().setAppCategoryOrder(category, apps);
        }

    }

    private Long getInstallTime(String packagename) {
        try {
            return getPackageManager()
                    .getPackageInfo(packagename, 0)
                    .firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            return -1L;
        }
    }

    private void initUI() {
        //mCategoriesScroller = (ScrollView) findViewById(R.id.layout_categories_scroller);
        mCategoriesLayout = (LinearLayout) findViewById(R.id.layout_categories);

        mIconSheetTopFrame = (FrameLayout) findViewById(R.id.layout_icons_topframe);
        mIconSheetScroller = (InteractiveScrollView) findViewById(R.id.layout_icons_scroller);

        mIconSheetHolder = (ViewGroup) findViewById(R.id.icon_sheet_holder);

        mIconSheetBottomFrame = (ViewGroup) findViewById(R.id.layout_icons_bottomframe);

        mRemoveDropzone = (FrameLayout) findViewById(R.id.remove_dropzone);
        mRemoveDropzone.setOnDragListener(mMainDragListener);
        mRemoveAppText = (TextView) findViewById(R.id.remove_dz_txt);

        mLinkDropzone = (FrameLayout) findViewById(R.id.link_dropzone);
        mLinkDropzone.setOnDragListener(mMainDragListener);

        mLinkDropzonePeek = (FrameLayout) findViewById(R.id.link_dropzone_peek);

        //((TextView) findViewById(R.id.link_dz_text)).setCompoundDrawablesWithIntrinsicBounds(0,0,0,R.drawable.linkicon);


        mCategoriesScroller = (ScrollView)findViewById(R.id.layout_categories_scroller);


        mQuickRow = (GridLayout) findViewById(R.id.layout_quickrow);

        mQuickRowScroller = (HorizontalScrollView) findViewById(R.id.layout_quickrow_scroll);


        mIconSheets = new TreeMap<>();
        mCategoryTabs = new TreeMap<>();
        mRevCategoryMap = new HashMap<>();
        mRevCategoryMap.put(mQuickRow, QUICK_ROW_CAT);

        mShowButtons = (ImageView) findViewById(R.id.settings_button);

        mShowButtons.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleButtonBar();
            }
        });

        mSortCategoryButton = findViewById(R.id.btn_sort_cat);
        mAddCategoryButton = findViewById(R.id.btn_add_cat);
        mRenameCategoryButton = findViewById(R.id.btn_rename_cat);

        mEditWidgetsButton = findViewById(R.id.btn_widgets);
        mOpenPrefsButton = findViewById(R.id.btn_prefs);


        mSortCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSortCategory(mCategory);
                showButtonBar(false, true);
            }
        });

        mRenameCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptRenameCategory(mCategory);
                showButtonBar(false, true);
            }
        });

        mAddCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptAddCategory();
                showButtonBar(false, true);
            }
        });


        mEditWidgetsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setupWidget();
                showButtonBar(false, true);
            }
        });

        mOpenPrefsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettings();
                showButtonBar(false, true);
            }
        });

    }

    public void openSettings() {
        Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(settingsIntent);
    }

    private void toggleButtonBar() {
        int vis = mIconSheetBottomFrame.getVisibility();
        showButtonBar(vis != View.VISIBLE, true);
    }


    //initialize the form members

    private void showButtonBar(boolean visible, boolean hideCats) {
        if (checkChildLock()) return;

        if (visible) {
            showHiddenCategories();
            if (mCategory.equals(Categories.CAT_SEARCH)) {
                mSortCategoryButton.setVisibility(View.INVISIBLE);
                mEditWidgetsButton.setVisibility(View.INVISIBLE);
            } else {
                mSortCategoryButton.setVisibility(View.VISIBLE);
                mEditWidgetsButton.setVisibility(View.VISIBLE);
            }
            mIconSheetBottomFrame.setVisibility(View.VISIBLE);
            mShowButtons.setImageResource(android.R.drawable.arrow_down_float);
        } else {
            if (hideCats) {hideHiddenCategories();}
            mIconSheetBottomFrame.setVisibility(View.GONE);
            mShowButtons.setImageResource(android.R.drawable.arrow_up_float);
        }
    }

    private String kidaccumecode = "";
    private String kidcode = "";

    private View.OnClickListener kidescape = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            view.startAnimation(itemClickedAnim);
            kidaccumecode += view.getTag();
            if (kidaccumecode.endsWith(kidcode)) {
                deactivateChildLock();
            } else if (kidaccumecode.length()>kidcode.length()) {
                kidaccumecode = kidaccumecode.substring(kidaccumecode.length()-1-kidcode.length());
            }
            mBackPressedSessionCount=0;
        }
    };

    private void deactivateChildLock() {
        mChildLock = false;
        mAppPreferences.edit().putBoolean("prefs_toddler_lock", false).apply();
        kidaccumecode = "";
        checkChildLock();
    }


    private boolean checkChildLock() {
        View kid_escape_area = findViewById(R.id.kid_escape_area);
        View decorView = getWindow().getDecorView();
       // View catswrap = findViewById(R.id.category_tabs_wrap);

        if (mChildLock ) {

            // Hide the status bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);


            if (!mChildLockSetup) {
                mQuickRowScroller.setVisibility(View.GONE);

                mIconSheetBottomFrame.setVisibility(View.GONE);
                mShowButtons.setVisibility(View.GONE);
                kid_escape_area.setVisibility(View.VISIBLE);

                TextView kid_code_txt = (TextView) findViewById(R.id.kid_code_txt);

                TextView[] b = new TextView[4];
                b[0] = (TextView) findViewById(R.id.btn_kid1);
                b[1] = (TextView) findViewById(R.id.btn_kid2);
                b[2] = (TextView) findViewById(R.id.btn_kid3);
                b[3] = (TextView) findViewById(R.id.btn_kid4);

                List<String> letters = Arrays.asList(getString(R.string.letters).split("(?!^)"));
                Collections.shuffle(letters);
                kidcode = "";
                for (int i = 0; i < b.length; i++) {

                    b[i].setOnClickListener(kidescape);
                    String c = letters.get(i);
                    b[i].setTag(c);
                    b[i].setText(c);
                    kidcode += c;
                }
                List<String> kidcodearr = Arrays.asList(kidcode.split("(?!^)"));
                String shuffled;
                do {
                    Collections.shuffle(kidcodearr);
                    shuffled = "";
                    for (String letter : kidcodearr) {
                        shuffled += letter;
                    }
                } while (kidcode.equals(shuffled));
                kidcode = shuffled;

                kid_code_txt.setText(getString(R.string.kid_escape_text, kidcode) );
                mChildLockSetup = true;


            }
            return true;
        } else {

            mChildLockSetup = false;
            mQuickRowScroller.setVisibility(View.VISIBLE);

            mShowButtons.setVisibility(View.VISIBLE);
            kid_escape_area.setVisibility(View.GONE);
            int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
            decorView.setSystemUiVisibility(uiOptions);
        }
        return false;
    }


    private void setColors() {


        cattabBackground = mAppPreferences.getInt("cattab_background", getResColor(R.color.cattab_background));
        cattabSelectedBackground = mAppPreferences.getInt("cattabselected_background", getResColor(R.color.cattabselected_background));

        dragoverBackground = mAppPreferences.getInt("dragover_background", getResColor(R.color.dragover_background));

        cattabTextColor =  mAppPreferences.getInt("cattabtextcolor", getResColor(R.color.textcolor));
        cattabTextColorInvert = mAppPreferences.getInt("cattabtextcolorinv", getResColor(R.color.textcolorinv));

        textColor = mAppPreferences.getInt("textcolor", getResColor(R.color.textcolor));

        itemClickedAnim = new ScaleAnimation(.85f,1,.85f,1,Animation.RELATIVE_TO_SELF,.5f,Animation.RELATIVE_TO_SELF,.5f);
        itemClickedAnim.setDuration(200);
        itemClickedAnim.setInterpolator(new AccelerateDecelerateInterpolator());
    }

    private int getResColor(int res) {
        if (Build.VERSION.SDK_INT >= 23) {
            return getColor(res);
        } else {
            return getResources().getColor(res);
        }
    }


    public boolean isAppInstalled(String packageName) {
        if (packageName.equals(AppShortcut.ACTION_PACKAGE)) return true;
        try {
            getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }


    public boolean isLandscape() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public Point getScreenDimensions() {

        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        return size;
    }



    enum CategoryTabStyle {Default, Normal, Selected, DragHover, Tiny}

    interface CategoryChangerListener {
        void onClick(DialogInterface dialog, int which, String category, String newDisplayName, String newDisplayFullName, boolean istiny);
    }


}
