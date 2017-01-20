package com.quaap.launchtime;

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
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.quaap.launchtime.components.AppCursorAdapter;
import com.quaap.launchtime.components.AppShortcut;
import com.quaap.launchtime.components.Categories;
import com.quaap.launchtime.components.ExceptionHandler;
import com.quaap.launchtime.components.InteractiveScrollView;
import com.quaap.launchtime.db.DB;
import com.quaap.launchtime.widgets.Widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MainActivity extends Activity implements
        View.OnLongClickListener {

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
    private View mAddCategoryButton;
    private View mRenameCategoryButton;
    private View mDeleteCategoryButton;
    private View mEditWidgetsButton;
    private View mOpenPrefsButton;


    private LinearLayout mCategoriesLayout;
    private TextView mRemoveAppText;
    private FrameLayout mRemoveDropzone;
    private PackageManager mPackageMan;
    private AppShortcut mBeingDragged;
    private volatile ViewGroup mDragDropSource;
    private SharedPreferences mPrefs;
    private View mBeingUninstalled;
    private Widget mWidgetHelper;
    private ViewGroup mSearchView;
    private int textColor;
    private int textColorInvert;
    private int cattabBackground;
    private int cattabSelectedBackground;
    private int cattabDragHoverBackground;
    private int dragoverBackground;
    private int backgroundDefault = Color.TRANSPARENT;
    private float categoryTabFontSize = 16;
    private int categoryTabPaddingHeight = 16;
    private int mColumns = 3;
//    private float categoryTabFontSizeHidden = 12;
//    private int mColumnsLandscape = 5;
//    private int mColumnsPortrait = 3;
//    private int mColumnMargin = 12;
//    private int categoryTabWidth = 108;

    private Point mScreenDim;

    private SharedPreferences mAppPreferences;

    private Map<String, AppWidgetHostView> mLoadedWidgets = new HashMap<>();
    public Map<AppShortcut,ViewGroup> mAppShortcutViews = new HashMap<>();

    private DB mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

        setContentView(R.layout.activity_main);


        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        //Setup some of our globals utils
        mDb = GlobState.getGlobState(this).getDB();
        mPackageMan = getApplicationContext().getPackageManager();
        mAppPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
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
        mCategory = mPrefs.getString("category", Categories.CAT_TALK);

        readPrefs();

        loadApplications();

        if (mDb.isFirstRun()) {
            mDb.backup("After install");
        }

    }

    private View.OnDragListener iconSheetDropRedirector = new View.OnDragListener() {
        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            return mMainDragListener.onDrag(mIconSheet, dragEvent);
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mScreenDim = getScreenDimensions();
        checkConfig();
    }

    @Override
    protected void onPause() {

        mPrefs.edit()
                .putInt("scrollpos", mIconSheetScroller.getScrollY())
                .putString("category", mCategory)
                .apply();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();


        mCategory = mPrefs.getString("category", Categories.CAT_TALK);
        switchCategory(mCategory);
        mIconSheetScroller.postDelayed(new Runnable() {
            @Override
            public void run() {
                mIconSheetScroller.smoothScrollTo(0, mPrefs.getInt("scrollpos",0));

            }
        },100);

    }


    @Override
    public void onDestroy() {
        mWidgetHelper.done();
        super.onDestroy();
    }

    private void switchCategory(String category) {
        if (category == null) return;
        mCategory = category;
        for (TextView catTab : mCategoryTabs.values()) {
            styleCategorySpecial(catTab, CategoryTabStyle.Default);
            catTab.setText(mDb.getCategoryDisplay(mRevCategoryMap.get(catTab)));
        }

        mCategoryTabs.get(category).setText(mDb.getCategoryDisplayFull(category));

        mIconSheet = mIconSheets.get(category);

        checkConfig();

        repopulateIconSheet(category);

        mIconSheetTopFrame.removeAllViews();
        if (category.equals(Categories.CAT_SEARCH)) {

            mIconSheetTopFrame.addView(mSearchView);
            populateRecentApps(mIconSheet);
        }


        mIconSheetHolder.removeAllViews();
        mIconSheetHolder.addView(mIconSheet);

        showButtonBar(false);
    }

    @Override
    public void onBackPressed() {
        showButtonBar(false);
        mQuickRowScroller.smoothScrollTo(0, 0);
        if (mIconSheetScroller.getScrollY()>0) {
            mIconSheetScroller.smoothScrollTo(0, 0);
        } else if (!mCategory.equals(Categories.CAT_TALK)){
            switchCategory(Categories.CAT_TALK);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_HOME) {
            switchCategory(Categories.CAT_TALK);
            mQuickRowScroller.smoothScrollTo(0, 0);
        }
        return super.onKeyDown(keyCode, event);
    }


    private void readPrefs() {


        try {

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
                    categoryTabFontSize = 18;
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


    private void checkConfig() {
        readPrefs();
        try {

            mScreenDim = getScreenDimensions();
            float shortcutw = getResources().getDimension(R.dimen.shortcut_width);
            float catwidth = getResources().getDimension(R.dimen.cattabbar_width);
            mColumns = (int)((mScreenDim.x - catwidth)/(shortcutw + 2));

            changeColumnCount(mIconSheet, mColumns);

            mShowButtons.setMinimumHeight(categoryTabPaddingHeight*3);
            //mShowButtons.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, categoryTabPaddingHeight*3));
            //mShowButtons.setPadding(2,categoryTabPaddingHeight,2,4);

        } catch (Exception e) {
            Log.e("Launch", e.getMessage(), e);
        }
    }

    public void launchApp(String activityname) {
        launchApp(mDb.getApp(activityname));
    }

    public void launchApp(final AppShortcut app) {
        String activityname = app.getLinkBaseActivityName();
        String packagename = app.getPackageName();
        String uristr = null;
        if (app.isLink()) {
            uristr = app.getLinkUri();
        }
        Log.d("Launch", app.getActivityName() + " " + app.getPackageName() + " " + uristr);

        try {
            Intent intent;
            if (app.isActionLink()) {
                if (activityname.startsWith("android.intent.action.CALL")) {
                    activityname = "android.intent.action.DIAL";
                }
                intent = new Intent(activityname, Uri.parse(uristr));
            } else {
                if (uristr == null) {
                    intent = new Intent(Intent.ACTION_MAIN);
                } else {
                    intent = new Intent(Intent.ACTION_MAIN, Uri.parse(uristr));
                }
                intent.setClassName(packagename, activityname);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            showButtonBar(false);
            mDb.appLaunched(app.getActivityName());
        } catch (Exception e) {
            Log.d("Launch", "Could not launch " + activityname, e);
            Toast.makeText(this, "Could not launch item: " + e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
        }

    }

    private void loadApplications() {

        //Make sure the displayed icons load first
        //Load the quickrow icons first
        for (String actvname: mDb.getAppCategoryOrder(QUICK_ROW_CAT)) {
            AppShortcut app = mDb.getApp(actvname);
            if (app!=null) {
                app.loadAppIconAsync(this, mPackageMan);
            }
        }

        //Load the selected category icons
        for (String actvname: mDb.getAppCategoryOrder(mCategory)) {
            AppShortcut app = mDb.getApp(actvname);
            if (app!=null) {
                app.loadAppIconAsync(this, mPackageMan);
            }
        }


        //Look for new apps
        final List<AppShortcut> shortcuts = processActivities();

        //loads the quickrow or adds default apps if it is empty
        processQuickApps(shortcuts);



        for (final String category : mDb.getCategories()) {

            createIconSheet(category);
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

    private void populateRecentApps(GridLayout iconSheet) {

        iconSheet.removeAllViews();

        int i=0;
        for (String actvname : mDb.getAppLaunchedList()) {
            AppShortcut app = mDb.getApp(actvname);
            //Log.d("Recent", "Trying " + actvname + " " + app);

            addAppToIconSheet(iconSheet, app, false);
            if (i++>60) break;
        }
    }

    private void repopulateIconSheet(String category) {
        GridLayout iconSheet = mIconSheets.get(category);

        iconSheet.removeAllViews();

        final List<String> apporder = mDb.getAppCategoryOrder(category);
        List<AppShortcut> apps = mDb.getApps(category);

        for (String actvname : apporder) {
            for (Iterator<AppShortcut> it = apps.iterator(); it.hasNext(); ) {
                AppShortcut app = it.next();
                if (actvname.equals(app.getActivityName())) {
                    addAppToIconSheet(iconSheet, app, true);
                    it.remove();
                }
            }
        }

        for (AppShortcut app : apps) {
            addAppToIconSheet(iconSheet, app, true);
        }

        if (apps.size()>0) {
            mDb.setAppCategoryOrder(category, iconSheet);
        }
    }

    private void addAppToIconSheet(GridLayout iconSheet, AppShortcut app, boolean reuse) {
        if (app != null) {
            if (isAppInstalled(app.getPackageName())) {
                ViewGroup item = getShortcutView(app, false, reuse);
                if (!app.iconLoaded()) {
                    app.loadAppIconAsync(this, mPackageMan);
                }
                ViewGroup parent = (ViewGroup) item.getParent();
                if (parent != null) parent.removeView(item);
                GridLayout.LayoutParams lp = getAppShortcutLayoutParams(app);
                iconSheet.addView(item, lp);
            } //else {
                //Log.d("LaunchTime", "Not showing recent " + app.getPackageName() + " " + app.getActivityName() + ": Not installed.");
            //}
        } else {
            Log.d("LaunchTime", "Not showing recent: Null.");
        }
    }

    @NonNull
    private GridLayout.LayoutParams getAppShortcutLayoutParams(AppShortcut app) {

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
                if (wcells > mColumns) {
                    wcells = mColumns;
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
                if (view==null) {
                    Log.d("gridrelayout", "null child at " + i);
                    continue;
                }
                childViews.add(view);
                gridLayout.removeView(view);

                GridLayout.LayoutParams lp;
                if (view.getTag() instanceof AppShortcut) {
                    AppShortcut app = (AppShortcut) view.getTag();

                    lp = getAppShortcutLayoutParams(app);
                } else {
                    lp = new GridLayout.LayoutParams();
                }
                view.setLayoutParams(lp);
            }
            gridLayout.setColumnCount(columnCount);

            Collections.reverse(childViews);

            for (View view: childViews) {
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

        List<String> dbactvnames = mDb.getAppActvNames();

        Set<String> pmactvnames = new HashSet<>();
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

            if (!pmactvnames.contains(actvname)) {
                pmactvnames.add(actvname);

                app = mDb.getApp(actvname);
                if (dbactvnames.contains(actvname) && app != null) {
                    app.loadAppIconAsync(this, mPackageMan);
                } else {
                    app = AppShortcut.createAppShortcut(this, mPackageMan, ri);
                    newapps.add(app);
                }

                shortcuts.add(app);

            }

        }

        //remove shortcuts if they are not in the system
        for (Iterator<String> it = dbactvnames.iterator(); it.hasNext(); ) {
            String dbactv = it.next();
            if (!pmactvnames.contains(dbactv)) {
                AppShortcut app = mDb.getApp(dbactv);
                if (!isAppInstalled(app.getPackageName())) {  //might be a widget, check packagename
                    Log.d("Launch", "Removing " + dbactv);
                    it.remove();
                    mDb.deleteApp(dbactv);
                    removeFromQuickApps(dbactv);
                }
            }
        }

        mDb.addApps(newapps);

        return shortcuts;
    }

    private void processQuickApps(List<AppShortcut> shortcuts) {
        List<AppShortcut> quickRowApps = new ArrayList<>();
        final List<String> quickRowOrder = mDb.getAppCategoryOrder(QUICK_ROW_CAT);

        MainHelper.checkDefaultApps(this, shortcuts, quickRowOrder, mQuickRow);


        for (AppShortcut app : shortcuts) {

            if (quickRowOrder.contains(app.getActivityName())) {
                AppShortcut qapp = AppShortcut.createAppShortcut(app);
                qapp.loadAppIconAsync(this, mPackageMan);
                quickRowApps.add(qapp);
            }
        }


        mQuickRow.removeAllViews();
        for (String actvname : quickRowOrder) {
            for (AppShortcut app : quickRowApps) {
                if (app.getActivityName().equals(actvname)) {
                    ViewGroup item = getShortcutView(app, true);
                    GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                    lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.TOP);
                    mQuickRow.addView(item, lp);
                }
            }
        }
        mDb.setAppCategoryOrder(mRevCategoryMap.get(mQuickRow), mQuickRow);

    }

    private void removeFromQuickApps(String actvname) {
        for (int i = mQuickRow.getChildCount()-1; i>=0; i--) {
            AppShortcut app = (AppShortcut) mQuickRow.getChildAt(i).getTag();
            if (app != null && actvname.equals(app.getActivityName())) {
                mQuickRow.removeView(mQuickRow.getChildAt(i));
            }
        }
    }

    public ViewGroup getShortcutView(final AppShortcut app) {
        return getShortcutView(app, false, true);
    }
    public ViewGroup getShortcutView(final AppShortcut app, boolean smallIcon) {
        return getShortcutView(app, smallIcon, true);
    }

    public ViewGroup getShortcutView(final AppShortcut app, boolean smallIcon, boolean reuse) {


        if (smallIcon) reuse = false;
        ViewGroup item;
        if (reuse) {
            item = mAppShortcutViews.get(app);
            if (item!=null) return item;
        }

        if (app.isWidget()) {
            item = new FrameLayout(this);

            AppWidgetHostView appwid = mLoadedWidgets.get(app.getActivityName());
            if (appwid == null) {
                appwid = mWidgetHelper.loadWidget(app);
                if (appwid!=null) {
                    mLoadedWidgets.put(app.getActivityName(), appwid);
                    AppWidgetProviderInfo pinfo = appwid.getAppWidgetInfo();
                    Log.d("widsize", "Min: " + pinfo.minWidth + "," + pinfo.minHeight);
                    Log.d("widsize", "MinResize: " + pinfo.minResizeWidth + "," + pinfo.minResizeHeight);
                    Log.d("widsize", "Resizemode: " + pinfo.resizeMode);

                    storeShortCutDimen(app, pinfo.minWidth, pinfo.minHeight);
                }
            }
            if (appwid != null) {
                ViewGroup parent = (ViewGroup) appwid.getParent();
                if (parent != null) {
                    parent.removeView(appwid);
                }
                item.addView(appwid);
                final View wrap = item;
                appwid.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        return MainActivity.this.onLongClick(wrap);
                    }
                });
                appwid.setOnDragListener(new View.OnDragListener() {
                    @Override
                    public boolean onDrag(View view, DragEvent dragEvent) {
                        return mMainDragListener.onDrag(wrap, dragEvent);
                    }
                });

            } else {
                Log.d("Widget2", "AppWidgetHostView was null for " + app.getActivityName() + " " + app.getPackageName());
            }

        } else {

            item = (ViewGroup) LayoutInflater.from(this).inflate(smallIcon ? R.layout.shortcut_small_icon : R.layout.shortcut_icon, (ViewGroup) null);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    launchApp(app);
                }
            });

            ImageView iconImage = (ImageView) item.findViewById(R.id.shortcut_icon);
            app.setIconImage(iconImage);

            if (!smallIcon) {
                TextView iconLabel = (TextView) item.findViewById(R.id.shortcut_text);
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

    private void addWidget(ComponentName cn) {

        String actvname = cn.getClassName();
        String pkgname = cn.getPackageName();

        Log.d("Widget", actvname + " " + pkgname);
        String label = pkgname;


        AppShortcut app = AppShortcut.createAppShortcut(actvname, pkgname, label, mCategory, true);

        mDb.addApp(app);
        mDb.addAppCategoryOrder(mCategory, app.getActivityName());

//        int sw = (int)getResources().getDimension(R.dimen.shortcut_width);
//        int sh = (int)getResources().getDimension(R.dimen.shortcut_height);

//        int wf = (int) Math.ceil(pinfo.minWidth / getResources().getDimension(R.dimen.shortcut_width));
//
//        int hf = (int) Math.ceil(pinfo.minHeight / getResources().getDimension(R.dimen.shortcut_height));

        //wid.updateAppWidgetSize(null,sw, sh, sw*wf, sh*hf);


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

    private ViewGroup getSearchView() {
        ViewGroup searchView = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.search_layout, (ViewGroup) null);

        final AutoCompleteTextView searchbox = (AutoCompleteTextView) searchView.findViewById(R.id.search_box);
        AppCursorAdapter searchAdapter = new AppCursorAdapter(this, searchbox, R.layout.search_item, mDb.getAppCursor("XXXXXX"), 0);
        searchbox.setAdapter(searchAdapter);
        searchbox.setOnItemClickListener(searchAdapter);

        return searchView;
    }

    private CategoryTabStyle getDefaultCategoryStyle(String category) {
        CategoryTabStyle catstyle = CategoryTabStyle.Normal;

        if (category.equals(mCategory)) {
            catstyle = CategoryTabStyle.Selected;
        } else if (mDb.isTinyCategory(category)) {
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
                categoryTab.setBackgroundColor(cattabDragHoverBackground);
                categoryTab.setTextSize(categoryTabFontSize);
                categoryTab.setShadowLayer(0, 0, 0, 0);
                break;
            case Selected:
                categoryTab.setPadding(6, categoryTabPaddingHeight, 2, categoryTabPaddingHeight);
                categoryTab.setBackgroundColor(cattabSelectedBackground);
                categoryTab.setTextSize(categoryTabFontSize);
                categoryTab.setShadowLayer(8, 4, 4, textColorInvert);
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
        categoryTab.setText(mDb.getCategoryDisplay(category));
        categoryTab.setTag(category);
        // categoryTab.setWidth((int)Utils.dpToPx(this,categoryTabWidth));

        categoryTab.setTextColor(textColor);
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
                switchCategory(category);

            }
        });
        categoryTab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ClipData data = ClipData.newPlainText(category, category);
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                view.startDrag(data, shadowBuilder, view, 0);
                mDragDropSource = mCategoriesLayout;
                if (!Categories.isSpeacialCategory(category)) {
                    showRemoveDropzone();
                }
                return true;
            }
        });

        categoryTab.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, final DragEvent event) {
                View view2 = (View) event.getLocalState();
                boolean isAppShortcut = view2.getTag() instanceof AppShortcut;
                boolean isSearch = category.equals(Categories.CAT_SEARCH);
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_ENTERED:
                        if (catstyle == CategoryTabStyle.Tiny || (!isAppShortcut || !isSearch)) {
                            styleCategorySpecial(categoryTab, CategoryTabStyle.DragHover);
                        }
                        break;

                    case DragEvent.ACTION_DRAG_LOCATION:
//                        int thresh = mCategoriesScroller.getHeight()/6;
//                        // System.out.println(view + " " + mIconSheet.getTop() + " " + view.getTop() + " " + event.getY());
//                        if (view.getTop() + event.getY() < mCategoriesScroller.getScrollY() + thresh) {
//                            mCategoriesScroller.smoothScrollBy(0,-5);
//                        } else if (view.getTop() + event.getY() > mCategoriesScroller.getScrollY()+mCategoriesScroller.getHeight() - thresh) {
//                            mCategoriesScroller.smoothScrollBy(0, 5);
//                        }
                        scrollOnDrag(view, event, mCategoriesScroller);
                        break;
                    case DragEvent.ACTION_DRAG_ENDED:
                        mBeingDragged = null;
                        hideRemoveDropzone();
                        hideHiddenCategories();
                    case DragEvent.ACTION_DRAG_EXITED:
//                        if (catstyle==CategoryTabStyle.Tiny) {
//                            styleCategorySpecial(categoryTab, CategoryTabStyle.Tiny);
//                        } else if (!isAppShortcut || !isSearch) {
//                            styleCategorySpecial(categoryTab, CategoryTabStyle.Normal);
//                        }
//                        if (category.equals(mCategory)) {
//                            styleCategorySpecial(categoryTab, CategoryTabStyle.Selected);
//                        }
                        styleCategorySpecial(categoryTab, CategoryTabStyle.Default);
                        break;

                    case DragEvent.ACTION_DROP:
                        if (isAppShortcut) {
                            if (!isSearch) {
                                mDb.updateAppCategory(mBeingDragged.getActivityName(), category);
                                mMainDragListener.onDrag(iconSheet, event);
                            }
                        } else {
                            ViewGroup container1 = (ViewGroup) view.getParent();
                            ViewGroup container2 = (ViewGroup) view2.getParent();


                            int index = -1;
                            for (int i = 0; i < container1.getChildCount(); i++) {
                                if (container1.getChildAt(i) == view) {
                                    index = i;
                                }
                            }
                            container2.removeView(view2);
                            if (index == -1) {
                                container1.addView(view2);
                            } else {
                                container1.addView(view2, index);
                            }
                            mDb.setCategoryOrder(container1);
                        }
                        break;
                }
                return true;
            }
        });
        mCategoriesLayout.addView(categoryTab);

        return categoryTab;
    }

    private View.OnDragListener mMainDragListener = new View.OnDragListener() {
        @Override
        public boolean onDrag(View view, DragEvent event) {
            View view2 = (View) event.getLocalState();
            boolean isShortcut = true;
            if (view2.getTag() == null || !(view2.getTag() instanceof AppShortcut )) {
                isShortcut = false;
            }
            boolean nocolor = view instanceof GridLayout || view == mRemoveDropzone || !isShortcut || mQuickRow == mDragDropSource;
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // do nothing
                    break;

                case DragEvent.ACTION_DRAG_LOCATION:
                    //scroll the scrollview

                    if (isShortcut) scrollOnDrag(view, event, mIconSheetScroller);
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    if (!nocolor ) {
                        view.setBackgroundColor(dragoverBackground);
                    }
                    break;
                case DragEvent.ACTION_DRAG_EXITED:

                    if (!nocolor) view.setBackgroundColor(backgroundDefault);
                    break;
                case DragEvent.ACTION_DROP:
                    if (!nocolor) view.setBackgroundColor(backgroundDefault);
                    // Dropped, reassign View to ViewGroup

                    if (view2 == view) {
                        // Log.d("sort", "self drop");
                        break;
                    }

                    if (handleDrop(view, view2, isShortcut)) return true;
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    if (!nocolor) view.setBackgroundColor(backgroundDefault);
                    mBeingDragged = null;
                    hideRemoveDropzone();
                    hideHiddenCategories();
                    break;
                default:
                    break;
            }
            return true;
        }

        private boolean handleDrop(View view, View view2, boolean isShortcut) {
            ViewGroup target;
            if (view == mRemoveDropzone) {  // need to delete the dropped thing
                //Stuff to be deleted
                if (mQuickRow == mDragDropSource || mBeingDragged!=null && (mBeingDragged.isWidget() || mBeingDragged.isLink())) {
                    removeDroppedItem(view2);
                } else if (mCategory.equals(Categories.CAT_SEARCH)) {
                    removeDroppedRecentItem(view2);
                } else if (mDragDropSource == mCategoriesLayout && !isShortcut) {
                    //delete category tab
                    promptDeleteCategory((String)view2.getTag());

                } else {
                    //uninstall app
                    mBeingUninstalled = view2;
                    launchUninstallIntent(mBeingDragged.getPackageName());
                }
                return true;
            } else if (view instanceof GridLayout) {
                target = (GridLayout) view;

            } else {
                target = (GridLayout) view.getParent();
            }


            //Find the drop position
            int index = -1;
            for (int i = 0; i < target.getChildCount(); i++) {
                if (target.getChildAt(i) == view) {
                    index = i;
                }
            }

            // Don't remove the source icon if it's on the quickrow, unless we're re-arranging
            if ((mDragDropSource == mQuickRow && mQuickRow == target) || (mDragDropSource != mQuickRow && mQuickRow != target)) {
                mDragDropSource.removeView(view2);
            }


            if (target == mQuickRow) {
                if (mQuickRow != mDragDropSource) {
                    //prevent copies of the same app on the quickrow
                    for (int i = 0; i < mQuickRow.getChildCount(); i++) {
                        AppShortcut dragging = (AppShortcut) view2.getTag();
                        AppShortcut inbar = (AppShortcut) mQuickRow.getChildAt(i).getTag();
                        if (dragging.getActivityName().equals(inbar.getActivityName())) {
                            return true;
                        }
                    }
                }
                //make a copy of the shortcut to put on the quickbar
                view2 = getShortcutView(AppShortcut.createAppShortcut((AppShortcut) view2.getTag()), true);

            }

            if (!(target != mQuickRow && mQuickRow == mDragDropSource)) {

                if (index == -1) {
                    target.addView(view2);
                } else {
                    target.addView(view2, index);
                }
            }

            //save the new order
            mDb.setAppCategoryOrder(mRevCategoryMap.get(target), target);
            mDb.setAppCategoryOrder(mRevCategoryMap.get(mDragDropSource), mDragDropSource);
            return false;
        }

        private void removeDroppedRecentItem(View view2) {
            mDb.deleteAppLaunchedRecord(mBeingDragged.getActivityName());
            mDragDropSource.removeView(view2);
        }

        private void removeDroppedItem(View view2) {
            mDragDropSource.removeView(view2);
            mDb.setAppCategoryOrder(mRevCategoryMap.get(mDragDropSource), mDragDropSource);

            if (mBeingDragged.isLink()) {
                mDb.deleteApp(mBeingDragged.getActivityName());
            }

            if (mBeingDragged.isWidget()) {

                mDb.deleteApp(mBeingDragged.getActivityName());
                mLoadedWidgets.remove(mBeingDragged.getActivityName());
            }
        }

    };

    private void scrollOnDrag(View view, DragEvent event, ScrollView scrollView) {
        float ty = view.getTop() + event.getY();

        //check if we're in the bounds of the scroller
        if (  view.getTop() > scrollView.getTop()
            &&
              view.getLeft() > scrollView.getLeft()
            &&
              view.getLeft() + view.getX() < scrollView.getLeft() + scrollView.getWidth()
            &&
                ty < scrollView.getTop() + scrollView.getHeight() + scrollView.getScrollY()) {

            int thresh = scrollView.getHeight() / 6;

            if (ty < scrollView.getScrollY() + thresh) {
                scrollView.smoothScrollBy(0, -10);
            } else if (ty > scrollView.getScrollY() + scrollView.getHeight() - thresh) {
                scrollView.smoothScrollBy(0, 10);
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        mBeingDragged = (AppShortcut) view.getTag();
        mDragDropSource = (ViewGroup) view.getParent();
        String label = mBeingDragged.getLabel();
        ClipData data = ClipData.newPlainText(label, label);
        View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
        view.startDrag(data, shadowBuilder, view, 0);
        //view.setVisibility(View.INVISIBLE);

        showHiddenCategories();
        showRemoveDropzone();

        return true;
    }



    private void showHiddenCategories() {
        for (String cat: Categories.CAT_HIDDENS) {
            mCategoryTabs.get(cat).setVisibility(View.VISIBLE);
        }
    }
    private void hideHiddenCategories() {
        for (String cat: Categories.CAT_HIDDENS) {
            if (!mCategory.equals(cat)) {
                mCategoryTabs.get(cat).setVisibility(View.GONE);
            } else {
                mCategoryTabs.get(cat).setVisibility(View.VISIBLE);
            }
        }
    }

    private void showRemoveDropzone() {
        mRemoveDropzone.setVisibility(View.VISIBLE);
        mRemoveDropzone.setBackgroundColor(Color.RED);

        if (mDragDropSource == mQuickRow
            || mDragDropSource == mCategoriesLayout
            || mCategory.equals(Categories.CAT_SEARCH)
            || (mBeingDragged!=null && (mBeingDragged.isWidget() || mBeingDragged.isLink())
        ) ) {
            mRemoveAppText.setText(R.string.remove_shortcut);
        } else {
            mRemoveAppText.setText(R.string.uninstall_app);
        }
    }

    private void hideRemoveDropzone() {
        mRemoveDropzone.setVisibility(View.GONE);
    }

    private void launchUninstallIntent(String packageName) {
        Log.d("Launch", "Uninstalling " + packageName);
        Uri packageUri = Uri.parse("package:" + packageName);
        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
        uninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        startActivityForResult(uninstallIntent, UNINSTALL_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UNINSTALL_RESULT) {

            switch (resultCode) {
                case RESULT_OK:
                    mDragDropSource.removeView(mBeingUninstalled);
                    mDb.setAppCategoryOrder(mRevCategoryMap.get(mDragDropSource), mDragDropSource);
                    Toast.makeText(this, R.string.app_was_uninstalled, Toast.LENGTH_SHORT).show();
                    String actvname = ((AppShortcut) mBeingUninstalled.getTag()).getActivityName();
                    mDb.deleteApp(actvname);
                    removeFromQuickApps(actvname);
                    break;
                case RESULT_CANCELED:
                    Toast.makeText(this, R.string.uninstall_canceled, Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(this, R.string.could_not_uninstall, Toast.LENGTH_LONG).show();

            }
        } else if (requestCode == PREF_REQUEST) {
            checkConfig();
            switchCategory(mCategory);
        } else {
            ComponentName cn = mWidgetHelper.onActivityResult(requestCode, resultCode, data);
            if (cn == null) {
                super.onActivityResult(requestCode, resultCode, data);
            } else {
                addWidget(cn);
            }
        }
    }

    private void promptGetCategoryName(String title, String message, final String category, String defName,
                                       String defFullName, boolean defIsTiny, final CategoryChangerListener categoryChangerListener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        ViewGroup view = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.category_name, (ViewGroup) null);

        TextView messageView = (TextView) view.findViewById(R.id.message_txt);
        final EditText shortname = (EditText) view.findViewById(R.id.shortname);
        final EditText fullname = (EditText) view.findViewById(R.id.fullname);
        final CheckBox isTiny = (CheckBox) view.findViewById(R.id.istiny_checkbox);

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
                categoryChangerListener.onClick(dialog, which, category, shortname.getText().toString(), fullname.getText().toString(), isTiny.isChecked());
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
                getString(R.string.rename_cat2),
                category,
                mDb.getCategoryDisplay(category),
                mDb.getCategoryDisplayFull(category),
                mDb.isTinyCategory(category),
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

        if (mDb.updateCategory(category, newDisplayName, newDisplayFullName, isTiny)) {

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

        if (mDb.addCategory(category, newDisplayName, newDisplayFullName, isTiny)) {
            createIconSheet(category);

            switchCategory(category);
        } else {
            Toast.makeText(MainActivity.this, R.string.no_add_cat, Toast.LENGTH_SHORT).show();
        }
    }

    private void promptDeleteCategory(final String category) {

        final String message = getString(R.string.cat_deleted, mDb.getCategoryDisplay(Categories.CAT_OTHER));
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

        if (mDb.deleteCategory(category)) {

            View iconSheet = mIconSheets.get(category);
            mRevCategoryMap.remove(iconSheet);


            mCategoryTabs.remove(category);
            mRevCategoryMap.remove(categoryTab);

            mCategoriesLayout.removeView(categoryTab);

            repopulateIconSheet(Categories.CAT_OTHER);
            //String newcat = mCategoryTabs.keySet().iterator().next();

            switchCategory(Categories.CAT_OTHER);

        } else {
            Toast.makeText(MainActivity.this, R.string.no_delete_cat, Toast.LENGTH_SHORT).show();
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

        mCategoriesScroller = (ScrollView)findViewById(R.id.layout_categories_scroller);

        hideRemoveDropzone();


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

        mAddCategoryButton = findViewById(R.id.btn_add_cat);
        mRenameCategoryButton = findViewById(R.id.btn_rename_cat);
        mDeleteCategoryButton = findViewById(R.id.btn_delete_cat);
        mEditWidgetsButton = findViewById(R.id.btn_widgets);
        mOpenPrefsButton = findViewById(R.id.btn_prefs);

        mRenameCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptRenameCategory(mCategory);
                showButtonBar(false);
            }
        });

        mAddCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptAddCategory();
                showButtonBar(false);
            }
        });

        mDeleteCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptDeleteCategory(mCategory);
                showButtonBar(false);
            }
        });

        mEditWidgetsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setupWidget();
            }
        });

        mOpenPrefsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(settingsIntent,PREF_REQUEST);
            }
        });

    }

    private static final int PREF_REQUEST=4353;

    private void toggleButtonBar() {
        int vis = mIconSheetBottomFrame.getVisibility();
        showButtonBar(vis != View.VISIBLE);
    }


    //initialize the form members

    private void showButtonBar(boolean visible) {

        if (visible) {
            showHiddenCategories();
            if (Categories.isSpeacialCategory(mCategory)) {
                mDeleteCategoryButton.setVisibility(View.INVISIBLE);
            } else {
                mDeleteCategoryButton.setVisibility(View.VISIBLE);
            }
            if (mCategory.equals(Categories.CAT_SEARCH)) {
                mEditWidgetsButton.setVisibility(View.INVISIBLE);
            } else {
                mEditWidgetsButton.setVisibility(View.VISIBLE);
            }
            mIconSheetBottomFrame.setVisibility(View.VISIBLE);
            mShowButtons.setImageResource(android.R.drawable.arrow_down_float);
        } else {
            hideHiddenCategories();
            mIconSheetBottomFrame.setVisibility(View.GONE);
            mShowButtons.setImageResource(android.R.drawable.arrow_up_float);
        }
    }

    private void setColors() {
        cattabBackground = getResColor(R.color.cattab_background);
        cattabSelectedBackground = getResColor(R.color.cattabselected_background);
        cattabDragHoverBackground = getResColor(R.color.cattabdraghover_background);
        dragoverBackground = getResColor(R.color.dragover_background);

        textColor = getResColor(R.color.textcolor);
        textColorInvert = getResColor(R.color.textcolorinv);

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
