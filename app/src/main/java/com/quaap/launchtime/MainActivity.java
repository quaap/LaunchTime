package com.quaap.launchtime;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private ScrollView mIconSheetScroller;

    private Map<String,GridLayout> mIconSheets;
    private Map<String,TextView> mCategoryTabs;

    private String mCategory;

    private GridLayout mQuickRow;

    private LinearLayout mCategoriesLayout;

    private PackageManager mPackageMan;

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

        loadApplications();

        switchCategory(mCategory);
    }


    private void switchCategory(String category) {
        mCategory = category;
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

    private void launchApp(final AppShortcut app) {

        Intent intent = mPackageMan.getLaunchIntentForPackage(app.getPackageName());
        MainActivity.this.startActivity(intent);
    }


    private volatile String mDragHoverCategory;

    private void loadApplications() {

        Map<String, List<AppShortcut>> shortcuts = new LinkedHashMap<>();


        // Set MAIN and LAUNCHER filters, so we only get activities with that defined on their manifest
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        // Get all activities that have those filters
        List<ResolveInfo> activities = mPackageMan.queryIntentActivities(intent, 0);

        DB db = getDB();

        List<String> dbpkgnames = db.getAppPkgNames();


        Set<String> pmpkgnames = new HashSet<>();

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
                    db.addApp(app);
                }
                List<AppShortcut> catapps = shortcuts.get(app.getCategory());
                if (catapps == null) {
                    catapps = new ArrayList<>();
                    shortcuts.put(app.getCategory(), catapps);
                }
                catapps.add(app);
            }

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

            final GridLayout iconSheet = new GridLayout(this);
            mIconSheets.put(category, iconSheet);

            iconSheet.setColumnCount(3);
            iconSheet.setOnDragListener(this);

            final TextView categoryTab = getCategoryTab(category, iconSheet);

            mCategoryTabs.put(category, categoryTab);
            mCategoriesLayout.addView(categoryTab);

            List<AppShortcut> catapps = shortcuts.get(category);
            Collections.sort(catapps);

            for (final AppShortcut app : catapps) {

                ViewGroup item = getShortcutView(app);

                iconSheet.addView(item);
            }
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
                System.out.println("CategoryTab " + category);
                if (mDragHoverCategory==null) {
                    switchCategory(category);
                }
            }
        });

        categoryTab.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, final DragEvent event) {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_EXITED:
                    case DragEvent.ACTION_DRAG_ENDED:
                        mDragHoverCategory = null;
                        break;
                    case DragEvent.ACTION_DRAG_ENTERED:
                        mDragHoverCategory = category;
                        categoryTab.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (mDragHoverCategory==category) {
                                    switchCategory(mDragHoverCategory);
                                    mDragHoverCategory=null;
                                }
                            }
                        }, 500);
                        break;

                    case DragEvent.ACTION_DROP:
                        //switchCategory(category);
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
                    //view.setBackgroundResource(R.drawable.sort_drop_target);
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

                ViewGroup owner = (ViewGroup) view2.getParent();

                GridLayout container;
                if (view instanceof GridLayout) {
                    container = (GridLayout) view;

                } else {
                    container = (GridLayout) view.getParent();

                }

                int index = -1;
                for (int i = 0; i < container.getChildCount(); i++) {
                    if (container.getChildAt(i) == view) {
                        index = i;
                    }
                }

                if (mQuickRow != container) {
                    owner.removeView(view2);
                } else {
                    view2 = getShortcutView(new AppShortcut((AppShortcut)view2.getTag()));
                }
                if (index == -1) {
                    container.addView(view2);
                } else {
                    container.addView(view2, index);
                }


                break;
            case DragEvent.ACTION_DRAG_ENDED:
                if (!islayout) view.setBackgroundColor(BACKGROUND_COLOR);
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean onLongClick(View view) {
        String label = ((AppShortcut)view.getTag()).getLabel();
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
