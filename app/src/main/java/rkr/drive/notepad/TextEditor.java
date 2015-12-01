package rkr.drive.notepad;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;

import java.util.Date;

import rkr.drive.notepad.database.File;
import rkr.drive.notepad.database.FileHelper;

public class TextEditor extends BaseDriveActivity implements FileRenameFragment.EditNameDialogListener {

    private File mFile = new File();
    public static final String INTENT_FILE_ID = "FILE_ID";
    public static final String INTENT_DRIVE_ID = "DRIVE_ID";
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
                FileRenameFragment alertDialog = FileRenameFragment.newInstance(mFile);
                alertDialog.show(fm, "fragment_alert");
            }
        });


        filesHelper = new FileHelper(this);

        Intent intent = getIntent();
        //File opened from history
        if (intent.hasExtra(INTENT_FILE_ID)) {
            long fileId = intent.getLongExtra(INTENT_FILE_ID, -1);
            File file = filesHelper.GetItem(fileId);
            OpenFile(file);
            return;
        }
        //File opened from drive dialog
        if (intent.hasExtra(INTENT_DRIVE_ID)) {
            DriveId driveId = intent.getParcelableExtra(INTENT_DRIVE_ID);
            OpenDriveFile(driveId);
            return;
        }
        //New file is being created
        getSupportActionBar().setTitle("Untitled document");
        mFile = new File();
        mFile.fileName = "Untitled document";
        mFile.contents = "";

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

    private void OpenDriveFile(final DriveId driveId){
        PendingResult<DriveResource.MetadataResult> metadataResult = driveId.asDriveFile().getMetadata(mGoogleApiClient);
        metadataResult.setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
            @Override
            public void onResult(final DriveResource.MetadataResult metadataResult) {
                Log.d("TextEditor", "In Open Drive File");
                String fileMime = metadataResult.getMetadata().getMimeType();
                long fileSize = metadataResult.getMetadata().getFileSize();

                String errors = GetFileOpenErrors(fileMime, fileSize);
                if (!errors.isEmpty()){
                    new android.support.v7.app.AlertDialog.Builder(getApplicationContext())
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Invalid file")
                            .setMessage(errors + "Are you sure you want to open?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    File file = SaveDriveFileToDB(driveId, metadataResult.getMetadata());
                                    OpenFile(file);
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .show();
                }
                else{
                    File file = SaveDriveFileToDB(driveId, metadataResult.getMetadata());
                    OpenFile(file);
                }
            }
        });
    }

    private File SaveDriveFileToDB(DriveId driveId, Metadata metadata){
        File file = filesHelper.GetItem(driveId);
        //File not in history
        if (file == null)
            file = new File();

        //file.fileName = metadata.getOriginalFilename();
        file.fileName = metadata.getTitle();
        file.fileSize = metadata.getFileSize();
        file.driveId = driveId;
        file.dateModified = metadata.getModifiedDate();
        file.id = filesHelper.SaveItem(file);
        return file;
    }

    private void OpenFile(File file) {
        mFile = file;
        final FileHelper fileHelper = new FileHelper(getApplicationContext());
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Downloading drive file...");
        pd.setMessage("Please wait.");
        pd.setCancelable(false);
        pd.show();

        PendingResult<DriveApi.DriveContentsResult> contentsResult = file.driveId.asDriveFile().open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null);
        contentsResult.setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
            @Override
            public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
                Log.d("DocumentList", "In file open");
                String contents = Utils.readFromInputStream(driveContentsResult.getDriveContents().getInputStream());

                EditText textField = (EditText) findViewById(R.id.editText);
                textField.setText(contents);
                pd.dismiss();

                UpdateFileViewDate();
                assert getSupportActionBar() != null;
                getSupportActionBar().setTitle(mFile.fileName);
            }
        });

        //TODO: check if file name has changed
    }

    private void UploadDriveFile() {
        Log.d("DocumentList", "In file save");

        if (mFile.driveId != null) {
            //File exists on google drive
            mFile.driveId.asDriveFile().open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null).setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
                    EditText textField = (EditText) findViewById(R.id.editText);
                    String contents = textField.getText().toString();
                    Utils.writeToOutputStream(driveContentsResult.getDriveContents().getOutputStream(), contents);

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(mFile.fileName)
                            .setPinned(true)
                            .build();

                    driveContentsResult.getDriveContents().commit(mGoogleApiClient, changeSet).setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status result) {
                            if (result.getStatus().isSuccess()) {
                                Log.d("DocumentList", "File saved");
                            }
                        }
                    });
                }
            });
        } else {
            //This is a new file
            Drive.DriveApi.newDriveContents(mGoogleApiClient).setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
                    EditText textField = (EditText) findViewById(R.id.editText);
                    String contents = textField.getText().toString();
                    Utils.writeToOutputStream(driveContentsResult.getDriveContents().getOutputStream(), contents);

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(mFile.fileName)
                            .setMimeType("text/plain")
                            .setPinned(true)
                            .build();

                    Drive.DriveApi.getRootFolder(mGoogleApiClient)
                            .createFile(mGoogleApiClient, changeSet, driveContentsResult.getDriveContents())
                            .setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {
                                @Override
                                public void onResult(DriveFolder.DriveFileResult driveFileResult) {
                                    if (driveFileResult.getStatus().isSuccess()) {
                                        mFile.driveId = driveFileResult.getDriveFile().getDriveId();
                                        filesHelper.SaveItem(mFile);

                                        Log.d("DocumentList", "File saved");
                                    }
                                }
                            });
                }
            });
        }
    }

    private String GetFileOpenErrors(String fileMime, long fileSize){
        Log.d("TextEditor", "Mime type: " + fileMime);
        Log.d("TextEditor", "File size: " + Long.toString(fileSize));

        String errors = "";
        if (fileSize > 8 * 1024 * 1024)
            errors += String.format("File size is: %d MB.\n", fileSize / 1024 / 1024);
        if (!fileMime.equals("text/plain") &&
                !fileMime.contains("xml"))
            errors += String.format("File format is: %s.\n", fileMime);
        return errors;
    }


    private void UpdateFileViewDate()
    {
        //File is not yet saved
        if (mFile.id == -1)
            return;

        File file = new File();
        file.id = mFile.id;
        file.dateViewed = new Date();

        filesHelper.SaveItem(file);
    }

    private void SaveFile()
    {

        //mFile.contents = textField.getText().toString();
        mFile.contents = "";
        mFile.fileSize = mFile.contents.length();
        mFile.dateViewed = new Date();
        //mFile.dateModified = new Date();

        filesHelper.SaveItem(mFile);

        UploadDriveFile();
    }

    @Override
    public void onFinishEditDialog(File file) {
        mFile.fileName = file.fileName;
        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle(mFile.fileName);
    }

}
