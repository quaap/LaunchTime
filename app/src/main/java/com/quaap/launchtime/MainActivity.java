package com.quaap.launchtime;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
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
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.quaap.launchtime.components.AppShortcut;
import com.quaap.launchtime.components.Categories;
import com.quaap.launchtime.components.Utils;
import com.quaap.launchtime.components.Widget;
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
    private static final int UNINSTALL_RESULT = 3454;
    private ScrollView mIconSheetScroller;
    private Map<String,GridLayout> mIconSheets;
    private GridLayout mIconSheet;
    private Map<String,TextView> mCategoryTabs;
    private Map<View, String> mRevCategoryMap;
    private volatile String mCategory;
    private GridLayout mQuickRow;
    private HorizontalScrollView mQuickRowScroller;
    //private ScrollView mCategoriesScroller;
    private LinearLayout mCategoriesLayout;
    private TextView mRemoveAppText;
    private FrameLayout mRemoveDropzone;
    private PackageManager mPackageMan;
    private AppShortcut mBeingDragged;
    private volatile ViewGroup mDragDropSource;
    private SharedPreferences mPrefs;
    private View mBeingUninstalled;
    private Widget mWidgetHost;


    private int cattabBackground;
    private int cattabSelectedBackground;
    private int dragoverBackground;
    private int backgroundDefault = Color.TRANSPARENT;
    private float categoryTabFontSize = 16;
    private float categoryTabFontSizeHidden = 12;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
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
        mIconSheetScroller.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                return MainActivity.this.onDrag(mIconSheet, dragEvent);
            }
        });



        loadApplications();

        Button settButt = (Button)findViewById(R.id.settings_button);
        settButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setupWidget();
            }
        });

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
        super.onPause();
        mPrefs.edit().putString("category", mCategory).apply();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mCategory = mPrefs.getString("category", mCategory);
        switchCategory(mCategory);
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

    private void checkConfig() {
        if (Utils.isLandscape(this)) {
            Utils.changeColumnCount(mIconSheet, mColumns*2, (int)getResources().getDimension(R.dimen.shortcut_width));
        } else {
            Utils.changeColumnCount(mIconSheet, mColumns, (int)getResources().getDimension(R.dimen.shortcut_width));
        }
    }


    protected DB getDB() {
        return GlobState.getGlobState(this).getDB();
    }

    private void launchApp(final AppShortcut app) {

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(app.getPackageName(), app.getActivityName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    private void loadApplications() {

        final DB db = getDB();


        final Map<String, List<AppShortcut>> shortcuts = processActivities(db);

        processQuickApps(db, shortcuts);

        for (final String category: db.getCategories()) {

            if (mCategory==null) mCategory=category;


            List<AppShortcut> capps = shortcuts.get(category);
            if (capps==null) capps = new ArrayList<>();

            final List<AppShortcut> catapps = capps;
            Collections.sort(catapps);

            final GridLayout iconSheet = getIconSheet(category);
            processIconSheet(db, category, iconSheet, catapps);

        }

    }

    private int mColumns = 4;
    @NonNull
    private GridLayout getIconSheet(String category) {
        final GridLayout iconSheet = new GridLayout(MainActivity.this);
        mIconSheets.put(category, iconSheet);
        mRevCategoryMap.put(iconSheet, category);

        iconSheet.setColumnCount(mColumns);
        iconSheet.setOnDragListener(MainActivity.this);


        final TextView categoryTab = getCategoryTab(category, iconSheet);

        mCategoryTabs.put(category, categoryTab);
        mRevCategoryMap.put(categoryTab, category);
        mCategoriesLayout.addView(categoryTab);
        return iconSheet;
    }

    private void processIconSheet(final DB db, final String category, final GridLayout iconSheet, final List<AppShortcut> catapps) {
        final List<String> apporder = db.getCategoryOrder(category);

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
                    db.setCategoryOrder(category, iconSheet);
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
        final List<String> quickRowOrder = db.getCategoryOrder(QUICK_ROW);

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

    private View getWidgetView(View item, final AppShortcut app) {
        item.setTag(app);
        item.setClickable(true);
        item.setOnLongClickListener(this);
        item.setOnDragListener(this);
        return item;
    }

    private TextView getCategoryTab(final String category, final GridLayout iconSheet) {
        final TextView categoryTab = new TextView(this);
        categoryTab.setText(getDB().getCategoryDisplay(category));
        final boolean ishidden = category.equals(Categories.CAT_HIDDEN);

        if (!ishidden) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.weight = 1;
            lp.gravity = Gravity.CENTER;
            lp.setMargins(2, 6, 2, 4);
            categoryTab.setLayoutParams(lp);

            categoryTab.setBackgroundColor(cattabBackground);

            categoryTab.setTextSize(categoryTabFontSize);
            categoryTab.setPadding(6, 24, 2, 24);
        } else {
            categoryTab.setTextSize(categoryTabFontSizeHidden);
        }

        categoryTab.setGravity(Gravity.CENTER);
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

                    case DragEvent.ACTION_DRAG_ENTERED:
                        if (ishidden) {
                            categoryTab.setTextSize(categoryTabFontSize);
                            categoryTab.setPadding(6, 24, 2, 24);
                        }
                        break;


                    case DragEvent.ACTION_DRAG_ENDED:
                        mBeingDragged = null;
                        hideRemoveDropzone();
                    case DragEvent.ACTION_DRAG_EXITED:
                        if (ishidden) {
                            categoryTab.setTextSize(categoryTabFontSizeHidden);
                            categoryTab.setPadding(1,1,1,1);
                        }
                        break;

                    case DragEvent.ACTION_DROP:
                        getDB().updateAppCategory(mBeingDragged.getActivityName(), category);
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

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }


    private void initUI() {
        //mCategoriesScroller = (ScrollView) findViewById(R.id.layout_categories_scroller);
        mCategoriesLayout = (LinearLayout)findViewById(R.id.layout_categories);


        mIconSheetScroller = (ScrollView)findViewById(R.id.layout_icons_scroller);

        mRemoveDropzone = (FrameLayout)findViewById(R.id.remove_dropzone);
        mRemoveDropzone.setOnDragListener(this);
        mRemoveAppText = (TextView) findViewById(R.id.remove_dz_txt);

        hideRemoveDropzone();


        mQuickRow = (GridLayout) findViewById(R.id.layout_quickrow);

        mQuickRowScroller = (HorizontalScrollView) findViewById(R.id.layout_quickrow_scroll);


        mIconSheets = new TreeMap<>();
        mCategoryTabs = new TreeMap<>();
        mRevCategoryMap = new HashMap<>();
        mRevCategoryMap.put(mQuickRow, QUICK_ROW);
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

}
