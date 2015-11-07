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

import rkr.drive.notepad.database.FilesHelper;

public class TextEditor extends BaseDriveActivity {

    private DriveId mCurrentDriveId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_editor);

        Intent intent = getIntent();
        mCurrentDriveId = intent.getParcelableExtra(INTENT_DRIVE_ID);
        DriveFile file = mCurrentDriveId.asDriveFile();
        //final PendingResult<DriveResource.MetadataResult> metadataResult = file.getMetadata(mGoogleApiClient);
        final PendingResult<DriveApi.DriveContentsResult> contentsResult = file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null);

        contentsResult.setResultCallback(onContentsCallback);
    }




    ResultCallback<DriveApi.DriveContentsResult> onContentsCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    Log.d("DocumentList", "In file open");
                    String contents = Utils.readFromInputStream(result.getDriveContents().getInputStream());
                    EditText textField = (EditText) findViewById(R.id.editText);
                    textField.setText(contents);

                    FilesHelper filesHelper = new FilesHelper(getApplicationContext());
                    filesHelper.AddItem(mCurrentDriveId, contents);
                }
            };
}
