package com.quaap.launchtime;

import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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



    public static final String QUICK_ROW = "QuickRow";

    private ScrollView mIconSheetScroller;

    private Map<String,GridLayout> mIconSheets;
    private GridLayout mIconSheet;
    private Map<String,TextView> mCategoryTabs;
    private Map<View, String> mRevCategoryMap;


    private volatile String mCategory;
    private GridLayout mQuickRow;
    private HorizontalScrollView mQuickRowScroller;

    private LinearLayout mCategoriesLayout;
    private TextView mRemoveAppText;

    private FrameLayout mRemoveDropzone;


    private PackageManager mPackageMan;

    private AppShortcut mBeingDragged;


    private volatile ViewGroup mDragDropSource;

    private int cattabBackground;
    private int cattabSelectedBackground;
    private int dragoverBackground;
    private int backgroundDefault = Color.TRANSPARENT;

    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mPackageMan = getApplicationContext().getPackageManager();

        setColors();

        mCategoriesLayout = (LinearLayout)findViewById(R.id.layout_categories);
        mIconSheetScroller = (ScrollView)findViewById(R.id.layout_icons_scroller);
        mIconSheetScroller.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                return MainActivity.this.onDrag(mIconSheet, dragEvent);
            }
        });

        mRemoveDropzone = (FrameLayout)findViewById(R.id.remove_dropzone);
        mRemoveDropzone.setOnDragListener(this);
        mRemoveAppText = (TextView) findViewById(R.id.remove_dz_txt);

        hideRemoveDropzone();


        mQuickRow = (GridLayout) findViewById(R.id.layout_quickrow);
        mQuickRow.setOnDragListener(this);

        mQuickRowScroller = (HorizontalScrollView) findViewById(R.id.layout_quickrow_scroll);
        mQuickRowScroller.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                return MainActivity.this.onDrag(mQuickRow, dragEvent);
            }
        });


        mIconSheets = new TreeMap<>();
        mCategoryTabs = new TreeMap<>();
        mRevCategoryMap = new HashMap<>();
        mRevCategoryMap.put(mQuickRow, QUICK_ROW);

        loadApplications();

        mPrefs = getSharedPreferences("default",MODE_PRIVATE);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        checkConfig();
    }


    @Override
    protected void onPause() {
        super.onPause();
        mPrefs.edit().putString("category", mCategory).apply();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mCategory = mPrefs.getString("category", mCategory);
        switchCategory(mCategory);
    }


    private void checkConfig() {
        if (isLandscape()) {
            changeColumnCount(mIconSheet, 6);
        } else {
            changeColumnCount(mIconSheet, 3);
        }
    }

    private void setColors() {
        cattabBackground = getResColor(R.color.cattab_background);
        cattabSelectedBackground = getResColor(R.color.cattabselected_background);
        dragoverBackground = getResColor(R.color.dragover_background);
    }

    private int getResColor(int res) {
        if (Build.VERSION.SDK_INT>=23) {
            return getColor(res);
        } else {
            return getResources().getColor(res);
        }
    }


    private void switchCategory(String category) {
        if (category==null) return;
        mCategory = category;
        for(TextView cat: mCategoryTabs.values()) {
            cat.setBackgroundColor(cattabBackground);
        }

        mIconSheetScroller.removeAllViews();
        mIconSheet = mIconSheets.get(category);

        checkConfig();

        mIconSheetScroller.addView(mIconSheet);
        mCategoryTabs.get(category).setBackgroundColor(cattabSelectedBackground);
    }

    private void changeColumnCount(GridLayout gridLayout, int columnCount) {
        if (gridLayout.getColumnCount() != columnCount) {
            final int viewsCount = gridLayout.getChildCount();
            for (int i = 0; i < viewsCount; i++) {
                View view = gridLayout.getChildAt(i);
                //new GridLayout.LayoutParams created with Spec.UNSPECIFIED
                //which are package visible
                view.setLayoutParams(new GridLayout.LayoutParams());
            }
            gridLayout.setColumnCount(columnCount);
        }
    }

    public boolean isLandscape() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE;
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
                    ViewGroup item = getShortcutView(app, true);
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


            GlobState.getGlobState(this).runAsync(new Runnable() {
                @Override
                public void run() {
         //   Log.d("category--------", category);

                    for (String pkgname: apporder) {
                       // Log.d("apporder", pkgname);
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
                          //  Log.d("no apporder", app.getPackageName());

                            ViewGroup item = getShortcutView(app);

                            iconSheet.addView(item);
                            reorder = true;
                        }
                    }
                    if (reorder) {
                        db.setCategoryOrder(category, iconSheet);
                    }

                }
            });

        }

    }
    private ViewGroup getShortcutView(final AppShortcut app) {
        return getShortcutView(app, false);
    }

    private ViewGroup getShortcutView(final AppShortcut app, boolean smallIcon) {


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


    private TextView getCategoryTab(final String category, final GridLayout iconSheet) {
        final TextView categoryTab = new TextView(this);
        categoryTab.setText(getDB().getCategoryDisplay(category));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.weight = 1;
        lp.gravity = Gravity.CENTER;
        lp.setMargins(2,6,2,4);
        categoryTab.setLayoutParams(lp);
        categoryTab.setGravity(Gravity.CENTER);
        categoryTab.setBackgroundColor(cattabBackground);

        categoryTab.setTextSize(16);
        categoryTab.setPadding(6,24,2,24);

        categoryTab.setClickable(true);
        categoryTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                          hideRemoveDropzone();
                           break;

                    case DragEvent.ACTION_DROP:
                        getDB().updateAppCategory(mBeingDragged.getPackageName(), category);
                        MainActivity.this.onDrag(iconSheet, event);
                        break;
                }
                return true;
            }
        });
        return categoryTab;
    }

    private View mBeingUninstalled;

    @Override
    public boolean onDrag(View view, DragEvent event) {
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
                View view2 = (View) event.getLocalState();
                if (view2 == view) {
                    // Log.d("sort", "self drop");
                    break;
                }

                ViewGroup target;
                if (view == mRemoveDropzone) {
                    if (mQuickRow == mDragDropSource) {
                        mDragDropSource.removeView(view2);
                        getDB().setCategoryOrder(mRevCategoryMap.get(mDragDropSource), mDragDropSource);
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


                if (mQuickRow == mDragDropSource || mQuickRow != target) {
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
                            if (dragging.getPackageName().equals(inbar.getPackageName())) {
                                return true;
                            }
                        }
                    }

                    view2 = getShortcutView(new AppShortcut((AppShortcut)view2.getTag()));

                }

                if (index == -1) {
                    target.addView(view2);
                } else {
                    target.addView(view2, index);
                }

                getDB().setCategoryOrder(mRevCategoryMap.get(target), target);
                getDB().setCategoryOrder(mRevCategoryMap.get(mDragDropSource), mDragDropSource);
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

    private static final int UNINSTALL_RESULT = 3454;

    private void launchUninstallIntent(String package_name) {
        Uri packageUri = Uri.parse("package:"+package_name);
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
                    getDB().setCategoryOrder(mRevCategoryMap.get(mDragDropSource), mDragDropSource);
                    Toast.makeText(this, R.string.app_was_uninstalled, Toast.LENGTH_SHORT).show();
                    break;
                case RESULT_CANCELED:
                    Toast.makeText(this, R.string.uninstall_canceled, Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(this, R.string.could_not_uninstall, Toast.LENGTH_LONG).show();

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }
}
