package rkr.drive.notepad;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.internal.view.menu.ActionMenuItemView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import java.util.Date;
import java.util.List;

import rkr.drive.notepad.database.File;
import rkr.drive.notepad.database.FileHelper;


public class DocumentList extends BaseDriveActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_document_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), TextEditor.class);
                view.getContext().startActivity(intent);
            }
        });
        LoadHistory();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_OPEN:
                if (resultCode == RESULT_OK) {
                    Log.d("DocumentList", "In Open activity result");
                    DriveId driveId = data.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                    FileSelectedInDriveDialog(driveId);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void FileSelectedInDriveDialog(final DriveId driveId){
        PendingResult<DriveResource.MetadataResult> metadataResult = driveId.asDriveFile().getMetadata(mGoogleApiClient);
        metadataResult.setResultCallback(new ResultCallback<DriveResource.MetadataResult>() {
            @Override
            public void onResult(DriveResource.MetadataResult metadataResult) {
                Log.d("DocumentList", "In file metadata");

                final File file = new File();
                file.fileName = metadataResult.getMetadata().getOriginalFilename();
                file.fileSize = metadataResult.getMetadata().getFileSize();
                file.driveId = driveId;
                file.dateModified = metadataResult.getMetadata().getModifiedDate();
                String fileMime = metadataResult.getMetadata().getMimeType();

                String errors = GetFileOpenErrors(fileMime, file.fileSize);
                if (!errors.isEmpty()){
                    new AlertDialog.Builder(getApplicationContext())
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Invalid file")
                            .setMessage(errors + "Are you sure you want to open?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    OpenDriveFile(file);
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
                    OpenDriveFile(file);
                }
            }
        });
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

    private void OpenDriveFile(final File file) {
        final FileHelper fileHelper = new FileHelper(getApplicationContext());
        File localFile = fileHelper.GetItem(file.driveId);
        //if (file.id)

        if (localFile != null) {
            //We have this file in history, redownload just in case
            file.id = localFile.id;
        }


        final ProgressDialog pd = new ProgressDialog(DocumentList.this);
        pd.setTitle("Downloading drive file...");
        pd.setMessage("Please wait.");
        pd.setCancelable(false);
        pd.show();

        PendingResult<DriveApi.DriveContentsResult> contentsResult = file.driveId.asDriveFile().open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null);
        contentsResult.setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
            @Override
            public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
                Log.d("DocumentList", "In file open");
                file.contents = Utils.readFromInputStream(driveContentsResult.getDriveContents().getInputStream());
                file.dateViewed = new Date();


                fileHelper.SaveItem(file);
                pd.dismiss();

                Intent intent = new Intent(DocumentList.this, TextEditor.class);
                intent.putExtra(TextEditor.INTENT_FILE_ID, file.id);
                DocumentList.this.startActivity(intent);
            }
        });
    }

    public static void UploadDriveFile(final File file, Context context) {
        Log.d("DocumentList", "In file save");

        final FileHelper fileHelper = new FileHelper(context);
        if (file.driveId != null) {
            //File exists on google drive
            PendingResult<DriveApi.DriveContentsResult> contentsResult = file.driveId.asDriveFile().open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null);
            contentsResult.setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
                    Utils.writeToOutputStream(driveContentsResult.getDriveContents().getOutputStream(), file.contents);
                    driveContentsResult.getDriveContents().commit(mGoogleApiClient, null).setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status result) {
                            file.dateModified = new Date();
                            fileHelper.SaveItem(file);

                            Log.d("DocumentList", "File saved");
                        }
                    });
                }
            });
        } else {
            //This is a new file
            Drive.DriveApi.newDriveContents(mGoogleApiClient).setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
                    if (!driveContentsResult.getStatus().isSuccess()) {
                        return;
                    }

                    Utils.writeToOutputStream(driveContentsResult.getDriveContents().getOutputStream(), file.contents);

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(file.fileName)
                            .setMimeType("text/plain").build();

                    Drive.DriveApi.getRootFolder(mGoogleApiClient)
                            .createFile(mGoogleApiClient, changeSet, driveContentsResult.getDriveContents())
                            .setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {
                                @Override
                                public void onResult(DriveFolder.DriveFileResult driveFileResult) {
                                    if (driveFileResult.getStatus().isSuccess()) {
                                        file.driveId = driveFileResult.getDriveFile().getDriveId();
                                        file.dateModified = new Date();
                                        fileHelper.SaveItem(file);

                                        Log.d("DocumentList", "File saved");
                                    }
                                }
                            });
                }
            });
        }
    }

    private void LoadHistory()
    {
        FileHelper fileHelper = new FileHelper(this);
        List<File> files = fileHelper.GetItems();
        ListView fileList = (ListView) findViewById(R.id.file_history);

        fileList.setAdapter(new listAdapter(this, files));
    }

    @Override
    protected void onResume() {
        super.onResume();
        LoadHistory();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //LoadHistory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_document_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

            return true;
        }
        if (id == R.id.action_open) {
            if (!mGoogleApiClient.isConnected()) {
                Log.e("DocumentList", "Not connected to drive");
                Toast.makeText(this, "Not connected to Drive", Toast.LENGTH_SHORT).show();
                return false;
            }

            IntentSender intent = Drive.DriveApi.newOpenFileActivityBuilder().build(mGoogleApiClient);

            try {
                startIntentSenderForResult(intent, REQUEST_CODE_OPEN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                Log.w("DocumentList", "Unable to send intent", e);
            }

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
    public View getView(final int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View vi = convertView;
        if (vi == null)
            vi = inflater.inflate(R.layout.content_document_list_row, null);
        TextView fileName = (TextView) vi.findViewById(R.id.text_file_name);
        fileName.setText(files.get(position).fileName);
        TextView lastUsed = (TextView) vi.findViewById(R.id.text_file_last_used);
        lastUsed.setText("Last viewed: " + files.get(position).dateViewed.toString());

        vi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Opening file with id", Long.toString(files.get(position).id));

                Intent intent = new Intent(context, TextEditor.class);
                intent.putExtra(TextEditor.INTENT_FILE_ID, files.get(position).id);
                context.startActivity(intent);
            }
        });

        return vi;
    }
}
