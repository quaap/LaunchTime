package com.quaap.launchtime.components;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Modified from http://stackoverflow.com/a/27000087
 *
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
public class StaticListView extends LinearLayout {
    protected Adapter mAdapter;
    protected Observer mObserver = new Observer(this);
    private OnItemClickListener mOnItemClickListener;

    public StaticListView(Context context) {
        super(context);
    }

    public StaticListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAdapter(Adapter adapter) {
        if (this.mAdapter != null)
            this.mAdapter.unregisterDataSetObserver(mObserver);
        this.mAdapter = adapter;
        adapter.registerDataSetObserver(mObserver);
        mObserver.onChanged();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    private void onItemClick(Object item, View itemView, int position, long id) {
        if (mOnItemClickListener !=null) {
            mOnItemClickListener.onItemClick(item, itemView, position, id);
        }
    }

    private Handler handler = new Handler();

    private class Observer extends DataSetObserver {
        private StaticListView staticListView;

        private AsyncTask<Void,Void,Void> mTask;
        private volatile boolean mStopTask;
        private volatile boolean mTaskDone;

        public Observer(StaticListView staticListView) {
            this.staticListView = staticListView;
        }

        @Override
        public void onChanged() {

            mStopTask = true;

            if (mTask!=null) {
                if (!mTaskDone && mTask.getStatus() != AsyncTask.Status.FINISHED){
                    try {
                        mTask.cancel(true);
                        Thread.sleep(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            List<View> oldViews = new ArrayList<>(staticListView.getChildCount());
            for (int i = 0; i < staticListView.getChildCount(); i++)
                oldViews.add(staticListView.getChildAt(i));

            final Iterator<View> iter = oldViews.iterator();
            staticListView.removeAllViews();



            mTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    mTaskDone = false;
                    mStopTask = false;
                    for (int i = 0; i < staticListView.mAdapter.getCount(); i++) {
                        if (mStopTask) {
                            mTaskDone = true;
                            Log.d("LaunchTime", "Stopping cursor task early1");
                            return null;
                        }

                        final int pos = i;

                        View convertView = iter.hasNext() ? iter.next() : null;
                        final View itemView = staticListView.mAdapter.getView(pos, convertView, staticListView);

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mStopTask) {
                                    mTaskDone = true;
                                    Log.d("LaunchTime", "Stopping cursor task early2");
                                    return;
                                }
                                staticListView.addView(itemView);
                                itemView.setOnClickListener(new OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        onItemClick(staticListView.mAdapter.getItem(pos), itemView, pos, staticListView.mAdapter.getItemId(pos));
                                    }
                                });

                            }
                        });

                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    Observer.super.onChanged();
                    super.onPostExecute(aVoid);
                    mTaskDone = true;

                }
            };
            mTask.execute();

        }

        @Override
        public void onInvalidated() {
            staticListView.removeAllViews();
            super.onInvalidated();
        }
    }

    interface OnItemClickListener {
        void onItemClick(Object item, View itemView, int position, long id);
    }
}