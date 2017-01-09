package com.quaap.launchtime;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

import com.quaap.launchtime.components.CachedThreadPool;
import com.quaap.launchtime.components.Categories;
import com.quaap.launchtime.db.DB;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

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
public class GlobState extends Application {

    private DB mDB;

    private ExecutorService mExecutor;

    public static GlobState getGlobState(Context context) {
        return (GlobState)context.getApplicationContext();
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mExecutor = new CachedThreadPool();
        Categories.init(this);

       // this.deleteDatabase(DB.DATABASE_NAME);

        mDB = new DB(this);
        if (mDB.isFirstRun()) {
            for (int i=0; i<Categories.DefCategoryOrder.length; i++) {
                String cat = Categories.DefCategoryOrder[i];
                mDB.addCategory(cat,cat,i);
            }
        }
    }

    public DB getDB() {
        return mDB;
    }

    private Handler handler = new Handler();

    public void runAsync(final Runnable r) {
        Runnable wraprun = new Runnable() {
            @Override
            public void run() {
                handler.post(r);
            }
        };

        mExecutor.submit(wraprun);
    }

    public <T> FutureTask<T> doAsync(Callable<T> c) {

        FutureTask<T> ftask = new FutureTask<>(c);
        mExecutor.submit(ftask);
        return ftask;
    }

    @Override
    public void onTerminate() {
        if (mDB!=null) {
            mDB.close();
        }
        if (mExecutor !=null) {
            mExecutor.shutdown();
            try {
                mExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!mExecutor.isShutdown()) {
                mExecutor.shutdownNow();
            }
        }
        super.onTerminate();
    }
}
