
package com.quaap.launchtime;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by tom on 8/30/17.
 */

public class UnreadReceiver extends BroadcastReceiver{

    private static final String DEFAULT_ACTION = "android.intent.action.BADGE_COUNT_UPDATE";
    private static final String DEFAULT_BADGE_COUNT = "badge_count";
    private static final String DEFAULT_BADGE_PACKAGENAME = "badge_count_package_name";
    private static final String DEFAULT_BADGE_ACTIVITY_NAME = "badge_count_class_name";


    private static final String SONY_ACTION = "com.sonyericsson.home.action.UPDATE_BADGE";
    private static final String SONY_BADGE_COUNT = "com.sonyericsson.home.intent.extra.badge.MESSAGE";
    private static final String SONY_BADGE_SHOW = "com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE";
    private static final String SONY_BADGE_PACKAGENAME = "com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME";
    private static final String SONY_BADGE_ACTIVITY_NAME = "om.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME";


    //"com.anddoes.launcher.UPDATE_COUNTER";

    private static final String APEX_ACTION = "com.anddoes.launcher.COUNTER_CHANGED";
    private static final String APEX_BADGE_PACKAGENAME = "package";
    private static final String APEX_BADGE_ACTIVITY_NAME = "class";
    private static final String APEX_BADGE_COUNT = "count";
//

    private String lastCountAction;
    private String lastCountActivity;
    private String lastCountPackage;
    private long lastCountTime;
    private int lastCount = -1;

    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            String action = intent.getAction();

            int badgeCount = 0;
            String badgePackage = null;
            String badgeActivity = null;


            if (action.equals(DEFAULT_ACTION)) {
                badgeCount = intent.getIntExtra(DEFAULT_BADGE_COUNT, 0);
                badgeActivity = intent.getStringExtra(DEFAULT_BADGE_ACTIVITY_NAME);
                badgePackage = intent.getStringExtra(DEFAULT_BADGE_PACKAGENAME);

            } else if (action.equals(APEX_ACTION)) {
                badgeCount = intent.getIntExtra(APEX_BADGE_COUNT, 0);
                badgeActivity = intent.getStringExtra(APEX_BADGE_ACTIVITY_NAME);
                badgePackage = intent.getStringExtra(APEX_BADGE_PACKAGENAME);

            } else if (action.equals(SONY_ACTION)) {
                if (intent.getBooleanExtra(SONY_BADGE_SHOW, false)) {
                    badgeCount = intent.getIntExtra(SONY_BADGE_COUNT, 0);
                }
                badgeActivity = intent.getStringExtra(SONY_BADGE_ACTIVITY_NAME);
                badgePackage = intent.getStringExtra(SONY_BADGE_PACKAGENAME);

            } else {
                Log.e("UnreadReceiver", "Unknown badge action '" + action + "' " + intent.toString());
            }


            Log.d("BADGE", action + " " + badgeCount + " " + badgeActivity + " " + badgePackage);

            if (badgeActivity != null && badgePackage != null) {

                //don't update badge if this app just previously got an update,
                // in case an app tries multiple broadcast types
                if (lastCountActivity!=null && badgeActivity.equals(lastCountActivity) &&
                        lastCountPackage!=null && badgePackage.equals(lastCountPackage) &&
                        System.currentTimeMillis() - lastCountTime<500 &&
                        lastCount==badgeCount) {
                    Log.d("UnreadReceiver", "app " + badgeActivity + " " + badgePackage + " using action " + action + " after using " + lastCountAction);

                } else {

                    GlobState.getBadger(context).setUnreadCount(badgeActivity, badgePackage, badgeCount);
                }

                lastCountAction = action;
                lastCountTime = System.currentTimeMillis();
                lastCountActivity = badgeActivity;
                lastCountPackage = badgePackage;
                lastCount = badgeCount;

            }


        } catch (Exception e) {
            Log.e("UnreadReceiver", e.getMessage(),e);
        }


//        Intent intent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
//        intent.putExtra("badge_count", count);
//        intent.putExtra("badge_count_package_name", context.getPackageName());
//        intent.putExtra("badge_count_class_name", launcherClassName);
//
//
//        intent.setAction("com.sonyericsson.home.action.UPDATE_BADGE");
//        intent.putExtra("com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME", launcherClassName);
//        intent.putExtra("com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE", true);
//        intent.putExtra("com.sonyericsson.home.intent.extra.badge.MESSAGE", String.valueOf(count));
//        intent.putExtra("com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME", context.getPackageName());


    }
}
