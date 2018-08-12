package com.quaap.launchtime;

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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
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
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;

import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.quaap.launchtime.apps.AppLauncher;
import com.quaap.launchtime.apps.Badger;
import com.quaap.launchtime.apps.DefaultApps;
import com.quaap.launchtime.apps.LaunchApp;
import com.quaap.launchtime.ui.InteractiveScrollView;
import com.quaap.launchtime.ui.ActionMenu;
import com.quaap.launchtime.components.Categories;
import com.quaap.launchtime.components.ExceptionHandler;
import com.quaap.launchtime.components.IconsHandler;
import com.quaap.launchtime.ui.MsgBox;
import com.quaap.launchtime.db.DB;
import com.quaap.launchtime.ui.QuickRow;
import com.quaap.launchtime.ui.SearchBox;
import com.quaap.launchtime.ui.Style;
import com.quaap.launchtime.widgets.Widget;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static android.view.Gravity.LEFT;

public class MainActivity extends Activity implements
        View.OnLongClickListener, SharedPreferences.OnSharedPreferenceChangeListener,
        Badger.BadgerCountChangeListener {

    // Things are getting very messy.  That's what happens when you figure it out as you go along.
    //TODO: everything needs a major refactor.


    private static final int UNINSTALL_RESULT = 3454;

    private LinearLayout mIconsArea;
    private LinearLayout mIconSheetTopFrame;
    private InteractiveScrollView mIconSheetScroller;
    private ViewGroup mIconSheetBottomFrame;
    private ViewGroup mIconSheetHolder;
    private Map<String, GridLayout> mIconSheets;

    private GridLayout mIconSheet;

    private InteractiveScrollView mCategoriesScroller;
    private Map<String, TextView> mCategoryTabs;
    private Map<View, String> mRevCategoryMap;
    private volatile String mCategory;
    private ImageView mShowButtons;
    private ImageView mHideButtons;
    private ImageView mShowCats;


    private TextView mSortCategoryButton;
    private TextView mAddCategoryButton;
    private TextView mRenameCategoryButton;
    private TextView mEditWidgetsButton;
    private ImageView mOpenPrefsButton;
    private ImageView mOpenPrefs2Button;


    private LinearLayout mCategoriesLayout;
    private TextView mRemoveAppText;
    private FrameLayout mRemoveDropzone;
    private FrameLayout mLinkDropzone;
    private FrameLayout mLinkDropzonePeek;
    private PackageManager mPackageMan;
    private AppLauncher mBeingDragged;
    private volatile ViewGroup mDragDropSource;
    private SharedPreferences mPrefs;
    private View mBeingUninstalled;
    private Widget mWidgetHelper;

    private Animation itemClickedAnim;

    private String mCategoryJustCreated;

    private int mColumns = 3;


    public Point mScreenDim;

    public SharedPreferences mAppPreferences;

    private final Map<String, AppWidgetHostView> mLoadedWidgets = new HashMap<>();
    private final Map<AppLauncher,ViewGroup> mAppLauncherViews = Collections.synchronizedMap(new HashMap<AppLauncher,ViewGroup>());

    private boolean mChildLock;
    private boolean mChildLockSetup;

    private boolean mDumbMode;

    private LaunchApp mLaunchApp;

    private QuickRow mQuickRow;

    private ProgressBar mProgressBar;


    //private DB db();

    private SearchBox mSearchBox;
    private TextView mCategoryLabel;

    private Style mStyle;

    private AddIconHandler iconHandler;
    private static final int ADD_ICON = 1;
    private static final int REMOVE_ALL_ICONS = 2;
    private static final int NO_ICONS = 3;

    private static final String TAG = "LaunchTime";

    private static String latestCategory;

    private int mAnimationDuration = 150;

    private ActionMenu mActionMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        if (GlobState.enableCrashReporter && !BuildConfig.DEBUG) Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

        setContentView(R.layout.activity_main);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }


        //Setup some of our globals utils

        mPackageMan = getApplicationContext().getPackageManager();
        mAppPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mPrefs = getSharedPreferences("default", MODE_PRIVATE);

        String key = "current";
        if (mPrefs.getInt(key,0) < 830) {

            if (mAppPreferences.getString(getString(R.string.pref_key_animate_duration), "150").equals("250")) {
                mAppPreferences.edit().putString(getString(R.string.pref_key_animate_duration), "150").apply();
            }

            mPrefs.edit().putInt(key,BuildConfig.VERSION_CODE).apply();
        }

        mWidgetHelper = new Widget(this);

        mQuickRow = new QuickRow(mMainDragListener, this);

        mScreenDim = getScreenDimensions();


        mActionMenu = new ActionMenu(this);


        //Load resources and init the form members
        initUI();

        mIconSheetHolder.setOnDragListener(iconSheetDropRedirector);

        mIconsArea.setOnDragListener(iconSheetDropRedirector);

        mSearchBox = new SearchBox(this, mIconSheetScroller);

        mLaunchApp = new LaunchApp(this);

        iconHandler = new AddIconHandler(this);

        mInitCalled = false;
        mInitCalling = false;
    }

    private StartupTask mStartupTask;
    private boolean mInitCalled = false;
    private boolean mInitCalling = false;

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");


        //in case the task didn't complete
        int startat = 0;
        if (mStartupTask!=null && mStartupTask.isCancelled() && !mStartupTask.isCompleted()) {
            startat = mStartupTask.mCompletedStage;
            mInitCalled = false;
            mInitCalling = false;
        }


        if (mInitCalling) return;
        if (mInitCalled) {
            myResume();
        } else {
            mStartupTask = new StartupTask(this, true, startat);
            mStartupTask.execute();
        }

    }


    private void myResume() {
        //Check how long we've been gone
        long pausetime = mPrefs.getLong("pausetime", -1);
        int homesetting = Integer.parseInt(mAppPreferences.getString(getString(R.string.pref_key_return_home), "9999999"));


        //We go "home" if it's been longer than the timeout
        boolean skiphome = false;
        if (pausetime>-1 && System.currentTimeMillis() - pausetime > homesetting*1000 && !mChildLock) {
            mCategory = getTopCategory();
            skiphome = true;
            mQuickRow.scrollToStart();
            mCategoriesScroller.smoothScrollTo(0, 0);
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
                    scrollToCategoryTab();
                    showButtonBar(false, true);

                }
            }, 100);
        }

        //rerun our query if needed
        if (mCategory.equals(Categories.CAT_SEARCH)) {
            mSearchBox.refreshSearch(false);
        }

        hideRemoveDropzone();

        hideCatsIfAutoHide(true);
        showButtonBar(false, true);
        //lock things up if it was in toddler mode
        checkChildLock();

    }



    private long mPauseTime = 0;

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        mBackPressedSessionCount=0;
        checkChildLock();

//        try {
//            if (mStartupTask!=null && !mStartupTask.isCancelled()) {
//                mStartupTask.cancel(true);
//                //mStartupTask = null;
//            }
//        } catch (Throwable t) {
//            Log.e(TAG, t.getMessage(), t);
//        }

        //save a few items
        mPrefs.edit()
                .putInt("scrollpos" + mCategory, mIconSheetScroller.getScrollY())
                .putString("category", mCategory)
                .putLong("pausetime", System.currentTimeMillis())
                .commit();

        //close our search cursor, if needed
        mSearchBox.closeSeachAdapter();
        mPauseTime = System.currentTimeMillis();



        super.onPause();
    }

    private static class StartupTask extends AsyncTask<Void,Integer,List<AppLauncher>> {

        private final WeakReference<MainActivity> mMain;
        private final boolean mShowProgress;

        final private int mStartAtStage;
        int mCompletedStage = 0;
        final int mStages = 6;

        StartupTask(MainActivity main, boolean showProgress, int startAtStage) {
            mMain = new WeakReference<>(main);
            mShowProgress = showProgress;
            main.mInitCalling = true;
            mStartAtStage = startAtStage;

        }

        boolean isCompleted() {
            return mCompletedStage == mStages;
        }

        @Override
        protected List<AppLauncher> doInBackground(Void... voids) {
            final MainActivity main = mMain.get();
            if (main == null) return null;
            try {

                if (main.mInitCalled) return null;

                if (mShowProgress) {
                    main.showProgressBar(100);
                    main.incProgressBar(1);
                    Thread.yield();
                }

                if (mStartAtStage<1) {
                    stage1(main);
                    mCompletedStage = 1;
                }

                if (mStartAtStage<2) {
                    stage2(main);
                    mCompletedStage = 2;
                }

                return main.processActivities(mShowProgress);
            } catch (Throwable t) {
                Log.e(TAG, t.getMessage(), t);
            } finally {
                main.mInitCalled = true;
            }
            return null;
        }

        private void stage1(MainActivity main) {
            DB db = GlobState.getGlobState(main).getDB();
            if (db.isFirstRun()) {
                main.mAppPreferences.edit()
                        .putBoolean(main.getString(R.string.pref_key_show_action_menus), Build.VERSION.SDK_INT >= 25)
                        .putBoolean(main.getString(R.string.pref_key_show_action_extra), Build.VERSION.SDK_INT >= 25)
                        .apply();
            }
        }

        private void stage2(MainActivity main) {
            if (mShowProgress) main.incProgressBar(1);

            main.init(mShowProgress);
            Thread.yield();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            final MainActivity main = mMain.get();
            if (main == null) return;
            main.mInitCalling = false;
            main.hideProgressBar();
        }

        @Override
        protected void onPostExecute(List<AppLauncher> launchers) {
            final MainActivity main = mMain.get();
            if (main == null) return;

            mCompletedStage = 3;

            try {

                if (launchers != null) {
                    if (mShowProgress) main.showProgressBar(5);

                    if (mStartAtStage<4) {
                        main.mQuickRow.processQuickApps(launchers);
                        main.mQuickRow.repopulate();

                        //main.db().setAppCategoryOrder(main.mRevCategoryMap.get(main.mQuickRow.getGridLayout()), main.mQuickRow.getGridLayout());
                        mCompletedStage = 4;
                    }

                    if (mShowProgress) main.incProgressBar(1);

                    if (main.mCategory.equals(Categories.CAT_SEARCH)) {
                        main.populateRecentApps();
                    } else {
                        main.repopulateIconSheet(main.mCategory);
                    }
                    if (mShowProgress) main.incProgressBar(1);

                    main.firstRunPostApps();
                    mCompletedStage = 5;

                    if (mShowProgress) main.incProgressBar(1);
                }

                if (mShowProgress) main.incProgressBar(1);

                main.myResume();
                mCompletedStage = 6;
                if (mShowProgress) main.incProgressBar(1);

                main.mStartupTask = null;

            } catch (Throwable t) {
                Log.e(TAG, t.getMessage(), t);
            } finally {
                try {
                    main.mInitCalling = false;
                    main.mStartupTask = null;
                    main.hideProgressBar();
                } catch (Throwable t) {
                    Log.e(TAG, t.getMessage(), t);
                }
            }
        }
    }


    private void init(boolean progress) {

        if (progress) setProgressBarMax(10);

        mCategory = mPrefs.getString("category", getTopCategory());
        latestCategory = mCategory;

        mStyle = GlobState.getStyle(this);
        GlobState.getBadger(this).setBadgerCountChangeListener(this);

        mAppPreferences.registerOnSharedPreferenceChangeListener(this);

        if (progress) incProgressBar(1);

        readPrefs();
        //create the grids for each existing category.
        if (progress) incProgressBar(1);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCategoriesLayout.removeAllViews();
            }
        });
        for (final String category : db().getCategories()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    createIconSheet(category, db().isFirstRun()?-1:-2);
                }
            });
            Thread.yield();
        }

        if (progress) incProgressBar(1);
        if (!db().isFirstRun()) {
            //Make sure the displayed icons load first
            //Load the quickrow icons first
            for (ComponentName actvname : db().getAppCategoryOrder(QuickRow.QUICK_ROW_CAT)) {
                if (db().isAppInstalled(actvname)) {
                    AppLauncher app = db().getApp(actvname);
                    if (app != null) {
                        app.loadAppIconAsync(this);
                    }
                }
            }
            if (progress) incProgressBar(1);
            //Load the selected category icons
            for (ComponentName actvname : db().getAppCategoryOrder(mCategory)) {
                if (db().isAppInstalled(actvname)) {
                    AppLauncher app = db().getApp(actvname);
                    if (app != null) {
                        app.loadAppIconAsync(this);
                    }
                }
            }
            if (progress) incProgressBar(1);
        }
    }


    private List<AppLauncher> processActivities(boolean showProgress) {
        final List<AppLauncher> launchers = new ArrayList<>();

        final List<ComponentName> dbactvnames = db().getAppNames();

        Set<ComponentName> pmactvnames = new HashSet<>();
        List<AppLauncher> newapps = new ArrayList<>();

        // Set MAIN and LAUNCHER filters, so we only get activities with that defined on their manifest
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        if (showProgress) setProgressBarMax(dbactvnames.size()+2);

        // Get all activities that have those filters
        final List<ResolveInfo> activities;

        try {
            if (showProgress) incProgressBar(1);
            activities = mPackageMan.queryIntentActivities(intent, PackageManager.GET_META_DATA);
            if (showProgress) incProgressBar(1);
        } catch (final Exception e) {
            Log.e(TAG, "Problem getting app list: " + e.getLocalizedMessage(), e);
            iconHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Problem getting app list: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
            return launchers;
        }

        if (showProgress) setProgressBarMax(activities.size() + dbactvnames.size()+2);

        for (int i = 0; i < activities.size(); i++) {

            if (showProgress && i%5==0) {
                incProgressBar(5);
            }
            try {
                AppLauncher app;

                ResolveInfo ri = activities.get(i);

                String actvname = ri.activityInfo.name;
                ComponentName appcn = new ComponentName(ri.activityInfo.packageName, actvname);

                if (!pmactvnames.contains(appcn)) {
                    pmactvnames.add(appcn);

                    app = db().getApp(appcn);

                    if (dbactvnames.contains(appcn) && app != null) {
                        app.loadAppIconAsync(this);
                        String label = ri.loadLabel(mPackageMan).toString();
                        if (app.getLabel()==null || !app.getLabel().equals(label)) {
                            db().updateAppLabel(ri.activityInfo.packageName, actvname, label);
                            app.setLabel(label);
                        }

                        //  Log.d(TAG, "app was in db " + actvname + " " +  ri.activityInfo.packageName);
                    } else {
                        //  Log.d(TAG, "app was not in db " + actvname + " " +  ri.activityInfo.packageName);
                        app = AppLauncher.createAppLauncher(this, mPackageMan, ri);
                        newapps.add(app);
                    }

                    launchers.add(app);


                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }

        }
        if (showProgress) setProgressBar(activities.size());

        //remove launchers if they are not in the system
        for (Iterator<ComponentName> it = dbactvnames.iterator(); it.hasNext(); ) {
            ComponentName dbactv = it.next();
            if (!pmactvnames.contains(dbactv)) {
                AppLauncher app = db().getApp(dbactv);
                if (app==null || !isAppInstalled(app.getPackageName())) {  //might be a widget, check packagename
                    Log.d(TAG, "Removing " + dbactv);
                    it.remove();
                    db().deleteApp(dbactv);
                    // removeFromQuickApps(dbactv);
                }
            }
            if (showProgress) incProgressBar(1);
        }

        db().addApps(newapps);


        return launchers;
    }


    private void firstRunPostApps() {
        if (db().isFirstRun()) {
            String selfAct = this.getPackageName() + "." + this.getClass().getSimpleName();
            Log.d(TAG, "My name is " + selfAct);

            //Move self icon to hidden
            db().updateAppCategory(selfAct, this.getPackageName(), Categories.CAT_HIDDEN);

            //Take a backup now that things are pre-sorted.
            //db().backup("After install");

            //Show the help screen on very first run.
            mCategoriesScroller.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent help = new Intent(MainActivity.this, AboutActivity.class);
                    help.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(help);
                }
            }, 3000);

        }

        MsgBox.showNewsMessage(this, mPrefs);

    }



    //All db access is routed through here.
    // We don't store the connection, because the connection might end up closed.
    public DB db() {
        return GlobState.getGlobState(this).getDB();
    }

    private final View.OnDragListener iconSheetDropRedirector = new View.OnDragListener() {
        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            return mMainDragListener.onDrag(mIconSheet, dragEvent);
        }
    };

    private final InteractiveScrollView.OnSwipeHorizontalListener mHSwipeListener = new InteractiveScrollView.OnSwipeHorizontalListener() {

        @Override
        public void onLeftSwipe(float absDist) {
            if (mChildLock) return;
            switchCategory(getNextCategory(mCategory, -1), AnimateDirection.Right, false);
            scrollToCategoryTab();
        }

        @Override
        public void onRightSwipe(float absDist) {
            if (mChildLock) return;
            switchCategory(getNextCategory(mCategory, 1), AnimateDirection.Left, false);
            scrollToCategoryTab();
        }
    };

    private void scrollToCategoryTab() {
        mCategoriesScroller.smoothScrollTo(0, mCategoryTabs.get(mCategory).getTop()-20);
    }

    private String getNextCategory(String category, int dir) {
        if (dir>0) dir = 1;
        if (dir<0) dir = -1;

        List<String> categories = db().getCategories();
        for (ListIterator<String> it = categories.listIterator(); it.hasNext();) {
            String cat = it.next();
            if (!cat.equals(category) && (Categories.isHiddenCategory(cat) || db().isHiddenCategory(cat))) it.remove();
        }
        int last = categories.size() -1;
        for (int i=0; i<categories.size(); i++) {
            if (categories.get(i).equals(category)) {
                if (i==0 && dir==-1) return categories.get(last);
                if (i==last && dir==1) return categories.get(0);
                return categories.get(i+dir);
            }
        }
        return null;
    }

    //screen rotation, etc.
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mScreenDim = getScreenDimensions();
        checkConfig();
        //showButtonBar(false, true);
    }

    private String getTopCategory() {
        for (String category: db().getCategories()) {
            // If the category has been deleted, pick a known-good category
            if (category==null || db().getCategoryDisplay(category)==null || db().isHiddenCategory(category) || Categories.isHiddenCategory(category)) {
                continue;
            }
            return category;
        }
        // If the category has been deleted, pick a known-good category
        return Categories.CAT_TALK;
    }


    private int getCategoryPos(String category) {
        View cattab = mCategoryTabs.get(category);
        int pos = -1;
        if (cattab!=null) {
            for (int i = 0; i < mCategoriesLayout.getChildCount(); i++) {
                if (mCategoriesLayout.getChildAt(i) == cattab) {
                    pos = i;
                    break;
                }
            }
        }
        return pos;
    }

    //private List<String> prefsChanging = Collections.synchronizedList(new ArrayList<String>());

    private final Object prefsChanging = new Object();

    private volatile int prefsUpdate;

    //int pc = 0;
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        synchronized (prefsChanging) {
            Log.d(TAG, "A preference has been changed: " + key);

            if (key != null) {


                // when the Theme updates many of these prefs at once, it sends "prefsUpdate" first so that we ignore everything until it is done.
                if (key.equals("prefsUpdate")) {
                    prefsUpdate = sharedPreferences.getBoolean(key, false) ? prefsUpdate+1 : prefsUpdate-1;
                    if (prefsUpdate<0) prefsUpdate=0;
                }

                if (prefsUpdate>0) return;
                Log.d(TAG, "still here " + key);

//                if (pc++ > 10) {
//                    Log.d("prefsChanging", "too many: bailing!");
//                    prefsUpdate--;
//                    pc--;
//                    return;
//                }

                checkConfig();

                boolean repop = false;

                //Delete our icon cache so the labels can be regenerated.
                if (key.equals(getString(R.string.pref_key_textcolor)) || key.equals(getString(R.string.pref_key_iconsize)) || key.equals("icon-update")) {
                    mAppLauncherViews.clear();
                    repop = true;
                }
                if (key.equals(getString(R.string.pref_key_icon_tint)) || key.equals("prefsUpdate")) {
                    AppLauncher.clearIcons();
                    mAppLauncherViews.clear();
                    repop = true;
                }
                if (key.equals(getString(R.string.pref_key_icons_pack))) {
                    AppLauncher.clearIcons();
                    mAppLauncherViews.clear();

                    //mIconSheet.removeAllViews();
                    IconsHandler ich = GlobState.getIconsHandler(this);

                    ich.loadIconsPack(sharedPreferences.getString(getString(R.string.pref_key_icons_pack), IconsHandler.DEFAULT_PACK));
                    //ich.updateStyles(mStyle);

                } else {

                    final boolean repop2 = repop;
                    mCategoriesScroller.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            switchCategory(mCategory);
                            if (repop2) mQuickRow.repopulate();

                        }
                    }, 200);


                    if (key.equals(getString(R.string.pref_key_toddler_lock))) {
                        mChildLock = sharedPreferences.getBoolean(getString(R.string.pref_key_toddler_lock), false);
                        if (mChildLock) mChildLockSetup = false;
                        checkChildLock();
                        hideCatsIfAutoHide(false);
                    }

                    if (key.equals(getString(R.string.pref_key_dumbmode))) {
                        mDumbMode = sharedPreferences.getBoolean(getString(R.string.pref_key_dumbmode), false);
                        if (mDumbMode) {
                            startDumbMode();
                        }
                    }



                    if (key.equals(getString(R.string.pref_key_show_badges))) {
                        if (!sharedPreferences.getBoolean(getString(R.string.pref_key_show_badges), true)) {
                            GlobState.getBadger(this).clearAll();
                        }
                    }
                    if (key.equals(getString(R.string.pref_key_autohide_cats_timeout))) {
                        handleAutohide();
                    }
                    if (key.equals(getString(R.string.pref_key_animate_duration))) {
                        readAnimationDuration();
                    }

                    if (key.equals(getString(R.string.pref_key_center_sheet))) {
                        setAllIconSheetsLayout();
                    }

                    if (key.equals(getString(R.string.pref_key_show_action_menus))
                            || key.equals(getString(R.string.pref_key_show_dropzones))
                            || key.equals(getString(R.string.pref_key_show_action_extra))
                            || key.equals(getString(R.string.pref_key_show_action_activities))) {
                        mActionMenu.readActionMenuConfig();
                    }

                }
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
        try {
            mWidgetHelper.done();
        } catch (Exception e) {
            Log.e(TAG, "Exception killing widgets", e);
        }
        super.onDestroy();
    }


    public static String getLatestCategory() {
        return latestCategory;
    }


    public synchronized void switchCategory(String category) {
        switchCategory(category, mStyle.isLeftHandCategories()?AnimateDirection.Left:AnimateDirection.Right);
    }

    private synchronized void switchCategory(String category, AnimateDirection dir) {
        switchCategory(category, dir, true);
    }


    private synchronized void switchCategory(String category, AnimateDirection dir, boolean bounce) {
        try {
            mActionMenu.dismissActionPopup();
            if (category == null) return;
            if (mCategory!=null && !mCategory.equals(category)) {
                animateHide(mIconsArea, dir, true, bounce);
            }
            mCategory = category;
            latestCategory = mCategory;

            //make sure selected category is in the database.
            if (db().getCategoryDisplay(mCategory) == null) {
                mCategory = getTopCategory();
            }
            setCategoryTabStyles();


            mIconSheet = mIconSheets.get(mCategory);

            //Check the screen rotation and changes column count, if needed
            checkConfig();

            //refresh icons on page
            mIconSheetHolder.postDelayed(new Runnable() {
                @Override
                public void run() {
                    repopulateIconSheet(mCategory);

                    //the top frame holds the search zone, but only on the search page.
                    mIconSheetTopFrame.removeAllViews();
                    if (mCategory.equals(Categories.CAT_SEARCH)) {

                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        lp.gravity = Gravity.START;
                        mIconSheetTopFrame.setLayoutParams(lp);
                        mIconSheetTopFrame.addView(mSearchBox.getSearchView());

                        //Show recent apps
                        populateRecentApps();

                        //load our cursor
                        mSearchBox.refreshSearch(true);

                    } else {
                        // not the search page: close the cursor
                        mSearchBox.closeSeachAdapter();

                        if (isAutohide() && mAppPreferences.getBoolean(getString(R.string.pref_key_cat_label), true)) {
                            // not the search page: close the cursor
                            mSearchBox.closeSeachAdapter();

                            if (mCategoryLabel==null) {
                                mCategoryLabel = new TextView(MainActivity.this);
                            }
                            mCategoryLabel.setTextColor(mStyle.getCattabTextColor());
                            mCategoryLabel.setText(db().getCategoryDisplayFull(mCategory));
                            mCategoryLabel.setTextSize(mStyle.getCategoryTabFontSize()+1);
                            mCategoryLabel.setShadowLayer(8,4,4,mStyle.getCattabTextColorInvert());
                            mCategoryLabel.setBackgroundColor(mStyle.getCattabBackground());
                            if (mStyle.isRoundedTabs()) {
                                mCategoryLabel.setBackground(mStyle.getBgDrawableFor(mCategoryLabel, Style.CategoryTabStyle.Default,true));
                            }
                            mCategoryLabel.setPadding(60,5,60,5);
                            mCategoryLabel.setAlpha(.96f);

                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            lp.gravity=Gravity.CENTER;
                            lp.topMargin=10;
                            mIconSheetTopFrame.setLayoutParams(lp);

                            mIconSheetTopFrame.addView(mCategoryLabel);
                        }

                        if (mCategory.equals(Categories.CAT_DUMB) && !mDumbMode) {
                            Button dumbmode = new Button(MainActivity.this);
                            dumbmode.setText(R.string.activate_dumbmode);
                            dumbmode.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    view.setVisibility(View.GONE);
                                    mAppPreferences.edit().putBoolean(getString(R.string.pref_key_dumbmode), true).apply();
                                }
                            });
                            mIconSheetTopFrame.addView(dumbmode);
                        }
                    }

                    //Actually switch the icon sheet.
                    mIconSheetHolder.removeAllViews();
                    mIconSheetHolder.addView(mIconSheet);
                }
            }, mAnimationDuration);

//
//            mIconSheetHolder.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    animateUpShow(mIconsArea);
//                }
//            },300);

            if (!isAutohide()) showButtonBar(false, true);
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "SwitchCat", e);
        }
    }

    private void setCategoryTabStyles() {
        //switch all category tabs to their default style and text
        for (TextView catTab : mCategoryTabs.values()) {
            styleCategorySpecial(catTab, Style.CategoryTabStyle.Default);
            catTab.setText(db().getCategoryDisplay(mRevCategoryMap.get(catTab)));
        }

        //change the selected tab to the full label name
        TextView catTab = mCategoryTabs.get(mCategory);
        catTab.setText(db().getCategoryDisplayFull(mCategory));
        catTab.setVisibility(View.VISIBLE);
    }


    private int mBackPressedSessionCount;
    @Override
    public void onBackPressed() {
        if (mInitCalling) return;
        try {
            hideCatsIfAutoHide(false);

            //back does nothign if in toddler mode
            if (mChildLock) {
                if (++mBackPressedSessionCount == 17) {
                    deactivateChildLock();
                }
                return;
            }


            String topCat = getTopCategory();
            if (mIconSheetBottomFrame.getVisibility() == View.VISIBLE) {
                showButtonBar(false, true);
            } else if (mQuickRow.getScrollPos() > 0) {
                mQuickRow.scrollToStart();
            } else if (mIconSheetScroller.getScrollY() > 0) {
                //Otherwise, scroll to top
                mIconSheetScroller.smoothScrollTo(0, 0);
            } else if (mCategory.equals(Categories.CAT_SEARCH) && mSearchBox.getSeachText().length() != 0) {
                //If search is open, clear the searchbox
                mSearchBox.setSearchText("");
            } else if (!mCategory.equals(topCat)) {
                //Otherwise, switch to known-good category
                switchCategory(topCat);
                mCategoriesScroller.smoothScrollTo(0, 0);
            } else if (mCategoriesScroller.getScrollY() > 0) {
                mCategoriesScroller.smoothScrollTo(0, 0);
            }
        } catch (Exception e){
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // Log.d(TAG, keyCode + "");
        if (!mChildLock &&keyCode == KeyEvent.KEYCODE_MENU) {
            openSettings(this);
        }
        return super.onKeyDown(keyCode, event);
    }


    //Catch home key press
    @Override
    protected void onNewIntent(Intent intent) {
        if (mInitCalling) return;

        super.onNewIntent(intent);


        if (System.currentTimeMillis() - mPauseTime < 1000  && Intent.ACTION_MAIN.equals(intent.getAction())) {

            final boolean alreadyOnHome =
                    ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                            != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            Log.d("LaunchTime", " new intent " + alreadyOnHome);
            if (alreadyOnHome && !mChildLock) {

                // If we are on home screen, reset most things and go to top category.
                mCategoriesScroller.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mActionMenu.dismissAppinfo();
                            mSearchBox.setSearchText("");
                            mCategoriesScroller.smoothScrollTo(0, 0);
                            showButtonBar(false, true);
                            mIconSheetScroller.smoothScrollTo(0, 0);
                            switchCategory(getTopCategory());
                            mQuickRow.scrollToStart();
                            mIconSheetScroller.smoothScrollTo(0, 0);
                            mCategoriesScroller.smoothScrollTo(0, 0);
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage(), e);
                        }

                    }
                }, 200);
            }

        }
    }

    private void readPrefs() {

        //Checks application preferences and adjust accordingly
        try {

            mStyle.readPrefs();

            mDumbMode = mAppPreferences.getBoolean(getString(R.string.pref_key_dumbmode), false);
            mChildLock = mAppPreferences.getBoolean(getString(R.string.pref_key_toddler_lock), false);
            readAnimationDuration();
            mActionMenu.readActionMenuConfig();

            WindowManager wm = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE));


            int orientationPref = Integer.parseInt(mAppPreferences.getString(getString(R.string.pref_key_orientation), "-1"));
            if (orientationPref==-1 && wm!=null) {

                Display display = wm.getDefaultDisplay();
                int rotation = display.getRotation();
                int orientation = getResources().getConfiguration().orientation;


                switch (rotation) {
                    case Surface.ROTATION_180:
                    case Surface.ROTATION_0:
                        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            orientationPref = 3;
                        } else {
                            orientationPref = 2;
                        }
                        break;
                    case Surface.ROTATION_270:
                    case Surface.ROTATION_90:
                    default:
                        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                            orientationPref = 3;
                        } else {
                            orientationPref = 2;
                        }

                        break;
                }


                mAppPreferences.edit().putString(getString(R.string.pref_key_orientation), orientationPref + "").apply();
            }


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
            Log.e(TAG, e.getMessage(), e);
        }
    }


    private void readAnimationDuration() {
        mAnimationDuration = Integer.parseInt(mAppPreferences.getString(getString(R.string.pref_key_animate_duration), "150"));
        mActionMenu.setAnimationDuration(mAnimationDuration);
        //if (mAnimationDuration==0) mAnimationDuration=1; //small hack to make everything still work
    }

    //This is run on switchcategory and screen rotation, etc.
    // Checks global preferences and
    //  if we need to change the column count, etc
    private void checkConfig() {
        readPrefs();

        //View view = findViewById(R.id.main_layout_view);
        //view.setBackgroundColor(mStyle.getWallpaperColor());
        int bgcolor = mStyle.getWallpaperColor();
        if (Color.alpha(bgcolor)==0) {
            bgcolor = 0;
            getWindow().setBackgroundDrawable(null); // needed to clear it
        }
        getWindow().setBackgroundDrawable(new ColorDrawable(bgcolor));

        //Log.d(TAG,"bg:" + mStyle.getWallpaperColor());

        itemClickedAnim = new ScaleAnimation(.85f,1,.85f,1,Animation.RELATIVE_TO_SELF,.5f,Animation.RELATIVE_TO_SELF,.5f);
        itemClickedAnim.setDuration(200);
        itemClickedAnim.setInterpolator(new AccelerateDecelerateInterpolator());

        boolean autohideCats = isAutohide();
        try {

            String force = mAppPreferences.getString(isLandscape()?getString(R.string.pref_key_columns_landscape):getString(R.string.pref_key_columns_portrait), "0");
            if (force.equals("0")) {
                mScreenDim = getScreenDimensions();
                //float launcherw = getResources().getDimension(R.dimen.launcher_width);
                float launcherw = mStyle.getLauncherSize();
                float catwidth = getResources().getDimension(R.dimen.cattabbar_width) + 3;

                float wr = (mScreenDim.x - (autohideCats ? 0 : catwidth)) / launcherw;

                //Log.d(TAG, "density=" + getResources().getDisplayMetrics().density + " wr=" + wr + " x=" + mScreenDim.x + " catwidth=" + catwidth + " launcherw=" + launcherw);
                if (wr < 3) mColumns = 2;
                else if (wr < 4.16) mColumns = 3;
                else if (wr < 5.4) mColumns = 4;
                else if (wr < 7.4) mColumns = 5;
                else if (wr < 9) mColumns = 6;
                else if (wr < 10.2) mColumns = 8;
                else if (wr < 13.2) mColumns = 9;
                else mColumns = 10;
            } else {
                mColumns = Integer.parseInt(force);
            }


            //Log.d(TAG, "x=" + mScreenDim.x + " catwidth=" + catwidth + " launcherw=" + launcherw);


            if (mIconSheet!=null && mIconSheet.getColumnCount() != mColumns) {
                changeColumnCount(mIconSheet, mColumns);
            }

            mShowButtons.setBackgroundColor(mStyle.getCattabBackground());
            mShowButtons.setColorFilter(mStyle.getCattabTextColor());
            mShowButtons.setMinimumHeight(mStyle.getCategoryTabPaddingHeight()*3);

            mHideButtons.setBackgroundColor(mStyle.getCattabBackground());
            mHideButtons.setColorFilter(mStyle.getCattabTextColor());
            mHideButtons.setMinimumHeight(mStyle.getCategoryTabPaddingHeight()*3);

            mShowCats.setBackgroundColor(mStyle.getCattabBackground());
            mShowCats.setColorFilter(mStyle.getCattabTextColor());
            mShowCats.setMinimumHeight(mStyle.getCategoryTabPaddingHeight()*3);
            mShowCats.setAlpha(.95f);

            //mShowButtons.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, categoryTabPaddingHeight*3));
            //mShowButtons.setPadding(2,categoryTabPaddingHeight,2,4);
            mSortCategoryButton.setBackgroundColor(mStyle.getCattabBackground());
            mAddCategoryButton.setBackgroundColor(mStyle.getCattabBackground());
            mRenameCategoryButton.setBackgroundColor(mStyle.getCattabBackground());
            mEditWidgetsButton.setBackgroundColor(mStyle.getCattabBackground());
            mOpenPrefsButton.setBackgroundColor(mStyle.getCattabBackground());

            mSortCategoryButton.setTextColor(mStyle.getCattabTextColor());
            mAddCategoryButton.setTextColor(mStyle.getCattabTextColor());
            mRenameCategoryButton.setTextColor(mStyle.getCattabTextColor());
            mEditWidgetsButton.setTextColor(mStyle.getCattabTextColor());
            mOpenPrefsButton.setColorFilter(mStyle.getCattabTextColor());

            int c = mStyle.getWallpaperColor();
            mIconSheetBottomFrame.setBackgroundColor(Color.argb(255, Color.red(c), Color.green(c), Color.blue(c)));


            handleAutohide();
            setAllIconSheetsLayout();
            showButtonBar(false, true);

            setWindowOpts();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }


    private void setWindowOpts() {

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                boolean full = mAppPreferences.getBoolean(getString(R.string.pref_key_use_fullscreen), false);
                if (full) {
                    //hard to get correct.
                    getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

                    // retrieve the position of the DecorView
                    Rect visibleFrame = new Rect();
                    getWindow().getDecorView().getWindowVisibleDisplayFrame(visibleFrame);

                    DisplayMetrics dm = getResources().getDisplayMetrics();
                    // check if the DecorView takes the whole screen vertically or horizontally
                    boolean isRight = dm.heightPixels == visibleFrame.bottom;
                    //boolean isBelow  = dm.widthPixels  == visibleFrame.right;


                    int status = 70;
                    int statusresourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                    if (statusresourceId > 0) {
                        status = getResources().getDimensionPixelSize(statusresourceId) + 8;
                    }
                    int nav = 125;
                    int navresourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
                    if (navresourceId > 0) {
                        nav = getResources().getDimensionPixelSize(navresourceId);
                    }
                    findViewById(R.id.whole_thing).setPadding(0, status, isRight?nav:0, isRight?0:nav);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    getWindow().setStatusBarColor(Color.TRANSPARENT);
                    if (full) {
                        getWindow().setNavigationBarColor(Color.TRANSPARENT);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }


    private int mOkToAutohide;

    private void cancelHide() {
        mOkToAutohide = 0;
    }

    private void hideCatsIfAutoHide(boolean delay) {
        if (delay) {
            int hidetime = getAutohideTimeout();

            mOkToAutohide = (int)(Math.random()*10000000)+2;
            final int okhidse = mOkToAutohide;
            mCategoriesLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isAutohide() && mOkToAutohide==okhidse) showCats(false);
                }
            }, hidetime);
        } else {
            cancelHide();
            if (isAutohide()) showCats(false);
        }
        mActionMenu.dismissActionPopup();
    }


    private void showCats(boolean show) {
        cancelHide();
        final View cats = findViewById(R.id.category_tabs_wrap);
        showButtonBar(false,false);

        if (!show) {
            if (cats.getVisibility() == View.VISIBLE) {
                animateDownHide(cats);
            }
            animateUpShow(mShowCats);
        } else {
            animateDownHide(mShowCats);
            if (cats.getVisibility() == View.GONE) {
                animateUpShow(cats);
            }
        }
        mActionMenu.dismissActionPopup();
    }

    private void animateUpShow(View view) {
        animateShow(view,AnimateDirection.Down);
    }

    private void animateDownHide(View view) {
        animateHide(view, AnimateDirection.Down,false, true);
    }

    enum AnimateDirection {Left, Up, Right, Down}

    private final Map<View,Long> aniHideStarted = new HashMap<>();

    private void animateHide(final View view, final AnimateDirection towards) {
        animateHide(view,towards, false, true);
    }

    private void animateHide(final View view, final AnimateDirection towards, final boolean andBack, final boolean bounce) {

//        Log.d(TAG, "animateHide " + view);
        if (mAnimationDuration==0) {
            view.clearAnimation();

            if (andBack) {
                ensureVisibleNoAni(view);

            } else {
                view.setVisibility(View.GONE);
            }
            return;
        }

        long now = System.currentTimeMillis();
        float fac = andBack?2.5f:1;
        Long then = aniHideStarted.get(view);
        if (then!=null && now - then < mAnimationDuration*fac) return;
        aniHideStarted.put(view,now);

        ViewPropertyAnimator animate = view.animate()
                .setDuration(mAnimationDuration)
                .setInterpolator(new AccelerateInterpolator())
                .alpha(0)
                .scaleY(.6f)
                .scaleX(.6f)

                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);

                        if (andBack) {
                            animateShow(view, towards, !bounce);
                        } else {
                            view.setVisibility(View.GONE);
                        }

                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        if (andBack) {
                            ensureVisibleNoAni(view);
                        } else {
                            view.setVisibility(View.GONE);
                        }
                    }
                });

        switch(towards) {
            case Down:
                animate.translationY(view.getHeight());
                break;
            case Up:
                animate.translationY(-view.getHeight());
                break;
            case Right:
                animate.translationX(view.getWidth());
                break;
            case Left:
                animate.translationX(-view.getHeight());
                break;
        }
        animate.setStartDelay(0).start();
    }

    private void ensureVisibleNoAni(View view) {
        view.setAlpha(1);
        view.setScaleX(1);
        view.setScaleY(1);
        view.setTranslationX(0);
        view.setTranslationY(0);
        view.setVisibility(View.VISIBLE);
    }

    private void animateShow(final View view, AnimateDirection from) {
        animateShow(view, from,false);
    }


    private void animateShow(final View view, AnimateDirection from, boolean reverse) {

        if (mAnimationDuration==0) {
            view.clearAnimation();
            ensureVisibleNoAni(view);
            //Log.d(TAG, "animateShow " + view);

            return;
        }

        if (reverse) {
            switch(from) {
                case Down:
                    view.setTranslationY(-view.getHeight());
                    break;
                case Up:
                    view.setTranslationY(view.getHeight());
                    break;
                case Right:
                    view.setTranslationX(-view.getWidth());
                    break;
                case Left:
                    view.setTranslationX(view.getWidth());
                    break;
            }
        }

        view.setVisibility(View.VISIBLE);

        ViewPropertyAnimator animate = view.animate()
                .setDuration(mAnimationDuration)
                .setInterpolator(new DecelerateInterpolator())
                .alpha(1)
                .scaleY(1)
                .scaleX(1)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        ensureVisibleNoAni(view);
                    }
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        ensureVisibleNoAni(view);
                    }
                });

        switch(from) {
            case Down:
            case Up:
                animate.translationY(0);
                break;
            case Right:
            case Left:
                animate.translationX(0);
                break;
        }


    }

    @SuppressLint("RtlHardcoded")
    private void handleAutohide() {
        //Switch the menu left/right

        boolean autohideCats = isAutohide();

        float catwidth = getResources().getDimension(R.dimen.cattabbar_width);

        FrameLayout.LayoutParams iconsarealp = (FrameLayout.LayoutParams)mIconsArea.getLayoutParams();
        if (iconsarealp==null){
            iconsarealp = new FrameLayout.LayoutParams(this,null);
        }

        View cats = findViewById(R.id.category_tabs_wrap);

        FrameLayout.LayoutParams catslp = (FrameLayout.LayoutParams)cats.getLayoutParams();
        if (catslp==null){
            catslp = new FrameLayout.LayoutParams(this,null);
        }

        if (mStyle.isLeftHandCategories()) {
            catslp.gravity = LEFT;
            ((FrameLayout.LayoutParams)mShowCats.getLayoutParams()).gravity=Gravity.LEFT|Gravity.BOTTOM;
            if (autohideCats) {
                iconsarealp.leftMargin = 2;
                iconsarealp.rightMargin = 2;
            } else {
                iconsarealp.leftMargin = (int) catwidth+1;
                iconsarealp.rightMargin = 2;
            }

        } else {
            catslp.gravity = Gravity.RIGHT;
            ((FrameLayout.LayoutParams)mShowCats.getLayoutParams()).gravity=Gravity.RIGHT|Gravity.BOTTOM;
            if (autohideCats) {
                iconsarealp.leftMargin = 2;
                iconsarealp.rightMargin = 2;
            } else {
                iconsarealp.leftMargin = 2;
                iconsarealp.rightMargin = (int) catwidth+1;
            }
        }
        int origback = mStyle.getCattabBackground();

        if (autohideCats) {
            GradientDrawable newback = new GradientDrawable(
                    mStyle.isLeftHandCategories()?
                            GradientDrawable.Orientation.RIGHT_LEFT:
                            GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[] {Color.argb(1, Color.red(origback), Color.green(origback), Color.blue(origback)),
                            Color.argb(160, Color.red(origback), Color.green(origback), Color.blue(origback))});
            //  Color.argb(50, Color.red(origback), Color.green(origback), Color.blue(origback));
            newback.setGradientCenter(.07f, .5f);
            cats.setBackground(newback);
        } else {
            cats.setBackground(null);
            cats.setBackgroundColor(Color.TRANSPARENT);
        }

        if (autohideCats) {
            hideCatsIfAutoHide(true);
        } else {
            showCats(true);
            mShowCats.setVisibility(View.GONE);
        }


    }

    private boolean isAutohide() {
        return !mAppPreferences.getString(getString(R.string.pref_key_autohide_cats_timeout), "-1").equals("-1");
    }

    private int getAutohideTimeout() {
        return Integer.parseInt(mAppPreferences.getString(getString(R.string.pref_key_autohide_cats_timeout), "1500"));
    }

    public LaunchApp getAppLauncher() {
        return mLaunchApp;
    }


    private void showProgressBar(final int max) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (max>0) {
                    mProgressBar.setMax(max);
                }
                mProgressBar.setProgress(0);
                mProgressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideProgressBar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.GONE);
            }
        });
    }

    private void setProgressBarMax(final int max) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setMax(max);
            }
        });
    }

    private void setProgressBar(final int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setProgress(progress);
            }
        });
    }

    private void incProgressBar(final int progressDiff) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.incrementProgressBy(progressDiff);
            }
        });
    }


    private void setAllIconSheetsLayout() {
        mQuickRow.setCenterIcons(mAppPreferences.getBoolean(getString(R.string.pref_key_center_sheet), true));
        for (GridLayout sheet: mIconSheets.values()) {
            setIconSheetLayout(sheet);
        }
    }

    private void setIconSheetLayout(GridLayout iconSheet) {

        FrameLayout.LayoutParams lp;
        if (mAppPreferences.getBoolean(getString(R.string.pref_key_center_sheet), true)) {
            lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
        } else {
            lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        iconSheet.setLayoutParams(lp);
    }


    private void createIconSheet(String category, int pos) {
        final GridLayout iconSheet = new GridLayout(MainActivity.this);
        mIconSheets.put(category, iconSheet);
        mRevCategoryMap.put(iconSheet, category);
        iconSheet.setColumnCount(mColumns);
        iconSheet.setOnDragListener(mMainDragListener);
        setIconSheetLayout(iconSheet);

        final TextView categoryTab = createCategoryTab(category, iconSheet, pos);


        mCategoryTabs.put(category, categoryTab);
        mRevCategoryMap.put(categoryTab, category);
    }

    public void populateRecentApps() {

        //GridLayout iconSheet = mIconSheets.get(Categories.CAT_SEARCH);

        removeIconSheetSend(Categories.CAT_SEARCH);
        //iconSheet.removeAllViews();

        int i=0;
        for (ComponentName actvname : db().getAppLaunchedList()) {
            if (db().isAppInstalled(actvname)) {
                AppLauncher app = db().getApp(actvname);
                //Log.d("Recent", "Trying " + actvname + " " + app);

                addAppToIconSheet(Categories.CAT_SEARCH, app, false);
                i++;
                if (i >= 45) break;
            }
        }
    }

    public void repopulateIconSheet(String category) {
        GridLayout iconSheet = mIconSheets.get(category);

        removeIconSheetSend(category);
        //iconSheet.removeAllViews();

        final List<ComponentName> apporder = db().getAppCategoryOrder(category);
        List<AppLauncher> apps = db().getApps(category);

        for (ComponentName actvname : apporder) {
            //Log.d("app", "repopulateIconSheet " + category + " " +  actvname.getClassName() + " " +  actvname.getPackageName());
            for (Iterator<AppLauncher> it = apps.iterator(); it.hasNext(); ) {
                AppLauncher app = it.next();
                if (actvname.equals(app.getComponentName())) {
                    addAppToIconSheet(category, app, true);
                    it.remove();
                }
            }
        }

        for (AppLauncher app : apps) {
            addAppToIconSheet(category, app, true);
            // Log.d(TAG, app.getActivityName());
        }

        if (apps.size()>0) {
            db().setAppCategoryOrder(category, iconSheet);
        }
        if (db().getAppCount(category) == 0) {
            showNoIconsSend(category);
        }
    }

    private void addAppToIconSheet(String category, AppLauncher app, boolean reuse) {
        addAppToIconSheetSend(category, app, -1, reuse);
    }



    private void addAppToIconSheetSend(String category, AppLauncher app, int pos, boolean reuse) {
        Message msg = new Message();
        msg.arg1 = ADD_ICON;
        Bundle data = new Bundle();
        data.putString("category", category);
        data.putParcelable("componentName", app.getComponentName());
        data.putInt("pos", pos);
        data.putBoolean("reuse", reuse);
        msg.setData(data);
        iconHandler.sendMessage(msg);
    }

    private void addAppToIconSheetRecv(Message msg) {
        Bundle data = msg.getData();
        String category = data.getString("category");
        ComponentName compName = data.getParcelable("componentName");
        AppLauncher app = AppLauncher.getAppLauncher(compName);
        if (app==null) {
            app = db().getApp(compName);
            if (app==null) return;
        }
        int pos = data.getInt("pos");
        boolean reuse = data.getBoolean("reuse");

        addAppToIconSheet(mIconSheets.get(category), app, pos, reuse);
    }


    private void removeIconSheetRecv(Message msg) {
        Bundle data = msg.getData();
        String category = data.getString("category");
        GridLayout iconSheet = mIconSheets.get(category);
        if (iconSheet!=null) {
            try {
                iconSheet.removeAllViews();
            } catch (Throwable t) {
                Log.e(TAG, t.getMessage(), t);
                Toast.makeText(this, t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void removeIconSheetSend(String category) {
        Message msg = new Message();
        msg.arg1 = REMOVE_ALL_ICONS;
        Bundle data = new Bundle();
        data.putString("category", category);
        msg.setData(data);
        iconHandler.sendMessage(msg);
    }

    private void showNoIconsRecv(Message msg) {
        Bundle data = msg.getData();
        String category = data.getString("category");
        GridLayout iconSheet = mIconSheets.get(category);
        if (iconSheet!=null) {
            TextView v = new TextView(this);
            v.setText(R.string.nothing_in_cat);
            v.setTextColor(mStyle.getTextColor());
            v.setTextSize(mStyle.getLauncherFontSize());
            v.setPadding(2,40,2,2);
            v.setMaxLines(3);
            iconSheet.addView(v);
        }
    }

    private void showNoIconsSend(String category) {
        Message msg = new Message();
        msg.arg1 = NO_ICONS;
        Bundle data = new Bundle();
        data.putString("category", category);
        msg.setData(data);
        iconHandler.sendMessage(msg);
    }

    private void addAppToIconSheet(GridLayout iconSheet, AppLauncher app, int pos, boolean reuse) {
        if (app != null) {
            try {
                if (((app.isWidget() || app.isLink()) && isAppInstalled(app.getPackageName())) || mLaunchApp.isValidActivity(app)) {
                    ViewGroup item = getLauncherView(app, false, reuse);
                    if (item != null) {
                        if (!app.iconLoaded()) {
                            app.loadAppIconAsync(this);
                        }
                        ViewGroup parent = (ViewGroup) item.getParent();
                        if (parent != null) parent.removeView(item);
                        GridLayout.LayoutParams lp = getAppLauncherLayoutParams(iconSheet, app);
                        iconSheet.addView(item, pos, lp);
                    }
                } else {
                    db().deleteApp(app.getComponentName());
                    Log.d(TAG, "removed " + app.getPackageName() + " " + app.getActivityName() + ": activity not valid.");
                }
            } catch (Exception e) {
                Log.e(TAG, "exception adding icon to sheet", e);
                Toast.makeText(this,"Couldn't place icon: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Not showing recent: Null.");
        }

    }


    //Too much magic here.  Widgets are weird.

    @NonNull
    private GridLayout.LayoutParams getAppLauncherLayoutParams(GridLayout grid, AppLauncher app) {

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();

        final int wDp = getLauncherWidth(app);
        final int hDp = getLauncherHeight(app);
        float w = dipToPx(wDp);
        float h = dipToPx(hDp);

        int wcells = getWidgetWCells(app);
        int hcells = getWidgetHCells(app);

        float sw = mStyle.getLauncherSize();

        float cellwidth = sw * 1f;
        float cellheight = cellwidth *1.1f;  // ~square cells


        if (w>0 || wcells>0) {

            if (wcells == 0) wcells = (int) Math.round(Math.max(w / cellwidth * .75, 1));

            if (wcells > 1) {
                int start = GridLayout.UNDEFINED;
                if (wcells > grid.getColumnCount()) {
                    wcells = grid.getColumnCount();
                }
                if (wcells > mStyle.getMaxWCells()) {
                    wcells = mStyle.getMaxWCells();
                }

                lp.columnSpec = GridLayout.spec(start, wcells, GridLayout.FILL);

                //Log.d("widcol", "w=" + w + " wcells=" + wcells + " start=" + start + " cellwidth=" + cellwidth + " r=" + cellwidth * wcells);
            } else {
                lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, GridLayout.FILL);
            }
        }
        if (h>0 || hcells>0) {

            if (hcells == 0) {
                hcells = (int) Math.round(Math.max(h / cellheight * .75, 1));
                if (hcells > mStyle.getMaxWCells()) {
                    hcells = mStyle.getMaxWCells();
                }
            }

            if (hcells > 1) {
                lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, hcells, GridLayout.FILL);
            } else {
                lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, GridLayout.FILL);
            }

        }

        final AppWidgetHostView appwid = mLoadedWidgets.get(app.getActivityName());

        if (appwid != null) {

            lp.width = (int)(cellwidth*wcells*1.16);

            int calcHeight = (int)(cellheight*hcells*(hcells/(hcells-.2)));
            lp.height = calcHeight;
//            if (h > cellheight*hcells*1.3) {
//                lp.height = calcHeight;
//            } else {
//                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;//(int)(cellheight*hcells*1.2);
//            }

            storeWidgetWCells(app, wcells);
            storeWidgetHCells(app, hcells);
            //lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;//(int)(cellheight*hcells*1.2);

            final int hDpf;
            if (h<calcHeight) {
                hDpf = pxToDip(calcHeight);
            } else {
                hDpf = hDp;
            }
            //Log.d("widcol2", "wDp=" + wDp + " w=" + w + " wcells=" + wcells  + " cellwidth=" + cellwidth + " r=" + cellwidth * wcells);
            //Log.d("widcol2", "hDp=" + hDp + " hDpf=" + hDpf + " h=" + h + " hcells=" + hcells  + " cellheight=" + cellheight + " r=" + cellheight * hcells);
            appwid.postDelayed(new Runnable() {
                @Override
                public void run() {
                    appwid.updateAppWidgetSize(null, wDp, hDpf, wDp, hDpf);
                    if (appwid.getParent()!=null) {
                        appwid.getParent().requestLayout();
                    }
                    appwid.requestLayout();
                    appwid.postInvalidate();
                }
            }, 1000);


        }

        return lp;
    }

    @SuppressLint("RtlHardcoded")
    public void showWidgetResize(final AppLauncher appitem) {

        AppWidgetHostView appwid = mLoadedWidgets.get(appitem.getActivityName());
        if (appwid!=null) {
            //final int resizeMode = appwid.getAppWidgetInfo().resizeMode;

            ViewGroup item = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.widget_size, null);


            final PopupWindow pw = new PopupWindow(item,
                    (int)getResources().getDimension(R.dimen.widget_resize_width),
                    (int)getResources().getDimension(R.dimen.widget_resize_height));

            pw.setOutsideTouchable(false);
            pw.setFocusable(true);

            Button ok = item.findViewById(R.id.wid_size_ok);
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pw.dismiss();
                }
            });

            Button defaults = item.findViewById(R.id.wid_set_default_size);

            final SeekBar width = item.findViewById(R.id.width_seek);
            final SeekBar height = item.findViewById(R.id.height_seek);

            final Runnable resize = new Runnable() {
                @Override
                public void run() {
                    //Log.d(TAG, (width.getProgress()+1) +","+ (height.getProgress()+1));
                    storeWidgetWCells(appitem, width.getProgress()+1);
                    storeWidgetHCells(appitem, height.getProgress()+1);

                    GridLayout.LayoutParams lp = getAppLauncherLayoutParams(mIconSheet, appitem);
                    View widframe = getLauncherView(appitem,false);
                    if (widframe != null) {
                        widframe.setLayoutParams(lp);
                    }
                }
            };

            defaults.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    storeWidgetWCells(appitem, 0);
                    storeWidgetHCells(appitem, 0);
                    GridLayout.LayoutParams lp = getAppLauncherLayoutParams(mIconSheet, appitem);
                    View widframe = getLauncherView(appitem,false);
                    if (widframe != null) {
                        widframe.setLayoutParams(lp);
                    }
                    pw.dismiss();
                }
            });


            int wcells = getWidgetWCells(appitem);
            width.setProgress(wcells-1);
            int hcells = getWidgetHCells(appitem);
            height.setProgress(hcells-1);

            TextView widthLab = item.findViewById(R.id.width_num);
            bindSeek(width, widthLab, wcells,  mStyle.getMaxWCells(), resize);


            TextView heightLab = item.findViewById(R.id.height_num);
            bindSeek(height, heightLab, hcells,8, resize);


            int xpos = 0;
            int ypos = mScreenDim.y*2/3;

            int gravity = Gravity.CENTER_HORIZONTAL|Gravity.TOP;

            View widframe = getLauncherView(appitem,false);
            int [] viewpos = new int[2];
            if (widframe!=null) {
                widframe.getLocationOnScreen(viewpos);

                if (viewpos[1] > mScreenDim.y*.3) {
                    ypos = mScreenDim.y/20;
                }
//                else  if (viewpos[1] > mScreenDim.y*.3 && viewpos[1] < mScreenDim.y*.4) {
//                    mIconSheetScroller.smoothScrollBy(0, (int)(mScreenDim.y*.25));
//                }

                if (mScreenDim.x>mScreenDim.y) {
                    gravity = Gravity.LEFT|Gravity.TOP;
                    if (viewpos[0] > mScreenDim.x * .3) {
                        xpos = mScreenDim.x / 24;
                    } else {
                        xpos = mScreenDim.x * 2 / 3;
                    }
                }

            }

            pw.showAtLocation(findViewById(R.id.icon_and_cat_wrap), gravity, xpos, ypos);
        }

    }

    private void bindSeek(SeekBar seekBar, final TextView seekLabel, int start, int max, final Runnable onChange) {

        final int min = 1;
        if (start<min) start = min;
        if (start>max) max = start;
        //seekLabel.setText(start+"");
        seekBar.setMax(max-min);
        seekBar.setProgress(start-min);

        String value = start + "";
        seekLabel.setText(value);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                String value = (i+min) + "";
                seekLabel.setText(value);
                if (onChange!=null) onChange.run();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }



    private int pxToDip(float pixel){
        float scale = getResources().getDisplayMetrics().density;
        return (int)((pixel - 0.5f)/scale);
    }

    private float dipToPx(float dip){
        if (dip==0) return 0;

        float scale = getResources().getDisplayMetrics().density;
        return (int)(dip * scale + .5f);
//        Resources r = getResources();
//        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, r.getDisplayMetrics());
        // return (int)((pixel - 0.5f)/scale);
    }

    private void changeColumnCount(GridLayout gridLayout, int columnCount) {
        if (gridLayout.getColumnCount() != columnCount) {

            List<View> childViews = new ArrayList<>();

            for (int i = gridLayout.getChildCount()-1; i >=0 ; i--) {
                View view = gridLayout.getChildAt(i);
                if (view == null) {
                    Log.d(TAG, "null child at " + i);
                    continue;
                }
                childViews.add(view);
                gridLayout.removeView(view);
            }


            gridLayout.setColumnCount(columnCount);
            Collections.reverse(childViews);

            for (View view: childViews) {
                GridLayout.LayoutParams lp;
                if (view.getTag() instanceof AppLauncher) {
                    AppLauncher app = (AppLauncher) view.getTag();

                    lp = getAppLauncherLayoutParams(gridLayout, app);
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
//            AppLauncher app;
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

    public ViewGroup getLauncherView(final AppLauncher app, boolean smallIcon) {
        return getLauncherView(app, smallIcon, true);
    }

    public ViewGroup getLauncherView(final AppLauncher app, boolean smallIcon, boolean reuse) {


        if (smallIcon) reuse = false;
        ViewGroup item = mAppLauncherViews.get(app);
        if (reuse) {
            if (item!=null) return item;
        }

        if (app.isWidget()) {
            item = new FrameLayout(this);

            AppWidgetHostView appwid = mLoadedWidgets.get(app.getActivityName());
            if (appwid == null) {
                appwid = mWidgetHelper.loadWidget(app);
                if (appwid==null) {
                    Log.d(TAG, "AppWidgetHostView was null for " + app.getActivityName() + " " + app.getPackageName());
                    // db().deleteApp(app.getActivityName());
                    return null;
                }
            }

            mLoadedWidgets.put(app.getActivityName(), appwid);
            AppWidgetProviderInfo pinfo = appwid.getAppWidgetInfo();
            //Log.d(TAG, "Min: " + pinfo.minWidth + "," + pinfo.minHeight);
            //Log.d(TAG, "MinResize: " + pinfo.minResizeWidth + "," + pinfo.minResizeHeight);
            //Log.d(TAG, "Resizemode: " + pinfo.resizeMode);

            storeLauncherDimen(app, pinfo.minWidth, pinfo.minHeight);

            ViewGroup parent = (ViewGroup) appwid.getParent();
            if (parent != null) {
                parent.removeView(appwid);
            }
            item.addView(appwid);
            final View wrap = item;
            appwid.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (mChildLock) {
                        return false;
                    }
                    return MainActivity.this.onLongClick(wrap);
                }
            });
            appwid.setOnDragListener(new View.OnDragListener() {
                @Override
                public boolean onDrag(View view, DragEvent dragEvent) {
                    if (mChildLock) {
                        return false;
                    }
                    return mMainDragListener.onDrag(wrap, dragEvent);
                }
            });



        } else {


            item = (ViewGroup) LayoutInflater.from(this).inflate(smallIcon ? R.layout.launcher_small_icon : R.layout.launcher_icon, null);



            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    view.startAnimation(itemClickedAnim);
                    mLaunchApp.launchApp(app);
                    showButtonBar(false, true);
                    hideCatsIfAutoHide(false);
                }
            });

            ImageView iconImage = item.findViewById(R.id.launcher_icon);

            if (!smallIcon) {

                setLayoutSize(item, mStyle.getLauncherSize(), ViewGroup.LayoutParams.WRAP_CONTENT);

                TextView iconLabel = item.findViewById(R.id.launcher_text);
                iconLabel.setTextColor(mStyle.getTextColor());
                iconLabel.setText(app.getLabel());

                setLayoutSize(iconImage, mStyle.getLauncherIconSize(), mStyle.getLauncherIconSize());

                iconLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, mStyle.getLauncherFontSize());
                setLayoutSize(iconLabel, mStyle.getLauncherSize(), mStyle.getLauncherSize()/2.1);

            }
            app.setIconImage(iconImage);
            app.loadAppIconAsync(this);

            ComponentName compName = app.getBaseComponentName();
            int bcount = GlobState.getBadger(this).getUnreadCount(compName);
            updateAppBadgeCount(item, bcount);
        }
        item.setTag(app);
        item.setClickable(true);
        item.setOnLongClickListener(this);
        item.setOnDragListener(mMainDragListener);

        if (reuse) {
            mAppLauncherViews.put(app, item);
        }
        return item;
    }

    @SuppressLint("SetTextI18n")
    private void updateAppBadgeCount(ViewGroup item, int bcount) {
        if (item!=null) {
            TextView badge = item.findViewById(R.id.launcher_badge);
            if (badge!=null) {
                if (bcount <= 0 || !mAppPreferences.getBoolean(getString(R.string.pref_key_show_badges), true)) {
                    badge.setVisibility(View.GONE);
                } else {
                    badge.setVisibility(View.VISIBLE);
                    badge.setText(bcount + "");
                }
            }
        }
    }

    @Override
    public void badgerCountChanged(ComponentName compname, int count) {
        AppLauncher app = AppLauncher.getAppLauncher(compname);
        if (app!=null) {
            ViewGroup view = mAppLauncherViews.remove(app);
            if (view!=null) {
                updateAppBadgeCount(view,count);
            }

            if (mQuickRow.appAlreadyHere(app)) {
                mQuickRow.repopulate();
            }
        }

    }



    private void setLayoutSize(View view, double width, double height) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp!=null) {
            lp.height = (int) height;
            lp.width = (int) width;
        } else {
            lp = new ViewGroup.LayoutParams((int) width, (int) height);
        }
        view.setLayoutParams(lp);
    }

    public void setupWidget() {
        try {
            mWidgetHelper.popupSelectWidget();
        } catch (Throwable t) {
            //very rare
            Toast.makeText(this, "Can't show widgets: " + t.getLocalizedMessage(),Toast.LENGTH_LONG).show();
            Log.e("Widgets", t.getMessage(), t);
        }
    }

    private String mActionCategory;


    private void addWidget(AppWidgetHostView appwid) {
        if (mChildLock) return;

        ComponentName cn = appwid.getAppWidgetInfo().provider;
        String actvname = cn.getClassName();
        String pkgname = cn.getPackageName();

        String catId = db().getAppCategory(cn);
        if (mActionCategory==null) mActionCategory = mCategory;
        if (catId == null || catId.equals(mActionCategory)) {

            //Log.d(TAG, actvname + " " + pkgname);

            mLoadedWidgets.put(actvname, appwid);

            AppLauncher.removeAppLauncher(cn);
            AppLauncher app = AppLauncher.createAppLauncher(actvname, pkgname, pkgname, mActionCategory, true);

            db().addApp(app);
            db().addAppCategoryOrder(mActionCategory, app.getComponentName());
        } else {
            Toast.makeText(this, getString(R.string.widget_alreay,db().getCategoryDisplay(catId)), Toast.LENGTH_LONG).show();
        }


    }

    private void storeWidgetWCells(AppLauncher app, int wcells) {
        SharedPreferences.Editor ePrefs = mPrefs.edit();

        ePrefs.putInt(app.getComponentName() + "_wcells", wcells);

        ePrefs.apply();

    }

    private void storeWidgetHCells(AppLauncher app, int hcells) {
        SharedPreferences.Editor ePrefs = mPrefs.edit();

        ePrefs.putInt(app.getComponentName() + "_hcells", hcells);

        ePrefs.apply();

    }

    private int getWidgetWCells(AppLauncher app) {
        return mPrefs.getInt(app.getComponentName() + "_wcells", 0);
    }

    private int getWidgetHCells(AppLauncher app) {

        //Log.d(TAG, "in getWidgetHCells " + cells);
        return mPrefs.getInt(app.getComponentName() + "_hcells", 0);
    }



    private void storeLauncherDimen(AppLauncher app, int width, int height) {
        SharedPreferences.Editor ePrefs = mPrefs.edit();

        ePrefs.putInt(app.getComponentName() + "_width", width);

        ePrefs.putInt(app.getComponentName() + "_height", height);

        ePrefs.apply();

    }

    private int getLauncherWidth(AppLauncher app) {
        return mPrefs.getInt(app.getComponentName() + "_width", 0);
    }

    private int getLauncherHeight(AppLauncher app) {
        return mPrefs.getInt(app.getComponentName() + "_height", 0);
    }


    private Style.CategoryTabStyle getDefaultCategoryStyle(String category) {
        Style.CategoryTabStyle catstyle =  Style.CategoryTabStyle.Normal;

        if (category.equals(mCategory)) {
            catstyle =  Style.CategoryTabStyle.Selected;
        } else if (db().isHiddenCategory(category) || Categories.isHiddenCategory(category)) {
            catstyle =  Style.CategoryTabStyle.Hidden;
        } else if (db().isTinyCategory(category)) {
            catstyle =  Style.CategoryTabStyle.Tiny;
        }
        return catstyle;
    }

    private void styleCategorySpecial(TextView categoryTab,  Style.CategoryTabStyle catstyle) {
        styleCategorySpecial(categoryTab, catstyle, mRevCategoryMap.get(categoryTab));
    }


    private void styleCategorySpecial(TextView categoryTab,  Style.CategoryTabStyle catstyle, String category) {

        if (catstyle ==  Style.CategoryTabStyle.Default) {
            catstyle = getDefaultCategoryStyle(category);
        }
        mStyle.styleCategoryStyle(categoryTab, catstyle, isAutohide());

    }

    @SuppressLint("ClickableViewAccessibility")
    private TextView createCategoryTab(final String category, final GridLayout iconSheet, int pos) {
        final TextView categoryTab = new TextView(this);
        categoryTab.setText(db().getCategoryDisplay(category));
        categoryTab.setTag(category);
        // categoryTab.setWidth((int)Utils.dpToPx(this,categoryTabWidth));

        categoryTab.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        final Style.CategoryTabStyle catstyle = getDefaultCategoryStyle(category);

        if (Categories.isHiddenCategory(category) || db().isHiddenCategory(category)) {
            categoryTab.setVisibility(View.GONE);
        }

        if (catstyle == Style.CategoryTabStyle.Normal) {
            lp.weight = 1;
        }
        lp.gravity = Gravity.CENTER;
        lp.setMargins(2, 4, 2, 3);

        categoryTab.setLayoutParams(lp);

        categoryTab.setGravity(Gravity.CENTER);

        styleCategorySpecial(categoryTab, Style.CategoryTabStyle.Default, category);


        categoryTab.setClickable(true);
        categoryTab.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                cancelHide();
                return false;
            }
        });
        categoryTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mChildLock) return;
                // view.startAnimation(itemClickedAnim);
                switchCategory(category);
                hideCatsIfAutoHide(true);

            }
        });
        categoryTab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View view) {
                if (mChildLock) return true;


                if (mActionMenu.useExtraActions() || !mActionMenu.useDropZones()) {
                    mActionCategory = category;
                    mActionMenu.showCatagoryActionMenu(categoryTab);
                    setMenuOnTouchListener(categoryTab, false, new Runnable() {
                        @Override
                        public void run() {
                            startDragCategory(categoryTab, category);
                            categoryTab.setOnTouchListener(new View.OnTouchListener() {
                                @Override
                                public boolean onTouch(View view, MotionEvent motionEvent) {
                                    cancelHide();
                                    return false;
                                }
                            });
                        }
                    });

                } else {
                    startDragCategory(categoryTab, category);

                }

                return true;
            }
        });

        categoryTab.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View overView, final DragEvent event) {
                if (mChildLock) return true;
                View dragObj = (View) event.getLocalState();
                if (dragObj==null) {
                    Log.e(TAG, "dragobj was null; " + event);
                    return true;
                }
                boolean isAppLauncher = dragObj.getTag() instanceof AppLauncher;
                boolean isNoDrop = Categories.isNoDropCategory(category);
                boolean isCurrent = category.equals(mCategory);
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_ENTERED:
                        if (!isCurrent && !isNoDrop) {
                            styleCategorySpecial(categoryTab, Style.CategoryTabStyle.DragHover);
                        }
                        //Log.d(TAG, "DRAG_ENTERED: " + category + ((AppLauncher)dragObj.getTag()).getActivityName());

                        break;

                    case DragEvent.ACTION_DRAG_LOCATION:

                        scrollOnDrag(overView, event, mCategoriesScroller);
                        break;
                    case DragEvent.ACTION_DRAG_ENDED:
                        mBeingDragged = null;
                        hideRemoveDropzone();
                        hideHiddenCategories();
                    case DragEvent.ACTION_DRAG_EXITED:
                        //Log.d(TAG, "DRAG_EXITED: " + category + ((AppLauncher)dragObj.getTag()).getActivityName());

                        styleCategorySpecial(categoryTab, Style.CategoryTabStyle.Default);

                        break;

                    case DragEvent.ACTION_DROP:
                        if (isAppLauncher) {
                            if (!isNoDrop && !isCurrent) {
                                mBeingDragged.setCategory(category);
                                db().updateAppCategory(mBeingDragged.getActivityName(), mBeingDragged.getPackageName(), category);
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

        if (pos == -1 && !category.equals(Categories.CAT_HIDDEN)) {
            pos = getCategoryPos(Categories.CAT_SEARCH);
        }

        if (pos>-1) {
            mCategoriesLayout.addView(categoryTab, pos);
        } else {

            mCategoriesLayout.addView(categoryTab);
        }

        return categoryTab;
    }

    private void startDragCategory(View view, String category) {
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
        cancelHide();
    }

    private long mDropZoneHover=0;
    private final View.OnDragListener mMainDragListener = new View.OnDragListener() {
        @Override
        public boolean onDrag(View droppedOn, DragEvent event) {
            if (mChildLock) return false;

            View dragObj = (View) event.getLocalState();
            if (dragObj==null) return true;

            boolean isLauncher = true;
            boolean isSpecial = false;
            boolean isApplink = false;
            if (dragObj.getTag() == null || !(dragObj.getTag() instanceof AppLauncher)) {
                isLauncher = false;
            } else  {
                AppLauncher app = (AppLauncher)dragObj.getTag();
                isSpecial = app.isLink() || app.isWidget();
                isApplink = app.isAppLink();
            }

            if (mCategory.equals(Categories.CAT_SEARCH) && isAncestor(mIconSheet, droppedOn)) return false;

            boolean nocolor = droppedOn instanceof GridLayout || droppedOn == mRemoveDropzone
                    || droppedOn == mLinkDropzone || !isLauncher || mQuickRow.isSelf(mDragDropSource)
                    || isAncestor(mSearchBox.getSearchView(), droppedOn);

            //prevent dropping categories anywhere but category area and trash
            if (mDragDropSource==mCategoriesLayout && !(droppedOn==mCategoriesLayout || droppedOn==mRemoveDropzone )) {
                return false;
            }

            if ((isSpecial && !isApplink) && (mQuickRow.isSelf(droppedOn) || isAncestor(mQuickRow.getGridLayout(), droppedOn))) return false;

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // do nothing
                    //Log.d(TAG, "" + dragObj.getTag());

                    if (mCategory.equals(Categories.CAT_SEARCH)) {
                        try {
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null)
                                imm.hideSoftInputFromWindow(findViewById(R.id.search_box).getWindowToken(), 0);
                        } catch (Throwable t) {
                            //Log.e(TAG, t.getMessage(), t);
                        }
                    }

                    break;

                case DragEvent.ACTION_DRAG_LOCATION:
                    //scroll the scrollview

                    if (isLauncher) {
                        scrollOnDrag(droppedOn, event, mIconSheetScroller);
                        hscrollOnDrag(droppedOn, event, mQuickRow.getScroller());

                        if (mActionMenu.useDropZones()) {
                            if (!isSpecial && !mCategory.equals(Categories.CAT_SEARCH) && mLinkDropzone.getVisibility() != View.VISIBLE && (droppedOn == mRemoveDropzone || droppedOn == mLinkDropzonePeek) && System.currentTimeMillis() - mDropZoneHover > 400) {
                                //mLinkDropzone.setVisibility(View.VISIBLE);
                                animateShow(mLinkDropzone, AnimateDirection.Right);
                                //mLinkDropzonePeek.setVisibility(View.GONE);
                                animateDownHide(mLinkDropzonePeek);
                                // Log.d(TAG, "mLinkDropzone.setVisibility(View.VISIBLE)");
                            }
                        }
                    }
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    if (!nocolor ) {
                        droppedOn.setBackgroundColor(mStyle.getDragoverBackground());
                    }
                    if (droppedOn==mRemoveDropzone || droppedOn==mLinkDropzonePeek) {
                        mDropZoneHover = System.currentTimeMillis();
                        //Log.d("LaunchTime", "DRAG_ENTERED: " + ((AppLauncher)dragObj.getTag()).getActivityName());
                    }
                    //Log.d("LaunchTime", "DRAG_ENTERED: " + ((AppLauncher)dragObj.getTag()).getActivityName());
                    break;
                case DragEvent.ACTION_DRAG_EXITED:

                    if (!nocolor) droppedOn.setBackgroundColor(mStyle.getBackgroundDefault());
                    break;
                case DragEvent.ACTION_DROP:

                    //Log.d("dropon", droppedOn.toString());

                    if (!nocolor) droppedOn.setBackgroundColor(mStyle.getBackgroundDefault());
                    // Dropped, reassign View to ViewGroup

                    if (dragObj == droppedOn) {
                        // Log.d(TAG, "self drop");
                        hideCatsIfAutoHide(false);
                        break;
                    }

                    try {
                        if (handleDrop(droppedOn, dragObj, isLauncher)) return true;
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    if (!nocolor) droppedOn.setBackgroundColor(mStyle.getBackgroundDefault());
                    mBeingDragged = null;
                    hideRemoveDropzone();
                    hideHiddenCategories();
                    break;
                default:
                    break;
            }
            return true;
        }

        private boolean handleDrop(View droppedOn, View dragObj, boolean isLauncher) {
            if (mChildLock) return false;

            ViewGroup target;
            Object droppedOnTag = droppedOn.getTag();
            if (droppedOn == mRemoveDropzone) {  // need to delete the dropped thing
                //Stuff to be deleted
                if (mQuickRow.isSelf(mDragDropSource)) {
                    removeDroppedItem(dragObj);
                    hideCatsIfAutoHide(false);
                } else if (mDragDropSource == mIconSheets.get(Categories.CAT_SEARCH)) {
                    removeDroppedRecentItem(dragObj);
                    hideCatsIfAutoHide(false);
                } else if (mBeingDragged != null && (mBeingDragged.isWidget() || mBeingDragged.isLink())) {
                    removeDroppedItem(dragObj);
                    mSearchBox.refreshSearch(true);
                    hideCatsIfAutoHide(false);

                } else if (mDragDropSource == mCategoriesLayout && !isLauncher) {
                    //delete category tab
                    promptDeleteCategory((String) dragObj.getTag());

                } else {
                    //uninstall app
                    if (mBeingDragged!=null) {
                        launchUninstallIntent(mBeingDragged, mDragDropSource, dragObj);
                    }
                    hideCatsIfAutoHide(true);
                }
                setAllIconSheetsLayout();
                return true;
            } else if (droppedOn == mLinkDropzone) {
                hideCatsIfAutoHide(false);
                if (isLauncher) {
                    AppLauncher app = (AppLauncher)dragObj.getTag();
                    makeAppLink(app);
                } else {
                    Log.d(TAG, "non-launcher dropped on linker: " + dragObj + " tag=" + dragObj.getTag());
                }
                return true;
            } else if (droppedOn instanceof GridLayout) {
                target = (GridLayout) droppedOn;

                hideCatsIfAutoHide(droppedOn!=mIconSheet);
            } else if (droppedOn instanceof FrameLayout) {
                target = (FrameLayout) droppedOn;
                hideCatsIfAutoHide(false);
            } else if (droppedOn.getParent() instanceof GridLayout){
                target = (GridLayout) droppedOn.getParent();
                hideCatsIfAutoHide(false);
            } else {
                hideCatsIfAutoHide(true);
                return true;
            }

            if (droppedOnTag!=null && droppedOnTag instanceof AppLauncher) {
                if (((AppLauncher)droppedOnTag).isWidget()) {
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
            if (mQuickRow.isSelf(mDragDropSource)  && mQuickRow.isSelf(target)) remove = true;

            if (!mCategory.equals(Categories.CAT_SEARCH)) {
                if (!mQuickRow.isSelf(mDragDropSource) && !mQuickRow.isSelf(target)) remove = true;
            }

            if (remove) {
                mDragDropSource.removeView(dragObj);
            } else {
                if (mQuickRow.isSelf(target)) {
                    if (!mQuickRow.isSelf(mDragDropSource)) {
                        //prevent copies of the same app on the quickrow
                        if (mQuickRow.appAlreadyHere((AppLauncher) dragObj.getTag())) {
                            return true;
                        }
                    }
                    //make a copy of the launcher to put on the quickbar
                    dragObj = getLauncherView(AppLauncher.createAppLauncher((AppLauncher) dragObj.getTag(), true), true);

                } else {
                    dragObj = getLauncherView(AppLauncher.createAppLauncher((AppLauncher) dragObj.getTag()), false, false);
                }
            }


            if (!(target != mQuickRow.getGridLayout() && mQuickRow.getGridLayout() == mDragDropSource)) {
                try {
                    ViewParent parent = dragObj.getParent();
                    if (parent!=null) {
                        Log.e(TAG, "dragObj " + dragObj + " still has parent " + parent, new Throwable() );
                        ((ViewGroup)parent).removeView(dragObj);
                    }

                    ViewGroup.LayoutParams lp = null;
                    if (target instanceof GridLayout && dragObj.getTag() instanceof AppLauncher) {
                        lp = getAppLauncherLayoutParams((GridLayout)target, (AppLauncher)dragObj.getTag());
                    }

                    if (index == -1) {
                        target.addView(dragObj, lp);
                    } else {
                        target.addView(dragObj, index, lp);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "exception adding icon to sheet", e);
                    Toast.makeText(MainActivity.this,"Couldn't place icon: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            //save the new order
            db().setAppCategoryOrder(mRevCategoryMap.get(target), target);
            if (!target.equals(mDragDropSource)) {
                db().setAppCategoryOrder(mRevCategoryMap.get(mDragDropSource), mDragDropSource);
            }

            if (mCategory.equals(Categories.CAT_SEARCH)) {
                mSearchBox.refreshSearch(true);
            }
            setAllIconSheetsLayout();
            return false;
        }

        private void removeDroppedRecentItem(View dragObj) {
            if (mChildLock) return;

            try {
                db().deleteAppLaunchedRecord(mBeingDragged.getComponentName());
                mDragDropSource.removeView(dragObj);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
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
                    removeWidget(mBeingDragged);
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }

    };

    public void makeAppLink(AppLauncher app) {
        Log.d(TAG, "Making link: " + app.getActivityName() + " " + app.getPackageName());
        AppLauncher applauncher = app.makeAppLink();
        String category = mCategory;
        if (Categories.isNoDropCategory(category)) {
            category = Categories.CAT_OTHER;
        }
        applauncher.setCategory(category);
        db().addApp(applauncher);
        repopulateIconSheet(category);
    }

    public void removeWidget(AppLauncher app) {
        db().deleteApp(app.getComponentName());
        AppWidgetHostView wid = mLoadedWidgets.remove(app.getActivityName());
        if (wid != null) {
            mWidgetHelper.widgetRemoved(wid.getAppWidgetId());
        }
    }


    public boolean isOnQuickRow(View view) {
        return isAncestor(mQuickRow.getGridLayout(), view);
    }

    public boolean isOnSearchView(View view) {
        return isAncestor(mSearchBox.getSearchView(), view);
    }

    public static boolean isAncestor(ViewGroup potentialParent, View potentialChild) {

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


    private View mDragPotential;
    @Override
    public boolean onLongClick(final View view) {
        if (mChildLock) return false;

        final AppLauncher dragitem = (AppLauncher) view.getTag();
        mDragPotential = view;
        final boolean iswidget = dragitem.isWidget();

        if ((mActionMenu.useActionMenus() || iswidget) && mActionMenu.displayActionShortcuts(view, dragitem)) {

            setMenuOnTouchListener(view, iswidget, new Runnable() {
                @Override
                public void run() {
                    startDrag();
                }
            });

        } else {
            startDrag();
        }
        return true;
    }

    private void setMenuOnTouchListener(final View view, final boolean iswidget, final Runnable startDrag) {
        View.OnTouchListener tl = new View.OnTouchListener() {
            float oX = -1;
            float oY = -1;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getActionMasked() == MotionEvent.ACTION_CANCEL && event.getSource()!= InputDevice.SOURCE_ANY) {
                    if (view != null) {
                        view.setOnTouchListener(null);
                        if (iswidget) {
                            setTouchListener(view, null);
                        }
                    }
                    mActionMenu.dismissActionPopup();
                    startDrag.run();
                } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {

                    if (oX == -1) {
                        oX = event.getX();
                        oY = event.getY();
                    } else {
                        final int slop  = ViewConfiguration.get(MainActivity.this).getScaledTouchSlop();
                        if (Math.abs(oX - event.getX()) > slop || Math.abs(oY - event.getY()) > slop) {
                            clearDragPotential(false);
                            mActionMenu.dismissActionPopup();
                            startDrag.run();
                        }
                    }
                    //Log.d("movet", event.getX() + "," + event.getY());
                } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    clearDragPotential(true);
                    setTouchListener(view, null);
                    //dismissActionPopup();
                } else {
                    Log.d(TAG, event.getActionMasked() + " event");
                }


                return false;
            }
        };

        view.setOnTouchListener(tl);

        if (iswidget) {
            setTouchListener(view, tl);
        }
    }

    private void setTouchListener(View view, View.OnTouchListener tl) {
        if (view==null) return;
        view.setOnTouchListener(tl);
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup)view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View vc = vg.getChildAt(i);
                setTouchListener(vc, tl);
            }
        }
    }

    private void startDrag() {

        if (mDragPotential==null) return;

        AppLauncher dragitem = (AppLauncher) mDragPotential.getTag();
        String label = dragitem.getLabel();
        ClipData data = ClipData.newPlainText(label, label);
        View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(mDragPotential);

        boolean dragstarted;
        if (Build.VERSION.SDK_INT>=24) {
            dragstarted = mDragPotential.startDragAndDrop(data, shadowBuilder, mDragPotential, 0);
        } else {
            dragstarted = mDragPotential.startDrag(data, shadowBuilder, mDragPotential, 0);
        }

        if (dragstarted) {
            mBeingDragged = dragitem;
            mDragDropSource = (ViewGroup) mDragPotential.getParent();
            Log.d(TAG, "Drag started: " + dragitem.getActivityName() +  ", source = " + mDragDropSource);
            showCats(true);
            showHiddenCategories();
            // Log.d(TAG, "source = " + mDragDropSource);
            //if (mDragDropSource.getId()!=R.id.icontarget) {
            showRemoveDropzone();
            //}
        }
        mDragPotential = null;

    }


    public void clearDragPotential(boolean nullit) {
        if (mDragPotential!=null) {
            mDragPotential.setOnTouchListener(null);
            if (mDragPotential instanceof ViewGroup) {
                setTouchListener(mDragPotential, null);
            }
        }
        if (nullit) {
            mDragPotential = null;
        }
    }


    private void showHiddenCategories() {
        for (String cat: db().getCategories()) {
            final View cattab = mCategoryTabs.get(cat);
            if (mCategoryTabs!=null && cat!=null && cattab!=null) {
                if (mAnimationDuration>0 && cattab.getVisibility() == View.GONE) {

                    animateChangingSize(cattab, 0, mStyle.getCategoryTabPaddingHeight()*2, new Runnable() {
                        @Override
                        public void run() {
                            cattab.setVisibility(View.VISIBLE);
                        }
                    }, null);
                } else {
                    cattab.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void hideHiddenCategories() {

        if (isAutohide()) {
            return;
        }

        for (String cat : db().getCategories()) {
            boolean isNewCat = mCategoryJustCreated!=null && cat.equals(mCategoryJustCreated);

            final TextView cattab = mCategoryTabs.get(cat);

            if (cattab!=null) {
                if (mCategory.equals(cat)) {
                    cattab.setVisibility(View.VISIBLE);
                } else if ((Categories.isHiddenCategory(cat) || db().isHiddenCategory(cat))
                        || (!cat.equals(Categories.CAT_SEARCH) && !mCategory.equals(cat) && db().getAppCount(cat) == 0 && !isNewCat)) {

                    if (mAnimationDuration>0 && cattab.getVisibility() != View.GONE) {
                        int h = cattab.getHeight();
                        animateChangingSize(cattab, h, 0, null, new Runnable() {
                            @Override
                            public void run() {
                                cattab.setVisibility(View.GONE);
                            }
                        });
                    } else {
                        cattab.setVisibility(View.GONE);
                    }
                } else {
                    cattab.setVisibility(View.VISIBLE);
                }
            }
        }

    }

    private void animateChangingSize(final View view, int startsize, int newsize, final Runnable before, final Runnable after) {
        final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)view.getLayoutParams();
        lp.height = startsize;
        view.setLayoutParams(lp);

        ValueAnimator o = ValueAnimator.ofInt(startsize,newsize);
        o.setDuration(mAnimationDuration);
        o.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                lp.height = (int)valueAnimator.getAnimatedValue();
                view.setLayoutParams(lp);
            }
        });
        o.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (before!=null) before.run();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                view.setLayoutParams(lp);
                if (after!=null) after.run();
            }
        });


        o.start();
    }

    private void showRemoveDropzone() {
        if (mChildLock) return;

        showButtonBar(false, false);

        if (mActionMenu.useDropZones()) {
            if (mDragDropSource == mQuickRow.getGridLayout()
                    || mDragDropSource == mCategoriesLayout
                    || mDragDropSource == mIconSheets.get(Categories.CAT_SEARCH)
                    || (mBeingDragged != null && (mBeingDragged.isWidget() || mBeingDragged.isLink())
            )) {
                mRemoveDropzone.setBackgroundColor(Color.YELLOW);
                //mRemoveAppText.setText(getString(R.string.remove_launcher) + "\n\u267B");
                mRemoveAppText.setText(R.string.remove_launcher);
                mRemoveAppText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.recycle);
                mRemoveAppText.setTextColor(Color.BLACK);
                //mLinkDropzonePeek.setVisibility(View.GONE);
                animateDownHide(mLinkDropzonePeek);
            } else {
                mRemoveDropzone.setBackgroundColor(Color.RED);
                // mRemoveAppText.setText(getString(R.string.uninstall_app) + "\n" + new String(Character.toChars(0x1F5D1)));
                mRemoveAppText.setText(R.string.uninstall_app);
                mRemoveAppText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.trash);
                //mRemoveAppText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.showy, 0,0,R.drawable.trash);
                mRemoveAppText.setTextColor(Color.WHITE);
                if (!Categories.CAT_SEARCH.equals(mCategory)) {
                    //mLinkDropzonePeek.setVisibility(View.VISIBLE);
                    animateUpShow(mLinkDropzonePeek);
                }
            }
            animateUpShow(mRemoveDropzone);
        }
        //mRemoveDropzone.setVisibility(View.VISIBLE);
        animateDownHide(mShowButtons);
        animateUpShow(mHideButtons);
        //mShowButtons.setVisibility(View.GONE);
    }

    private void hideRemoveDropzone() {

        animateDownHide(mRemoveDropzone);
        animateHide(mLinkDropzone, AnimateDirection.Right);
        animateDownHide(mLinkDropzonePeek);
//        mRemoveDropzone.setVisibility(View.GONE);
//        mLinkDropzone.setVisibility(View.GONE);
//        mLinkDropzonePeek.setVisibility(View.GONE);
        //mShowButtons.setVisibility(View.VISIBLE);
        animateUpShow(mShowButtons);
        animateDownHide(mHideButtons);
    }

    public void launchUninstallIntent(AppLauncher app, View launcher) {
        launchUninstallIntent(app, null, launcher);
    }

    private void launchUninstallIntent(AppLauncher app, ViewGroup parent, View launcher) {
        if (mChildLock) return;

        mDragDropSource = parent;
        mBeingUninstalled = launcher;
        Log.d(TAG, "Uninstalling " + app.getPackageName());
        Uri packageUri = Uri.parse("package:" + app.getPackageName());
        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
        uninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        startActivityForResult(uninstallIntent, UNINSTALL_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == UNINSTALL_RESULT) {
                final ComponentName actvname = ((AppLauncher) mBeingUninstalled.getTag()).getComponentName();
                switch (resultCode) {
                    case RESULT_OK:
                        db().deleteApp(actvname);
                        mQuickRow.removeFromQuickApps(actvname);
                        AppLauncher.removeAppLauncher(actvname);
                        mSearchBox.refreshSearch(true);
                        if (mDragDropSource!=null) {
                            mDragDropSource.removeView(mBeingUninstalled);
                            db().setAppCategoryOrder(mRevCategoryMap.get(mDragDropSource), mDragDropSource);
                        }
                        Toast.makeText(this, R.string.app_was_uninstalled, Toast.LENGTH_SHORT).show();
                        break;
                    case RESULT_CANCELED:
                        Toast.makeText(this, R.string.uninstall_canceled, Toast.LENGTH_LONG).show();
                        break;
                    default:
                        AlertDialog.Builder build = new AlertDialog.Builder(this);
                        build.setMessage(R.string.could_not_uninstall);
                        build.setPositiveButton(R.string.go_to_settings, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    openInSettings(actvname.getPackageName());
                                } catch (Throwable t) {
                                    Log.e(TAG, t.getMessage(), t);
                                }
                            }
                        });
                        build.setNeutralButton(R.string.cancel, null);
                        build.show();
                        //Toast.makeText(this, R.string.could_not_uninstall, Toast.LENGTH_LONG).show();

                }
            } else {
                AppWidgetHostView appwid = mWidgetHelper.onActivityResult(requestCode, resultCode, data);
                if (appwid == null) {
                    Log.d(TAG, "appwid is null.");
                    ComponentName cn = mWidgetHelper.getComponentNameFromIntent(data);
                    if (cn != null) {
                        Log.d(TAG, "classname is " + cn.getClassName());
                        db().deleteApp(cn);
                    } else {
                        super.onActivityResult(requestCode, resultCode, data);
                    }
                } else {
                    addWidget(appwid);
                }
            }
            //  setCategoryTabStyles();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void openInSettings(String packagename) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packagename));
            startActivity(intent);
        } catch (ActivityNotFoundException e ) {
            Toast.makeText(this, "Package not found", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
            startActivity(intent);
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
                                       String defFullName, boolean defIsTiny, boolean defIsHidden, final CategoryChangerListener categoryChangerListener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        ViewGroup view = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.category_name, null);

        TextView messageView = view.findViewById(R.id.message_txt);

        final TextView catDeletedLabel = view.findViewById(R.id.cat_deleted_label);
        final Spinner catDeletedSpinner = view.findViewById(R.id.cat_deleted_spinner);

        final EditText shortname = view.findViewById(R.id.shortname);
        final EditText fullname = view.findViewById(R.id.fullname);
        final CheckBox isTiny = view.findViewById(R.id.istiny_checkbox);
        final CheckBox isHidden = view.findViewById(R.id.ishidden_checkbox);

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
        if (Categories.isHiddenCategory(category)) {
            isHidden.setVisibility(View.INVISIBLE);
        } else {
            isHidden.setVisibility(View.VISIBLE);
            isHidden.setChecked(defIsHidden);
        }

        builder.setView(view);

        builder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String delcat = (String)catDeletedSpinner.getSelectedItem();
                categoryChangerListener.onClick(dialog, which, (delcat!=null && delcat.length()>0?delcat:category), shortname.getText().toString(), fullname.getText().toString(), isTiny.isChecked(), isHidden.isChecked());
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm!=null) imm.hideSoftInputFromWindow(shortname.getWindowToken(), 0);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm!=null) imm.hideSoftInputFromWindow(shortname.getWindowToken(), 0);
            }
        });


        builder.show();
    }


    ///////////
    // Category manipulation

    public void promptRenameCategory(final String category) {

        promptGetCategoryName(getString(R.string.rename_cat),
                getString(Categories.isSpeacialCategory(category)?R.string.rename_cat3:R.string.rename_cat2),
                category,
                db().getCategoryDisplay(category),
                db().getCategoryDisplayFull(category),
                db().isTinyCategory(category),
                db().isHiddenCategory(category),
                new CategoryChangerListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, String category, String newDisplayName, String newDisplayFullName, boolean isTiny, boolean isHidden) {
                        try {
                            renameCategory(category, newDisplayName, newDisplayFullName, isTiny, isHidden);
                        } catch (IllegalArgumentException e) {

                            Toast.makeText(MainActivity.this, R.string.need_name, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void renameCategory(String category, String newDisplayName, String newDisplayFullName, boolean isTiny, boolean isHidden) {
        newDisplayName = newDisplayName.trim();
        newDisplayFullName = newDisplayFullName.trim();

        if (newDisplayFullName.length() == 0) {
            newDisplayFullName = newDisplayName;
        }

        if (newDisplayName.length() < 1) {
            throw new IllegalArgumentException("Must give a name");
        }

        if (db().updateCategory(category, newDisplayName, newDisplayFullName, isTiny, isHidden)) {

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

    public void promptAddCategory() {

        promptGetCategoryName(getString(R.string.add_cat),
                getString(R.string.add_cat2),
                "",
                "",
                "",
                false,
                false,
                new CategoryChangerListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, String category, String newDisplayName, String newDisplayFullName, boolean isTiny, boolean isHidden) {
                        try {
                            addCategory(category, newDisplayName, newDisplayFullName, isTiny, isHidden);
                        } catch (IllegalArgumentException e) {

                            Toast.makeText(MainActivity.this, R.string.need_name, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void addCategory(String category, String newDisplayName, String newDisplayFullName,  boolean isTiny, boolean isHidden) {
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
        mCategoryJustCreated = category;

        Log.d(TAG, category +", " + newDisplayName +", " +  newDisplayFullName +", " +  isTiny);
        if (db().addCategory(category, newDisplayName, newDisplayFullName, isTiny, isHidden)) {
            if (mActionCategory==null) mActionCategory = mCategory;

            createIconSheet(category, getCategoryPos(mActionCategory));

            switchCategory(category);

            db().setCategoryOrder(mCategoriesLayout);
        } else {
            Toast.makeText(MainActivity.this, R.string.no_add_cat, Toast.LENGTH_SHORT).show();
        }
    }

    public void promptDeleteCategory(final String category) {

        if (Categories.isSpeacialCategory(category)) return;

        final String message = getString(R.string.cat_deleted, db().getCategoryDisplay(Categories.CAT_OTHER));
        new AlertDialog.Builder(MainActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.delete_cat)
                .setMessage(R.string.delete_cat_prompt)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (deleteCategory(category)) {
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    }

                })
                .setNegativeButton(R.string.cancel, null)
                .show();

    }



    private boolean deleteCategory(final String category) {
        if (Categories.isSpeacialCategory(category)) return false;

        TextView categoryTab = mCategoryTabs.get(category);

        boolean appsInCat = db().getAppCount(category) > 0;

        if (db().deleteCategory(category)) {

            View iconSheet = mIconSheets.get(category);
            mRevCategoryMap.remove(iconSheet);

            mCategoryTabs.remove(category);
            mRevCategoryMap.remove(categoryTab);

            mCategoriesLayout.removeView(categoryTab);

            //repopulateIconSheet(Categories.CAT_OTHER);
            //String newcat = mCategoryTabs.keySet().iterator().next();

            if (category.equals(mCategory) && appsInCat) {
                switchCategory(Categories.CAT_OTHER);
                mCategoryTabs.get(Categories.CAT_OTHER).setVisibility(View.VISIBLE);
                return true;
            } else if (category.equals(mCategory)) {
                switchCategory(getTopCategory());
            }

        } else {
            Toast.makeText(MainActivity.this, R.string.no_delete_cat, Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    public void hideCategory(String category) {
        db().hideCategory(category, !db().isHiddenCategory(category));
        setCategoryTabStyles();
        //hideHiddenCategories();
        showButtonBar(false, true);
    }


    private static final int APPSORT_NONE = -1;
    private static final int APPSORT_LABEL = 0;
    private static final int APPSORT_USAGE = 1;
    private static final int APPSORT_INSTALL_REV = 2;
    private static final int APPSORT_INSTALL = 3;
    private static final int APPSORT_PACKAGE = 4;

    public void promptSortCategory(final String category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.sort_prompt_title);

        builder.setItems(R.array.sort_strings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                sortCategory(category, i);
                repopulateIconSheet(category);

            }
        });

        builder.setNegativeButton(R.string.cancel, null);

        builder.show();
    }

    private void sortCategory(String category, final int sortby) {

        final List<ComponentName> recents = db().getAppLaunchedList();
        if (sortby != APPSORT_NONE) {
            List<AppLauncher> apps = db().getApps(category);
            Collections.sort(apps, new Comparator<AppLauncher>() {
                @Override
                public int compare(AppLauncher appfirst, AppLauncher appsecond) {
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

        mIconsArea = findViewById(R.id.iconarea_wrap);

        mCategoriesLayout = findViewById(R.id.layout_categories);

        mIconSheetTopFrame = findViewById(R.id.layout_icons_topframe);
        mIconSheetScroller = findViewById(R.id.layout_icons_scroller);

        mIconSheetHolder = findViewById(R.id.icon_sheet_holder);

        mIconSheetBottomFrame = findViewById(R.id.layout_icons_bottomframe);

        mRemoveDropzone = findViewById(R.id.remove_dropzone);
        mRemoveDropzone.setOnDragListener(mMainDragListener);
        mRemoveAppText = findViewById(R.id.remove_dz_txt);

        mLinkDropzone = findViewById(R.id.link_dropzone);
        mLinkDropzone.setOnDragListener(mMainDragListener);

        mLinkDropzonePeek = findViewById(R.id.link_dropzone_peek);

        //((TextView) findViewById(R.id.link_dz_text)).setCompoundDrawablesWithIntrinsicBounds(0,0,0,R.drawable.linkicon);


        mCategoriesScroller = findViewById(R.id.layout_categories_scroller);

        mIconSheetScroller.setHSwipeListener(mHSwipeListener);
        mCategoriesScroller.setHSwipeListener(mHSwipeListener);

        mProgressBar = findViewById(R.id.progressBar);



        mIconSheets = new TreeMap<>();
        mCategoryTabs = new TreeMap<>();
        mRevCategoryMap = new HashMap<>();
        mRevCategoryMap.put(mQuickRow.getGridLayout(), QuickRow.QUICK_ROW_CAT);

        mShowButtons = findViewById(R.id.show_buttonbar);

        mShowButtons.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelHide();
                showButtonBar(true, !isAutohide());
                hideCatsIfAutoHide(false);
            }
        });


        mHideButtons = findViewById(R.id.hide_buttonbar);

        mHideButtons.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelHide();
                showButtonBar(false, !isAutohide());
                hideCatsIfAutoHide(false);
            }
        });


        mShowCats = findViewById(R.id.show_cats_buttom);
        mShowCats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelHide();
                if (!mChildLock) {
                    showCats(true);
                    showButtonBar(true, true);
                }
            }
        });

        mSortCategoryButton = findViewById(R.id.btn_sort_cat);
        mAddCategoryButton = findViewById(R.id.btn_add_cat);
        mRenameCategoryButton = findViewById(R.id.btn_rename_cat);

        mEditWidgetsButton = findViewById(R.id.btn_widgets);
        mOpenPrefsButton = findViewById(R.id.btn_prefs);
        mOpenPrefs2Button = findViewById(R.id.btn_prefs2);



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

        View.OnClickListener prefsclick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettings(MainActivity.this);
                showButtonBar(false, true);
            }
        };

        mOpenPrefsButton.setOnClickListener(prefsclick);
        mOpenPrefs2Button.setOnClickListener(prefsclick);


        mIconSheetScroller.setOnTouchListener(mDismissClick);
        mIconSheetScroller.setOnPositionChangedListener(new InteractiveScrollView.OnPositionChangedListener() {
            @Override
            public void onPositionChanged(float percentUp, float percentDown, int distFromTop, int distFromBottom) {
                mActionMenu.dismissActionPopup();
                hideCatsIfAutoHide(false);
            }
        });

    }

    private final View.OnTouchListener mDismissClick = new View.OnTouchListener() {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mActionMenu.dismissActionPopup();
                mActionMenu.dismissAppinfo();
                hideCatsIfAutoHide(false);
            }
            return false;
        }
    };


    public static void openSettings(Activity activity) {
        Intent settingsIntent = new Intent(activity, SettingsActivity.class);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        activity.startActivity(settingsIntent);
    }


    //initialize the form members

    public void showButtonBar(boolean makevisible, boolean hideCats) {
        if (mChildLock) return;


//        StackTraceElement from = Thread.currentThread().getStackTrace()[3];
//
//        Log.d(TAG,"showButtonBar(" + makevisible + ", " + hideCats + ") from "  + from.getMethodName() + " line " + from.getLineNumber());

        if (makevisible) {
            mActionCategory = mCategory;
            showHiddenCategories();
            if (!mActionMenu.useExtraActions()) {
                if (mCategory.equals(Categories.CAT_SEARCH)) {
                    mSortCategoryButton.setVisibility(View.INVISIBLE);
                    mEditWidgetsButton.setVisibility(View.INVISIBLE);
                } else {
                    mSortCategoryButton.setVisibility(View.VISIBLE);
                    mEditWidgetsButton.setVisibility(View.VISIBLE);
                }

                animateUpShow(mIconSheetBottomFrame);
            } else {
                animateUpShow(mOpenPrefs2Button);
            }
            animateDownHide(mShowButtons);

            animateUpShow(mHideButtons);


        } else {
            if (hideCats) {hideHiddenCategories();}

            //if (!mUseExtraActions) {
            animateDownHide(mIconSheetBottomFrame);
            //} else {
            animateDownHide(mOpenPrefs2Button);
            //}

            animateDownHide(mHideButtons);

            animateUpShow(mShowButtons);

        }
    }

    private String kidaccumecode = "";
    private String kidcode = "";

    private final View.OnClickListener kidescape = new View.OnClickListener() {
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
        boolean oldDumbMode = mDumbMode;
        mChildLock = false;
        mDumbMode = false;
        mAppPreferences.edit().putBoolean(getString(R.string.pref_key_toddler_lock), false).apply();
        mAppPreferences.edit().putBoolean(getString(R.string.pref_key_dumbmode), false).apply();
        kidaccumecode = "";
        checkChildLock();
        if (oldDumbMode) switchCategory(getTopCategory());
    }


    private void checkChildLock() {
        View kid_escape_area = findViewById(R.id.kid_escape_area);
        View decorView = getWindow().getDecorView();
        // View catswrap = findViewById(R.id.category_tabs_wrap);
        if (mDumbMode) {
            switchCategory(Categories.CAT_DUMB);
        }

        if (mChildLock ) {

            // Hide the status bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);


            if (!mChildLockSetup) {
                mQuickRow.getScroller().setVisibility(View.GONE);

                mIconSheetBottomFrame.setVisibility(View.GONE);
                mShowButtons.setVisibility(View.GONE);
                mHideButtons.setVisibility(View.GONE);
                mShowCats.setVisibility(View.GONE);
                kid_escape_area.setVisibility(View.VISIBLE);

                TextView kid_code_txt = findViewById(R.id.kid_code_txt);

                TextView[] b = new TextView[4];
                b[0] = findViewById(R.id.btn_kid1);
                b[1] = findViewById(R.id.btn_kid2);
                b[2] = findViewById(R.id.btn_kid3);
                b[3] = findViewById(R.id.btn_kid4);

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
                if (mDumbMode) {
                    //kidcode += kidcode + kidcode;
                    kidcode += kidcode;
                }

                List<String> kidcodearr = Arrays.asList(kidcode.split("(?!^)"));
                String shuffled;
                int i = 0;
                do {
                    Collections.shuffle(kidcodearr);
                    shuffled = "";
                    for (String letter : kidcodearr) {
                        shuffled += letter;
                    }
                    if (i++>100) break;
                } while (kidcode.equals(shuffled) || (new StringBuilder(kidcode)).reverse().toString().equals(shuffled));
                kidcode = shuffled;

                kid_code_txt.setText(getString(R.string.kid_escape_text, kidcode) );
                mChildLockSetup = true;


            }
        } else {

            mChildLockSetup = false;
            mQuickRow.getScroller().setVisibility(View.VISIBLE);

            mShowButtons.setVisibility(View.VISIBLE);
            kid_escape_area.setVisibility(View.GONE);
            int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }


    private void startDumbMode() {

        mDumbMode = true;

        final String category = Categories.CAT_DUMB;
        if (db().getCategoryDisplay(category)==null) {
            db().addCategory(category, Categories.getCatLabel(this, category), Categories.getCatFullLabel(this, category), Categories.isTinyCategory(Categories.CAT_DUMB),true, 100);
            createIconSheet(category, -2);
            final List<AppLauncher> appLauncherss = processActivities(false);
            List<ComponentName> apps = new ArrayList<>();
            List<String> onlyTypes = new ArrayList<>();
            onlyTypes.add("camera");
            onlyTypes.add("phone");
            onlyTypes.add("msg");
            onlyTypes.add("music");
            DefaultApps.checkDefaultApps(this, appLauncherss, apps, onlyTypes);

            String [] needs = new String[] {"calc", "clock", "calendar", "maps"};


            NEEDS:
            for(String key: needs) {
                for (AppLauncher app: appLauncherss) {
                    String packagename = app.getPackageName().toLowerCase();
                    if (packagename.contains(key)) {
                        apps.add(app.getComponentName());
                        continue NEEDS;
                    }
                }
            }


            for(ComponentName ap: apps) {
                Log.d(TAG, "Trying " + ap);
                if (ap!=null) {
                    AppLauncher app = db().getApp(ap);
                    if (app==null) {
                        Log.d(TAG, "null app for " + ap);
                    } else {
                        AppLauncher applauncher = app.makeAppLink();
                        applauncher.setCategory(category);
                        db().addApp(applauncher);
                        //addAppToIconSheet(category, applauncher, true);
                        // applauncher.loadAppIconAsync(this,mPackageMan);

                    }
                } else {
                    Log.d(TAG, "null ap");
                }
            }
        }

        mAppPreferences.edit().putBoolean(getString(R.string.pref_key_toddler_lock), true).apply();

        //switchCategory(Categories.CAT_DUMB);
//        mChildLock=true;
//        mChildLockSetup = false;

        checkChildLock();


    }


    private boolean isAppInstalled(String packageName) {
        if (packageName.equals(AppLauncher.ACTION_PACKAGE)) return true;
        try {
            getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }


    private boolean isLandscape() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public Point getScreenDimensions() {

        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        return size;
    }


    private static class AddIconHandler extends Handler {
        private final WeakReference<MainActivity> instref;
        AddIconHandler(MainActivity inst) {
            super();
            instref = new WeakReference<>(inst);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity inst = instref.get();
            if (inst!=null) {
                switch (msg.arg1) {
                    case ADD_ICON:
                        inst.addAppToIconSheetRecv(msg);
                        break;
                    case REMOVE_ALL_ICONS:
                        inst.removeIconSheetRecv(msg);
                        break;
                    case NO_ICONS:
                        inst.showNoIconsRecv(msg);
                }
            }
        }
    }


    public void removeViewFromQuickBar(View view) {
        mQuickRow.getGridLayout().removeView(view);
        //mQuickRow.removeFromQuickApps(appitem.getComponentName());
        db().setAppCategoryOrder(QuickRow.QUICK_ROW_CAT, mQuickRow.getGridLayout());
    }


    public void launchApp(AppLauncher appitem) {
        mLaunchApp.launchApp(appitem);
    }

    public AppWidgetHostView getAppWidgetHostView(AppLauncher appitem) {
        return mLoadedWidgets.get(appitem.getActivityName());
    }


    public String getCurrentCategory() {
        if (mCategory==null) return getTopCategory();
        return mCategory;
    }


    interface CategoryChangerListener {
        void onClick(DialogInterface dialog, int which, String category, String newDisplayName, String newDisplayFullName, boolean istiny, boolean ishidden);
    }


}
