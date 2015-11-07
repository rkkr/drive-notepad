package rkr.drive.notepad;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
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

import java.util.ArrayList;
import java.util.List;

import rkr.drive.notepad.database.File;
import rkr.drive.notepad.database.FileHelper;


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

        /*Handler h = new Handler();
        h.post(new Runnable() {
            @Override
            public void run() {
                LoadHistory();
            }
        });*/
        LoadHistory();
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
    }

    private void LoadHistory()
    {
        FileHelper fileHelper = new FileHelper(this);
        List<File> files = fileHelper.LoadItems();
        ListView fileList = (ListView) findViewById(R.id.file_history);

        /*ArrayAdapter<File> arrayAdapter = new ArrayAdapter<File>(
                this,
                R.layout.content_document_list_row,
                files);*/

        fileList.setAdapter(new listAdapter(this, files));
    }

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

class listAdapter extends BaseAdapter {

    Context context;
    List<File> files;
    private static LayoutInflater inflater = null;

    public listAdapter(Context context, List<File> files) {
        // TODO Auto-generated constructor stub
        this.context = context;
        this.files = files;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return this.files.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return this.files.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View vi = convertView;
        if (vi == null)
            vi = inflater.inflate(R.layout.content_document_list_row, null);
        TextView fileName = (TextView) vi.findViewById(R.id.text_file_name);
        fileName.setText(this.files.get(position).fileName);
        TextView lastUsed = (TextView) vi.findViewById(R.id.text_file_last_used);
        lastUsed.setText("Last used: " + this.files.get(position).lastUsed.toString());
        return vi;
    }
}
