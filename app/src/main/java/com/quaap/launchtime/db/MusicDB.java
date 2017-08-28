package com.quaap.launchtime.db;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by tom on 8/27/17.
 */

public class MusicDB {
    ContentResolver mCr;

    String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION
    };

    public MusicDB(ContentResolver cr) {
        mCr = cr;
    }


    public void getMusic(String matching) {
       // ContentResolver cr = getActivity().getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0 and " + MediaStore.Audio.Media.TITLE + matching;
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cur = mCr.query(uri, projection, selection, null, sortOrder);

        if(cur != null) {

            while(cur.moveToNext()) {
                Map<String,Object> row = new LinkedHashMap<>();

                for (String col: projection) {
                    row.put(col, cur.getString(cur.getColumnIndex(col)));
                }

                //String data = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));

            }


            cur.close();
        }


    }
}
