package com.quaap.launchtime;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.quaap.launchtime.components.FsTools;
import com.quaap.launchtime.db.DB;

import java.io.File;
import java.util.regex.Pattern;

public class BackupActivity extends Activity {

    private String selectedBackup;
    private boolean selected;

    LinearLayout backupsLayout;
    Button newbk;
    Button restorebk;
    Button delbk;
    Button savebk;
    Button loadbk;
    TextView showExt;
    View btnbar;
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

        btnbar = findViewById(R.id.bak_ext_btns);

        showExt = (TextView)findViewById(R.id.bak_show_ext_btns);
        showExt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showHideExternalButtons();
            }
        });

        showExternalButtons(true);
    }
    private void showHideExternalButtons() {
        showExternalButtons(btnbar.getVisibility() == View.VISIBLE);
    }

    private void showExternalButtons(boolean show) {

        if (show) {
            showExt.setText(R.string.show_extfs_opts);
            btnbar.setVisibility(View.GONE);
        } else {
            showExt.setText(R.string.hide_extfs_opts);
            btnbar.setVisibility(View.VISIBLE);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        newbk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptNew();

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
                confirmDelete();
            }
        });

        savebk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptExtDir();
            }
        });

        loadbk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptRestoreExtFile();
            }
        });

        db = ((GlobState)getApplicationContext()).getDB();

        populateBackupsList();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        finish();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_HOME) {
            setResult(RESULT_OK);
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }


    private void populateBackupsList() {
        backupsLayout.removeAllViews();
        RadioGroup baks = new RadioGroup(this);

        makeRadioButton(baks, getString(R.string.no_backup_selected), false).setChecked(true);

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
        tag.setHint(R.string.opt_name);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.new_backup)
                .setView(tag)
                .setPositiveButton(R.string.take_backup, new DialogInterface.OnClickListener() {
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
            message = getString(R.string.backup_success);
        } else {
            message = getString(R.string.backup_failed);
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        populateBackupsList();
    }

    private void promptRestoreExtFile() {
        if (checkStorageAccess()) {
            new FsTools(this).selectExternalLocation(new FsTools.SelectionMadeListener() {
                @Override
                public void selected(File selection) {
                    confirmRestoreFile(selection);
                }
            }, getString(R.string.select_file_restore), false, Pattern.quote(DB.BK_PRE) + ".+");
        }
    }

    private void restore() {
        if (selected) {


            if (db.hasBackup(selectedBackup)) {
                File backupFile = db.pullBackup(selectedBackup);
                confirmRestoreFile(backupFile);
            } else{
                String  message = getString(R.string.no_backup) + selectedBackup + "\"";
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }

        }
    }

    private void confirmRestoreFile (final File backupFile) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_restore1)
                    .setMessage(R.string.confirm_restore2)
                    .setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String message = restoreFromFile(backupFile);
                            Toast.makeText(BackupActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    }).setNegativeButton(R.string.cancel, null);
            builder.show();

    }
    @NonNull
    private String restoreFromFile(File backupFile) {
        String message;
        File prev = db.backup("Before restore");
        if (db.restoreFullpathBackup(backupFile)) {
            message = getString(R.string.restore_successful);

            backupsLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent mainIntent = new Intent(BackupActivity.this, MainActivity.class);
                    mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getApplicationContext().startActivity(mainIntent);
                    BackupActivity.this.finish();

                }
            }, 1000);

        } else {
            message = getString(R.string.restore_failed_rollback);
            if (!db.restoreFullpathBackup(prev)) {
                message = getString(R.string.restore_failed2);
            }

            populateBackupsList();
        }
        return message;
    }

    private void confirmDelete() {
        if (selected) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_delete1)
                    .setMessage(getString(R.string.confirm_delete2) + selectedBackup + "'?")
                    .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
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
                message = getString(R.string.delete_success);
            } else {
                message = getString(R.string.delete_failed);
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            populateBackupsList();
        }
    }

    private void promptExtDir() {
        if (checkStorageAccess()) {
            new FsTools(this).selectExternalLocation(new FsTools.SelectionMadeListener() {
                @Override
                public void selected(File selection) {
                    String message;
                    if (FsTools.copyFileToDir(db.pullBackup(selectedBackup), selection)!=null) {
                        message = getString(R.string.copy_sucess);
                    } else {
                        message = getString(R.string.copy_failed);
                    }
                    Toast.makeText(BackupActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }, getString(R.string.select_save_location), true);
        }
    }


    private boolean checkStorageAccess() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);
            return false;
        }
        return true;
    }

    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 4334;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this, R.string.perm_granted, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, R.string.perm_not_granted, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

}
