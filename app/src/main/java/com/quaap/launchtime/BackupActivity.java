package com.quaap.launchtime;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.icu.text.DateFormat;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.quaap.launchtime.db.DB;

public class BackupActivity extends Activity {

    private String selectedBackup;
    private boolean selected;

    LinearLayout backupsLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        backupsLayout = (LinearLayout) findViewById(R.id.bakups_list);

        Button newbk = (Button)findViewById(R.id.btn_newbak);
        Button restorebk = (Button)findViewById(R.id.btn_restorebak);
        Button delbk = (Button)findViewById(R.id.btn_deletebak);

        newbk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newBackup();
                populateBackupsList();
            }
        });

        restorebk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                restore();
            }
        });

        delbk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delete();
                populateBackupsList();
            }
        });
        populateBackupsList();
    }

    private void populateBackupsList() {
        backupsLayout.removeAllViews();
        RadioGroup baks = new RadioGroup(this);

        makeRadioButton(baks, "None selected", false);

        for(final String bk: DB.listBackups(this)) {

            makeRadioButton(baks, bk, true);
        }
        backupsLayout.addView(baks);
    }

    private void makeRadioButton(RadioGroup baks, final String bk, boolean item) {
        RadioButton bkb = new RadioButton(this);
        bkb.setText(bk);
        bkb.setTag(item);

        bkb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                selectedBackup = bk;
                selected = (boolean) compoundButton.getTag();
            }
        });

        baks.addView(bkb);
    }

    private void newBackup() {
        DB db = ((GlobState)getApplicationContext()).getDB();

        String message;
        if (db.backup(this)) {
            message = "Backup successful!";
        } else {
            message = "Backup failed";
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

    }

    private void restore() {
        if (selected) {
            DB db = ((GlobState) getApplicationContext()).getDB();
            String message;
            if (db.restoreBackup(this, selectedBackup)) {
                message = "Restore successful! Will now restart.";

                backupsLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent mainIntent = new Intent(BackupActivity.this, MainActivity.class);
                        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplicationContext().startActivity(mainIntent);

                    }
                },1000);

//                Intent mainIntent = new Intent(this, MainActivity.class);
//                int mPendingIntentId = 34233;
//                PendingIntent mPendingIntent = PendingIntent.getActivity(this, mPendingIntentId, mainIntent, PendingIntent.FLAG_CANCEL_CURRENT);
//                AlarmManager mgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
//                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
//                android.os.Process.killProcess(android.os.Process.myPid());

            } else {
                message = "Restore failed";
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void delete() {
        if (selected) {
            DB db = ((GlobState) getApplicationContext()).getDB();
            String message;
            if (db.deleteBackup(this, selectedBackup)) {
                message = "Delete successful!";
            } else {
                message = "Delete failed";
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }
}
