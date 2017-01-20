package com.quaap.launchtime;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.quaap.launchtime.db.DB;

import java.io.File;

public class BackupActivity extends Activity {

    private String selectedBackup;
    private boolean selected;

    LinearLayout backupsLayout;
    Button newbk;
    Button restorebk;
    Button delbk;
    Button savebk;
    Button loadbk;
    DB db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        backupsLayout = (LinearLayout) findViewById(R.id.bakups_list);

        newbk = (Button)findViewById(R.id.btn_newbak);
        restorebk = (Button)findViewById(R.id.btn_restorebak);
        delbk = (Button)findViewById(R.id.btn_deletebak);
        savebk = (Button)findViewById(R.id.btn_savebak);
        loadbk = (Button)findViewById(R.id.btn_loadbak);

        newbk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptNew();

            }
        });

        restorebk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmRestore();
            }
        });

        delbk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmDelete();
            }
        });

        savebk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        db = ((GlobState)getApplicationContext()).getDB();

        populateBackupsList();
    }


    private void populateBackupsList() {
        backupsLayout.removeAllViews();
        RadioGroup baks = new RadioGroup(this);

        makeRadioButton(baks, "None selected", false).setChecked(true);

        for(final String bk: db.listBackups()) {

            makeRadioButton(baks, bk, true);
        }
        backupsLayout.addView(baks);
    }

    private RadioButton makeRadioButton(RadioGroup baks, final String bk, final boolean item) {
        RadioButton bkb = new RadioButton(this);
        bkb.setText(bk);


        bkb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    selectedBackup = bk;
                    selected = item;
                    Log.d("backuppage", "selected = " + selectedBackup);
                    backupSelected(selected);
                }
            }
        });

        baks.addView(bkb);
        return bkb;
    }


    private void backupSelected(boolean isselected) {
        restorebk.setEnabled(isselected);
        delbk.setEnabled(isselected);
        savebk.setEnabled(isselected);
    }

    private void promptNew() {

        final EditText tag = new EditText(this);
        tag.setHint("Optional name");

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("New backup")
                .setView(tag)
                .setPositiveButton("Take backup", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        newBackup(tag.getText().toString());
                    }
                }).setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void newBackup(String optionalName) {


        String message;
        if (db.backup(optionalName)!=null) {
            message = "Backup successful!";
        } else {
            message = "Backup failed";
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        populateBackupsList();
    }

    private void confirmRestore() {
        if (selected) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("Restore?")
                    .setMessage("If you restore, all your changes since back up '" + selectedBackup + "' will be lost.")
                    .setPositiveButton("Restore", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            restore();
                        }
                    }).setNegativeButton(R.string.cancel, null);
            builder.show();
        }
    }

    private void restore() {
        if (selected) {
            String message;

            if (db.hasBackup(selectedBackup)) {
                File prev = db.backup("Before restore");
                if (db.restoreBackup(selectedBackup)) {
                    message = "Restore successful! Will now restart.";

                    backupsLayout.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent mainIntent = new Intent(BackupActivity.this, MainActivity.class);
                            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getApplicationContext().startActivity(mainIntent);

                        }
                    }, 1000);

                } else {
                    message = "Restore failed. Rolling back";
                    if (!db.restoreFullpathBackup(prev)) {
                        message = "Restore failed. Database state unknown.";
                    }

                    populateBackupsList();
                }
            } else{
                message = "No such backup \"" + selectedBackup + "\"";
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDelete() {
        if (selected) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("Delete?")
                    .setMessage("Are you sure you want to delete the back up '" + selectedBackup + "'?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            delete();
                        }
                    }).setNegativeButton(R.string.cancel, null);
            builder.show();
        }
    }

    private void delete() {
        if (selected) {

            String message;
            if (db.deleteBackup(selectedBackup)) {
                message = "Delete successful!";
            } else {
                message = "Delete failed";
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            populateBackupsList();
        }
    }


}
