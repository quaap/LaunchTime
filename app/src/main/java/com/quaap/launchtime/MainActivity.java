package com.quaap.launchtime;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;

import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
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
import com.quaap.launchtime.components.InteractiveScrollView;
import com.quaap.launchtime.components.Utils;
import com.quaap.launchtime.components.Widget;
import com.quaap.launchtime.db.DB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
    private Map<String,GridLayout> mIconSheets;
    private GridLayout mIconSheet;
    private Map<String,TextView> mCategoryTabs;
    private Map<View, String> mRevCategoryMap;
    private volatile String mCategory;
    private GridLayout mQuickRow;
    private HorizontalScrollView mQuickRowScroller;


    private ImageView mShowButtons;
    private View mAddCategoryButton;
    private View mRenameCategoryButton;
    private View mDeleteCategoryButton;
    private View mEditWidgetsButton;



    private LinearLayout mCategoriesLayout;
    private TextView mRemoveAppText;
    private FrameLayout mRemoveDropzone;
    private PackageManager mPackageMan;
    private AppShortcut mBeingDragged;
    private volatile ViewGroup mDragDropSource;
    private SharedPreferences mPrefs;
    private View mBeingUninstalled;
    private Widget mWidgetHost;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mPackageMan = getApplicationContext().getPackageManager();

        mWidgetHost = new Widget(this);

        setColors();
        initUI();

        mQuickRow.setOnDragListener(this);
        mQuickRowScroller.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                return MainActivity.this.onDrag(mQuickRow, dragEvent);
            }
        });
        mIconSheetHolder.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                return MainActivity.this.onDrag(mIconSheet, dragEvent);
            }
        });

        mSearchView = getSearchView();

        loadApplications();


       // mCategoriesLayout

        mPrefs = getSharedPreferences("default",MODE_PRIVATE);

    }



    private void setupWidget() {
        mWidgetHost.popupSelectWidget();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        checkConfig();
    }

    @Override
    protected void onPause() {
        mPrefs.edit().putString("category", mCategory).apply();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCategory = mPrefs.getString("category", Categories.CAT_TALK);
        switchCategory(mCategory);
    }


    private void switchCategory(String category) {
        if (category==null) return;
        mCategory = category;
        for(TextView catTab: mCategoryTabs.values()) {
            styleCategorySpecial(catTab, CategoryTabStyle.Default);
            catTab.setText(getDB().getCategoryDisplay(mRevCategoryMap.get(catTab)));
        }

        mCategoryTabs.get(category).setText(getDB().getCategoryDisplayFull(category));

        mIconSheetHolder.removeAllViews();
        mIconSheet = mIconSheets.get(category);

        checkConfig();

        mIconSheetHolder.addView(mIconSheet);


        mIconSheetTopFrame.removeAllViews();
        if (category.equals(Categories.CAT_SEARCH)) {

            mIconSheetTopFrame.addView(mSearchView);
            populateRecentApps(mIconSheet);
        }


    }

    private void checkConfig() {
        if (Utils.isLandscape(this)) {
            mColumns = mColumnsLandscape;
        } else {
            mColumns = mColumnsPortrait;
        }

        Utils.changeColumnCount(mIconSheet, mColumns, (int)getResources().getDimension(R.dimen.shortcut_width));

    }


    public DB getDB() {
        return GlobState.getGlobState(this).getDB();
    }

    public void launchApp(String activityname) {
        launchApp(getDB().getApp(activityname));
    }

    public void launchApp(final AppShortcut app) {
        launchApp(app.getActivityName(),app.getPackageName());
    }

    public void launchApp(String activityname, String packagename) {
        getDB().appLaunched(activityname);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(packagename, activityname);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    private void loadApplications() {

        final DB db = getDB();

        final Map<String, List<AppShortcut>> shortcuts = processActivities(db);

        processQuickApps(db, shortcuts);

        for (final String category: db.getCategories()) {

            List<AppShortcut> capps = shortcuts.get(category);
            if (capps==null) capps = new ArrayList<>();

            final List<AppShortcut> catapps = capps;
            Collections.sort(catapps);

            final GridLayout iconSheet = getIconSheet(category);
            processIconSheet(db, category, iconSheet, catapps);

        }

    }

    @NonNull
    private GridLayout getIconSheet(String category) {
        final GridLayout iconSheet = new GridLayout(MainActivity.this);
        mIconSheets.put(category, iconSheet);
        mRevCategoryMap.put(iconSheet, category);
        iconSheet.setColumnCount(mColumns);
        iconSheet.setOnDragListener(MainActivity.this);

        if (category.equals(Categories.CAT_SEARCH)) {


            populateRecentApps(iconSheet);
        }


        final TextView categoryTab = getCategoryTab(category, iconSheet);


        mCategoryTabs.put(category, categoryTab);
        mRevCategoryMap.put(categoryTab, category);
        return iconSheet;
    }

    private void populateRecentApps(GridLayout iconSheet) {
        DB db = getDB();

        for (int i=iconSheet.getChildCount()-1; i>=0; i--) {
            View child = iconSheet.getChildAt(i);

            if (child!=null && child.getTag()!=null && child.getTag() instanceof AppShortcut) {
                iconSheet.removeView(child);
            }
        }

        for (String actvname: db.getAppLaunchedList()) {
            AppShortcut app = db.getApp(actvname);
            app.loadAppIconAsync(mPackageMan);
            iconSheet.addView(getShortcutView(app));
        }
    }

    private void processIconSheet(final DB db, final String category, final GridLayout iconSheet, final List<AppShortcut> catapps) {
        final List<String> apporder = db.getAppCategoryOrder(category);

        GlobState.getGlobState(this).runAsync(new Runnable() {
            @Override
            public void run() {
     //   Log.d("category--------", category);

                for (String actvname: apporder) {
                   // Log.d("apporder", pkgname);
                    for (AppShortcut app : catapps) {
                        if (app.getActivityName().equals(actvname)) {
                            ViewGroup item = getShortcutView(app);
                            iconSheet.addView(item);
                        }
                    }
                }

                boolean reorder = false;
                for (AppShortcut app : catapps) {
                    if (!apporder.contains(app.getActivityName())) {
                      //  Log.d("no apporder", app.getPackageName());

                        ViewGroup item = getShortcutView(app);

                        iconSheet.addView(item);
                        reorder = true;
                    }
                }
                if (reorder) {
                    db.setAppCategoryOrder(category, iconSheet);
                }

            }
        });
    }


    private Map<String, List<AppShortcut>> processActivities(DB db) {
        final Map<String, List<AppShortcut>> shortcuts = new LinkedHashMap<>();

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

                if (dbactvnames.contains(actvname)) {
                    app = db.getApp(actvname);
                    app.loadAppIconAsync(mPackageMan);
                } else {
                    app = new AppShortcut(mPackageMan, ri);
                    //db.addApp(app);
                    newapps.add(app);
                }


                List<AppShortcut> catapps = shortcuts.get(app.getCategory());
                if (catapps == null) {
                    catapps = new ArrayList<>();
                    shortcuts.put(app.getCategory(), catapps);
                }
                catapps.add(app);


            } else {
                Log.d("Launch", actvname + " " + ri.activityInfo.name);
            }
        }

        //remove shortcuts if they are not in the system
        for (Iterator<String> it = dbactvnames.iterator(); it.hasNext();) {
            String dbactv = it.next();
            if (!pmactvnames.contains(dbactv)) {
                it.remove();
                //db.remove(dbpkg);
            }
        }

        db.addApps(newapps);

        return shortcuts;
    }

    private void processQuickApps(DB db, Map<String, List<AppShortcut>> shortcuts) {
        List<AppShortcut> quickRowApps = new ArrayList<>();
        final List<String> quickRowOrder = db.getAppCategoryOrder(QUICK_ROW_CAT);

        MainHelper.checkDefaultApps(this, shortcuts, quickRowOrder, mQuickRow);

        for (List<AppShortcut> catlist: shortcuts.values()) {
            for (AppShortcut app: catlist) {

                if (quickRowOrder.contains(app.getActivityName())) {
                    AppShortcut qapp = new AppShortcut(app);
                    qapp.loadAppIconAsync(mPackageMan);
                    quickRowApps.add(qapp);
                }
            }
        }

        mQuickRow.removeAllViews();
        for (String actvname: quickRowOrder) {
            for (AppShortcut app : quickRowApps) {
                if (app.getActivityName().equals(actvname)) {
                    ViewGroup item = getShortcutView(app, true);
                    mQuickRow.addView(item);
                }
            }
        }
        getDB().setAppCategoryOrder(mRevCategoryMap.get(mQuickRow), mQuickRow);

    }


    public ViewGroup getShortcutView(final AppShortcut app) {
        return getShortcutView(app, false);
    }

    public ViewGroup getShortcutView(final AppShortcut app, boolean smallIcon) {

        ViewGroup item = (ViewGroup) LayoutInflater.from(this).inflate(smallIcon?R.layout.shortcut_small_icon:R.layout.shortcut_icon, (ViewGroup) null);

        item.setTag(app);
        item.setClickable(true);
        item.setOnLongClickListener(this);
        item.setOnDragListener(this);

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
        return item;
    }

    private View getWidgetView(View item, final AppShortcut app) {
        item.setTag(app);
        item.setClickable(true);
        item.setOnLongClickListener(this);
        item.setOnDragListener(this);
        return item;
    }



    private ViewGroup getSearchView() {
        ViewGroup searchView = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.search_layout, (ViewGroup) null);

        final AutoCompleteTextView searchbox = (AutoCompleteTextView)searchView.findViewById(R.id.search_box);
        AppCursorAdapter searchAdapter = new AppCursorAdapter(this, searchbox, R.layout.search_item, getDB().getAppCursor("XXXXXX"), 0);
        searchbox.setAdapter(searchAdapter);
        searchbox.setOnItemClickListener(searchAdapter);

        return searchView;
    }

    enum CategoryTabStyle {Default, Normal, Selected, DragHover, Tiny}

    private CategoryTabStyle getDefaultCategoryStyle(String category) {
        CategoryTabStyle catstyle = CategoryTabStyle.Normal;

        if (category.equals(mCategory)) {
            catstyle = CategoryTabStyle.Selected;
        } else if (Arrays.asList(Categories.CAT_TINY).contains(category)) {
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


    private int categoryTabWidth = 108;

    private TextView getCategoryTab(final String category, final GridLayout iconSheet) {
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
        styleCategorySpecial(categoryTab,CategoryTabStyle.Default, category);
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
                        if (catstyle==CategoryTabStyle.Tiny || (!isAppShortcut || !isSearch)) {
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
                            ViewGroup container1 = (ViewGroup)view.getParent();
                            ViewGroup container2 = (ViewGroup)view2.getParent();


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
        if (view2.getTag()==null || !(view2.getTag() instanceof AppShortcut)) {
            return false;
        }
        boolean islayout = view instanceof GridLayout;
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                // do nothing
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
                    if (mQuickRow == mDragDropSource) {
                        mDragDropSource.removeView(view2);
                        getDB().setAppCategoryOrder(mRevCategoryMap.get(mDragDropSource), mDragDropSource);
                    } else if (mDragDropSource == mCategoriesLayout) {
                        //delete category tab

                    } else {
                        //uninstall app
                        mBeingUninstalled = view2;
                        launchUninstallIntent(mBeingDragged.getPackageName());
                    }
                    return true;
                } else  if (view instanceof GridLayout) {
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

                    view2 = getShortcutView(new AppShortcut((AppShortcut)view2.getTag()), true);

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
        mBeingDragged = (AppShortcut)view.getTag();
        mDragDropSource = (ViewGroup)view.getParent();
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

        if (mDragDropSource == mQuickRow) {
            mRemoveAppText.setText(R.string.remove_shortcut);
        } else {
            mRemoveAppText.setText(R.string.uninstall_app);
        }
    }

    private void hideRemoveDropzone() {
        mRemoveDropzone.setVisibility(View.GONE);
    }

    private void launchUninstallIntent(String packageName) {
        Uri packageUri = Uri.parse("package:"+packageName);
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
                    break;
                case RESULT_CANCELED:
                    Toast.makeText(this, R.string.uninstall_canceled, Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(this, R.string.could_not_uninstall, Toast.LENGTH_LONG).show();

            }
        } else {
            AppWidgetHostView wid = mWidgetHost.onActivityResult(requestCode, resultCode, data);
            if (wid==null) {
                super.onActivityResult(requestCode, resultCode, data);
            } else {
                AppWidgetProviderInfo pinfo = wid.getAppWidgetInfo();
                String actvname = pinfo.provider.getClassName();
                String pkgname = pinfo.provider.getPackageName();
                String label;
                if (Build.VERSION.SDK_INT>=21) {
                    label = pinfo.loadLabel(mPackageMan);
                } else {
                    label = pinfo.label;
                }
                AppShortcut app = new AppShortcut(actvname, pkgname, label, mCategory, true);
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();

                float wf = pinfo.minResizeWidth / getResources().getDimension(R.dimen.shortcut_width);
                if (wf>1.1) {
                    lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, (int)wf);
                }

                mIconSheet.addView(getWidgetView(wid, app), lp);
            }
        }
    }



    interface CategoryChangerListener {
        void onClick(DialogInterface dialog, int which, String category, String newDisplayName, String newDisplayFullName);
    }
    private void promptGetCategoryName(String title, String message, final String category, String defName,
                                       String defFullName, final CategoryChangerListener categoryChangerListener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        ViewGroup view = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.category_name, (ViewGroup) null);

        TextView messageView = (TextView)view.findViewById(R.id.message_txt);
        final EditText shortname = (EditText)view.findViewById(R.id.shortname);
        final EditText fullname = (EditText)view.findViewById(R.id.fullname);

        shortname.setSelectAllOnFocus(true);
        fullname.setSelectAllOnFocus(true);

        messageView.setText(message);
        shortname.setText(defName);
        fullname.setText(defFullName);

        builder.setView(view);

        builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                categoryChangerListener.onClick(dialog, which, category, shortname.getText().toString(), fullname.getText().toString());
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(shortname.getWindowToken(), 0);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(shortname.getWindowToken(), 0);
            }
        });


        builder.show();
    }

    private void promptRenameCategory(final String category) {

        promptGetCategoryName("Rename Category",
                "Rename this category",
                category,
                getDB().getCategoryDisplay(category),
                getDB().getCategoryDisplayFull(category),
                new CategoryChangerListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, String category, String newDisplayName, String newDisplayFullName) {
                        try {
                            renameCategory(category,newDisplayName, newDisplayFullName);
                        } catch (IllegalArgumentException e){

                            Toast.makeText(MainActivity.this, "You must give a name", Toast.LENGTH_SHORT);
                        }
                    }
                });
    }

    private void renameCategory(String category, String newDisplayName, String newDisplayFullName) {
        newDisplayName = newDisplayName.trim();
        newDisplayFullName = newDisplayFullName.trim();

        if (newDisplayFullName.length()==0) {
            newDisplayFullName = newDisplayName;
        }

        if (newDisplayName.length()<1) {
            throw new IllegalArgumentException("Must give a name");
        }

        getDB().updateCategory(category, newDisplayName, newDisplayFullName);

        TextView categoryTab = mCategoryTabs.get(category);
        if (category.equals(mCategory)) {
            categoryTab.setText(newDisplayFullName);
        } else {
            categoryTab.setText(newDisplayName);
        }

    }

    private void promptAddCategory() {

        promptGetCategoryName("Add Category",
                "Add a category",
                "",
                "",
                "",
                new CategoryChangerListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, String category, String newDisplayName, String newDisplayFullName) {
                        try {
                            addCategory(category, newDisplayName, newDisplayFullName);
                        } catch (IllegalArgumentException e){

                            Toast.makeText(MainActivity.this, "You must give a name", Toast.LENGTH_SHORT);
                        }
                    }
                });
    }

    private void addCategory(String category, String newDisplayName, String newDisplayFullName) {
        category = category.trim();
        newDisplayName = newDisplayName.trim();
        newDisplayFullName = newDisplayFullName.trim();

        if (category.length()==0) {
            category = newDisplayName;
        }
        if (newDisplayName.length()==0) {
            category = newDisplayName;
        }

        if (newDisplayFullName.length()==0) {
            newDisplayFullName = newDisplayName;
        }

        if (newDisplayName.length()<1) {
            throw new IllegalArgumentException("Must give a name");
        }

        getDB().addCategory(category, newDisplayName, newDisplayFullName);

        getIconSheet(category);

        switchCategory(category);
    }




    private void initUI() {
        //mCategoriesScroller = (ScrollView) findViewById(R.id.layout_categories_scroller);
        mCategoriesLayout = (LinearLayout)findViewById(R.id.layout_categories);

        mIconSheetTopFrame = (FrameLayout)findViewById(R.id.layout_icons_topframe);
        mIconSheetScroller = (InteractiveScrollView)findViewById(R.id.layout_icons_scroller);

        mIconSheetHolder = (ViewGroup)findViewById(R.id.icon_sheet_holder);

        mIconSheetBottomFrame = (ViewGroup)findViewById(R.id.layout_icons_bottomframe);

        mRemoveDropzone = (FrameLayout)findViewById(R.id.remove_dropzone);
        mRemoveDropzone.setOnDragListener(this);
        mRemoveAppText = (TextView) findViewById(R.id.remove_dz_txt);

        hideRemoveDropzone();


        mQuickRow = (GridLayout) findViewById(R.id.layout_quickrow);

        mQuickRowScroller = (HorizontalScrollView) findViewById(R.id.layout_quickrow_scroll);


        mIconSheets = new TreeMap<>();
        mCategoryTabs = new TreeMap<>();
        mRevCategoryMap = new HashMap<>();
        mRevCategoryMap.put(mQuickRow, QUICK_ROW_CAT);

        mShowButtons = (ImageView)findViewById(R.id.settings_button);

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

        mRenameCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptRenameCategory(mCategory);
            }
        });

        mAddCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptAddCategory();
            }
        });


    }

    private void toggleButtonBar() {
        int vis = mIconSheetBottomFrame.getVisibility();
        if (vis== View.VISIBLE) {
            mIconSheetBottomFrame.setVisibility(View.GONE);
            mShowButtons.setImageResource(android.R.drawable.arrow_up_float);
        } else {
            mIconSheetBottomFrame.setVisibility(View.VISIBLE);
            mShowButtons.setImageResource(android.R.drawable.arrow_down_float);
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
        if (Build.VERSION.SDK_INT>=23) {
            return getColor(res);
        } else {
            return getResources().getColor(res);
        }
    }



}
