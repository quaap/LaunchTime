package com.quaap.launchtime.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;

/**
 * Created by tom on 1/16/17.
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
public class IconCache {


    public static String makeSafeName(String name) {

        try {
            byte[] inbytes = name.getBytes("UTF-8");

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] shabytes = md.digest(inbytes);

            StringBuffer hexString = new StringBuffer();
            for (int i=0;i<shabytes.length;i++) {
                String hex=Integer.toHexString(0xff & shabytes[i]);
                if(hex.length()==1) hexString.append('0');
                hexString.append(hex);
            }

            //Log.d("Icon", "name " + name + " => " + hexString);
            return hexString.toString() + ".png";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //return "icon-" + name.replaceAll("(\\.\\.|[\\\\//$&():=#])+", "_");
    }

    public static void saveBitmap(Context context, String name, Bitmap bitmap) {

        try  {
            name = makeSafeName(name);
            FileOutputStream fos = context.openFileOutput(name, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG,100,fos);
            fos.close();
            Log.d("IconCache", "Saved icon " + name);
        } catch (IOException e) {
            Log.e("IconCache", e.getMessage(), e);
        }
    }

    public static  Bitmap loadBitmap(Context context, String name) {
        Bitmap bitmap = null;
        try  {
//            for (String fn: context.fileList()) {
//                Log.d("IconCache", " I see file " + fn);
//            }
            name = makeSafeName(name);
            if (fileExists(context, name)) {
                FileInputStream fis = context.openFileInput(name);
                bitmap = BitmapFactory.decodeStream(fis);
                fis.close();
                Log.d("IconCache", "Got icon " + name);
            }

        } catch (IOException e) {
            Log.e("IconCache", e.getMessage(), e);
        }
        return bitmap;
    }

    public static boolean fileExists(Context context, String filename) {
        File file = context.getFileStreamPath(filename);
        if(file == null || !file.exists()) {
            return false;
        }
        return true;
    }


}
