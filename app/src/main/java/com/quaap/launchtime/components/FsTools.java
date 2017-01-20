package com.quaap.launchtime.components;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.util.Log;

import com.quaap.launchtime.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by tom on 1/20/17.
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
public class FsTools {

    private Context mContext;

    public FsTools(Context context) {
        mContext = context;
    }

    private String [] listDirsInDir(File extdir) {

        FilenameFilter filter = new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                File sel = new File(dir, filename);
                return sel.isDirectory();
            }

        };

        List<String> files = new ArrayList<String>(Arrays.asList(extdir.list(filter)));

        if (extdir.getParent()!=null && !extdir.equals(Environment.getExternalStorageDirectory())) {
            files.add(0, "..");
        }

        Collections.sort(files, new Comparator<String>() {
            @Override
            public int compare(String s, String t1) {
                return s.compareToIgnoreCase(t1);
            }
        });


        for (String x: files) {
            Log.d("DD", x);
        }
        return files.toArray(new String[0]);

    }

    public void selectExternalDir(final SelectionMadeListener listener) {
        selectExternalDir(listener, null);
    }


    public void selectExternalDir(final SelectionMadeListener listener, String startdir) {
        final File currentDir = startdir==null ? Environment.getExternalStorageDirectory() :  new File(startdir);

        final String [] items = listDirsInDir(currentDir);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        builder.setTitle("Select a location\n" + currentDir.getPath());
        //builder.setMessage(currentDir.getPath());
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                selectExternalDir(listener,new File(currentDir,items[i]).getPath());
            }
        });
        builder.setPositiveButton("Select current", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                listener.selected(currentDir);
            }
        }).setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    public interface SelectionMadeListener {
        void selected(File selection);
    }

    public static File copyFileToDir(File srcFile, File destDir){
        if (!destDir.isDirectory()) throw new IllegalArgumentException("Destination must be a directory");

        return copyFile(srcFile, new File(destDir,srcFile.getName()));
    }

    public static File copyFile(File srcFile, File destFile){
        if (destFile.exists() && !destFile.isFile()) throw new IllegalArgumentException("Destination must be a normal file");
        try {

            FileInputStream fis = new FileInputStream(srcFile);

            try {

                OutputStream output = new FileOutputStream(destFile);

                try {
                    // Transfer bytes from the inputfile to the outputfile
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        output.write(buffer, 0, length);
                    }

                    output.flush();
                    return destFile;
                } finally {
                    output.close();
                }
            } finally {
                fis.close();
            }

        } catch (IOException e) {
            Log.e("DB", "Copy failed", e);
        }
        return null;
    }

}
