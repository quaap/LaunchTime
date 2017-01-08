package com.quaap.launchtime.db;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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
    public static final String PKGNAME = "pkgname";
    public static final String LABEL = "label";
    public static final String CATEGORY = "category";
    public static final String INDEX = "index";

    private static final String[] appcolumns = {PKGNAME, LABEL, CATEGORY};
    private static final String[] appcolumntypes = {"TEXT", "TEXT", "TEXT"};
    private static final String APP_TABLE_CREATE = buildCreateTableStmt(APP_TABLE, appcolumns, appcolumntypes);

    private static final String[] appcolumnsindex = {PKGNAME, CATEGORY};

    private static final String APP_ORDER_TABLE = "apps_order";
    private static final String[] appordercolumns = {CATEGORY, PKGNAME, INDEX};
    private static final String[] appordercolumntypes = {"TEXT", "TEXT", "INT"};
    private static final String APP_ORDER_TABLE_CREATE = buildCreateTableStmt(APP_ORDER_TABLE, appordercolumns, appordercolumntypes);

    private static final String[] appordercolumnsindex = {CATEGORY + ", " + PKGNAME, INDEX};

    public DB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(APP_TABLE_CREATE);
        for(String createind: appcolumnsindex) {
            sqLiteDatabase.execSQL(createind);
        }

        sqLiteDatabase.execSQL(APP_ORDER_TABLE_CREATE);
        for(String createind: appordercolumnsindex) {
            sqLiteDatabase.execSQL(createind);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public List<String> getAppPkgNames() {
        List<String> pkgnames = new ArrayList<>();

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(APP_TABLE, new String[]{PKGNAME}, null, null, null, null, null);

        while(cursor.moveToNext()) {
            pkgnames.add(cursor.getString(0));
        }
        cursor.close();
        return pkgnames;
    }

    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(true, APP_TABLE, new String[]{CATEGORY}, null, null, null, null, CATEGORY, null);

        while(cursor.moveToNext()) {
            categories.add(cursor.getString(0));
        }
        cursor.close();
        return categories;
    }

    public AppShortcut getApp(String pkgname) {

        AppShortcut appShortcut = null;
        SQLiteDatabase db = this.getWritableDatabase();

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

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(APP_TABLE, appcolumns, CATEGORY + "=?", new String[]{category}, null, null, null);

        while(cursor.moveToNext()) {
            String pkgname = cursor.getString(0);
            String label = cursor.getString(1);

            apps.add(new AppShortcut(pkgname, label, category));
        }
        cursor.close();

        return apps;
    }


    public void addApp(String pkgname, String label, String category) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(PKGNAME, pkgname);
        values.put(LABEL, label);
        values.put(CATEGORY, category);

        db.insert(APP_TABLE, null, values);
    }




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
