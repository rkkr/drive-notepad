package rkr.drive.notepad;

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
    private long fileSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_editor);

        Intent intent = getIntent();
        driveId = intent.getParcelableExtra(INTENT_DRIVE_ID);
        DriveFile file = driveId.asDriveFile();
        final PendingResult<DriveResource.MetadataResult> metadataResult = file.getMetadata(mGoogleApiClient);
        final PendingResult<DriveApi.DriveContentsResult> contentsResult = file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null);

        contentsResult.setResultCallback(onContentsCallback);
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

                    TryAddFileToHistory();
                }
            };

    ResultCallback<DriveResource.MetadataResult> onMetadataCallback =
            new ResultCallback<DriveResource.MetadataResult>() {
                @Override
                public void onResult(DriveResource.MetadataResult result) {
                    Log.d("DocumentList", "In file metadata");

                    fileName = result.getMetadata().getOriginalFilename();
                    fileSize = result.getMetadata().getFileSize();

                    TryAddFileToHistory();
                }
            };

    private boolean TryAddFileToHistory()
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

        filesHelper.AddItem(file);
        return true;
    }
}
