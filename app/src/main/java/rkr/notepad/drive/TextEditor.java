package rkr.notepad.drive;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;

import java.util.Date;

import rkr.notepad.drive.database.File;
import rkr.notepad.drive.database.FileHelper;

public class TextEditor extends BaseDriveActivity implements
        FileRenameFragment.EditNameDialogListener {

    private File mFile;
    public static final String INTENT_FILE_ID = "FILE_ID";
    public static final String INTENT_DRIVE_ID = "DRIVE_ID";

    private FileHelper filesHelper;
    private ProgressDialog pd;
    private Handler handler = new Handler();
    private boolean saveScheduled = false;

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
                FragmentManager fm = getFragmentManager();
                FileRenameFragment alertDialog = FileRenameFragment.newInstance(mFile);
                alertDialog.show(fm, "fragment_alert");
            }
        });

        filesHelper = new FileHelper(this);

        pd = new ProgressDialog(this);
        pd.setTitle("Downloading file...");
        pd.setMessage("Please wait.");
        pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });


        Intent intent = getIntent();
        if (intent.hasExtra(INTENT_FILE_ID) ||
                intent.hasExtra(INTENT_DRIVE_ID)) {
            pd.show();
            return;
        }

        //New file is being created
        getSupportActionBar().setTitle("Untitled document.txt");
        mFile = new File();
        mFile.fileName = "Untitled document.txt";

        RegisterEditWatcher();
    }

    @Override
    protected void onPause() {
        //only save if unsaved changes are made
        if (saveScheduled)
            SaveFile();
        super.onPause();
    }

    private void RegisterEditWatcher() {
        EditText textField = (EditText) findViewById(R.id.editText);
        textField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                //Log.d("Text Editor", "Text has changed");
                if (saveScheduled)
                    return;

                saveScheduled = true;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        saveScheduled = false;
                        SaveFile();
                    }
                }, 1000 * 5);


            }
        });
    }

    @Override
    protected void ServiceConnected () {
        //File already loaded
        if (mFile != null)
            return;

        Log.i("TextEditor", "Loading content after connection");

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
                finish();
                return true;
            case R.id.rename:
                FragmentManager fm = getFragmentManager();
                FileRenameFragment alertDialog = FileRenameFragment.newInstance(mFile);
                alertDialog.show(fm, "fragment_alert");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void OpenDriveFile(final DriveId driveId){
        PendingResult<DriveResource.MetadataResult> metadataResult = driveId.asDriveFile().getMetadata(driveService.getApiClient());
        metadataResult.setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
            @Override
            public void onResult(final DriveResource.MetadataResult metadataResult) {
                Log.d("TextEditor", "In Open Drive File");
                String fileMime = metadataResult.getMetadata().getMimeType();
                long fileSize = metadataResult.getMetadata().getFileSize();

                String errors = GetFileOpenErrors(fileMime, fileSize);
                if (!errors.isEmpty()){
                    new AlertDialog.Builder(TextEditor.this)
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
        file.driveId = driveId;
        file = filesHelper.SaveItem(file);
        return file;
    }

    private void OpenFile(final File file) {
        mFile = file;

        driveService.DownloadFile(file.driveId, new DriveService.OnFileDownloaded() {
            @Override
            public void onFileDownloaded(String contents) {
                if (contents == null) {
                    new AlertDialog.Builder(TextEditor.this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Invalid file")
                            .setMessage("Document has no content")
                            .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .show();
                    FileHelper fileHelper = new FileHelper(getApplicationContext());
                    fileHelper.DeleteItem(file);
                    return;
                }

                EditText textField = (EditText) findViewById(R.id.editText);
                textField.setText(contents);
                pd.dismiss();

                UpdateFileViewDate();
                RegisterEditWatcher();
                assert getSupportActionBar() != null;
                getSupportActionBar().setTitle(mFile.fileName);
            }
        });
    }

    private void UploadDriveFile() {
        Log.d("DocumentList", "In file save");

        EditText textField = (EditText) findViewById(R.id.editText);
        String contents = textField.getText().toString();
        driveService.UploadFile(mFile.driveId, contents, mFile.fileName, new DriveService.OnFileUploaded() {
            @Override
            public void onFileUploaded(DriveId driveId) {
                if (driveId == null) {
                    Toast.makeText(TextEditor.this, "File save failed", Toast.LENGTH_SHORT).show();
                    return;
                }
                //New file to save
                if (mFile.driveId == null) {
                    mFile.driveId = driveId;
                    mFile = filesHelper.SaveItem(mFile);
                }

                Log.d("DocumentList", "File saved");
            }
        });
    }

    private String GetFileOpenErrors(String fileMime, long fileSize){
        Log.d("TextEditor", "Mime type: " + fileMime);
        Log.d("TextEditor", "File size: " + Long.toString(fileSize));

        String errors = "";
        if (fileSize > 128 * 1024)
            errors += String.format("File size is too big: %d KB.\n", fileSize / 1024);
        if (!fileMime.equals("text/plain"))
            errors += String.format("File format may be unsupported: %s.\n", fileMime);
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

    private void SaveFile() {
        mFile.dateViewed = new Date();
        mFile = filesHelper.SaveItem(mFile);

        UploadDriveFile();
    }

    @Override
    public void onFinishEditDialog(File file) {
        mFile.fileName = file.fileName;
        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle(mFile.fileName);
        SaveFile();
    }

}
