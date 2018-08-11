package com.quaap.launchtime.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.quaap.launchtime.GlobState;
import com.quaap.launchtime.MainActivity;
import com.quaap.launchtime.R;
import com.quaap.launchtime.apps.AppLauncher;
import com.quaap.launchtime.components.Categories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ActionMenu {

    private static final String TAG = "ActionMenu";

    private MainActivity mMain;

    private boolean mUseActionMenus = false;
    private boolean mDevModeActivities = true;
    private boolean mUseDropZones = true;
    private boolean mUseExtraActions = false;

    private PopupWindow mAppinfoWindow;
    private ScrollView mShortcutActionsPopup;
    private LinearLayout mShortcutActionsList;
    private Style mStyle;
    private int mAnimationDuration = 100;

    private int mOldNum = 0;
    private int mOldHeight = 0;
    
    private Point mScreenDim;
    
    public ActionMenu(MainActivity main) {
        mMain = main;
        mShortcutActionsPopup = mMain.findViewById(R.id.action_menu);
        mShortcutActionsList = mMain.findViewById(R.id.action_menu_items);
        mStyle = GlobState.getStyle(mMain);

        mScreenDim = mMain.getScreenDimensions();
        readActionMenuConfig();
    }

    public void readActionMenuConfig() {
        mUseDropZones = true;
        mUseExtraActions = false;
        mDevModeActivities = false;

        mUseActionMenus = mMain.mAppPreferences.getBoolean(mMain.getString(R.string.pref_key_show_action_menus), Build.VERSION.SDK_INT >= 25);

        if (mUseActionMenus) {
            mUseExtraActions = mMain.mAppPreferences.getBoolean(mMain.getString(R.string.pref_key_show_action_extra), Build.VERSION.SDK_INT >= 25);
            mDevModeActivities = mMain.mAppPreferences.getBoolean(mMain.getString(R.string.pref_key_show_action_activities), false);
            if (mUseExtraActions) {
                mUseDropZones = mMain.mAppPreferences.getBoolean(mMain.getString(R.string.pref_key_show_dropzones), false);
            }
        }

    }

    public void setAnimationDuration(int animationDuration) {
        mAnimationDuration = animationDuration*2/3;
    }

    public boolean useActionMenus() {
        return mUseActionMenus;
    }

    public boolean useDropZones() {
        return mUseDropZones;
    }

    public boolean useExtraActions() {
        return mUseExtraActions;
    }

    public boolean displayActionShortcuts(final View view, final AppLauncher appitem) {

        //Log.d(TAG, appitem.getPackageName() + " " +  appitem.getBaseComponentName());

        List<ShortcutInfo> shortcutInfos = getOreoShortcutInfos(appitem);

        try {
            initializeActionMenu();

            if (appitem.isWidget()) {
                AppWidgetHostView appwid = mMain.getAppWidgetHostView(appitem);
                if (appwid!=null) {
                    addActionMenuItem("Resize", android.R.drawable.arrow_up_float, new Runnable() {
                        @Override
                        public void run() {
                            mMain.showWidgetResize(appitem);
                            mMain.showButtonBar(false, true);
                        }
                    });
                }
            }

            if (!appitem.isWidget()) {
                addActionMenuItem(appitem.getLabel(), appitem.getIconDrawable(), new Runnable() {
                    @Override
                    public void run() {
                        mMain.launchApp(appitem);
                        mMain.showButtonBar(false, true);
                    }
                });
            }


            addOreoShortcutsToMenu(shortcutInfos);

            addDevModeActivitiesToMenu(appitem);

            addActionMenuItem(mMain.getString(R.string.appinfo_label), android.R.drawable.ic_menu_info_details, new Runnable() {
                @Override
                public void run() {
                    mAppinfoWindow = AppInfo.showAppinfo(mMain, view, appitem);
                    mAppinfoWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                        @Override
                        public void onDismiss() {
                            dismissAppinfo();
                        }
                    });
                }
            });

            addExtraActionsToMenu(view, appitem);

            addCancelToMenu();

            showBuiltActionMenu(view);

            return true;


        } catch (Exception e) {
            Log.e(TAG, "Couldn't create menu", e);
        }


        return false;
    }

    public void addCancelToMenu() {
        addActionMenuItem(mMain.getString(android.R.string.cancel), android.R.drawable.ic_menu_close_clear_cancel, new Runnable() {
            @Override
            public void run() {
                dismissActionPopup();
            }
        });
    }

    public void showCatagoryActionMenu(TextView categoryTab) {
        initializeActionMenu();
        addExtraActionsToMenu(categoryTab);
        addCancelToMenu();
        showBuiltActionMenu(categoryTab);
    }


    public void initializeActionMenu() {
        mScreenDim = mMain.getScreenDimensions();

        mShortcutActionsList.removeAllViews();

        mShortcutActionsPopup.setBackgroundColor(Color.argb(128, 255, 255, 255));

        if (mStyle.isRoundedTabs()) {
            mShortcutActionsPopup.setBackground(mStyle.getBgDrawableFor(mShortcutActionsPopup, Style.CategoryTabStyle.White,true));
        }
    }

    public void showBuiltActionMenu(View view) {
        //mShortcutActionsPopup.setVisibility(View.VISIBLE);

        //FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)mShortcutActionsPopup.getLayoutParams();

        int width = (int)(mMain.getResources().getDimension(R.dimen.action_menu_width) * 1.1);

        int height = (int)(mShortcutActionsList.getChildCount()
                * (mMain.getResources().getDimension(R.dimen.action_icon_width)*1.4 + 28)) + 28;

        int [] viewpos = new int[2];
        view.getLocationOnScreen(viewpos);

        int top = viewpos[1] - height - 20;
        int left = viewpos[0] + view.getWidth()/2 - width/2;

        if (top <= 0) {
            top = viewpos[1] + view.getHeight()*2/3;
        }

        if (top+height>=mScreenDim.y) {
            top = mScreenDim.y - height - 10;
            if (left>mScreenDim.x/2) {
                left = viewpos[0] - width;
            } else if (left<mScreenDim.x/2) {
                left = viewpos[0] + view.getWidth();
            }
        }

        if (height>=mScreenDim.y || top <= 0) {
            top = 10;
        }

        if (left <= 0) {
            left = viewpos[0]+4;
        } else if (left + width >= mScreenDim.x) {
            left = mScreenDim.x - (int)(width*1.2);
        }

        if (mShortcutActionsPopup.getVisibility()!=View.VISIBLE) {
            mShortcutActionsPopup.setX(left);
            mShortcutActionsPopup.setY(viewpos[1]);
            mShortcutActionsPopup.setScaleY(0f);
            mShortcutActionsPopup.setAlpha(0f);


        }
        //mShortcutActionsPopup.setTop(0);
        //mShortcutActionsPopup.setLeft(0);

        mShortcutActionsPopup.setVisibility(View.VISIBLE);
        if (mAnimationDuration>0) {
            mShortcutActionsPopup.animate()
                    .x(left)
                    .y(top)
                    .alpha(1)
                    .scaleY(1)
                    .setDuration(mAnimationDuration)
                    .setListener(null)
                    .start();
        } else {
            mShortcutActionsPopup.setX(left);
            mShortcutActionsPopup.setY(top);
            mShortcutActionsPopup.setAlpha(1f);
            mShortcutActionsPopup.setScaleY(1f);
        }

        mOldNum = mShortcutActionsList.getChildCount();
    }


    public void dismissActionPopup() {
        if (mAnimationDuration>0) {
            mShortcutActionsPopup.animate()
                    //.yBy(-mShortcutActionsPopup.getHeight())
                    .scaleY(0)
                    .alpha(0)
                    .setDuration(mAnimationDuration)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationCancel(Animator animation) {
                            mShortcutActionsPopup.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mShortcutActionsPopup.setVisibility(View.GONE);
                        }
                    });
        } else {
            mShortcutActionsPopup.setVisibility(View.GONE);
        }
//        if (mShortcutActionsPopup!=null) {
//            //mShortcutActionsPopup.setVisibility(View.GONE);
//            mShortcutActionsPopup = null;
//
//        }
    }



    public void addExtraActionsToMenu(final TextView categoryTab) {
        final String category = (String)categoryTab.getTag();

        if (!mMain.getCurrentCategory().equals(category)) {
            addActionMenuItem(categoryTab.getText().toString(), android.R.drawable.ic_menu_compass, new Runnable() {
                @Override
                public void run() {
                    mMain.switchCategory(category);
                }
            });
        }

        if (mUseExtraActions) {
            addActionMenuItem(mMain.getString(R.string.rename_category), android.R.drawable.ic_menu_edit, new Runnable() {
                @Override
                public void run() {
                    mMain.promptRenameCategory(category);
                }
            });
            if (!Categories.isNoDropCategory((String) categoryTab.getTag())) {
                addActionMenuItem(mMain.getString(R.string.sort_category), android.R.drawable.ic_menu_sort_alphabetically, new Runnable() {
                    @Override
                    public void run() {
                        mMain.promptSortCategory(category);
                    }
                });

                addActionMenuItem(mMain.getString(R.string.add_widgets), android.R.drawable.ic_input_add, new Runnable() {
                    @Override
                    public void run() {
                        mMain.setupWidget();
                    }
                });
            }

            if (!Categories.isHiddenCategory((String) categoryTab.getTag())) {
                int action = R.string.hide;
                if (mMain.db().isHiddenCategory(category)) action = R.string.show;
                addActionMenuItem(mMain.getString(action), android.R.drawable.ic_menu_view, new Runnable() {
                    @Override
                    public void run() {
                        mMain.hideCategory(category);

                    }
                });
            }

            if (!Categories.isSpeacialCategory((String) categoryTab.getTag())) {

                addActionMenuItem(mMain.getString(R.string.remove), R.drawable.trash, new Runnable() {
                    @Override
                    public void run() {
                        mMain.promptDeleteCategory(category);
                    }
                });
            }

            addActionMenuItem(mMain.getString(R.string.add_category), android.R.drawable.ic_menu_add, new Runnable() {
                @Override
                public void run() {
                    mMain.promptAddCategory();
                }
            });

//            addActionMenuItem(getString(R.string.go_to_settings), android.R.drawable.ic_menu_preferences, new Runnable() {
//                @Override
//                public void run() {
//                    openSettings(MainActivity.this);
//                }
//            });
        }

    }


    private void addExtraActionsToMenu(final View view, final AppLauncher appitem) {
        if (mUseExtraActions) {

            if (mMain.isOnQuickRow(view)) {
                addActionMenuItem(mMain.getString(R.string.remove), R.drawable.trash, new Runnable() {
                    @Override
                    public void run() {
                        mMain.removeViewFromQuickBar(view);
                    }
                });
            } else if (mMain.getCurrentCategory().equals(Categories.CAT_SEARCH) && !mMain.isOnSearchView(view)) {
                addActionMenuItem(mMain.getString(R.string.remove), R.drawable.trash, new Runnable() {
                    @Override
                    public void run() {
                        mMain.db().deleteAppLaunchedRecord(appitem.getComponentName());
                        mMain.populateRecentApps();
                    }
                });
            } else if (appitem.isNormalApp()) {
                if (!Categories.isNoDropCategory(mMain.getCurrentCategory())) {
                    addActionMenuItem(mMain.getString(R.string.link), R.drawable.linkicon, new Runnable() {
                        @Override
                        public void run() {
                            mMain.makeAppLink(appitem);
                        }
                    });
                }
                addActionMenuItem(mMain.getString(R.string.uninstall_app), R.drawable.trash, new Runnable() {
                    @Override
                    public void run() {
                        mMain.launchUninstallIntent(appitem, view);
                    }
                });
            } else {
                addActionMenuItem(mMain.getString(R.string.remove), R.drawable.trash, new Runnable() {
                    @Override
                    public void run() {
                        if (appitem.isWidget()) {
                            mMain.removeWidget(appitem);
                        } else {
                            mMain.db().deleteApp(appitem.getComponentName());
                        }
                        mMain.repopulateIconSheet(mMain.getCurrentCategory());
                    }
                });
            }

        }
    }

    private void addDevModeActivitiesToMenu(AppLauncher appitem) {
        if (mDevModeActivities) {

            class Record implements Comparable<Record>{
                String label;
                private ComponentName component;
                Drawable icon;

                @Override
                public int compareTo(@NonNull Record other) {
                    return this.label.compareTo(other.label);
                }
            }

            List<Record> activityItems = new ArrayList<>();

            try {
                Intent intent = new Intent(Intent.ACTION_MAIN, null);
                intent.setPackage(appitem.getPackageName());
                //intent.addCategory(Intent.CATEGORY_DEFAULT);
                final List<ResolveInfo> activities = mMain.getPackageManager().queryIntentActivities(intent, PackageManager.GET_META_DATA | PackageManager.GET_RESOLVED_FILTER);

                //ActivityInfo[] list = getPackageManager().getPackageInfo(appitem.getPackageName(), PackageManager.GET_ACTIVITIES).activities;

                List<String> names = new ArrayList<>();
                names.add(appitem.getLabel());

                List<String> bannedActivities = Arrays.asList(
                        "com.android.settings.BandMode",
                        "com.android.settings.sim.SimDialogActivity",
                        "com.android.settings.FallbackHome"
                );
                for (ResolveInfo ri : activities) {

                    try {
                        if (ri == null || ri.activityInfo == null || !ri.activityInfo.exported) {
                            continue;
                        }

                        if (ri.activityInfo.name.equals(appitem.getActivityName())) continue;

                        if (ri.activityInfo.permission != null
                                && ContextCompat.checkSelfPermission(mMain.getApplicationContext(), ri.activityInfo.permission)
                                == PackageManager.PERMISSION_DENIED) {
                            continue;
                        }

                        if (bannedActivities.contains(ri.activityInfo.name)) continue;


                        CharSequence label = ri.activityInfo.loadLabel(mMain.getPackageManager());

                        ComponentName cn = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);

                        Log.d(TAG, label + " " + ri.activityInfo.packageName + " " + ri.activityInfo.name + " " + ri.activityInfo.permission);

                        if (label == null || label.toString().trim().equals("") || names.contains(label.toString().trim())) {
                            label = ri.activityInfo.name
                                    .replaceAll("^.*[.$]|Activity", "")
                                    .replaceAll("(\\P{Lu})(\\p{Lu})", "$1 $2");
                        }

                        names.add(label.toString().trim());

                        label = "{" + label + "}";

//                        IntentFilter fi = ri.filter;
//                        if (fi != null) {
//                            for (Iterator<String> it = fi.actionsIterator(); it != null && it.hasNext(); ) {
//                                Log.d(TAG, "  action: " + it.next());
//                            }
//                            for (Iterator<String> it = fi.categoriesIterator(); it != null && it.hasNext(); ) {
//                                Log.d(TAG, "  cat: " + it.next());
//                            }
//                            for (Iterator<String> it = fi.schemesIterator(); it != null && it.hasNext(); ) {
//                                Log.d(TAG, "  scheme: " + it.next());
//                            }
//
//                        }


                        Record item = new Record();
                        item.label = label.toString();
                        item.component = cn;
                        item.icon = ri.activityInfo.loadIcon(mMain.getPackageManager());
                        activityItems.add(item);

                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage(), e);
                        Toast.makeText(mMain, "Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                Collections.sort(activityItems);

                for (Record item: activityItems) {
                    final Intent launchIntent = new Intent(Intent.ACTION_MAIN);
                    launchIntent.setComponent(item.component);
                    addActionMenuItem(item.label, item.icon, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mMain.startActivity(launchIntent);
                            } catch (Exception e) {
                                Log.e(TAG, e.getMessage(), e);
                                Toast.makeText(mMain, "Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                }

            } catch (Exception e) {
                Log.e(TAG, "Couldn't query activities", e);
            }
        }
    }

    private void addOreoShortcutsToMenu(List<ShortcutInfo> shortcutInfos) {
        if (Build.VERSION.SDK_INT >= 25) {
            if (shortcutInfos != null && shortcutInfos.size()>0) {
                final LauncherApps launcherApps = mMain.getSystemService(LauncherApps.class);
                if (launcherApps == null) return;

                sortShorcutsByRank(shortcutInfos);

                for (final ShortcutInfo shortcutInfo : shortcutInfos) {
                    if (shortcutInfo.isDynamic()) {
                        addShortcutToActionPopup(launcherApps, shortcutInfo);
                    }
                }

                for (final ShortcutInfo shortcutInfo : shortcutInfos) {
                    if (shortcutInfo.isDeclaredInManifest()) {
                        addShortcutToActionPopup(launcherApps, shortcutInfo);
                    }
                }
            }
        }

    }

    private void sortShorcutsByRank(List<ShortcutInfo> shortcutInfos) {
        Collections.sort(shortcutInfos, new Comparator<ShortcutInfo>() {
            @Override
            public int compare(ShortcutInfo a, ShortcutInfo b) {
                if (Build.VERSION.SDK_INT >= 25) {
                    return Integer.compare(a.getRank(), b.getRank());
                }
                return 0;
            }
        });
    }

    @Nullable
    private List<ShortcutInfo> getOreoShortcutInfos(AppLauncher appitem) {
        List<ShortcutInfo> shortcutInfos = null;
        if (Build.VERSION.SDK_INT>=25) {
            final LauncherApps launcherApps = (LauncherApps) mMain.getSystemService(Context.LAUNCHER_APPS_SERVICE);

            if (launcherApps!=null && launcherApps.hasShortcutHostPermission()) {
                try {

                    LauncherApps.ShortcutQuery q = new LauncherApps.ShortcutQuery();
                    q.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST);

                    if (appitem.isShortcut()) {
                        Intent launchIntent = Intent.parseUri(appitem.getLinkUri(), 0);
                        q.setPackage(launchIntent.getPackage());
                        q.setActivity(launchIntent.getComponent());
                    } else if (appitem.isOreoShortcut()) {
                        LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
                        query.setPackage(appitem.getPackageName());
                        query.setShortcutIds(Collections.singletonList(appitem.getLinkUri()));
                        query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);
                        List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(query,android.os.Process.myUserHandle());
                        if (shortcuts!=null && shortcuts.size()>0) {
                            q.setPackage(appitem.getPackageName());
                            q.setActivity(shortcuts.get(0).getActivity());
                        }
                    } else {
                        q.setPackage(appitem.getPackageName());
                        q.setActivity(appitem.getBaseComponentName());
                    }

                    shortcutInfos = launcherApps.getShortcuts(q, android.os.Process.myUserHandle());

                    //Log.d(TAG, "Queried shortcuts");

                } catch (Exception e) {
                    Log.e(TAG, "Couldn't query shortcuts", e);
                }
            }
        }
        return shortcutInfos;
    }

    private void addActionMenuItem(String label, int iconResource, final Runnable action) {
        addActionMenuItem(label, mMain.getResources().getDrawable(iconResource), action);
    }

    private void addActionMenuItem(String label, Drawable icon, final Runnable action) {

        if (label==null) return;

        label = label.replaceAll("\\s+|\\r|\\n", " ");

        final ViewGroup item = (ViewGroup) LayoutInflater.from(mMain).inflate(R.layout.action_menu_entry, null);

        item.setBackgroundColor(mStyle.getCattabSelectedBackground());

        if (mStyle.isRoundedTabs()) {
            item.setBackground(mStyle.getBgDrawableFor(item, Style.CategoryTabStyle.Normal,true));
        }

        TextView itemText = item.findViewById(R.id.action_menu_text);

        itemText.setTextColor(mStyle.getCattabTextColor());
        itemText.setTextSize(TypedValue.COMPLEX_UNIT_SP, mStyle.getCategoryTabFontSize()-1);

        if (label!=null && label.length()>30) label = label.substring(0,28) + "...";
        itemText.setText(label);

        ImageView itemIcon = item.findViewById(R.id.action_menu_icon);
        itemIcon.setImageDrawable(icon);
        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View item) {
                action.run();
                dismissActionPopup();
                mMain.clearDragPotential(true);
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int)mMain.getResources().getDimension(R.dimen.action_menu_width), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(12,12,12,12);

        item.setLayoutParams(lp);
        mShortcutActionsList.addView(item);
        if (mAnimationDuration>0) {

            if (mShortcutActionsList.getChildCount() > mOldNum) {
                item.setVisibility(View.GONE);
            }
            item.setScaleY(.1f);
            item.animate()
                    .scaleY(1f)
                    .setDuration(mAnimationDuration)
                    .setStartDelay(mShortcutActionsList.getChildCount() * 10 + 10)
                    .withStartAction(new Runnable() {
                        @Override
                        public void run() {
                            item.setVisibility(View.VISIBLE);
                        }
                    });
        }
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

                    Drawable icon = launcherApps.getShortcutIconDrawable(shortcutInfo, DisplayMetrics.DENSITY_DEFAULT);
                    addActionMenuItem(label.trim(), icon, new Runnable() {
                        @Override
                        public void run() {
                            if (Build.VERSION.SDK_INT >= 25) {
                                try {
                                    launcherApps.startShortcut(shortcutInfo, null, null);
                                } catch (Exception e) {
                                    Log.e(TAG, "Couldn't Launch shortcut", e);
                                }
                            }
                            dismissActionPopup();
                        }
                    });

                }
            }
        }
    }


    public void dismissAppinfo() {
        if (mAppinfoWindow!=null) {
            if (mAppinfoWindow.isShowing()) mAppinfoWindow.dismiss();
            mAppinfoWindow = null;
        }
    }

}
