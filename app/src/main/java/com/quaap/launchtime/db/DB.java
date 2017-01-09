package com.quaap.launchtime.db;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.quaap.launchtime.components.AppShortcut;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tom on 1/8/17.
 * <p>
 * Copyright (C) 2017  tom
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
public class DB extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "db";
    private static final int DATABASE_VERSION = 1;

    private static final String APP_TABLE = "apps";
    private static final String PKGNAME = "pkgname";
    private static final String LABEL = "label";
    private static final String CATEGORY = "category";
    private static final String INDEX = "pos";
    private static final String ID = "id";


    private static final String[] appcolumns = {PKGNAME, LABEL, CATEGORY};
    private static final String[] appcolumntypes = {"TEXT primary key", "TEXT", "TEXT"};
    private static final String APP_TABLE_CREATE = buildCreateTableStmt(APP_TABLE, appcolumns, appcolumntypes);

    private static final String[] appcolumnsindex = {PKGNAME, CATEGORY};

    private static final String APP_ORDER_TABLE = "apps_order";
    private static final String[] appordercolumns = {CATEGORY, PKGNAME, INDEX};
    private static final String[] appordercolumntypes = {"TEXT", "TEXT", "INT"};
    private static final String APP_ORDER_TABLE_CREATE = buildCreateTableStmt(APP_ORDER_TABLE, appordercolumns, appordercolumntypes);

    private static final String[] appordercolumnsindex = {CATEGORY + ", " + PKGNAME, INDEX};


    private static final String TAB_ORDER_TABLE = "tab_order";
    private static final String[] tabordercolumns = {ID, CATEGORY, INDEX};
    private static final String[] tabordercolumntypes = {"TEXT primary key", "TEXT", "INT"};
    private static final String TAB_ORDER_TABLE_CREATE = buildCreateTableStmt(TAB_ORDER_TABLE, tabordercolumns, tabordercolumntypes);

    private static final String[] tabordercolumnsindex = {ID, INDEX};



    private boolean firstRun;

    public DB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        firstRun = true;
        sqLiteDatabase.execSQL(APP_TABLE_CREATE);
        for(String createind: appcolumnsindex) {
            sqLiteDatabase.execSQL(buildIndexStmt(APP_TABLE, createind));
        }

        sqLiteDatabase.execSQL(APP_ORDER_TABLE_CREATE);
        for(String createind: appordercolumnsindex) {
            sqLiteDatabase.execSQL(buildIndexStmt(APP_ORDER_TABLE, createind));
        }

        sqLiteDatabase.execSQL(TAB_ORDER_TABLE_CREATE);
        for(String createind: tabordercolumnsindex) {
            sqLiteDatabase.execSQL(buildIndexStmt(TAB_ORDER_TABLE, createind));
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public boolean isFirstRun() {
        return firstRun;
    }


    public List<String> getAppPkgNames() {
        List<String> pkgnames = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(APP_TABLE, new String[]{PKGNAME}, null, null, null, null, null);

        while(cursor.moveToNext()) {
            pkgnames.add(cursor.getString(0));
        }
        cursor.close();
        return pkgnames;
    }


    public AppShortcut getApp(String pkgname) {

        AppShortcut appShortcut = null;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(APP_TABLE, appcolumns, PKGNAME + "=?", new String[]{pkgname}, null, null, null);

        if(cursor.moveToNext()) {
            String label = cursor.getString(1);
            String category = cursor.getString(2);

            appShortcut = new AppShortcut(pkgname, label, category);
        }
        cursor.close();
        return appShortcut;
    }

    public List<AppShortcut> getApps(String category) {

        List<AppShortcut> apps = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(APP_TABLE, appcolumns, CATEGORY + "=?", new String[]{category}, null, null, null);

        while(cursor.moveToNext()) {
            String pkgname = cursor.getString(0);
            String label = cursor.getString(1);

            apps.add(new AppShortcut(pkgname, label, category));
        }
        cursor.close();

        return apps;
    }

    public void addApp(AppShortcut shortcut) {
        addApp(shortcut.getPackageName(), shortcut.getLabel(), shortcut.getCategory());
    }

    public void addApp(String pkgname, String label, String category) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(PKGNAME, pkgname);
            values.put(LABEL, label);
            values.put(CATEGORY, category);

            db.insert(APP_TABLE, null, values);
        } catch (Exception e) {
            Log.e("LaunchDB", "Can't insert package " + pkgname, e);
        }
    }


    public void addCategory(String catID, String displayName, int index) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            Log.d("DB", "adding catagory " + catID);
            ContentValues values = new ContentValues();
            values.put(ID, catID);
            values.put(CATEGORY, displayName);
            values.put(INDEX, index);

            db.insert(TAB_ORDER_TABLE, null, values);
        } catch (Exception e) {
            Log.e("LaunchDB", "Can't select category " + catID, e);
        }
    }


    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(true, TAB_ORDER_TABLE, new String[]{ID}, null, null, null, null, INDEX, null);

        Log.d("DB", "getting catagories");
        while(cursor.moveToNext()) {

            categories.add(cursor.getString(0));
            Log.d("DB", "got catagory " + cursor.getString(0));
        }
        cursor.close();
        return categories;
    }

    public String getCategoryDisplay(String catID) {
        String display = null;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TAB_ORDER_TABLE, new String[]{CATEGORY}, ID + "=?", new String[]{catID}, null, null, null, null);

        if(cursor.moveToNext()) {
            display = cursor.getString(0);
        }
        cursor.close();
        return display;
    }

//    public List<String> getCategories() {
//        List<String> categories = new ArrayList<>();
//
//        SQLiteDatabase db = this.getWritableDatabase();
//
//        Cursor cursor = db.query(true, APP_TABLE, new String[]{CATEGORY}, null, null, null, null, CATEGORY, null);
//
//        while(cursor.moveToNext()) {
//            categories.add(cursor.getString(0));
//        }
//        cursor.close();
//        return categories;
//    }
    private static String buildCreateTableStmt(String tablename, String[] cols, String[] coltypes) {

        String create =  "CREATE TABLE " + tablename + " (";
        for (int i=0; i<cols.length; i++) {
            if (i!=0) create += ", ";
            create += cols[i] + " " + coltypes[i];
        }
        create += ");";
        return create;

    }

    private static String buildIndexStmt(String tablename, String col) {


        return   "CREATE INDEX "  + ((col + tablename).replaceAll("\\W+","_")) + " on " + tablename + "(" + col + ");";


    }

}
