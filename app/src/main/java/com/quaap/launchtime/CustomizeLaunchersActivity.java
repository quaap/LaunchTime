package com.quaap.launchtime;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.quaap.launchtime.apps.AppLauncher;
import com.quaap.launchtime.components.ExceptionHandler;
import com.quaap.launchtime.components.IconsHandler;
import com.quaap.launchtime.components.SpecialIconStore;
import com.quaap.launchtime.db.DB;

import java.io.InputStream;
import java.util.List;

/**
 *
 * Some portions of this file are derived from from ADW.Launcher (AnderWeb <anderweb@gmail.com>),
 * which is under the Apache 2.0 License.
 *
 * See also https://github.com/AnderWeb/android_packages_apps_Launcher
 *
 * Modified by Tom Kliethermes. 2017
 *
 * Rest Copyright (C) 2017   Tom Kliethermes
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

        if (GlobState.enableCrashReporter && !BuildConfig.DEBUG) Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

        mIconSize = (int) getResources().getDimension(android.R.dimen.app_icon_size);

        list = findViewById(R.id.custom_launchers_layout);

        final DB db = GlobState.getGlobState(this).getDB();

        final IconsHandler ich = GlobState.getIconsHandler(this);

        final Handler handler = new Handler();

        final int iconSize = (int)this.getResources().getDimension(R.dimen.icon_width);


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

                if (app.isWidget()) continue;

                final boolean hasCustIcon = SpecialIconStore.hasBitmap(CustomizeLaunchersActivity.this, app.getComponentName(), SpecialIconStore.IconType.Custom);
                final boolean hasCustLabel = db.appHasCustomLabel(app.getComponentName());

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

                        if (hasCustIcon || hasCustLabel) {
                            applayout.setBackgroundColor(Color.DKGRAY);
                        }

                        final ImageView iconView = new ImageView(CustomizeLaunchersActivity.this);

                        iconView.setPadding(12,12,12,12);
                        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(iconSize, iconSize);
                        ilp.setMargins(12,12,24,12);
                        iconView.setLayoutParams(ilp);

                        Drawable icon = ich.getCustomIcon(app.getComponentName());

                        if (icon == null) {
                            icon = ich.getDrawableIconForPackage(app);
                        }

                        iconView.setImageDrawable(icon);

                        if (hasCustIcon) {
                            setItemModified(iconView, true);
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
                            setItemModified(label, true);
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
            //list.postInvalidate();


        }

    }

    @Override
    public void onBackPressed() {
        pickingIcon = false;
        finish();
        MainActivity.openSettings(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!pickingIcon) {
            finish();
            Log.d("Customizer", "finishing in onpause");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        pickingIcon = false;
    }

    private void promptForAppLabel() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.custom_icon_text);

            final EditText input = new EditText(this);

            input.setText(mAppClicked.getLabel());

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
            if (GlobState.getGlobState(this).getDB().appHasCustomLabel(mAppClicked.getComponentName())) {
                builder.setNeutralButton(R.string.clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateAppLabel(null);
                    }
                });
            }
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        } catch (Exception|Error e) {
            Log.e("custLaunch", e.getMessage(), e);
        }

        //if (dialog.getWindow()!=null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);

    }

    private void updateAppLabel(String labeltext) {
        DB db = GlobState.getGlobState(CustomizeLaunchersActivity.this).getDB();
        mAppClicked.setLabel(labeltext);
        AppLauncher.removeAppLauncher(mAppClicked.getComponentName());
        db.setAppCustomLabel(mAppClicked.getActivityName(), mAppClicked.getPackageName(), labeltext);
        mAppClicked = db.getApp(mAppClicked.getComponentName());


        mClickedTextView.setText(mAppClicked.getLabel());

        setItemModified(mClickedTextView, labeltext!=null && !labeltext.isEmpty());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int num = prefs.getInt("icon-update", 0);
        prefs.edit().putInt("icon-update", num+1).apply();
    }


    private boolean pickingIcon;


    private static final int PICK_CUSTOM_ICON=1;
    private static final int PICK_FROM_ICON_PACK=6;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        pickingIcon = false;

        if (resultCode == RESULT_OK) {
            //DB db = GlobState.getGlobState(this).getDB();
            try {
                Bitmap bitmap = null;
                switch (requestCode) {
                    case PICK_FROM_ICON_PACK:
                        bitmap = data.getParcelableExtra("data");
                        if (bitmap != null) {
                            if (bitmap.getWidth() > mIconSize) {
                                bitmap = Bitmap.createScaledBitmap(bitmap, mIconSize, mIconSize, true);
                            }
                            bitmap.setHasAlpha(true);
                        }
                        break;
                    case PICK_CUSTOM_ICON:
                        Uri photoUri = data.getData();
                        if (photoUri!=null)
                        try {
                            InputStream is = getContentResolver().openInputStream(photoUri);
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
                                    bitmap = Bitmap.createScaledBitmap(bitmap, mIconSize, mIconSize, true);
                                }
                                bitmap.setHasAlpha(true);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;

                }

                if (bitmap != null && mAppClicked != null) {
                    updateBitmap(bitmap);
                } else {
                    Log.d("custLaunch", "NULL1!!" + bitmap + " " + mAppClicked);
                }
            } catch (Exception|Error e) {
                Log.e("custLaunch", e.getMessage(), e);
            }
        }
    }

    private void setItemModified(View v, boolean modified) {
        if (modified) {
            v.setBackgroundColor(Color.parseColor("#99335533"));
        } else {
            v.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void updateBitmap(Bitmap bitmap) {
        if (mAppClicked!=null && mClickedIconView!=null) {
            try {
                if (bitmap == null) {
                    SpecialIconStore.deleteBitmap(this, mAppClicked.getComponentName(), SpecialIconStore.IconType.Custom);

                    IconsHandler ich = GlobState.getIconsHandler(this);

                    Drawable icon = ich.getDrawableIconForPackage(mAppClicked);

                    mClickedIconView.setImageDrawable(icon);


                } else {
                    SpecialIconStore.saveBitmap(this, mAppClicked.getComponentName(), bitmap, SpecialIconStore.IconType.Custom);
                    mClickedIconView.setImageDrawable(new BitmapDrawable(this.getResources(), bitmap));
                }

                setItemModified(mClickedIconView, bitmap != null);

                AppLauncher.removeAppLauncher(mAppClicked.getComponentName());
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                int num = prefs.getInt("icon-update", 0);
                prefs.edit().putInt("icon-update", num + 1).apply();
            } catch (Exception|Error e) {
                Log.e("custLaunch", e.getMessage(), e);
            }
        } else {
            Log.d("custLaunch", "NULL!!");
        }
    }

    class IconTypeDialog implements DialogInterface.OnClickListener,
            DialogInterface.OnCancelListener, DialogInterface.OnDismissListener,
            DialogInterface.OnShowListener {

        private ArrayAdapter<String> mAdapter;


        Dialog createDialog() {
            mAdapter = new ArrayAdapter<>(CustomizeLaunchersActivity.this, R.layout.add_list_item);
            mAdapter.add(getString(R.string.custom_icon_select_picture));
            mAdapter.add(getString(R.string.custom_icon_icon_packs));
            if (SpecialIconStore.hasBitmap(CustomizeLaunchersActivity.this, mAppClicked.getComponentName(), SpecialIconStore.IconType.Custom)) {
                mAdapter.add(getString(R.string.custom_icon_clear_icon));
            }

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
            pickingIcon = true;
            switch (which) {
                case 0:
                    //Select icon
                    Intent pickerIntent=new Intent(Intent.ACTION_PICK);
                    pickerIntent.setType("image/*");
                    startActivityForResult(Intent.createChooser(pickerIntent, getString(R.string.custom_icon_select_icon_type)), PICK_CUSTOM_ICON);
                    break;
                case 1:
                    Intent packIntent=new Intent(CustomizeLaunchersActivity.this, ChooseIconFromPackActivity.class);
                    startActivityForResult(packIntent, PICK_FROM_ICON_PACK);
                    break;
                case 2:
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
