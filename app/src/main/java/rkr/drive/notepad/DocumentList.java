package rkr.drive.notepad;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.OpenFileActivityBuilder;


public class DocumentList extends BaseDriveActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_document_list);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentSender intent = Drive.DriveApi.newOpenFileActivityBuilder().build(mGoogleApiClient);

                try {
                    startIntentSenderForResult(intent, REQUEST_CODE_OPEN, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    Log.w("DocumentList", "Unable to send intent", e);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_OPEN:
                if (resultCode == RESULT_OK) {
                    Log.d("DocumentList", "In Open activity result");
                    DriveId mCurrentDriveId = data.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                    OpenFile(mCurrentDriveId);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void OpenFile(DriveId mCurrentDriveId) {
        Log.d("DocumentList", "Retrieving...");

        Intent intent = new Intent(this, TextEditor.class);
        intent.putExtra(INTENT_DRIVE_ID, mCurrentDriveId);
        startActivity(intent);


        /*DriveFile file = mCurrentDriveId.asDriveFile();

        //final PendingResult<DriveResource.MetadataResult> metadataResult = file.getMetadata(mGoogleApiClient);
        final PendingResult<DriveApi.DriveContentsResult> contentsResult = file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null);

        contentsResult.setResultCallback(onContentsCallback);*/
    }

    /*ResultCallback<DriveApi.DriveContentsResult> onContentsCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    Log.d("DocumentList", "In file open");
                    String contents = Utils.readFromInputStream(result.getDriveContents().getInputStream());
                    TextView backGroundText = (TextView) findViewById(R.id.text_background);
                    backGroundText.setText(contents);
                }
            };*/

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);
        Toast.makeText(this, "Connected", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        super.onConnectionFailed(result);
        Toast.makeText(this, "Failed to connect", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_document_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
