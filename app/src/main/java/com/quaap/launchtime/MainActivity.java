package com.quaap.launchtime;

import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.quaap.launchtime.components.AppShortcut;
import com.quaap.launchtime.db.DB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements
        View.OnTouchListener, View.OnLongClickListener, View.OnDragListener {

    public static final int BACKGROUND_COLOR = Color.TRANSPARENT;
    public static final String QUICK_ROW = "QuickRow";

    private ScrollView mIconSheetScroller;

    private Map<String,GridLayout> mIconSheets;
    private Map<String,TextView> mCategoryTabs;
    private Map<View, String> mRevCategoryMap;


    private volatile String mCategory;

    private GridLayout mQuickRow;

    private LinearLayout mCategoriesLayout;

    private PackageManager mPackageMan;

    private AppShortcut mBeingDragged;

    //private volatile String mDragHoverCategory;
    private volatile String mDragDropCategory;
    private volatile ViewGroup mDragDropSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mPackageMan = getApplicationContext().getPackageManager();

        mCategoriesLayout = (LinearLayout)findViewById(R.id.layout_categories);
        mIconSheetScroller = (ScrollView)findViewById(R.id.layout_icons_scroller);

        mQuickRow = (GridLayout) findViewById(R.id.layout_quickrow);
        mQuickRow.setOnDragListener(this);

//        mIconSheet = (GridLayout) findViewById(R.id.layout_icons);
//        mIconSheet.setColumnCount(3);

        mIconSheets = new TreeMap<>();
        mCategoryTabs = new TreeMap<>();
        mRevCategoryMap = new HashMap<>();
        mRevCategoryMap.put(mQuickRow, QUICK_ROW);

        loadApplications();

        switchCategory(mCategory);

    }


    private void switchCategory(String category) {
        if (category==null) return;
        mCategory = category;
        for(TextView cat: mCategoryTabs.values()) {
            cat.setBackgroundColor(Color.TRANSPARENT);
        }

        mIconSheetScroller.removeAllViews();
        mIconSheetScroller.addView(mIconSheets.get(category));
        mCategoryTabs.get(category).setBackgroundColor(Color.argb(127,127,127,250));
    }

    protected DB getDB() {
        return GlobState.getGlobState(this).getDB();
    }

    private void launchApp(final AppShortcut app) {

        Intent intent = mPackageMan.getLaunchIntentForPackage(app.getPackageName());
        MainActivity.this.startActivity(intent);
    }




    private void loadApplications() {

        final Map<String, List<AppShortcut>> shortcuts = new LinkedHashMap<>();


        // Set MAIN and LAUNCHER filters, so we only get activities with that defined on their manifest
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        // Get all activities that have those filters
        List<ResolveInfo> activities = mPackageMan.queryIntentActivities(intent, 0);

        final DB db = getDB();

        List<String> dbpkgnames = db.getAppPkgNames();


        Set<String> pmpkgnames = new HashSet<>();

        List<AppShortcut> newapps = new ArrayList<>();
        List<AppShortcut> quickRowApps = new ArrayList<>();
        final List<String> quickRowOrder = db.getCategoryOrder(QUICK_ROW);

        for (int i = 0; i < activities.size(); i++) {

            AppShortcut app;

            ResolveInfo ri = activities.get(i);
            String pkgname = ri.activityInfo.packageName;
            if (!pmpkgnames.contains(pkgname)) {
                pmpkgnames.add(pkgname);

                if (dbpkgnames.contains(pkgname)) {
                    app = db.getApp(pkgname);
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

                if (quickRowOrder.contains(app.getPackageName())) {
                    AppShortcut qapp = new AppShortcut(app);
                    qapp.loadAppIconAsync(mPackageMan);
                    quickRowApps.add(qapp);
                }
            }
        }

        db.addApps(newapps);

        //remove shortcuts if they are not in the system
        for (Iterator<String> it=dbpkgnames.iterator(); it.hasNext();) {
            String dbpkg = it.next();
            if (!pmpkgnames.contains(dbpkg)) {
                it.remove();
                //db.remove(dbpkg);
            }
        }


        mQuickRow.removeAllViews();
        for (String pkgname: quickRowOrder) {
            for (AppShortcut app : quickRowApps) {
                if (app.getPackageName().equals(pkgname)) {
                    ViewGroup item = getShortcutView(app);
                    mQuickRow.addView(item);
                }
            }
        }


        for (final String category: db.getCategories()) {

            if (mCategory==null) mCategory=category;

            final GridLayout iconSheet = new GridLayout(MainActivity.this);
            mIconSheets.put(category, iconSheet);
            mRevCategoryMap.put(iconSheet, category);

            final List<String> apporder = db.getCategoryOrder(category);

            iconSheet.setColumnCount(3);
            iconSheet.setOnDragListener(MainActivity.this);


            final TextView categoryTab = getCategoryTab(category, iconSheet);

            mCategoryTabs.put(category, categoryTab);
            mRevCategoryMap.put(categoryTab, category);
            mCategoriesLayout.addView(categoryTab);

            final List<AppShortcut> catapps = shortcuts.get(category);
            Collections.sort(catapps);


//            GlobState.getGlobState(this).runAsync(new Runnable() {
//                @Override
//                public void run() {
            Log.d("category--------", category);

                    for (String pkgname: apporder) {
                        Log.d("apporder", pkgname);
                        for (AppShortcut app : catapps) {
                            if (app.getPackageName().equals(pkgname)) {
                                ViewGroup item = getShortcutView(app);
                                iconSheet.addView(item);
                            }
                        }
                    }

                    boolean reorder = false;
                    for (AppShortcut app : catapps) {
                        if (!apporder.contains(app.getPackageName())) {
                            Log.d("no apporder", app.getPackageName());

                            ViewGroup item = getShortcutView(app);

                            iconSheet.addView(item);
                            reorder = true;
                        }
                    }
                    if (reorder) {
                        db.setCategoryOrder(category, iconSheet);
                    }

//                }
//            });

        }

    }

    @NonNull
    private ViewGroup getShortcutView(final AppShortcut app) {
        ViewGroup item = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.shortcut_icon, (ViewGroup) null);

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

        TextView iconLabel = (TextView) item.findViewById(R.id.shortcut_text);
        iconLabel.setText(app.getLabel());
        return item;
    }

    @NonNull
    private TextView getCategoryTab(final String category, final GridLayout iconSheet) {
        final TextView categoryTab = new TextView(this);
        categoryTab.setText(getDB().getCategoryDisplay(category));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.weight = 1;
        lp.gravity = Gravity.CENTER;
        lp.setMargins(2,4,2,4);
        categoryTab.setLayoutParams(lp);
        categoryTab.setGravity(Gravity.CENTER);
        categoryTab.setBackgroundColor(Color.rgb(127, 127, 255));

        categoryTab.setTextSize(16);
        categoryTab.setPadding(6,24,2,24);

        categoryTab.setClickable(true);
        categoryTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //System.out.println("CategoryTab " + category);
                switchCategory(category);

            }
        });

        categoryTab.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, final DragEvent event) {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_EXITED:
                    case DragEvent.ACTION_DRAG_ENDED:
                          mBeingDragged = null;
                          mDragDropCategory = null;
//                         mDragHoverCategory = null;
                           break;
//                    case DragEvent.ACTION_DRAG_ENTERED:
//                        mDragHoverCategory = category;
//                        categoryTab.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (mDragHoverCategory==category) {
//                                    switchCategory(mDragHoverCategory);
//                                    mDragHoverCategory=null;
//                                }
//                            }
//                        }, 500);
//                        break;

                    case DragEvent.ACTION_DROP:
                        //switchCategory(category);
                        mDragDropCategory = category;

                        getDB().updateAppCategory(mBeingDragged.getPackageName(), category);
                        MainActivity.this.onDrag(iconSheet, event);
                        break;
                }
                return true;
            }
        });
        return categoryTab;
    }


    @Override
    public boolean onDrag(View view, DragEvent event) {
        boolean islayout = view instanceof GridLayout;
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                // do nothing
                break;
            case DragEvent.ACTION_DRAG_ENTERED:
                if (!islayout) {
                    view.setBackgroundColor(Color.BLUE);
                }
                break;
            case DragEvent.ACTION_DRAG_EXITED:

                if (!islayout) view.setBackgroundColor(BACKGROUND_COLOR);
                break;
            case DragEvent.ACTION_DROP:
                if (!islayout) view.setBackgroundColor(BACKGROUND_COLOR);
                // Dropped, reassign View to ViewGroup
                View view2 = (View) event.getLocalState();
                if (view2 == view) {
                    // Log.d("sort", "self drop");
                    break;
                }

               // ViewGroup owner = (ViewGroup) view2.getParent();

                GridLayout target;
                if (view instanceof GridLayout) {
                    target = (GridLayout) view;

                } else {
                    target = (GridLayout) view.getParent();

                }

                int index = -1;
                for (int i = 0; i < target.getChildCount(); i++) {
                    if (target.getChildAt(i) == view) {
                        index = i;
                    }
                }

                if (mQuickRow == mDragDropSource || mQuickRow != target) {
                    mDragDropSource.removeView(view2);
                }

                if (target == mQuickRow) {
                    mDragDropCategory = QUICK_ROW;
                    if (mQuickRow != mDragDropSource) {
                        for (int i = 0; i < mQuickRow.getChildCount(); i++) {
                            AppShortcut dragging = (AppShortcut) view2.getTag();
                            AppShortcut inbar = (AppShortcut) mQuickRow.getChildAt(i).getTag();
                            if (dragging.getPackageName().equals(inbar.getPackageName())) {
                                return true;
                            }
                        }
                    }

                    view2 = getShortcutView(new AppShortcut((AppShortcut)view2.getTag()));
                    LinearLayout sc = (LinearLayout)view2;

                    sc.setScaleX(.8f);
                    sc.setScaleY(.8f);
                }



                if (index == -1) {
                    target.addView(view2);
                } else {
                    target.addView(view2, index);
                }

                getDB().setCategoryOrder(mRevCategoryMap.get(target), target);
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                if (!islayout) view.setBackgroundColor(BACKGROUND_COLOR);
                mBeingDragged = null;
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
        return true;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }
}
