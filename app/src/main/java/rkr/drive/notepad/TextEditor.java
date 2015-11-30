package rkr.drive.notepad;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.MetadataChangeSet;

import java.util.Date;

import rkr.drive.notepad.database.File;
import rkr.drive.notepad.database.FileHelper;

public class TextEditor extends BaseDriveActivity implements FileRenameFragment.EditNameDialogListener {

    private File file = new File();
    public static final String INTENT_FILE_ID = "FILE_ID";
    private FileHelper filesHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_editor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_textEditor);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getSupportFragmentManager();
                FileRenameFragment alertDialog = FileRenameFragment.newInstance(file.fileName);
                alertDialog.show(fm, "fragment_alert");
            }
        });
        getSupportActionBar().setTitle("Untitled document");

        filesHelper = new FileHelper(this);

        Intent intent = getIntent();
        if (intent.hasExtra(INTENT_FILE_ID)) {
            long fileId = intent.getLongExtra(INTENT_FILE_ID, -1);
            file = filesHelper.GetItem(fileId);
            getSupportActionBar().setTitle(file.fileName);

            EditText textField = (EditText) findViewById(R.id.editText);
            textField.setText(file.contents);

            UpdateFileModificationDate();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_text_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_save:
                SaveFile();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*



    */

    private void UpdateFileModificationDate()
    {
        File file = new File();
        file.id = this.file.id;
        file.dateViewed = new Date();

        filesHelper.SaveItem(file);
    }

    private void SaveFile()
    {
        EditText textField = (EditText) findViewById(R.id.editText);
        file.contents = textField.getText().toString();
        file.fileSize = file.contents.length();
        file.dateViewed = new Date();
        file.dateModified = new Date();

        filesHelper.SaveItem(file);

        DocumentList.UploadDriveFile(file);
    }

    @Override
    public void onFinishEditDialog(String inputText) {
        file.fileName = inputText;
        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle(inputText);
    }

}
