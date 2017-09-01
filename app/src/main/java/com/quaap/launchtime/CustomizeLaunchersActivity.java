package com.quaap.launchtime;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.quaap.launchtime.apps.AppLauncher;
import com.quaap.launchtime.components.IconsHandler;
import com.quaap.launchtime.components.SpecialIconStore;
import com.quaap.launchtime.db.DB;

import java.io.InputStream;
import java.util.List;

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
 *
 *
 * Some portions of this file are derived from from ADW.Launcher (AnderWeb <anderweb@gmail.com>),
 * which is under the Apache 2.0 License.
 *
 * See also https://github.com/AnderWeb/android_packages_apps_Launcher
 *
 *
 **/

public class CustomizeLaunchersActivity extends Activity {

    private LinearLayout list;

    private int mIconSize;

    private AppLauncher mAppClicked;

    private ImageView mClickedIconView;
    private TextView mClickedTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize_launchers);

        mIconSize = (int) getResources().getDimension(android.R.dimen.app_icon_size);

        list = (LinearLayout)findViewById(R.id.custom_launchers_layout);

        final DB db = GlobState.getGlobState(this).getDB();

        final IconsHandler ich = GlobState.getIconsHandler(this);

        final Handler handler = new Handler();

        final int iconSize = (int)this.getResources().getDimension(R.dimen.icon_width);

        final AsyncTask<Void, Void, Void> loadappstask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                for (final String catID: db.getCategories()) {

                    List<AppLauncher> apps = db.getApps(catID);
                    if (apps.isEmpty()) continue;

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            TextView cat = new TextView(CustomizeLaunchersActivity.this);
                            cat.setText(db.getCategoryDisplayFull(catID));
                            cat.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
                            cat.setBackgroundColor(Color.BLACK);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                            lp.gravity = Gravity.CENTER;
                            lp.setMargins(12,18,12,12);
                            cat.setLayoutParams(lp);
                            cat.setPadding(12,6,6,6);
                            list.addView(cat);
                        }
                    });


                    for (final AppLauncher app: apps) {

                        if (!app.isNormalApp()) continue;

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                final LinearLayout applayout = new LinearLayout(CustomizeLaunchersActivity.this);
                                applayout.setOrientation(LinearLayout.HORIZONTAL);

                                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                lp.gravity = Gravity.CENTER_VERTICAL;
                                lp.setMargins(24,12,12,12);
                                applayout.setLayoutParams(lp);
                                applayout.setPadding(18,6,6,6);
                                boolean hasCustIcon = SpecialIconStore.hasBitmap(CustomizeLaunchersActivity.this, app.getComponentName(), SpecialIconStore.IconType.Custom);
                                boolean hasCustLabel = db.appHasCustomLabel(app.getComponentName());

                                if (hasCustIcon || hasCustLabel) {
                                    applayout.setBackgroundColor(Color.DKGRAY);
                                }

                                final ImageView iconView = new ImageView(CustomizeLaunchersActivity.this);

                                iconView.setPadding(12,12,12,12);
                                LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(iconSize, iconSize);
                                ilp.setMargins(12,12,24,12);
                                iconView.setLayoutParams(ilp);

                                Drawable icon = ich.getCustomIcon(app.getComponentName(), app.getLinkUri());

                                if (icon == null) {
                                    icon = ich.getDrawableIconForPackage( app.getBaseComponentName(), app.getLinkUri());
                                }

                                iconView.setImageDrawable(icon);

                                if (hasCustIcon) {
                                    iconView.setBackgroundColor(Color.parseColor("#99339933"));
                                }


                                iconView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        mAppClicked = app;
                                        mClickedIconView = iconView;
                                        new IconTypeDialog().createDialog().show();

                                    }
                                });
                                applayout.addView(iconView);

                                final TextView label = new TextView(CustomizeLaunchersActivity.this);
                                label.setText(app.getLabel());
                                label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                                label.setPadding(18,6,18,6);
                                label.setTextColor(Color.WHITE);
                                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                llp.setMargins(24,12,24,12);
                                label.setLayoutParams(llp);

                                if (hasCustLabel) {
                                    label.setBackgroundColor(Color.parseColor("#99339933"));
                                }

                                label.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        mAppClicked = app;
                                        mClickedTextView = label;
                                        promptForAppLabel();
                                    }
                                });
                                applayout.addView(label);


                                list.addView(applayout);
                            }
                        });

                    }


                }

                return null;
            }
        };

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadappstask.execute();
            }
        }, 200);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void promptForAppLabel() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.custom_icon_text);

        final EditText input = new EditText(this);

        input.setHint(mAppClicked.getLabel());

        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        builder.setView(input);


        builder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String labeltext = input.getText().toString();
                if (!labeltext.isEmpty()) {
                    updateAppLabel(labeltext);
                }
            }
        });
        builder.setNeutralButton(R.string.clear, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                updateAppLabel(null);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.show();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);

    }

    private void updateAppLabel(String labeltext) {
        DB db = GlobState.getGlobState(CustomizeLaunchersActivity.this).getDB();
        AppLauncher.removeAppLauncher(mAppClicked.getComponentName());
        db.setAppCustomLabel(mAppClicked.getActivityName(), mAppClicked.getPackageName(), labeltext);
        AppLauncher app = db.getApp(mAppClicked.getComponentName());

        mClickedTextView.setText(app.getLabel());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int num = prefs.getInt("icon-update", 0);
        prefs.edit().putInt("icon-update", num+1).apply();
    }





    private static final int PICK_CUSTOM_ICON=1;
    private static final int PICK_CUSTOM_PICTURE=5;
    private static final int PICK_FROM_ICON_PACK=6;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            //DB db = GlobState.getGlobState(this).getDB();

            Bitmap bitmap = null;
            switch (requestCode) {
                case PICK_FROM_ICON_PACK:
                case PICK_CUSTOM_PICTURE:
                    bitmap = (Bitmap) data.getParcelableExtra("data");
                    if (bitmap != null) {
                        if (bitmap.getWidth() > mIconSize) {
                            bitmap = Bitmap.createScaledBitmap(bitmap,mIconSize,mIconSize, true);
                        }
                        bitmap.setHasAlpha(true);
                    }
                    break;
                case PICK_CUSTOM_ICON:
                    Uri photoUri = data.getData();
                    try {
                        InputStream is = getContentResolver().openInputStream(
                                photoUri);
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inJustDecodeBounds = true;
                        bitmap = BitmapFactory.decodeStream(is, null, opts);

                        BitmapFactory.Options ops2 = new BitmapFactory.Options();
                        int width = mIconSize;
                        float w = opts.outWidth;
                        //int scale = Math.round(w / width);
                        int scale = (int) (w / width);
                        ops2.inSampleSize = scale;
                        is = getContentResolver().openInputStream(photoUri);
                        bitmap = BitmapFactory.decodeStream(is, null, ops2);
                        if (bitmap != null) {
                            if (bitmap.getWidth() > mIconSize) {
                                bitmap = Bitmap.createScaledBitmap(bitmap,mIconSize,mIconSize, true);
                            }
                            bitmap.setHasAlpha(true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

            }

            if (bitmap!=null) {
                updateBitmap(bitmap);
            }
        }
    }

    private void updateBitmap(Bitmap bitmap) {
        if (bitmap==null) {
            SpecialIconStore.deleteBitmap(this, mAppClicked.getComponentName(),SpecialIconStore.IconType.Custom);

            IconsHandler ich = GlobState.getIconsHandler(this);

            Drawable icon = ich.getDrawableIconForPackage( mAppClicked.getBaseComponentName(), mAppClicked.getLinkUri());

            mClickedIconView.setImageDrawable(icon);
        } else {
            SpecialIconStore.saveBitmap(this, mAppClicked.getComponentName(), bitmap, SpecialIconStore.IconType.Custom);
            mClickedIconView.setImageDrawable(new BitmapDrawable(this.getResources(), bitmap));
        }

        AppLauncher.removeAppLauncher(mAppClicked.getComponentName());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int num = prefs.getInt("icon-update", 0);
        prefs.edit().putInt("icon-update", num+1).apply();
    }

    protected class IconTypeDialog implements DialogInterface.OnClickListener,
            DialogInterface.OnCancelListener, DialogInterface.OnDismissListener,
            DialogInterface.OnShowListener {

        private ArrayAdapter<String> mAdapter;


        public  Dialog createDialog() {
            mAdapter = new ArrayAdapter<String>(CustomizeLaunchersActivity.this, R.layout.add_list_item);
            mAdapter.add(getString(R.string.custom_icon_select_picture));
            mAdapter.add(getString(R.string.custom_icon_crop_picture));
            mAdapter.add(getString(R.string.custom_icon_icon_packs));
            mAdapter.add(getString(R.string.custom_icon_clear_icon));

            final AlertDialog.Builder builder = new AlertDialog.Builder(CustomizeLaunchersActivity.this);
            builder.setTitle(R.string.custom_icon_select_icon_type);
            builder.setAdapter(mAdapter, this);

            //builder.setInverseBackgroundForced(false);

            AlertDialog dialog = builder.create();
            dialog.setOnCancelListener(this);
            dialog.setOnDismissListener(this);
            dialog.setOnShowListener(this);
            return dialog;
        }
        public void onCancel(DialogInterface dialog) {
            cleanup();
        }
        public void onDismiss(DialogInterface dialog) {
        }
        private void cleanup() {
        }
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case 0:
                    //Select icon
                    Intent pickerIntent=new Intent(Intent.ACTION_PICK);
                    pickerIntent.setType("image/*");
                    startActivityForResult(Intent.createChooser(pickerIntent, getString(R.string.custom_icon_select_icon_type)), PICK_CUSTOM_ICON);
                    break;
                case 1:
                    //Crop picture
                    int width;
                    int height;
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    width = height= mIconSize;
                    intent.putExtra("crop", "true");
                    intent.putExtra("outputX", width);
                    intent.putExtra("outputY", height);
                    intent.putExtra("aspectX", width);
                    intent.putExtra("aspectY", height);
                    intent.putExtra("noFaceDetection", true);
                    intent.putExtra("return-data", true);
                    startActivityForResult(intent, PICK_CUSTOM_PICTURE);
                    break;
                case 2:
                    //Icon packs
                    Intent packIntent=new Intent(CustomizeLaunchersActivity.this, ChooseIconFromPackActivity.class);
                    startActivityForResult(Intent.createChooser(packIntent, getString(R.string.custom_icon_select_icon_pack)), PICK_FROM_ICON_PACK);
                    break;
                case 3:
                    updateBitmap(null);
                    break;
                default:
                    break;
            }
            cleanup();
        }
        public void onShow(DialogInterface dialog) {
        }
    }
}
