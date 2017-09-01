package com.quaap.launchtime;

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
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

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

        Map<String,String> iconpacks = IconPack.listAvailableIconsPacks(this);

        LinkedHashMap<String,String> iconpacks2 = new LinkedHashMap<>();
        iconpacks2.put("", "Select Icon Pack");
        iconpacks2.putAll(iconpacks);


        final Spinner iconpackSpinner = (Spinner) findViewById(R.id.icon_pack_spinner);
        final MapAdapter<String,String> adapter = new MapAdapter<>(this, android.R.layout.simple_spinner_item, iconpacks2);
        iconpackSpinner.setAdapter(adapter);

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
        GridView gv = (GridView)findViewById(R.id.icon_pack_icons);

        final ImageAdapter adapter = new ImageAdapter(this, new ArrayList<Drawable>(iconPack.getAllIcons().values()));
        gv.setAdapter(adapter);

        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Bitmap bitmap = (Bitmap)((BitmapDrawable) ((Drawable)adapter.getItem(position))).getBitmap();
                Intent returndata = new Intent();
                returndata.putExtra("data", bitmap);
                setResult(RESULT_OK, returndata);
                finish();
            }
        });

    }


    private class ImageAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<Drawable> mDrawables;

        ImageAdapter(Context c, ArrayList<Drawable> drawables) {
            mContext = c;
            mDrawables = drawables;
        }

        public int getCount() {
            return mDrawables.size();
        }

        public Object getItem(int position) {
            return mDrawables.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = new ImageView(mContext);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }

            imageView.setImageDrawable(mDrawables.get(position));
            return imageView;
        }


    }


    private class MapAdapter<K,V> extends ArrayAdapter<V> {

        private LinkedHashMap<K,V> mMap;
        private List<K> mKeys = new ArrayList<>();

        public MapAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull LinkedHashMap<K,V> map) {
            super(context, resource, new ArrayList<V>(map.values()));

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
