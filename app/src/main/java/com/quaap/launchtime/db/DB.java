package com.quaap.launchtime.db;


import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import android.view.ViewGroup;

import com.quaap.launchtime.R;
import com.quaap.launchtime.apps.AppLauncher;
import com.quaap.launchtime.components.Categories;
import com.quaap.launchtime.components.FsTools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class DB extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "db";
    private static final int DATABASE_VERSION = 6;

    private static final String ACTVNAME = "actvname";
    private static final String PKGNAME = "pkgname";
    private static final String LABEL = "label";
    private static final String LABELFULL = "labelfull";
    private static final String CATID = "catID";
    private static final String ISWIDGET = "iswidget";
    private static final String ISUNINSTALLED = "isuninstalled";
    private static final String CUSTOMLABEL = "customlabel";

    private static final String INDEX = "pos";
    private static final String TIME = "time";
    private static final String ISTINY = "tiny";
    private static final String LEVEL = "level";



    private static final String APP_TABLE_OLD = "apps";
    private static final String APP_TABLE = "apps2";
    private static final String[] appcolumns = {ACTVNAME, PKGNAME, LABEL, CATID, ISWIDGET, ISUNINSTALLED, CUSTOMLABEL};
    private static final String[] appcolumntypes = {"TEXT", "TEXT", "TEXT", "TEXT", "SHORT", "SHORT", "TEXT"};
    private static final String APP_TABLE_CREATE = buildCreateTableStmt(APP_TABLE, appcolumns, appcolumntypes);

    private static final String[] appcolumnsindex = {ACTVNAME, PKGNAME, CATID, ISUNINSTALLED};

    private static final String APP_ORDER_TABLE = "apps_order";
    private static final String[] appordercolumns = {CATID, ACTVNAME, INDEX, PKGNAME};
    private static final String[] appordercolumntypes = {"TEXT", "TEXT", "INT", "TEXT"};
    private static final String APP_ORDER_TABLE_CREATE = buildCreateTableStmt(APP_ORDER_TABLE, appordercolumns, appordercolumntypes);

    private static final String[] appordercolumnsindex = {CATID + ", " + ACTVNAME, INDEX};


    private static final String TAB_ORDER_TABLE = "tab_order";
    private static final String[] tabordercolumns = {CATID, LABEL, LABELFULL, ISTINY, INDEX};
    private static final String[] tabordercolumntypes = {"TEXT primary key", "TEXT", "TEXT", "SHORT", "INT"};
    private static final String TAB_ORDER_TABLE_CREATE = buildCreateTableStmt(TAB_ORDER_TABLE, tabordercolumns, tabordercolumntypes);

    private static final String[] tabordercolumnsindex = {INDEX};


    private static final String APP_HISTORY_TABLE = "apps_hist";
    private static final String[] apphistorycolumns = {ACTVNAME, TIME, PKGNAME};
    private static final String[] apphistorycolumntypes = {"TEXT", "DATETIME", "TEXT"};
    private static final String APP_HISTORY_TABLE_CREATE = buildCreateTableStmt(APP_HISTORY_TABLE, apphistorycolumns, apphistorycolumntypes);

    private static final String[] apphistorycolumnsindex = {ACTVNAME, PKGNAME};


    private static final String APP_CAT_MAP_TABLE = "app_category_map";
    private static final String[] appcatmapcolumns = {ACTVNAME, PKGNAME, CATID, LEVEL};
    private static final String[] appcatmapcolumntypes = {"TEXT", "TEXT", "TEXT", "SHORT"};
    private static final String APP_CAT_MAP_TABLE_CREATE = buildCreateTableStmt(APP_CAT_MAP_TABLE, appcatmapcolumns, appcatmapcolumntypes);


    private static final String[] appcatmapcolumnsindex = {"ACTVNAME, LEVEL", "PKGNAME, LEVEL"};

    private boolean firstRun;

    private Context mContext;

    private DBClosedListener mDBClosedListener;

    private ComponentName latestInstall;
    private long latestInstallTime;

    public static DB openDB(Context context, DBClosedListener dBClosedListener) {
         return new DB(context, dBClosedListener);
    }

    private DB(Context context, DBClosedListener dBClosedListener ) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
        mDBClosedListener = dBClosedListener;
        this.getWritableDatabase();//force onCreate();

        if (isFirstRun()) {
            Log.d("db", "first run: creating categories");
            for (int i = 0; i < Categories.DefCategoryOrder.length; i++) {
                String cat = Categories.DefCategoryOrder[i];
                addCategory(cat, Categories.getCatLabel(mContext, cat), Categories.getCatFullLabel(mContext, cat), Categories.isTinyCategory(cat), i);
            }
        } else {
            Log.d("db", "opening database");
//            SharedPreferences p = context.getSharedPreferences(DB.class.toString(), Context.MODE_PRIVATE);
//            if (!p.getBoolean("removedoldconfig", false)) {
//                Log.d("db", "remove old com.quaap.launchtime.SettingsActivity");
//                this.deleteApp("com.quaap.launchtime.SettingsActivity");
//                p.edit().putBoolean("removedoldconfig", true).apply();
//            }
        }

//        ContentValues values = new ContentValues();
//        values.put(ISUNINSTALLED, 0);
//        this.getWritableDatabase().update(APP_TABLE, values, null, null);

    }

    private static String buildCreateTableStmt(String tablename, String[] cols, String[] coltypes) {

        String create = "CREATE TABLE " + tablename + " (";
        for (int i = 0; i < cols.length; i++) {
            if (i != 0) create += ", ";
            create += cols[i] + " " + coltypes[i];
        }
        create += ");";
        return create;

    }

    private static String buildIndexStmt(String tablename, String col) {


        return "CREATE INDEX " + ((col + tablename).replaceAll("\\W+", "_")) + " on " + tablename + "(" + col + ");";


    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        firstRun = true;
        Log.i("db", "create database");
        sqLiteDatabase.execSQL(APP_TABLE_CREATE);
        for (String createind : appcolumnsindex) {
            sqLiteDatabase.execSQL(buildIndexStmt(APP_TABLE, createind));
        }

        sqLiteDatabase.execSQL(APP_ORDER_TABLE_CREATE);
        for (String createind : appordercolumnsindex) {
            sqLiteDatabase.execSQL(buildIndexStmt(APP_ORDER_TABLE, createind));
        }

        sqLiteDatabase.execSQL(TAB_ORDER_TABLE_CREATE);
        for (String createind : tabordercolumnsindex) {
            sqLiteDatabase.execSQL(buildIndexStmt(TAB_ORDER_TABLE, createind));
        }

        sqLiteDatabase.execSQL(APP_HISTORY_TABLE_CREATE);
        for (String createind : apphistorycolumnsindex) {
            sqLiteDatabase.execSQL(buildIndexStmt(APP_HISTORY_TABLE, createind));
        }

        buildCatTable(sqLiteDatabase);


    }

    private void buildCatTable(SQLiteDatabase sqLiteDatabase) {
        Log.i("db", "creating category table");
        sqLiteDatabase.execSQL("drop table if exists " + APP_CAT_MAP_TABLE);
        sqLiteDatabase.execSQL(APP_CAT_MAP_TABLE_CREATE);
        for (String createind : appcatmapcolumnsindex) {
            sqLiteDatabase.execSQL(buildIndexStmt(APP_CAT_MAP_TABLE, createind));
        }

        loadCategories(sqLiteDatabase, true, R.raw.submitted_activities,1);
        loadCategories(sqLiteDatabase, false, R.raw.submitted_packages, 2);
        loadCategories(sqLiteDatabase, false, R.raw.packages1,3);
        loadCategories(sqLiteDatabase, false, R.raw.packages2,4);
    }


    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.i("db", "upgrade database");

        if (oldVersion==1) {
            sqLiteDatabase.execSQL("alter table " + APP_TABLE_OLD + " add column " + ISUNINSTALLED + " SHORT");
            //sqLiteDatabase.execSQL(buildIndexStmt(APP_TABLE_OLD,  ISUNINSTALLED));

            ContentValues values = new ContentValues();
            values.put(ISUNINSTALLED, 0);
            sqLiteDatabase.update(APP_TABLE_OLD, values, null, null);
        }
        if (oldVersion<=2) {

            sqLiteDatabase.execSQL(APP_TABLE_CREATE);
            for (String createind : appcolumnsindex) {
                sqLiteDatabase.execSQL(buildIndexStmt(APP_TABLE, createind));
            }

            boolean first = true;
            String cols = "";
            for (String col : appcolumns) {
                if (first) first = false;
                else cols += ", ";
                cols += col;

            }


            Log.i("db", "copy to new table");
            sqLiteDatabase.execSQL("insert into " + APP_TABLE + "(" + cols + ") select " + cols + " from " + APP_TABLE_OLD);
            sqLiteDatabase.execSQL("drop table " + APP_TABLE_OLD);

            sqLiteDatabase.execSQL("alter table " + APP_ORDER_TABLE + " add column " + PKGNAME + " TEXT");
            sqLiteDatabase.execSQL("alter table " + APP_HISTORY_TABLE + " add column " + PKGNAME + " TEXT");

            Cursor cursor = sqLiteDatabase.query(APP_TABLE, new String[]{ACTVNAME, PKGNAME}, null, null, null, null, ACTVNAME);
            try {
                while (cursor.moveToNext()) {
                    String actv = cursor.getString(0);
                    String pkg = cursor.getString(1);

                    ContentValues values = new ContentValues();
                    values.put(PKGNAME, pkg);
                    sqLiteDatabase.update(APP_ORDER_TABLE, values, ACTVNAME + "=?", new String[]{actv});
                    values.put(PKGNAME, pkg);
                    sqLiteDatabase.update(APP_HISTORY_TABLE, values, ACTVNAME + "=?", new String[]{actv});
                }
            } finally {
                cursor.close();
            }

            ContentValues values = new ContentValues();
            values.put(INDEX, 1);

            //move search/recent away from top to correct old order.
            //{CATID, LABEL, LABELFULL, ISTINY, INDEX};
            if (sqLiteDatabase.update(TAB_ORDER_TABLE, values, CATID + "=\"" + Categories.CAT_SEARCH + "\" and " + INDEX + "=0", null)>0) {
                values.put(INDEX, 0);
                sqLiteDatabase.update(TAB_ORDER_TABLE, values, CATID + "!=\"" + Categories.CAT_SEARCH + "\" and " + INDEX + "=1", null);
            }
        }

        if (oldVersion<6) {
            sqLiteDatabase.execSQL("alter table " + APP_TABLE + " add column " + CUSTOMLABEL + " TEXT");

        }

        //if (oldVersion<=3) {
            buildCatTable(sqLiteDatabase);
        //}
        sqLiteDatabase.delete(APP_ORDER_TABLE, PKGNAME + " is null", null);
    }

    public boolean isFirstRun() {
        return firstRun;
    }


    public List<ComponentName> getAppNames() {
        List<ComponentName> actvnames = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(APP_TABLE, new String[]{ACTVNAME, PKGNAME}, ISUNINSTALLED+"=0", null, null, null, LABEL);
        try {
            while (cursor.moveToNext()) {
                String actv = cursor.getString(0);
                String pkg = cursor.getString(1);
                try {
                    ComponentName cn = new ComponentName(pkg, actv);
                    actvnames.add(cn);
                } catch (Exception e) {
                    Log.e("LaunchDB", e.getMessage(), e);
                }
            }
        } finally {
            cursor.close();
        }
        return actvnames;
    }

    public AppLauncher getApp(ComponentName appname) {

        String actvname = appname.getClassName();
        String pkgname =  appname.getPackageName();

        AppLauncher appLauncher = null;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(APP_TABLE, appcolumns, ACTVNAME + "=? and " + PKGNAME + "=?", new String[]{actvname, pkgname}, null, null, null);
        try {
            if (cursor.moveToNext()) { //ACTVNAME, PKGNAME, LABEL, CATID
                //pkgname = cursor.getString(1);
                String label = cursor.getString(2);
                String catID = cursor.getString(3);
                boolean widget = cursor.getShort(4) == 1;
                String customlabel = cursor.getString(cursor.getColumnIndex(CUSTOMLABEL));

                // Log.d("LaunchDB", "getApp " + pkgname + " " + catID);
                appLauncher = AppLauncher.createAppLauncher(actvname, pkgname, customlabel==null?label:customlabel, catID, widget);
            }
        } finally {
            cursor.close();
        }
        return appLauncher;
    }

    public List<AppLauncher> getAppsForPackage(String pkgname) {


        List<AppLauncher> appLaunchers = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(APP_TABLE, appcolumns, PKGNAME + "=?", new String[]{pkgname}, null, null, null);
        try {
            while (cursor.moveToNext()) { //ACTVNAME, PKGNAME, LABEL, CATID
                String actvname = cursor.getString(0);
                //pkgname = cursor.getString(1);
                String label = cursor.getString(2);
                String catID = cursor.getString(3);
                boolean widget = cursor.getShort(4) == 1;
                String customlabel = cursor.getString(cursor.getColumnIndex(CUSTOMLABEL));

                // Log.d("LaunchDB", "getApp " + pkgname + " " + catID);
                appLaunchers.add(AppLauncher.createAppLauncher(actvname, pkgname, customlabel==null?label:customlabel, catID, widget));
            }
        } finally {
            cursor.close();
        }
        return appLaunchers;
    }



    public String getAppCategory(ComponentName appname) {
        String actvname = appname.getClassName();
        String pkgname =  appname.getPackageName();

        String catID = null;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(APP_TABLE, new String[]{CATID}, ACTVNAME + "=? and " + PKGNAME + "=?", new String[]{actvname, pkgname}, null, null, null);
        try {
            if (cursor.moveToNext()) { //ACTVNAME, PKGNAME, LABEL, CATID
                catID = cursor.getString(0);
            }
        } finally {
            cursor.close();
        }
        return catID;
    }

    public boolean isAppInstalled(ComponentName appname) {
        String actvname = appname.getClassName();
        String pkgname =  appname.getPackageName();

        boolean installed = false;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(APP_TABLE, new String[]{ISUNINSTALLED}, ACTVNAME + "=? and " + PKGNAME + "=?", new String[]{actvname, pkgname}, null, null, null);
        try {
            if (cursor.moveToNext()) { //ACTVNAME, PKGNAME, LABEL, CATID
                installed = cursor.getShort(0) != 1;
            }
        } finally {
            cursor.close();
        }
        return installed;
    }


    public int getAppCount(String catID) {
        int count = 0;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(APP_TABLE,  new String[]{"count(*)"}, CATID + "=? and ("+ISUNINSTALLED+"=0)", new String[]{catID}, null, null, null);
        try {
            if (cursor.moveToNext()) {
                count = cursor.getInt(0);
            }
        } finally {
            cursor.close();
        }
        return count;
    }

    public List<AppLauncher> getApps(String catID) {

        List<AppLauncher> apps = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(APP_TABLE, appcolumns, CATID + "=? and ("+ISUNINSTALLED+"=0)", new String[]{catID}, null, null, CUSTOMLABEL + " ," + LABEL);
        try {
            while (cursor.moveToNext()) {
                String actvname = cursor.getString(0);
                String pkgname = cursor.getString(1);
                String label = cursor.getString(2);
                boolean widget = cursor.getShort(4) == 1;
                String customlabel = cursor.getString(cursor.getColumnIndex(CUSTOMLABEL));

                if (widget) {
                    Log.d("db", "Found widget: " + actvname + " " + pkgname);
                }
                apps.add(AppLauncher.createAppLauncher(actvname, pkgname, customlabel==null?label:customlabel, catID, widget));
            }
        } finally {
            cursor.close();
        }

        return apps;
    }

    public boolean addApp(AppLauncher launcher) {
        return addApp(launcher.getActivityName(), launcher.getPackageName(), launcher.getLabel(), launcher.getCategory(), launcher.isWidget());
    }

    public void addApps(List<AppLauncher> launchers) {
        SQLiteDatabase db = this.getWritableDatabase();
        for (AppLauncher launcher : launchers) {
            addApp(db, launcher.getActivityName(), launcher.getPackageName(), launcher.getLabel(), launcher.getCategory(), launcher.isWidget());
        }
    }

    public boolean addApp(String actvname, String pkgname, String label, String catID, boolean widget) {
        SQLiteDatabase db = this.getWritableDatabase();
       return addApp(db, actvname, pkgname, label, catID, widget);
    }

    public long getLatestInstallTime() {
        return latestInstallTime;
    }

    public ComponentName getLatestInstall() {
        return latestInstall;
    }

    private boolean addApp(SQLiteDatabase db, String actvname, String pkgname, String label, String catID, boolean widget) {
        try {

            if (pkgname!=null && actvname!=null) {
                latestInstall = new ComponentName(pkgname, actvname);
                latestInstallTime = System.currentTimeMillis();
            }

            String dbcat = getCategoryDisplay(catID);
            if (dbcat==null) {
                Log.i("LaunchDB", "Use of category not in the database: " + catID, new Throwable());
                catID = Categories.CAT_OTHER;  //the user deleted the category
            }

            //Log.d("LaunchDB", "actvname " + actvname + " pkgname "  +pkgname + " added to db");
            ContentValues values = new ContentValues();
            values.put(LABEL, label);
            values.put(CATID, catID);
            values.put(ISWIDGET, widget ? 1 : 0);
            values.put(ISUNINSTALLED, 0);

            if (db.update(APP_TABLE, values, ACTVNAME + "=? and " + PKGNAME + "=?", new String[]{actvname, pkgname})==0) {
                values.put(ACTVNAME, actvname);
                values.put(PKGNAME, pkgname);
                db.insert(APP_TABLE, null, values);
               // Log.i("LaunchDB", "inserted " + actvname + " " + pkgname);
            }
            AppLauncher.removeAppLauncher(actvname,pkgname);

            return true;
        } catch (Exception e) {
            Log.e("LaunchDB", "Can't insert package " + pkgname, e);
        }
        return false;
    }

    public void updateAppLabel(String actvname, String pkgname, String label) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LABEL, label);
        db.update(APP_TABLE, values, ACTVNAME + "=? and " + PKGNAME + "=?", new String[]{actvname, pkgname});
        AppLauncher.removeAppLauncher(actvname,pkgname);
    }

    public void setAppCustomLabel(String actvname, String pkgname, String customlabel) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (customlabel==null) {
            values.putNull(CUSTOMLABEL);
        } else {
            values.put(CUSTOMLABEL, customlabel);
        }
        db.update(APP_TABLE, values, ACTVNAME + "=? and " + PKGNAME + "=?", new String[]{actvname, pkgname});
        AppLauncher.removeAppLauncher(actvname,pkgname);

    }


    public Cursor getAppCursor(String filter) {
        SQLiteDatabase db = this.getReadableDatabase();

        //Cursor cursor = db.query(APP_TABLE, new String[]{CATID}, null, null, null, null, INDEX, null);
        Cursor cursor = db.rawQuery(
                "select " + ACTVNAME + " _id, " + PKGNAME + " pkg,  app." + LABEL + " label, tab." + LABEL + " category " + ", app." + CUSTOMLABEL + " customlabel" +
                        " from " + APP_TABLE + " as app " +
                        " inner join " + TAB_ORDER_TABLE + " as tab on app." + CATID + "=tab." + CATID +
                        " where (app." + LABEL + " like ? or app." + CUSTOMLABEL + " like ?) and " +  ISWIDGET + "=0 and (" + ISUNINSTALLED+"=0)" +
                        " order by 3 ",
                new String[]{filter, filter});

        return cursor;
    }

    public void updateAppCategory(String actvname, String catID) {
        SQLiteDatabase db = this.getWritableDatabase();


        ContentValues values = new ContentValues();
        values.put(CATID, catID);

        db.update(APP_TABLE, values, ACTVNAME + "=?", new String[]{actvname});

        // Log.d("LaunchDB", "update " + pkgname + " " + catID);
    }

//    public boolean deleteApp(ComponentName appname) {
//        return deleteApp(actvorpkgname, false);
//    }

    public boolean deleteApp(ComponentName appname) {

        String actvname = appname.getClassName();

        String pkgname = appname.getPackageName();

        try {
            AppLauncher app = getApp(appname);
            if (app != null && (app.isLink() || app.isWidget())) {
                SQLiteDatabase db = this.getWritableDatabase();

                db.delete(APP_TABLE, ACTVNAME + "=? and " + PKGNAME + "=?", new String[]{actvname, pkgname});
                AppLauncher.removeAppLauncher(actvname,pkgname);

                return true;
            }
        } catch (Exception e) {
            Log.e("LaunchDB", "Can't delete app " + actvname, e);
        }



        return deleteApp(actvname,pkgname);
    }

    public boolean deleteApp(String actvname, String pkgname) {

        SQLiteDatabase db = this.getWritableDatabase();

        try {
            if (actvname==null) {

                List<AppLauncher> apps = getAppsForPackage(pkgname);
                if (apps != null) {
                    for (AppLauncher app: apps) {
                        if (app.isWidget() || app.isLink() || app.isActionLink()) {
                            db.delete(APP_TABLE, ACTVNAME + "=?", new String[]{app.getActivityName()});
                        }

                    }
                }
                ContentValues values = new ContentValues();
                values.put(ISUNINSTALLED, 1);

                db.update(APP_TABLE, values, PKGNAME + "=?", new String[]{pkgname});
            } else {


                ContentValues values = new ContentValues();
                values.put(ISUNINSTALLED, 1);

                db.update(APP_TABLE, values, ACTVNAME + "=? and " + PKGNAME + "=?", new String[]{actvname, pkgname});
            }
            AppLauncher.removeAppLauncher(actvname,pkgname);

            //db.delete(APP_TABLE, (isPackagename?PKGNAME:ACTVNAME) + "=?", new String[]{actvorpkgname});
        } catch (Exception e) {
            Log.e("LaunchDB", "Can't delete app " + actvname, e);
            return false;
        }
        return true;
    }

    public boolean addCategory(String catID, String displayName, String displayNameFull) {
        return addCategory(catID, displayName, displayNameFull, -1);
    }

    public boolean addCategory(String catID, String displayName, String displayNameFull, int index) {
        return addCategory(catID, displayName, displayNameFull, false, index);
    }

    public boolean addCategory(String catID, String displayName, String displayNameFull, boolean isTiny) {
        return addCategory(catID, displayName, displayNameFull, isTiny, -1);
    }

    public boolean addCategory(String catID, String displayName, String displayNameFull, boolean isTiny, int index) {
        try {

            for (String existcat: getCategories()) {
                if (existcat.equalsIgnoreCase(catID)) return false;
            }

            SQLiteDatabase db = this.getWritableDatabase();

            // Log.d("DB", "adding catID " + catID);
            ContentValues values = new ContentValues();
            values.put(CATID, catID);
            values.put(LABEL, displayName);
            values.put(LABELFULL, displayNameFull);
            values.put(ISTINY, isTiny?1:0);
            values.put(INDEX, index);

            db.insert(TAB_ORDER_TABLE, null, values);
        } catch (Exception e) {
            Log.e("LaunchDB", "Can't add catID " + catID, e);
            return false;
        }
        return true;
    }


    public boolean updateCategory(String catID, String displayName, String displayNameFull) {
        return updateCategory(catID, displayName, displayNameFull, false);
    }

    public boolean updateCategory(String catID, String displayName, String displayNameFull, boolean isTiny) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            // Log.d("DB", "adding catID " + catID);
            ContentValues values = new ContentValues();

            values.put(LABEL, displayName);
            values.put(LABELFULL, displayNameFull);
            values.put(ISTINY, isTiny?1:0);

            db.update(TAB_ORDER_TABLE, values, CATID + "=?", new String[]{catID});
        } catch (Exception e) {
            Log.e("LaunchDB", "Can't select catID " + catID, e);
            return false;
        }
        return true;
    }

    public boolean deleteCategory(String catID) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {

            db.beginTransaction();

            {
                //Update existing apps with the deleted category to category other
                ContentValues values = new ContentValues();
                values.put(CATID, Categories.CAT_OTHER);
                db.update(APP_TABLE, values, CATID + "=?", new String[]{catID});
            }

            {
                //Move old cat app order to category other
                List<ComponentName> oldAppOrder = getAppCategoryOrder(catID);
                List<ComponentName> otherAppOrder = getAppCategoryOrder(Categories.CAT_OTHER);

                otherAppOrder.addAll(oldAppOrder);
                setAppCategoryOrder(Categories.CAT_OTHER, otherAppOrder, true);
            }

            db.delete(TAB_ORDER_TABLE, CATID + "=?", new String[]{catID});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("LaunchDB", "Can't delete catID " + catID, e);
            return false;
        } finally {
            db.endTransaction();
        }
        return true;
    }

    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(true, TAB_ORDER_TABLE, new String[]{CATID}, null, null, null, null, INDEX, null);
        try {
            //Log.d("DB", "getting catagories");
            while (cursor.moveToNext()) {

                categories.add(cursor.getString(0));
                //          Log.d("DB", "got catID " + cursor.getString(0));
            }
        } finally {
            cursor.close();
        }
        cursor.close();
        return categories;
    }

    public String getCategoryDisplay(String catID) {
        String display = null;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TAB_ORDER_TABLE, new String[]{LABEL}, CATID + "=?", new String[]{catID}, null, null, null, null);
        try {
            if (cursor.moveToNext()) {
                display = cursor.getString(0);
            }
        } finally {
            cursor.close();
        }
        return display;
    }

    public String getCategoryDisplayFull(String catID) {
        String display = null;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TAB_ORDER_TABLE, new String[]{LABELFULL}, CATID + "=?", new String[]{catID}, null, null, null, null);
        try {
            if (cursor.moveToNext()) {
                display = cursor.getString(0);
            }
        } finally {
            cursor.close();
        }
        return display;
    }

    public boolean isTinyCategory(String catID) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TAB_ORDER_TABLE, new String[]{ISTINY}, CATID + "=?", new String[]{catID}, null, null, null, null);
        boolean tiny = false;
        try {
            if (cursor.moveToNext()) {
                tiny = cursor.getShort(0) == 1;
            }
        } catch (Exception e) {
            Log.e("LaunchDB", "tiny error.", e);
        } finally {
            cursor.close();
        }
        return tiny;
    }

    public void setCategoryOrder(ViewGroup container) {

        List<String> cats = new ArrayList<>();

        for (int i = 0; i < container.getChildCount(); i++) {
            Object tag = container.getChildAt(i).getTag();
            if (tag instanceof String) {
                cats.add((String) tag);
            }
        }

        setCategoryOrder(cats);
    }

    public void setCategoryOrder(List<String> apps) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            db.beginTransaction();

            for (int i = 0; i < apps.size(); i++) {

                ContentValues values = new ContentValues();

                values.put(INDEX, i);

                db.update(TAB_ORDER_TABLE, values, CATID + "=?", new String[]{apps.get(i)});
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("LaunchDB", "Can't setCategoryOrder ", e);

        } finally {
            db.endTransaction();
        }
    }

    public void setAppCategoryOrder(String catID, ViewGroup container) {

        List<AppLauncher> apps = new ArrayList<>();

        for (int i = 0; i < container.getChildCount(); i++) {
            Object tag = container.getChildAt(i).getTag();
            if (tag instanceof AppLauncher) {
                apps.add((AppLauncher) tag);
            }
        }

        setAppCategoryOrder(catID, apps);
    }

    public void setAppCategoryOrder(String catID, List<AppLauncher> apps) {
        List<ComponentName> actvnames = new ArrayList<>();
        for (AppLauncher app : apps) {
            actvnames.add(new ComponentName(app.getPackageName(), app.getActivityName()));
        }
        setAppCategoryOrder(catID, actvnames, true);
    }

    public void setAppCategoryOrder(String catID, List<ComponentName> actvnames, boolean dummy) {
        SQLiteDatabase db = this.getWritableDatabase();
       // Log.d("db", "setAppCategoryOrder " + catID);
        try {
            db.beginTransaction();
            db.delete(APP_ORDER_TABLE, CATID + "=?", new String[]{catID}); //CATID, PKGNAME, INDEX};

            for (int i = 0; i < actvnames.size(); i++) {
                ContentValues values = new ContentValues();
                values.put(CATID, catID);
                values.put(ACTVNAME, actvnames.get(i).getClassName());
                values.put(PKGNAME, actvnames.get(i).getPackageName());
                values.put(INDEX, i);
                db.insert(APP_ORDER_TABLE, null, values);
               // Log.d("db", "  " + i + " " + actvnames.get(i).getPackageName());
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("LaunchDB", "Can't setAppCategoryOrder for catID " + catID, e);

        } finally {
            db.endTransaction();
        }
    }

    public void addAppCategoryOrder(String catID, ComponentName actvname) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {

            ContentValues values = new ContentValues();
            values.put(CATID, catID);
            values.put(ACTVNAME, actvname.getClassName());
            values.put(PKGNAME, actvname.getPackageName());
            values.put(INDEX, 100);
            db.insert(APP_ORDER_TABLE, null, values);

        } catch (Exception e) {
            Log.e("LaunchDB", "Can't setAppCategoryOrder for catID " + catID, e);

        }
    }

    public List<ComponentName> getAppCategoryOrder(String catID) {
        List<ComponentName> actvnames = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        //Log.d("db", "getAppCategoryOrder " + catID);

        Cursor cursor = db.query(APP_ORDER_TABLE, new String[]{ACTVNAME, PKGNAME}, CATID + "=?", new String[]{catID}, null, null, INDEX);
        try {
           // int i=0;
            while (cursor.moveToNext()) {
                try {
                    actvnames.add(new ComponentName(cursor.getString(1), cursor.getString(0)));
                } catch (Exception e) {
                    Log.e("LaunchDB", e.getMessage(), e);
                }
              //  Log.d("db", "  " + i + " " + cursor.getInt(2) + " " + cursor.getString(1));
              //  i++;
            }
        } finally {
            cursor.close();
        }
        return actvnames;

    }

    public void appLaunched(ComponentName activityname) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ACTVNAME, activityname.getClassName());
        values.put(PKGNAME, activityname.getPackageName());
        values.put(TIME, System.currentTimeMillis());
        db.insert(APP_HISTORY_TABLE, null, values);


        //delete old apps
        Calendar old = Calendar.getInstance();
        old.add(Calendar.MONTH, -2);

        db.delete(APP_HISTORY_TABLE, TIME+"<?", new String[] { old.getTimeInMillis() + ""});

    }

    public void deleteAppLaunchedRecord(ComponentName appname) {

        String actvname = appname.getClassName();
        String pkgname =  appname.getPackageName();

        SQLiteDatabase db = this.getWritableDatabase();


        db.delete(APP_HISTORY_TABLE, ACTVNAME + "=? and " + PKGNAME + "=?", new String[]{actvname, pkgname});

    }

    public void deleteAppLaunchedRecords() {

        SQLiteDatabase db = this.getWritableDatabase();


        db.delete(APP_HISTORY_TABLE, null, null);

    }

    public List<ComponentName> getAppLaunchedList() {

        List<ComponentName> activitynames = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                true,
                APP_HISTORY_TABLE,
                new String[]{ACTVNAME, PKGNAME},
                null, null, null, null, TIME + " desc", null);

        try {
            while (cursor.moveToNext()) {
                if (!cursor.isNull(0) && !cursor.isNull(1) ) {
                    activitynames.add(new ComponentName(cursor.getString(1), cursor.getString(0)));
                }
            }
        } finally {
            cursor.close();
        }
        return activitynames;

    }

    public int getAppLaunchedCount(ComponentName activityname) {
        return getAppLaunchedCount(activityname, new Date(0));
    }

    public int getAppLaunchedCount(ComponentName activityname, Date after) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                APP_HISTORY_TABLE,
                new String[]{"count(*)"},
                ACTVNAME + "=? and " + PKGNAME + "=? and "+ TIME + ">?",
                new String[]{activityname.getClassName(), activityname.getPackageName(), after.getTime() + ""},
                null, null, null, null);

        int count = 0;
        try {
            if (cursor.moveToNext()) {
                count = cursor.getInt(0);
            }
        } finally {
            cursor.close();
        }
        return count;

    }

    public String getCategoryForPackage(String packagename) {
        SQLiteDatabase db = this.getReadableDatabase();

        // {ACTVNAME, PKGNAME, CATID, LEVEL};

        Cursor cursor = db.query(
                APP_CAT_MAP_TABLE, new String[]{CATID},
                PKGNAME + "=?",   new String[]{packagename}, null, null, LEVEL + "");

        String catid = null;
        try {
            if (cursor.moveToNext()) {
                catid = cursor.getString(0);
            }
        } finally {
            cursor.close();
        }
        return catid;
    }


    public String getCategoryForActivity(String activityname) {
        SQLiteDatabase db = this.getReadableDatabase();

        // {ACTVNAME, PKGNAME, CATID, LEVEL};

        Cursor cursor = db.query(
                APP_CAT_MAP_TABLE, new String[]{CATID},
                ACTVNAME + "=?",   new String[]{activityname}, null, null, LEVEL + "");

        String catid = null;
        try {
            if (cursor.moveToNext()) {
                catid = cursor.getString(0);
            }
        } finally {
            cursor.close();
        }
        return catid;
    }

    private void loadCategories(SQLiteDatabase db, boolean isactivity, int set, int level) {

        Log.d("LaunchDB", "loadCategories " + level + " " + set);

        InputStream inputStream = mContext.getResources().openRawResource(set);
        String line;
        String[] lineSplit;

        int count = 0;
        try {
            db.beginTransaction();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String sql = "INSERT INTO " + APP_CAT_MAP_TABLE + " values(?,?,?,?)";
            SQLiteStatement statement = db.compileStatement(sql);

            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty() && !line.startsWith("#")) {
                    lineSplit = line.split("=",2);
                    if (lineSplit.length==2) {  // {ACTVNAME, PKGNAME, CATID, LEVEL};
                        statement.clearBindings();
                        if (isactivity) {
                            statement.bindString(1, lineSplit[0]);
                            statement.bindString(2, "");
                        } else {
                            statement.bindString(1, "");
                            statement.bindString(2, lineSplit[0]);
                        }
                        statement.bindString(3, lineSplit[1]);
                        statement.bindLong(4, level);
                        try {
                            statement.executeInsert();
                        } catch (Exception e) {
                            Log.d("LaunchDB", "Can't add category", e);
                        }

                        if (count++%1000==0) {
                            db.setTransactionSuccessful();
                            db.endTransaction();
                            db.beginTransaction();
                        }
                    }
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            db.endTransaction();
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.d("LaunchDB", " loaded " + count + " rows");

    }




    //synchronize these so that we can also synchronize on the backup and restore, just in case

    @Override
    public synchronized SQLiteDatabase getReadableDatabase() {
        return super.getReadableDatabase();
    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        return super.getWritableDatabase();
    }



    public static final String BK_PRE = "db_bak.";


    public List<String> listBackups() {
        List<String> list = new ArrayList<>();
        Pattern bakpat = Pattern.compile("^" + Pattern.quote(BK_PRE)  + "(.+)");
        for (String file: mContext.fileList()) {
            Matcher m = bakpat.matcher(file);
            if (m.matches()) {
                list.add(m.group(1));
            }
        }
        Collections.sort(list);
        Collections.reverse(list);
        return list;
    }

    public boolean hasBackup(String backupName) {
        return listBackups().contains(backupName);
    }

    public File pullBackup(String backupName) {
        return mContext.getFileStreamPath(BK_PRE + backupName);
    }

    public synchronized File backup(String optionalName) {

        close();

        if (optionalName!=null && optionalName.length()>0) {
            optionalName =  "." + optionalName.replaceAll("[./(){}\"*|\\\\$]", "_");
            if (optionalName.length()>20) {
                optionalName = optionalName.substring(0,20);
            }
        } else {
            optionalName = "";
        }

        File srcFile = mContext.getDatabasePath(DATABASE_NAME);

        File destFile= mContext.getFileStreamPath(BK_PRE + (new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(new Date())) + optionalName);

        return FsTools.compressFile(srcFile, destFile);
    }

    public boolean restoreFullpathBackup( String filePath) {
        File srcFile = new File(filePath);

        return restore(srcFile);
    }

    public boolean restoreFullpathBackup(File file) {
        File srcFile = new File(file.getPath());

        return restore(srcFile);
    }
    public boolean restoreBackup(String backupName) {

        File srcFile = mContext.getFileStreamPath(BK_PRE + backupName);

        return restore(srcFile);
    }

    private synchronized boolean restore(File srcFile) {

        close();
        boolean ret = false;
        if (srcFile.exists() && srcFile.canRead()) {
            File destFile = mContext.getDatabasePath(DATABASE_NAME);

            ret = FsTools.decompressFile(srcFile, destFile)!=null;

            try {
                getWritableDatabase().close();
            } catch (Exception e) {
                Log.e("LaunchDB","couldn't load db after restore", e);
                return false;
            }
        }
        return ret;
    }


    public boolean deleteBackup(String backupName) {

        File srcFile = mContext.getFileStreamPath(BK_PRE + backupName);

        return srcFile.delete();

    }

    public synchronized void deleteDatabase() {
        close();
        Thread.yield();
        mContext.deleteDatabase(DB.DATABASE_NAME);
        Thread.yield();
        File destFile = mContext.getDatabasePath(DATABASE_NAME);
        if (destFile!=null && destFile.exists()) {
            if (destFile.delete()) {
                Log.d("LaunchTime", "db was there");
            }
        }

    }

    @Override
    public synchronized void close() {
        if (mDBClosedListener!=null) {
            mDBClosedListener.onDBClosed();
        }
        super.close();
    }

    public interface DBClosedListener {
        void onDBClosed();
    }


}
