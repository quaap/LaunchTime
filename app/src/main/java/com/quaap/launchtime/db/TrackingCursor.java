package com.quaap.launchtime.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import android.util.Log;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by tom on 1/25/17.
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
public class TrackingCursor extends SQLiteCursor {
    TrackingCursorFactory mParentFactory;


    public TrackingCursor(SQLiteCursorDriver driver, String editTable, SQLiteQuery query, TrackingCursorFactory parentFactory) {
        super(driver, editTable, query);
        mParentFactory = parentFactory;
    }

    public void close() {
        mParentFactory.cursorClosing(this);
    }




    public static class TrackingCursorFactory implements SQLiteDatabase.CursorFactory {
        private List<Cursor> cursors = Collections.synchronizedList(new LinkedList<Cursor>());

        @Override
        public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
            TrackingCursor cursor = new TrackingCursor(masterQuery, editTable, query, this);
            cursors.add(cursor);
            return cursor;
        }

        public List<Cursor> getOpenCursors() {
            return cursors;
        }

        void cursorClosing(TrackingCursor cursor) {
            cursors.remove(cursor);
        }

        public void closeAll() {
            for (int i=0;i<cursors.size(); i++) {
                try {
                    Cursor c = cursors.get(i);
                    if (!c.isClosed()) c.close();
                } catch (Exception e) {
                    Log.e("LaunchDB", "closeAll exception", e);
                }
            }
            cursors.clear();
        }

    }
}
