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

import android.annotation.SuppressLint;
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
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
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
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.quaap.launchtime.apps.AppLauncher;
import com.quaap.launchtime.apps.Badger;
import com.quaap.launchtime.apps.LaunchApp;
import com.quaap.launchtime.apps.InteractiveScrollView;
import com.quaap.launchtime.components.Categories;
import com.quaap.launchtime.components.ExceptionHandler;
import com.quaap.launchtime.components.IconsHandler;
import com.quaap.launchtime.components.MsgBox;
import com.quaap.launchtime.db.DB;
import com.quaap.launchtime.ui.QuickRow;
import com.quaap.launchtime.ui.SearchBox;
import com.quaap.launchtime.ui.Style;
import com.quaap.launchtime.widgets.Widget;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

public class MainActivity extends Activity implements
        View.OnLongClickListener, SharedPreferences.OnSharedPreferenceChangeListener,
        Badger.BadgerCountChangeListener {

    //TODO: everything needs a major refactor.
    // custom views or fragments?

    private static final int UNINSTALL_RESULT = 3454;

    private FrameLayout mIconSheetTopFrame;
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
    private ImageView mShowCats;


    private TextView mSortCategoryButton;
    private TextView mAddCategoryButton;
    private TextView mRenameCategoryButton;
    private TextView mEditWidgetsButton;
    private ImageView mOpenPrefsButton;


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


    private int mColumns = 3;


    private Point mScreenDim;

    private SharedPreferences mAppPreferences;

    private Map<String, AppWidgetHostView> mLoadedWidgets = new HashMap<>();
    public Map<AppLauncher,ViewGroup> mAppLauncherViews = Collections.synchronizedMap(new HashMap<AppLauncher,ViewGroup>());

    private boolean mChildLock;
    private boolean mChildLockSetup;

    private LaunchApp mLaunchApp;

    private QuickRow mQuickRow;

    //private DB db();

    private SearchBox mSearchBox;

    private Style mStyle;

    private AddIconHandler iconHandler;
    private static final int ADD_ICON = 1;
    private static final int REMOVE_ALL_ICONS = 2;

    private static final String TAG = "LaunchTime";

    private static String latestCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

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

        mQuickRow = new QuickRow(mMainDragListener, this);

        mScreenDim = getScreenDimensions();

        //Load resources and init the form members

        initUI();


        mIconSheetHolder.setOnDragListener(iconSheetDropRedirector);

        findViewById(R.id.iconarea_wrap).setOnDragListener(iconSheetDropRedirector);


        mSearchBox = new SearchBox(this, mIconSheetScroller);
        mPrefs = getSharedPreferences("default", MODE_PRIVATE);
        mCategory = mPrefs.getString("category", getTopCategory());
        latestCategory = mCategory;

        mStyle = GlobState.getStyle(this);

        readPrefs();

        mLaunchApp = new LaunchApp(this);

        iconHandler = new AddIconHandler(this);

        // get all the apps installed and process them
        loadApplications();

        GlobState.getBadger(this).setBadgerCountChangeListener(this);


//        for (int i=0; i<100; i++) {
//            ComponentName cn = db().getAppNames().get(1);
//            AppLauncher app = db().getApp(cn);
//            AppLauncher applauncher = app.makeAppLink();
//            //applauncher.setCategory(mCategory);
//            db().addApp(applauncher);
//        }
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

    private InteractiveScrollView.OnSwipeHorizontalListener mHSwipeListener = new InteractiveScrollView.OnSwipeHorizontalListener() {

        @Override
        public void onLeftSwipe(float absDist) {
            if (mChildLock) return;
            switchCategory(getNextCategory(mCategory, -1));
            scrollToCategoryTab();
        }

        @Override
        public void onRightSwipe(float absDist) {
            if (mChildLock) return;
            switchCategory(getNextCategory(mCategory, 1));
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
            if (Categories.isHiddenCategory(cat)) it.remove();
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
    }

    private long mPauseTime = 0;

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        mBackPressedSessionCount=0;
        checkChildLock();

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

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        //Check how long we've been gone
        long pausetime = mPrefs.getLong("pausetime", -1);
        int homesetting = Integer.parseInt(mAppPreferences.getString("pref_return_home", "9999999"));

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

                }
            }, 100);
        }

        //rerun our query if needed
        if (mCategory.equals(Categories.CAT_SEARCH)) {
            mSearchBox.refreshSearch(false);
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
                if (key.equals("textcolor") || key.equals("preference_iconsize") || key.equals("icon-update")) {
                    mAppLauncherViews.clear();
                    repop = true;
                }
                if (key.equals("icon_tint") || key.equals("prefsUpdate")) {
                    AppLauncher.clearIcons();
                    mAppLauncherViews.clear();
                    repop = true;
                }
                if (key.equals("icons-pack")) {
                    AppLauncher.clearIcons();
                    mAppLauncherViews.clear();

                    //mIconSheet.removeAllViews();
                    IconsHandler ich = GlobState.getIconsHandler(this);

                    ich.loadIconsPack(sharedPreferences.getString("icons-pack", IconsHandler.DEFAULT_PACK));
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


                    if (key.equals("prefs_toddler_lock")) {
                        mChildLock = sharedPreferences.getBoolean("prefs_toddler_lock", false);
                        if (mChildLock) mChildLockSetup = false;
                        checkChildLock();
                    }

                    if (key.equals("pref_show_badges")) {
                        if (!sharedPreferences.getBoolean("pref_show_badges", true)) {
                            GlobState.getBadger(this).clearAll();
                        }
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

    private synchronized void switchCategory(String category) {
        try {
            if (category == null) return;
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
            repopulateIconSheet(mCategory);

            //the top frame holds the search zone, but only on the search page.
            mIconSheetTopFrame.removeAllViews();
            if (mCategory.equals(Categories.CAT_SEARCH)) {

                mIconSheetTopFrame.addView(mSearchBox.getSearchView());

                //Show recent apps
                populateRecentApps();

                //load our cursor
                mSearchBox.refreshSearch(true);

            } else {

                // not the search page: close the cursor
                mSearchBox.closeSeachAdapter();
            }

            //Actually switch the icon sheet.
            mIconSheetHolder.removeAllViews();
            mIconSheetHolder.addView(mIconSheet);

            showButtonBar(false, true);
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
        } else if (mQuickRow.getScrollPos()>0) {
            mQuickRow.scrollToStart();
        } else if (mIconSheetScroller.getScrollY()>0) {
            //Otherwise, scroll to top
            mIconSheetScroller.smoothScrollTo(0, 0);
        } else if (mCategory.equals(Categories.CAT_SEARCH) && mSearchBox.getSeachText().length()!=0) {
            //If search is open, clear the searchbox
            mSearchBox.setSearchText("");
        } else if (!mCategory.equals(topCat)){
            //Otherwise, switch to known-good category
            switchCategory(topCat);
            mCategoriesScroller.smoothScrollTo(0, 0);
        } else if (mCategoriesScroller.getScrollY()>0) {
            mCategoriesScroller.smoothScrollTo(0, 0);
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

            mChildLock = mAppPreferences.getBoolean("prefs_toddler_lock", false);


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
            Log.e(TAG, e.getMessage(), e);
        }
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

            mScreenDim = getScreenDimensions();
            //float launcherw = getResources().getDimension(R.dimen.launcher_width);
            float launcherw = mStyle.getLauncherSize();
            float catwidth = getResources().getDimension(R.dimen.cattabbar_width);

            float wr = ( mScreenDim.x - (autohideCats?0:catwidth))/launcherw;

            Log.d(TAG, "wr=" + wr + " x=" + mScreenDim.x + " catwidth=" + catwidth + " launcherw=" + launcherw);
            if (wr<3) mColumns = 2;
            else if (wr<4) mColumns = 3;
            else if (wr<5.4) mColumns = 4;
            else if (wr<7.4) mColumns = 5;
            else if (wr<9) mColumns = 6;
            else if (wr<11) mColumns = 8;
            else mColumns = 9;


            //Log.d(TAG, "x=" + mScreenDim.x + " catwidth=" + catwidth + " launcherw=" + launcherw);


            if (mIconSheet!=null && mIconSheet.getColumnCount() != mColumns) {
                changeColumnCount(mIconSheet, mColumns);
            }

            mShowButtons.setBackgroundColor(mStyle.getCattabBackground());
            mShowButtons.setColorFilter(mStyle.getCattabTextColor());
            mShowButtons.setMinimumHeight(mStyle.getCategoryTabPaddingHeight()*3);

            mShowCats.setBackgroundColor(mStyle.getCattabBackground());
            mShowCats.setColorFilter(mStyle.getCattabTextColor());
            mShowCats.setMinimumHeight(mStyle.getCategoryTabPaddingHeight()*3);


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

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void hideCatsIfAutoHide() {
        if (isAutohide()) showCats(false);
    }

    private void showCats(boolean show) {
        View cats = findViewById(R.id.category_tabs_wrap);
        cats.setVisibility(!show?View.GONE:View.VISIBLE);
        mShowCats.setVisibility(show?View.GONE:View.VISIBLE);
    }

    private void handleAutohide() {
        //Switch the menu left/right

        boolean autohideCats = isAutohide();

        float catwidth = getResources().getDimension(R.dimen.cattabbar_width);

        View iconsarea = findViewById(R.id.iconarea_wrap);
        FrameLayout.LayoutParams iconsarealp = (FrameLayout.LayoutParams)iconsarea.getLayoutParams();
        if (iconsarealp==null){
            iconsarealp = new FrameLayout.LayoutParams(this,null);
        }

        View cats = findViewById(R.id.category_tabs_wrap);

        showCats(!autohideCats);

        FrameLayout.LayoutParams catslp = (FrameLayout.LayoutParams)cats.getLayoutParams();
        if (catslp==null){
            catslp = new FrameLayout.LayoutParams(this,null);
        }

        if (mStyle.isLeftHandCategories()) {
            catslp.gravity = Gravity.LEFT;

            if (autohideCats) {
                iconsarealp.leftMargin = 2;
                iconsarealp.rightMargin = 2;
            } else {
                iconsarealp.leftMargin = (int) catwidth;
                iconsarealp.rightMargin = 2;
            }

        } else {
            catslp.gravity = Gravity.RIGHT;
            if (autohideCats) {
                iconsarealp.leftMargin = 2;
                iconsarealp.rightMargin = 2;
            } else {
                iconsarealp.leftMargin = 2;
                iconsarealp.rightMargin = (int) catwidth;
            }
        }
        //cats.setLayoutParams(catslp);
    }

    private boolean isAutohide() {
        return mAppPreferences.getBoolean("pref_autohide_cats", false);
    }

    public LaunchApp getAppLauncher() {
        return mLaunchApp;
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
            for (ComponentName actvname : db().getAppCategoryOrder(QuickRow.QUICK_ROW_CAT)) {
                if (db().isAppInstalled(actvname)) {
                    AppLauncher app = db().getApp(actvname);
                    if (app != null) {
                        app.loadAppIconAsync(this, mPackageMan);
                    }
                }
            }

            //Load the selected category icons
            for (ComponentName actvname : db().getAppCategoryOrder(mCategory)) {
                if (db().isAppInstalled(actvname)) {
                    AppLauncher app = db().getApp(actvname);
                    if (app != null) {
                        app.loadAppIconAsync(this, mPackageMan);
                    }
                }
            }
        }

        //Look for new apps
        //final List<AppLauncher> launchers = processActivities();

        //new LoadAppsAsyncTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        //loads the quickrow or adds default apps if it is empty
       // processQuickApps(launchers);


        iconHandler.post(new Runnable() {
            @Override
            public void run() {
                final List<AppLauncher> appLauncherss = processActivities();

                mQuickRow.processQuickApps(appLauncherss, mPackageMan);
                db().setAppCategoryOrder(mRevCategoryMap.get(mQuickRow.getGridLayout()), mQuickRow.getGridLayout());

                if (mCategory.equals(Categories.CAT_SEARCH)) {
                    populateRecentApps();
                } else {
                    repopulateIconSheet(mCategory);
                }


                firstRunPostApps();
            }
        });




    }

    private void firstRunPostApps() {
        if (db().isFirstRun()) {
            String selfAct = this.getPackageName() + "." + this.getClass().getSimpleName();
            Log.d(TAG, "My name is " + selfAct);

            //Move self icon to hidden
            db().updateAppCategory(selfAct, this.getPackageName(), Categories.CAT_HIDDEN);

            //Take a backup no that things are pre-sorted.
            db().backup("After install");

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

    private void repopulateIconSheet(String category) {
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
            iconSheet.removeAllViews();
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

    private void addAppToIconSheet(GridLayout iconSheet, AppLauncher app, int pos, boolean reuse) {
        if (app != null) {
            try {
                if (((app.isWidget() || app.isOreoShortcut()) && isAppInstalled(app.getPackageName())) || mLaunchApp.isValidActivity(app)) {
                    ViewGroup item = getLauncherView(app, false, reuse);
                    if (item != null) {
                        if (!app.iconLoaded()) {
                            app.loadAppIconAsync(this, mPackageMan);
                        }
                        ViewGroup parent = (ViewGroup) item.getParent();
                        if (parent != null) parent.removeView(item);
                        GridLayout.LayoutParams lp = getAppLauncherLayoutParams(iconSheet, app);
                        iconSheet.addView(item, pos, lp);
                        return;
                    }
                } else {
                    db().deleteApp(app.getComponentName());
                    Log.d(TAG, "removed " + app.getPackageName() + " " + app.getActivityName() + ":activity not valid.");
                }
            } catch (Exception e) {
                Log.e(TAG, "exception adding icon to sheet", e);
                Toast.makeText(this,"Couldn't place icon: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Not showing recent: Null.");
        }

    }

    @NonNull
    private GridLayout.LayoutParams getAppLauncherLayoutParams(GridLayout grid, AppLauncher app) {

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();

        int w = getLauncherWidth(app);
        int h = getLauncherHeight(app);

        if (w>0 || h>0) {
            //float sw = getResources().getDimension(R.dimen.launcher_width);
            float sw = mStyle.getLauncherSize();
            //float sh = getResources().getDimension(R.dimen.launcher_height);

            //int width = (int)(sw + 20) * mColumns;

            float cellwidth = sw * 1f;
            float cellheight = cellwidth *1.5f;  // ~square cells


            int wcells = (int) Math.ceil(w / cellwidth);
            if (wcells > 1) {
                int start = GridLayout.UNDEFINED;
                if (wcells > grid.getColumnCount()) {
                    wcells = grid.getColumnCount();
                }
               // if (wcells > 1) start = 0;
                lp.columnSpec = GridLayout.spec(start, wcells, GridLayout.FILL);

                //Log.d("widcol", "w=" + w + " wcells=" + wcells + " start=" + start + " cellwidth=" + cellwidth + " r=" + cellwidth * wcells);
            } else {
                lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, GridLayout.FILL);
            }

            int hcells = (int) Math.ceil(h / cellheight);
            if (hcells > 1) {
                lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, hcells, GridLayout.FILL);
            } else {
                lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, GridLayout.FILL);
            }

            final AppWidgetHostView appwid = mLoadedWidgets.get(app.getActivityName());

            if (appwid != null) {

                cellheight *= 0.82f;
                //appwid.updateAppWidgetSize(null, (int) sw, (int) sh, (int) (cellwidth * wcells), (int) (cellheight * hcells));

                //magic numbers to properly expand widgets...
                final int wDp = pxToDip(cellwidth*wcells);
                final int hDp = pxToDip(cellheight*hcells*4/3);
                lp.width = (int)(cellwidth*wcells*1.1);
                lp.height = (int)(cellheight*hcells*1.5);

                //Log.d("widcol2", "w=" + w + " wcells=" + wcells  + " cellwidth=" + cellwidth + " r=" + cellwidth * wcells);
                //Log.d("widcol2", "h=" + w + " hcells=" + hcells  + " cellheight=" + cellheight + " r=" + cellheight * hcells);
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

    public int dipToPx(float dip){
        float scale = getResources().getDisplayMetrics().density;
        return (int)(dip * scale + .5f);
       // return (int)((pixel - 0.5f)/scale);
    }

    public void changeColumnCount(GridLayout gridLayout, int columnCount) {
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

    private List<AppLauncher> processActivities() {
        final List<AppLauncher> launchers = new ArrayList<>();

        List<ComponentName> dbactvnames = db().getAppNames();

        Set<ComponentName> pmactvnames = new HashSet<>();
        List<AppLauncher> newapps = new ArrayList<>();

        // Set MAIN and LAUNCHER filters, so we only get activities with that defined on their manifest
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        // Get all activities that have those filters
        List<ResolveInfo> activities;

        try {
            activities = mPackageMan.queryIntentActivities(intent, PackageManager.GET_META_DATA);
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


        for (int i = 0; i < activities.size(); i++) {

            try {
                AppLauncher app;

                ResolveInfo ri = activities.get(i);
                String actvname = ri.activityInfo.name;
                ComponentName appcn = new ComponentName(ri.activityInfo.packageName, actvname);

                if (!pmactvnames.contains(appcn)) {
                    pmactvnames.add(appcn);

                    app = db().getApp(appcn);

                    if (dbactvnames.contains(appcn) && app != null) {
                        app.loadAppIconAsync(this, mPackageMan);
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
        }

        db().addApps(newapps);

        return launchers;
    }

    
    
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


            item = (ViewGroup) LayoutInflater.from(this).inflate(smallIcon ? R.layout.launcher_small_icon : R.layout.launcher_icon, null);



            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    view.startAnimation(itemClickedAnim);
                    mLaunchApp.launchApp(app);
                    showButtonBar(false, true);
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
            app.loadAppIconAsync(this,mPackageMan);

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

    private void updateAppBadgeCount(ViewGroup item, int bcount) {
        if (item!=null) {
            TextView badge = item.findViewById(R.id.launcher_badge);
            if (badge!=null) {
                if (bcount <= 0 || !mAppPreferences.getBoolean("pref_show_badges", true)) {
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

    private void setupWidget() {
        try {
            mWidgetHelper.popupSelectWidget();
        } catch (Throwable t) {
            //very rare
            Toast.makeText(this, "Can't show widgets: " + t.getLocalizedMessage(),Toast.LENGTH_LONG).show();
            Log.e("Widgets", t.getMessage(), t);
        }
    }

    private void addWidget(AppWidgetHostView appwid) {
        if (mChildLock) return;

        ComponentName cn = appwid.getAppWidgetInfo().provider;
        String actvname = cn.getClassName();
        String pkgname = cn.getPackageName();

        String catId = db().getAppCategory(cn);
        if (catId == null || catId.equals(mCategory)) {

            //Log.d(TAG, actvname + " " + pkgname);

            mLoadedWidgets.put(actvname, appwid);

            String label = pkgname;

            AppLauncher.removeAppLauncher(cn);
            AppLauncher app = AppLauncher.createAppLauncher(actvname, pkgname, label, mCategory, true);

            db().addApp(app);
            db().addAppCategoryOrder(mCategory, app.getComponentName());
        } else {
            Toast.makeText(this, getString(R.string.widget_alreay,db().getCategoryDisplay(catId)), Toast.LENGTH_LONG).show();
        }


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
        mStyle.styleCategoryStyle(categoryTab, catstyle);

    }

    private TextView createCategoryTab(final String category, final GridLayout iconSheet) {
        final TextView categoryTab = new TextView(this);
        categoryTab.setText(db().getCategoryDisplay(category));
        categoryTab.setTag(category);
        // categoryTab.setWidth((int)Utils.dpToPx(this,categoryTabWidth));

        categoryTab.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        final Style.CategoryTabStyle catstyle = getDefaultCategoryStyle(category);

        if (Categories.isHiddenCategory(category)) {
            categoryTab.setVisibility(View.GONE);
        }

        if (catstyle == Style.CategoryTabStyle.Normal) {
            lp.weight = 1;
        }
        styleCategorySpecial(categoryTab, Style.CategoryTabStyle.Default, category);
        lp.gravity = Gravity.CENTER;
        lp.setMargins(2, 4, 2, 3);

        categoryTab.setLayoutParams(lp);

        categoryTab.setGravity(Gravity.CENTER);


        categoryTab.setClickable(true);
        categoryTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mChildLock) return;
               // view.startAnimation(itemClickedAnim);
                switchCategory(category);
                hideCatsIfAutoHide();

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
                boolean isAppLauncher = dragObj.getTag() instanceof AppLauncher;
                boolean isSearch = category.equals(Categories.CAT_SEARCH);
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_ENTERED:
                        if (catstyle == Style.CategoryTabStyle.Tiny || (!isAppLauncher || !isSearch)) {
                            styleCategorySpecial(categoryTab, Style.CategoryTabStyle.DragHover);
                        }
                       // Log.d(TAG, "DRAG_ENTERED: " + ((AppLauncher)dragObj.getTag()).getActivityName());

                        break;

                    case DragEvent.ACTION_DRAG_LOCATION:

                        scrollOnDrag(overView, event, mCategoriesScroller);
                        break;
                    case DragEvent.ACTION_DRAG_ENDED:
                        mBeingDragged = null;
                        hideRemoveDropzone();
                        hideHiddenCategories();
                    case DragEvent.ACTION_DRAG_EXITED:

                        styleCategorySpecial(categoryTab, Style.CategoryTabStyle.Default);
                        break;

                    case DragEvent.ACTION_DROP:
                        if (isAppLauncher) {
                            if (!isSearch) {
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
        mCategoriesLayout.addView(categoryTab);

        return categoryTab;
    }

    private long mDropZoneHover=0;
    private View.OnDragListener mMainDragListener = new View.OnDragListener() {
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
                    break;

                case DragEvent.ACTION_DRAG_LOCATION:
                    //scroll the scrollview

                    if (isLauncher) {
                        scrollOnDrag(droppedOn, event, mIconSheetScroller);
                        hscrollOnDrag(droppedOn, event, mQuickRow.getScroller());

                        if (!isSpecial && !mCategory.equals(Categories.CAT_SEARCH) && mLinkDropzone.getVisibility()!=View.VISIBLE && (droppedOn==mRemoveDropzone || droppedOn==mLinkDropzonePeek) && System.currentTimeMillis()-mDropZoneHover > 400) {
                            mLinkDropzone.setVisibility(View.VISIBLE);
                            mLinkDropzonePeek.setVisibility(View.GONE);
                           // Log.d(TAG, "mLinkDropzone.setVisibility(View.VISIBLE)");
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
                    hideCatsIfAutoHide();
                } else if (mDragDropSource == mIconSheets.get(Categories.CAT_SEARCH)) {
                    removeDroppedRecentItem(dragObj);
                    hideCatsIfAutoHide();
                } else if (mBeingDragged != null && (mBeingDragged.isWidget() || mBeingDragged.isLink())) {
                    removeDroppedItem(dragObj);
                    mSearchBox.refreshSearch(true);
                    hideCatsIfAutoHide();

                } else if (mDragDropSource == mCategoriesLayout && !isLauncher) {
                    //delete category tab
                    promptDeleteCategory((String) dragObj.getTag());

                } else {
                    //uninstall app
                    if (mBeingDragged!=null) {
                        mBeingUninstalled = dragObj;
                        launchUninstallIntent(mBeingDragged.getPackageName());
                    }
                    hideCatsIfAutoHide();
                }
                return true;
            } else if (droppedOn == mLinkDropzone) {
                hideCatsIfAutoHide();
                if (isLauncher) {
                    AppLauncher app = (AppLauncher)dragObj.getTag();
                    Log.d(TAG, "Making link: " + app.getActivityName() + " " + app.getPackageName());
                    AppLauncher applauncher = app.makeAppLink();
                    applauncher.setCategory(mCategory);
                    db().addApp(applauncher);
                    repopulateIconSheet(mCategory);
                } else {
                    Log.d(TAG, "non-launcher dropped on linker: " + dragObj + " tag=" + dragObj.getTag());
                }
                return true;
            } else if (droppedOn instanceof GridLayout) {
                target = (GridLayout) droppedOn;
            } else if (droppedOn instanceof FrameLayout) {
                target = (FrameLayout) droppedOn;
            } else if (droppedOn.getParent() instanceof GridLayout){
                target = (GridLayout) droppedOn.getParent();
            } else {
                hideCatsIfAutoHide();
                return true;
            }
            hideCatsIfAutoHide();

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

                    db().deleteApp(mBeingDragged.getComponentName());
                    AppWidgetHostView wid = mLoadedWidgets.remove(mBeingDragged.getActivityName());
                    if (wid != null) {
                        mWidgetHelper.widgetRemoved(wid.getAppWidgetId());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
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


    private View mDragPotential;
    @Override
    public boolean onLongClick(View view) {
        if (mChildLock) return false;

        AppLauncher dragitem = (AppLauncher) view.getTag();
        mDragPotential = view;

        if (handle25Shortcuts(view, dragitem)) {

            final int slop  = ViewConfiguration.get(this).getScaledTouchSlop();
            mDragPotential.setOnTouchListener(new View.OnTouchListener() {
                float oX = -1;
                float oY = -1;

                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                        dismissActionPopup();
                        startDrag();
                    } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                        if (oX == -1) {
                            oX = event.getX();
                            oY = event.getY();
                        } else {
                            if (Math.abs(oX - event.getX()) > slop || Math.abs(oY - event.getY()) > slop) {
                                dismissActionPopup();
                                startDrag();
                            }
                        }
                        //Log.d("movet", event.getX() + "," + event.getY());
                    } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                        if (mDragPotential != null) mDragPotential.setOnTouchListener(null);
                        mDragPotential = null;
                        //dismissActionPopup();
                    } //else {
                        //Log.d(TAG, event.getActionMasked() + "");
                    //}


                    return false;
                }
            });
        } else {
            startDrag();
        }
        return true;
    }

    public boolean startDrag() {

        if (mDragPotential==null) return false;

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
            return true;
        }


        return false;
    }





    private PopupMenu mShortcutActionsPopup;

    private boolean handle25Shortcuts(View view, final AppLauncher appitem) {
        if (Build.VERSION.SDK_INT>=25) {


            final LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
            if (launcherApps==null) return false;
            List<ShortcutInfo> shortcutInfos = null;
            if (launcherApps.hasShortcutHostPermission()) {
                try {

                    LauncherApps.ShortcutQuery q = new LauncherApps.ShortcutQuery();
                    q.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST);
                    q.setPackage(appitem.getPackageName());
                    q.setActivity(appitem.getComponentName());


                    shortcutInfos = launcherApps.getShortcuts(q, android.os.Process.myUserHandle());

                    //Log.d(TAG, "Queried shortcuts");

                } catch (SecurityException | IllegalStateException e) {
                    Log.e(TAG, "Couldn't query shortcuts", e);
                }
            }

            try {
                if (shortcutInfos != null && shortcutInfos.size()>0) {
                    dismissActionPopup();

                    mShortcutActionsPopup = new PopupMenu(this, view);
                    setForceShowIcon(mShortcutActionsPopup);

                    MenuItem appmenuItem = mShortcutActionsPopup.getMenu().add(appitem.getLabel());
                    appmenuItem.setIcon(appitem.getIconDrawable());
                    appmenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            mLaunchApp.launchApp(appitem);
                            showButtonBar(false, true);
                            dismissActionPopup();
                            return true;
                        }
                    });

                    for (final ShortcutInfo shortcutInfo : shortcutInfos) {

                        addShortcutToActionPopup(launcherApps, shortcutInfo);
                    }

                    MenuItem menuItem = mShortcutActionsPopup.getMenu().add("⌧");
                    menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            dismissActionPopup();
                            return true;
                        }
                    });

                    mShortcutActionsPopup.show();
                    return true;
                }


            } catch (Exception e) {
                Log.e(TAG, "Couldn't create menu", e);
            }


        }
        return false;
    }

    private void addShortcutToActionPopup(final LauncherApps launcherApps, final ShortcutInfo shortcutInfo) {
        if (Build.VERSION.SDK_INT>=25) {
            if (shortcutInfo != null && shortcutInfo.getActivity() != null) {
                //Log.d(TAG, shortcutInfo.getShortLabel() + " " + shortcutInfo.getActivity().getClassName());

                if (shortcutInfo.isEnabled()) {

                    String label = "";
                    if (shortcutInfo.getShortLabel() != null)
                        label += shortcutInfo.getShortLabel();

                    if (shortcutInfo.getLongLabel() != null && !label.contentEquals(shortcutInfo.getLongLabel()))
                        label += " (" + shortcutInfo.getLongLabel() + ")";

                    MenuItem menuItem = mShortcutActionsPopup.getMenu().add(label.trim());


                    Drawable icon = launcherApps.getShortcutIconDrawable(shortcutInfo, DisplayMetrics.DENSITY_DEFAULT);
                    if (icon != null) menuItem.setIcon(icon);


                    menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            if (Build.VERSION.SDK_INT >= 25) {
                                try {
                                    launcherApps.startShortcut(shortcutInfo, null, null);
                                } catch (Exception e) {
                                    Log.e(TAG, "Couldn't Launch shortcut", e);
                                }
                            }

                            return true;
                        }
                    });
                }
            }
        }
    }

    private void dismissActionPopup() {
        if (mShortcutActionsPopup!=null) {
            mShortcutActionsPopup.dismiss();
            mShortcutActionsPopup = null;

        }
    }

    public static void setForceShowIcon(PopupMenu popupMenu) {
        try {
            Field[] fields = popupMenu.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper
                            .getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod(
                            "setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
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
                } else if (!Categories.isHiddenCategory(cat)) {
                    mCategoryTabs.get(cat).setVisibility(View.VISIBLE);
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

        if (mDragDropSource == mQuickRow.getGridLayout()
            || mDragDropSource == mCategoriesLayout
            || mDragDropSource == mIconSheets.get(Categories.CAT_SEARCH)
            || (mBeingDragged!=null && (mBeingDragged.isWidget() || mBeingDragged.isLink())
        ) ) {
            mRemoveDropzone.setBackgroundColor(Color.YELLOW);
            //mRemoveAppText.setText(getString(R.string.remove_launcher) + "\n\u267B");
            mRemoveAppText.setText(R.string.remove_launcher);
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
            if (!Categories.CAT_SEARCH.equals(mCategory)) {
                mLinkDropzonePeek.setVisibility(View.VISIBLE);
            }
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

        Log.d(TAG, "Uninstalling " + packageName);
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
                        ComponentName actvname = ((AppLauncher) mBeingUninstalled.getTag()).getComponentName();
                        db().deleteApp(actvname);
                        mQuickRow.removeFromQuickApps(actvname);
                        AppLauncher.removeAppLauncher(actvname);
                        mSearchBox.refreshSearch(true);
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

        ViewGroup view = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.category_name, null);

        TextView messageView = view.findViewById(R.id.message_txt);

        final TextView catDeletedLabel = view.findViewById(R.id.cat_deleted_label);
        final Spinner catDeletedSpinner = view.findViewById(R.id.cat_deleted_spinner);

        final EditText shortname = view.findViewById(R.id.shortname);
        final EditText fullname = view.findViewById(R.id.fullname);
        final CheckBox isTiny = view.findViewById(R.id.istiny_checkbox);

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
        Log.d(TAG, category +", " + newDisplayName +", " +  newDisplayFullName +", " +  isTiny);
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

        mIconSheets = new TreeMap<>();
        mCategoryTabs = new TreeMap<>();
        mRevCategoryMap = new HashMap<>();
        mRevCategoryMap.put(mQuickRow.getGridLayout(), QuickRow.QUICK_ROW_CAT);

        mShowButtons = findViewById(R.id.settings_button);

        mShowButtons.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleButtonBar();
                hideCatsIfAutoHide();
            }
        });

        mShowCats = findViewById(R.id.show_cats_buttom);
        mShowCats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCats(true);
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
                openSettings(MainActivity.this);
                showButtonBar(false, true);
            }
        });

    }

    public static void openSettings(Activity activity) {
        Intent settingsIntent = new Intent(activity, SettingsActivity.class);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        activity.startActivity(settingsIntent);
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
                mQuickRow.getScroller().setVisibility(View.GONE);

                mIconSheetBottomFrame.setVisibility(View.GONE);
                mShowButtons.setVisibility(View.GONE);
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
            return true;
        } else {

            mChildLockSetup = false;
            mQuickRow.getScroller().setVisibility(View.VISIBLE);

            mShowButtons.setVisibility(View.VISIBLE);
            kid_escape_area.setVisibility(View.GONE);
            int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
            decorView.setSystemUiVisibility(uiOptions);
        }
        return false;
    }




    public boolean isAppInstalled(String packageName) {
        if (packageName.equals(AppLauncher.ACTION_PACKAGE)) return true;
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


    private static class AddIconHandler extends Handler {
        private WeakReference<MainActivity> instref;
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
                }
            }
        }
    }


    interface CategoryChangerListener {
        void onClick(DialogInterface dialog, int which, String category, String newDisplayName, String newDisplayFullName, boolean istiny);
    }


}
