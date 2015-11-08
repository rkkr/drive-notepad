package rkr.drive.notepad;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;

import java.util.Date;

import rkr.drive.notepad.database.File;
import rkr.drive.notepad.database.FileHelper;

public class TextEditor extends BaseDriveActivity {

    private DriveId driveId;
    private String fileContents;
    private String fileName;
    private String fileMime;
    private long fileSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_editor);

        Intent intent = getIntent();
        driveId = intent.getParcelableExtra(INTENT_DRIVE_ID);
        PendingResult<DriveResource.MetadataResult> metadataResult = driveId.asDriveFile().getMetadata(mGoogleApiClient);
        metadataResult.setResultCallback(onMetadataCallback);
    }

    ResultCallback<DriveApi.DriveContentsResult> onContentsCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    Log.d("DocumentList", "In file open");
                    fileContents = Utils.readFromInputStream(result.getDriveContents().getInputStream());

                    EditText textField = (EditText) findViewById(R.id.editText);
                    textField.setText(fileContents);

                    AddFileToHistory();
                }
            };

    ResultCallback<DriveResource.MetadataResult> onMetadataCallback =
            new ResultCallback<DriveResource.MetadataResult>() {
                @Override
                public void onResult(DriveResource.MetadataResult result) {
                    Log.d("DocumentList", "In file metadata");

                    fileName = result.getMetadata().getOriginalFilename();
                    fileSize = result.getMetadata().getFileSize();
                    fileMime = result.getMetadata().getMimeType();

                    if (IsFileSane())
                    {
                        PendingResult<DriveApi.DriveContentsResult> contentsResult = driveId.asDriveFile().open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null);
                        contentsResult.setResultCallback(onContentsCallback);
                    }
                }
            };

    private boolean IsFileSane()
    {
        Log.d("TextEditor", "Mime type: " + fileMime);
        Log.d("TextEditor", "File size: " + Long.toString(fileSize));

        String errors = "";
        if (fileSize > 8 * 1024 * 1024)
            errors += String.format("File size is: %d MB.\n", fileSize / 1024 / 1024);
        if (fileMime != "text/plain" &&
                !fileMime.contains("xml"))
            errors += String.format("File format is: %s.\n", fileMime);

        if (errors != "") {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Invalid file")
                    .setMessage(errors + "Are you sure you want to open?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PendingResult<DriveApi.DriveContentsResult> contentsResult = driveId.asDriveFile().open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null);
                            contentsResult.setResultCallback(onContentsCallback);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
            return false;
        }

        return true;
    }

    private boolean AddFileToHistory()
    {
        if (driveId == null || fileName == null || fileContents == null)
            return false;

        FileHelper filesHelper = new FileHelper(getApplicationContext());
        File file = new File();
        file.contents = fileContents;
        file.driveId = driveId;
        file.fileName = fileName;
        file.fileSize = fileSize;
        file.lastUsed = new Date();

        if (filesHelper.ItemExists(file))
            filesHelper.UpdateItem(file);
        else
            filesHelper.AddItem(file);
        return true;
    }

    public static void OpenFile(Context context, DriveId mCurrentDriveId) {
        Log.d("DocumentList", "Retrieving...");

        Intent intent = new Intent(context, TextEditor.class);
        intent.putExtra(INTENT_DRIVE_ID, mCurrentDriveId);
        context.startActivity(intent);
    }

}
