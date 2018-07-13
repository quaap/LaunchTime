package com.quaap.launchtime;


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


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.quaap.launchtime.components.ExceptionHandler;
import com.quaap.launchtime.components.IconPack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChooseIconFromPackActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_icon_from_pack);

        if (GlobState.enableCrashReporter && !BuildConfig.DEBUG) Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

        Map<String,String> iconpacks = IconPack.listAvailableIconsPacks(this);

        LinkedHashMap<String,String> iconpacks2 = new LinkedHashMap<>();

        if (iconpacks.size()>0) {

            iconpacks2.put("", getString(R.string.custom_icon_select_icon_pack));
            iconpacks2.putAll(iconpacks);
        } else {
            iconpacks2.put("", "No icon packs installed");
        }


        final Spinner iconpackSpinner = findViewById(R.id.icon_pack_spinner);
        final MapAdapter<String,String> adapter = new MapAdapter<>(this, android.R.layout.simple_list_item_1, iconpacks2);
        iconpackSpinner.setAdapter(adapter);

        iconpackSpinner.setSelection(0);


        iconpackSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String packagename = adapter.getKey(iconpackSpinner.getSelectedItemPosition());

                Log.d("ICONS", packagename);
                if (!packagename.equals("")) {
                    displayIcons(packagename);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    private void displayIcons( String packname) {
        IconPack iconPack = new IconPack(this, packname);
        GridView gv = findViewById(R.id.icon_pack_icons);


        final ImageAdapter adapter = new ImageAdapter(this, iconPack);
        gv.setAdapter(adapter);

        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    BitmapDrawable bmdraw = (BitmapDrawable) (adapter.getItem(position));
                    if (bmdraw != null) {
                        Bitmap bitmap = bmdraw.getBitmap();
                        Intent returndata = new Intent();
                        returndata.putExtra("data", bitmap);
                        setResult(RESULT_OK, returndata);
                        finish();
                    }
                } catch (Exception e) {
                    Log.e("ChooseIconPack", e.getMessage(), e);
                }
            }
        });

    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }


    private class ImageAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<String> mDrawableNames;
        private SparseArray<Drawable> mDrawables  = new SparseArray<>();
        private IconPack mIconPack;

        private int mIconSize;

        ImageAdapter(Context c, IconPack iconPack) {
            mContext = c;
            mIconPack = iconPack;
            mDrawableNames = new ArrayList<>(mIconPack.getUniqueIconNames());
            mIconSize = (int)mContext.getResources().getDimension(R.dimen.icon_width);
        }

        public int getCount() {
            return mDrawableNames.size();
        }

        public Object getItem(int position) {
            return mDrawableNames.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(mContext);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(new GridView.LayoutParams(mIconSize, mIconSize));
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }

            Drawable d = mDrawables.get(position);
            if (d==null) {
                d = mIconPack.get(mDrawableNames.get(position));
                mDrawables.put(position, d);
            }
            imageView.setImageDrawable(d);
            return imageView;
        }


    }


    private class MapAdapter<K,V> extends ArrayAdapter<V> {

        private LinkedHashMap<K,V> mMap;
        private List<K> mKeys = new ArrayList<>();

        public MapAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull LinkedHashMap<K,V> map) {
            super(context, resource, new ArrayList<>(map.values()));

            mMap = map;

            mKeys.addAll(mMap.keySet());
        }


        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView v;
            if (convertView == null) {
                v = new TextView(parent.getContext());
            } else {
                v = (TextView)convertView;
            }

            v.setText(mMap.get(mKeys.get(position)).toString());

            return v;
        }

        public K getKey(int position) {
            return mKeys.get(position);
        }

    }
}
