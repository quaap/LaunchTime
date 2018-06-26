package com.quaap.launchtime;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.util.Log;

import com.quaap.launchtime.apps.AppLauncher;
import com.quaap.launchtime.components.IconsHandler;

public class PinShortcutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Pinshort", "onCreate");

        try {
            if (Build.VERSION.SDK_INT >= 26) {
                Intent intent = getIntent();
                //Log.d("Pinshort", "Intent " + intent);
                if (intent == null) {
                    return;
                }
                LauncherApps launcherApps =  this.getSystemService(LauncherApps.class);
                if (launcherApps==null) return;

                LauncherApps.PinItemRequest request = launcherApps.getPinItemRequest(intent);

                if (request == null) {
                    return;
                }

                ShortcutReceiver shrecv =  GlobState.getShortcutReceiver(this);
                if (shrecv==null) {
                    return;
                }

                ShortcutInfo si = request.getShortcutInfo();
                if (si==null) {
                    return;
                }
                Drawable iconDrawable = launcherApps.getShortcutIconDrawable(si, 0);

                Bitmap icon = null;

                if (iconDrawable!=null) {
                    icon = IconsHandler.drawableToBitmap(iconDrawable);
                }

                String label = null;
                if (si.getShortLabel()!=null) {
                    label = si.getShortLabel().toString();

                    CharSequence longlabel = si.getLongLabel();
                    if (longlabel!=null) {
                        if (longlabel.toString().startsWith(label) ){
                            label = longlabel.toString();
                        } else {
                            label += " " + longlabel;
                        }
                    }


                }

                shrecv.addOreoLink(this, si.getId(), si.getPackage(), label, icon);

                request.accept();

            }

        } finally {

            finish();
        }
    }
}
