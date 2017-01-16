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
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quaap.launchtime.components.AppCursorAdapter;
import com.quaap.launchtime.components.AppShortcut;
import com.quaap.launchtime.components.Categories;
import com.quaap.launchtime.components.InteractiveScrollView;
import com.quaap.launchtime.db.DB;
import com.quaap.launchtime.widgets.Widget;

import java.lang.reflect.Method;
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
        View.OnLongClickListener, View.OnDragListener {

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
    private float categoryTabFontSizeHidden = 12;
    private int categoryTabPaddingHeight = 16;
    private int mColumns = 3;
    private int mColumnsLandscape = 5;
    private int mColumnsPortrait = 3;
    private int mColumnMargin = 12;
    private int categoryTabWidth = 108;

    private Point mScreenDim;

    private SharedPreferences mAppPreferences;

    private Map<String, AppWidgetHostView> mLoadedWidgets = new HashMap<>();
    public Map<AppShortcut,ViewGroup> mAppShortcutViews = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mPackageMan = getApplicationContext().getPackageManager();

        mAppPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mScreenDim = getScreenDimensions();

        mWidgetHelper = new Widget(this);

        setColors();
        initUI();

        mQuickRow.setOnDragListener(this);
        mQuickRowScroller.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                return MainActivity.this.onDrag(mQuickRow, dragEvent);
            }
        });
        mIconSheetHolder.setOnDragListener(iconSheetDropRedirector);

        findViewById(R.id.iconarea_wrap).setOnDragListener(iconSheetDropRedirector);


        mSearchView = getSearchView();
        mPrefs = getSharedPreferences("default", MODE_PRIVATE);
        mCategory = mPrefs.getString("category", Categories.CAT_TALK);

        loadApplications();

    }

    private View.OnDragListener iconSheetDropRedirector = new View.OnDragListener() {
        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            return MainActivity.this.onDrag(mIconSheet, dragEvent);
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

       // setLockUI(true);

        mCategory = mPrefs.getString("category", Categories.CAT_TALK);
        switchCategory(mCategory);
        mIconSheetScroller.postDelayed(new Runnable() {
            @Override
            public void run() {
                mIconSheetScroller.smoothScrollTo(0, mPrefs.getInt("scrollpos",0));

            }
        },100);

    }

//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        setLockUI(true);
//    }

    public void setLockUI(boolean lock) {
        Log.d("Launch", "Setting lockui to " + lock);
        View topmar= findViewById(R.id.txttop_margin);
        if (lock) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);

            topmar.setVisibility(View.VISIBLE);

            topmar.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    switch (MotionEventCompat.getActionMasked(event)) {
                        case MotionEvent.ACTION_DOWN:
                            updateTouchDown(event);
                            break;

                        case MotionEvent.ACTION_MOVE:
                            tryConsumeSwipe(event);
                            break;

                        case MotionEvent.ACTION_UP:
                            // We only want to launch the activity if the touch was not consumed yet!
                            if (!touchConsumed) {
                                //                            Intent i = mPacMan.getLaunchIntentForPackage(app.name.toString());
                                //                            startActivity(i);
                            }
                            break;
                    }

                    return touchConsumed;
                }
            });
        } else {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
            decorView.setSystemUiVisibility(uiOptions);
            topmar.setOnTouchListener(null);
        }
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
            catTab.setText(getDB().getCategoryDisplay(mRevCategoryMap.get(catTab)));
        }

        mCategoryTabs.get(category).setText(getDB().getCategoryDisplayFull(category));

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
        if (mIconSheetScroller.getScrollY()>0) {
            mIconSheetScroller.smoothScrollTo(0, 0);
        } else if (!mCategory.equals(Categories.CAT_TALK)){
            switchCategory(Categories.CAT_TALK);
        }
    }

    private void checkConfig() {

        int orientationPref = 0;
        try {
            orientationPref = Integer.parseInt(mAppPreferences.getString("preference_orientation", "0"));
        } catch (Exception e) {
            Log.e("Launch", e.getMessage(), e);
        }

        switch (orientationPref) {
            case 0:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
                break;
            case 1:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case 2:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;

        }


        mScreenDim = getScreenDimensions();
        float shortcutw = getResources().getDimension(R.dimen.shortcut_width);
        float catwidth = getResources().getDimension(R.dimen.cattabbar_width);
        mColumns = (int)((mScreenDim.x - catwidth)/(shortcutw + 2));

//        if (isLandscape()) {
//            mColumns = mColumnsLandscape;
//        } else {
//            mColumns = mColumnsPortrait;
//        }

        changeColumnCount(mIconSheet, mColumns);

    }

    public DB getDB() {
        return GlobState.getGlobState(this).getDB();
    }

    public void launchApp(String activityname) {
        launchApp(getDB().getApp(activityname));
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
            if (uristr==null) {
                intent = new Intent(Intent.ACTION_MAIN);
            } else {
                intent = new Intent(Intent.ACTION_MAIN, Uri.parse(uristr));
            }
            intent.setClassName(packagename, activityname);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            showButtonBar(false);
            getDB().appLaunched(activityname);
        } catch (Exception e) {
            Log.d("Launch", "Could not launch " + activityname, e);
        }

    }

    private void loadApplications() {

        final DB db = getDB();

        //Make sure the displayed icons load first
        //Load the quickrow icons first
        for (String actvname: db.getAppCategoryOrder(QUICK_ROW_CAT)) {
            AppShortcut app = db.getApp(actvname);
            if (app!=null) {
                app.loadAppIconAsync(this, mPackageMan);
            }
        }

        //Load the selected category icons
        for (String actvname: db.getAppCategoryOrder(mCategory)) {
            AppShortcut app = db.getApp(actvname);
            if (app!=null) {
                app.loadAppIconAsync(this, mPackageMan);
            }
        }


        //Look for new apps
        final List<AppShortcut> shortcuts = processActivities(db);

        //loads the quickrow or adds default apps if it is empty
        processQuickApps(db, shortcuts);



        for (final String category : db.getCategories()) {

            createIconSheet(category);
        }

    }

    @NonNull
    private GridLayout createIconSheet(String category) {
        final GridLayout iconSheet = new GridLayout(MainActivity.this);
        mIconSheets.put(category, iconSheet);
        mRevCategoryMap.put(iconSheet, category);
        iconSheet.setColumnCount(mColumns);
        iconSheet.setOnDragListener(MainActivity.this);

        final TextView categoryTab = createCategoryTab(category, iconSheet);


        mCategoryTabs.put(category, categoryTab);
        mRevCategoryMap.put(categoryTab, category);
        return iconSheet;
    }

    private void populateRecentApps(GridLayout iconSheet) {
        DB db = getDB();

        iconSheet.removeAllViews();

        int i=0;
        for (String actvname : db.getAppLaunchedList()) {
            AppShortcut app = db.getApp(actvname);

            addAppToIconSheet(iconSheet, app, false);
            if (i++>60) break;
        }
    }

    private void repopulateIconSheet(String category) {
        GridLayout iconSheet = mIconSheets.get(category);

        iconSheet.removeAllViews();
        DB db = getDB();

        final List<String> apporder = db.getAppCategoryOrder(category);
        List<AppShortcut> apps = db.getApps(category);

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
            getDB().setAppCategoryOrder(category, iconSheet);
        }
    }

    private void addAppToIconSheet(GridLayout iconSheet, AppShortcut app, boolean reuse) {
        if (app != null && isAppInstalled(app.getPackageName())) {
            ViewGroup item = getShortcutView(app, false, reuse);
            if (!app.iconLoaded()) {
                app.loadAppIconAsync(this, mPackageMan);
            }
            GridLayout.LayoutParams lp = getAppShortcutLayoutParams(app);
            iconSheet.addView(item, lp);
        }
    }

    @NonNull
    private GridLayout.LayoutParams getAppShortcutLayoutParams(AppShortcut app) {

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();

        int w = getShortCutWidth(app);
        int h = getShortCutHeight(app);

        if (w>0 || h>0) {
            float sw = getResources().getDimension(R.dimen.shortcut_width);
            float sh = getResources().getDimension(R.dimen.shortcut_height);

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

                Bundle b = new Bundle();

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


    private void listAll() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);


        // Get all activities that have those filters
        List<ResolveInfo> activities = mPackageMan.queryIntentActivities(intent, 0);


        for (int i = 0; i < activities.size(); i++) {

            AppShortcut app;

            ResolveInfo resolveInfo = activities.get(i);
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            String name = activityInfo.name;
            String packageName = activityInfo.applicationInfo.packageName;
            String className = activityInfo.applicationInfo.className;
            String label = resolveInfo.loadLabel(mPackageMan).toString();
            boolean enabled = activityInfo.enabled;
            boolean exported = activityInfo.exported;

        }
    }

    private List<AppShortcut> processActivities(DB db) {
        final List<AppShortcut> shortcuts = new ArrayList<>();

        List<String> dbactvnames = db.getAppActvNames();

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

                app = db.getApp(actvname);
                if (dbactvnames.contains(actvname) && app != null) {
                    app.loadAppIconAsync(this, mPackageMan);
                } else {
                    app = AppShortcut.createAppShortcut(this, mPackageMan, ri);
                    newapps.add(app);
                }

                shortcuts.add(app);

            }
            //else {
            // Log.d("Launch", actvname + " " + ri.activityInfo.name);
            //}
        }

        //remove shortcuts if they are not in the system
        for (Iterator<String> it = dbactvnames.iterator(); it.hasNext(); ) {
            String dbactv = it.next();
            if (!pmactvnames.contains(dbactv)) {
                AppShortcut app = db.getApp(dbactv);
                if (!isAppInstalled(app.getPackageName())) {  //might be a widget, check packagename
                    Log.d("Launch", "Removing " + dbactv);
                    it.remove();
                    db.deleteApp(dbactv);
                    removeFromQuickApps(dbactv);
                }
            }
        }

        db.addApps(newapps);

        return shortcuts;
    }

    private void processQuickApps(DB db, List<AppShortcut> shortcuts) {
        List<AppShortcut> quickRowApps = new ArrayList<>();
        final List<String> quickRowOrder = db.getAppCategoryOrder(QUICK_ROW_CAT);

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
        db.setAppCategoryOrder(mRevCategoryMap.get(mQuickRow), mQuickRow);

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
                        return MainActivity.this.onDrag(wrap, dragEvent);
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
        item.setOnDragListener(this);

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

        getDB().addApp(app);
        getDB().addAppCategoryOrder(mCategory, app.getActivityName());

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
        AppCursorAdapter searchAdapter = new AppCursorAdapter(this, searchbox, R.layout.search_item, getDB().getAppCursor("XXXXXX"), 0);
        searchbox.setAdapter(searchAdapter);
        searchbox.setOnItemClickListener(searchAdapter);

        return searchView;
    }

    private CategoryTabStyle getDefaultCategoryStyle(String category) {
        CategoryTabStyle catstyle = CategoryTabStyle.Normal;

        if (category.equals(mCategory)) {
            catstyle = CategoryTabStyle.Selected;
        } else if (Categories.isTinyCategory(category)) {
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
                categoryTab.setPadding(6, 0, 2, 0);
                categoryTab.setBackgroundColor(cattabBackground);
                categoryTab.setTextSize(categoryTabFontSizeHidden);
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
        categoryTab.setText(getDB().getCategoryDisplay(category));
        categoryTab.setTag(category);
        // categoryTab.setWidth((int)Utils.dpToPx(this,categoryTabWidth));

        categoryTab.setTextColor(textColor);
        categoryTab.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        final CategoryTabStyle catstyle = getDefaultCategoryStyle(category);

        if (catstyle == CategoryTabStyle.Normal) {
            lp.weight = 1;
        }
        styleCategorySpecial(categoryTab, CategoryTabStyle.Default, category);
        lp.gravity = Gravity.CENTER;
        lp.setMargins(2, 6, 2, 8);
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


                    case DragEvent.ACTION_DRAG_ENDED:
                        mBeingDragged = null;
                        hideRemoveDropzone();
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
                                getDB().updateAppCategory(mBeingDragged.getActivityName(), category);
                                MainActivity.this.onDrag(iconSheet, event);
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
                            getDB().setCategoryOrder(container1);
                        }
                        break;
                }
                return true;
            }
        });
        mCategoriesLayout.addView(categoryTab);

        return categoryTab;
    }

    @Override
    public boolean onDrag(View view, DragEvent event) {
        View view2 = (View) event.getLocalState();
        if (view2.getTag() == null || !(view2.getTag() instanceof AppShortcut)) {
            return false;
        }
        boolean islayout = view instanceof GridLayout;
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                // do nothing
                break;

            case DragEvent.ACTION_DRAG_LOCATION:
                int thresh = mScreenDim.y/6;
               // System.out.println(view + " " + mIconSheet.getTop() + " " + view.getTop() + " " + event.getY());
                if (view.getTop() + event.getY() < mIconSheetScroller.getScrollY() + thresh) {
                    mIconSheetScroller.smoothScrollBy(0,-10);
                } else if (view.getTop() + event.getY() > mIconSheetScroller.getScrollY()+mIconSheetScroller.getHeight() - thresh) {
                    mIconSheetScroller.smoothScrollBy(0, 10);
                }
                break;
            case DragEvent.ACTION_DRAG_ENTERED:
                if (!islayout) {
                    view.setBackgroundColor(dragoverBackground);
                }
                break;
            case DragEvent.ACTION_DRAG_EXITED:

                if (!islayout) view.setBackgroundColor(backgroundDefault);
                break;
            case DragEvent.ACTION_DROP:
                if (!islayout) view.setBackgroundColor(backgroundDefault);
                // Dropped, reassign View to ViewGroup

                if (view2 == view) {
                    // Log.d("sort", "self drop");
                    break;
                }

                ViewGroup target;
                if (view == mRemoveDropzone) {
                    if (mQuickRow == mDragDropSource || mBeingDragged.isWidget()) {
                        mDragDropSource.removeView(view2);
                        getDB().setAppCategoryOrder(mRevCategoryMap.get(mDragDropSource), mDragDropSource);
                        if (mBeingDragged.isWidget()) {

                            getDB().deleteApp(mBeingDragged.getActivityName());
                            mLoadedWidgets.remove(mBeingDragged.getActivityName());
                        }

                    } else if (mDragDropSource == mCategoriesLayout) {
                        //delete category tab

                    } else if (mBeingDragged.isLink()) {
                        getDB().deleteApp(mBeingDragged.getActivityName());
                        mDragDropSource.removeView(view2);
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


                if ((mDragDropSource == mQuickRow && mQuickRow == target) || (mDragDropSource != mQuickRow && mQuickRow != target)) {
                    mDragDropSource.removeView(view2);
                }


                int index = -1;
                for (int i = 0; i < target.getChildCount(); i++) {
                    if (target.getChildAt(i) == view) {
                        index = i;
                    }
                }

                if (target == mQuickRow) {
                    if (mQuickRow != mDragDropSource) {
                        for (int i = 0; i < mQuickRow.getChildCount(); i++) {
                            AppShortcut dragging = (AppShortcut) view2.getTag();
                            AppShortcut inbar = (AppShortcut) mQuickRow.getChildAt(i).getTag();
                            if (dragging.getActivityName().equals(inbar.getActivityName())) {
                                return true;
                            }
                        }
                    }

                    view2 = getShortcutView(AppShortcut.createAppShortcut((AppShortcut) view2.getTag()), true);

                }

                if (!(target != mQuickRow && mQuickRow == mDragDropSource)) {

                    if (index == -1) {
                        target.addView(view2);
                    } else {
                        target.addView(view2, index);
                    }
                }


                getDB().setAppCategoryOrder(mRevCategoryMap.get(target), target);
                getDB().setAppCategoryOrder(mRevCategoryMap.get(mDragDropSource), mDragDropSource);
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                if (!islayout) view.setBackgroundColor(backgroundDefault);
                mBeingDragged = null;
                hideRemoveDropzone();
                break;
            default:
                break;
        }
        return true;
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

        showRemoveDropzone();

        return true;
    }

    private void showRemoveDropzone() {
        mRemoveDropzone.setVisibility(View.VISIBLE);
        mRemoveDropzone.setBackgroundColor(Color.RED);

        if (mDragDropSource == mQuickRow || (mBeingDragged!=null && (mBeingDragged.isWidget()||mBeingDragged.isLink())) ) {
            mRemoveAppText.setText(R.string.remove_shortcut);
        } else {
            mRemoveAppText.setText(R.string.uninstall_app);
        }
    }

    private void hideRemoveDropzone() {
        mRemoveDropzone.setVisibility(View.GONE);
    }

    private void launchUninstallIntent(String packageName) {
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
                    getDB().setAppCategoryOrder(mRevCategoryMap.get(mDragDropSource), mDragDropSource);
                    Toast.makeText(this, R.string.app_was_uninstalled, Toast.LENGTH_SHORT).show();
                    String actvname = ((AppShortcut) mBeingUninstalled.getTag()).getActivityName();
                    getDB().deleteApp(actvname);
                    removeFromQuickApps(actvname);
                    break;
                case RESULT_CANCELED:
                    Toast.makeText(this, R.string.uninstall_canceled, Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(this, R.string.could_not_uninstall, Toast.LENGTH_LONG).show();

            }
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
                                       String defFullName, final CategoryChangerListener categoryChangerListener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        ViewGroup view = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.category_name, (ViewGroup) null);

        TextView messageView = (TextView) view.findViewById(R.id.message_txt);
        final EditText shortname = (EditText) view.findViewById(R.id.shortname);
        final EditText fullname = (EditText) view.findViewById(R.id.fullname);

        shortname.setSelectAllOnFocus(true);
        fullname.setSelectAllOnFocus(true);

        messageView.setText(message);
        shortname.setText(defName);
        fullname.setText(defFullName);

        builder.setView(view);

        builder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                categoryChangerListener.onClick(dialog, which, category, shortname.getText().toString(), fullname.getText().toString());
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
                getDB().getCategoryDisplay(category),
                getDB().getCategoryDisplayFull(category),
                new CategoryChangerListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, String category, String newDisplayName, String newDisplayFullName) {
                        try {
                            renameCategory(category, newDisplayName, newDisplayFullName);
                        } catch (IllegalArgumentException e) {

                            Toast.makeText(MainActivity.this, R.string.need_name, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void renameCategory(String category, String newDisplayName, String newDisplayFullName) {
        newDisplayName = newDisplayName.trim();
        newDisplayFullName = newDisplayFullName.trim();

        if (newDisplayFullName.length() == 0) {
            newDisplayFullName = newDisplayName;
        }

        if (newDisplayName.length() < 1) {
            throw new IllegalArgumentException("Must give a name");
        }

        if (getDB().updateCategory(category, newDisplayName, newDisplayFullName)) {

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
                new CategoryChangerListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, String category, String newDisplayName, String newDisplayFullName) {
                        try {
                            addCategory(category, newDisplayName, newDisplayFullName);
                        } catch (IllegalArgumentException e) {

                            Toast.makeText(MainActivity.this, R.string.need_name, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void addCategory(String category, String newDisplayName, String newDisplayFullName) {
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

        if (getDB().addCategory(category, newDisplayName, newDisplayFullName)) {
            createIconSheet(category);

            switchCategory(category);
        } else {
            Toast.makeText(MainActivity.this, R.string.no_add_cat, Toast.LENGTH_SHORT).show();
        }
    }

    private void promptDeleteCategory(final String category) {

        new AlertDialog.Builder(MainActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.delete_cat)
                .setMessage(R.string.delete_cat_prompt)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteCategory(category);
                        Toast.makeText(MainActivity.this, R.string.cat_deleted, Toast.LENGTH_SHORT).show();
                    }

                })
                .setNegativeButton(R.string.cancel, null)
                .show();

    }

    private void deleteCategory(final String category) {
        TextView categoryTab = mCategoryTabs.get(category);

        if (getDB().deleteCategory(category)) {

            View iconSheet = mIconSheets.get(category);
            mRevCategoryMap.remove(iconSheet);


            mCategoryTabs.remove(category);
            mRevCategoryMap.remove(categoryTab);

            mCategoriesLayout.removeView(categoryTab);

            repopulateIconSheet(Categories.CAT_OTHER);
            String newcat = mCategoryTabs.keySet().iterator().next();

            switchCategory(newcat);

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
        mRemoveDropzone.setOnDragListener(this);
        mRemoveAppText = (TextView) findViewById(R.id.remove_dz_txt);

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
                startActivity(settingsIntent);
            }
        });

    }

    private void toggleButtonBar() {
        int vis = mIconSheetBottomFrame.getVisibility();
        showButtonBar(vis != View.VISIBLE);
    }


    //initialize the form members

    private void showButtonBar(boolean visible) {

        if (visible) {
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
        void onClick(DialogInterface dialog, int which, String category, String newDisplayName, String newDisplayFullName);
    }


    private float lastX, lastY;
    private boolean touchConsumed; // Did we consume the touch event yet? This will avoid calling it twice
    private float touchSlop;

    void updateTouchDown(MotionEvent event) {
        lastX = event.getX();
        lastY = event.getY();
        touchConsumed = false;
    }

    void tryConsumeSwipe(MotionEvent event) {
        if (!touchConsumed) {
            // Also subtract the X: we want to trigger if we scroll down, not to the sides
            float downSpeed = event.getY() - lastY - Math.abs(lastX - event.getX());
            if (downSpeed > touchSlop) {
                // The user swiped down, show the status bar and consume the event
                expandNotificationPanel();
                touchConsumed = true;
            } else {
                updateTouchDown(event);
            }
        }
    }

    void expandNotificationPanel() {
        Log.d("Launch", "Expanding status");
        try
        {
            //noinspection WrongConstant
            Object service = getSystemService("statusbar");
            Class<?> clazz = Class.forName("android.app.StatusBarManager");
            Method expand = Build.VERSION.SDK_INT <= 16 ?
                    clazz.getMethod("expand") :
                    clazz.getMethod("expandNotificationsPanel");

            expand.invoke(service);
        }
        catch (Exception localException) {
            localException.printStackTrace();
        }
    }
}
