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

    void hideAll() {
        for (int i = 0; i < getChildCount(); i++)
            getChildAt(i).setVisibility(GONE);
    }

    private class Observer extends DataSetObserver {
        private StaticListView staticListView;

        public Observer(StaticListView staticListView) {
            this.staticListView = staticListView;
        }

        @Override
        public void onChanged() {
            staticListView.hideAll();

            int kids = staticListView.getChildCount();

            for (int i = 0; i < staticListView.mAdapter.getCount(); i++) {

                final int pos = i;

                View convertView = pos<kids ? staticListView.getChildAt(pos)  : null;
                final View itemView = staticListView.mAdapter.getView(pos, convertView, staticListView);

                if (convertView==null) {
                    staticListView.addView(itemView);
                }
                itemView.setVisibility(VISIBLE);
                itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onItemClick(staticListView.mAdapter.getItem(pos), itemView, pos, staticListView.mAdapter.getItemId(pos));
                    }
                });
            }

        }

        @Override
        public void onInvalidated() {
            staticListView.hideAll();
            super.onInvalidated();
        }
    }

    interface OnItemClickListener {
        void onItemClick(Object item, View itemView, int position, long id);
    }
}