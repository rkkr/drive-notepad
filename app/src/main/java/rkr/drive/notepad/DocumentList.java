package rkr.drive.notepad;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

import rkr.drive.notepad.database.File;
import rkr.drive.notepad.database.FileHelper;


public class DocumentList extends BaseDriveActivity implements FileRenameFragment.EditNameDialogListener {

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
                    Intent intent = new Intent(this, TextEditor.class);
                    intent.putExtra(TextEditor.INTENT_DRIVE_ID, driveId);
                    this.startActivity(intent);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void LoadHistory() {
        //TODO: load file names from drive

        FileHelper fileHelper = new FileHelper(this);
        List<File> files = fileHelper.GetItems();
        ListView fileList = (ListView) findViewById(R.id.file_history);

        fileList.setAdapter(new listAdapter(this, files, getSupportFragmentManager()));
    }

    /*private void RefreshDataFromDrive() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ListView fileList = (ListView) findViewById(R.id.file_history);
                for (int i=0; i < fileList.getCount(); i++) {
                    File file = (File)fileList.getItemAtPosition(i);

                    DriveResource.MetadataResult metaResult = file.driveId.asDriveFile().getMetadata(mGoogleApiClient).await(30, TimeUnit.SECONDS);
                    if (!metaResult.getStatus().isSuccess()) {
                        Log.e("Metada update", "Connection is " + metaResult.getStatus().getStatusMessage());
                        return;
                    }

                    file.

                    TextView fileName = (TextView) fileList.findViewById(R.id.list_row_document_name);
                    fileName.setText(file.fileName);
                }


            }
        };
        new Thread(runnable).start();
    }*/

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

    @Override
    public void onFinishEditDialog(File file) {
        ListView fileList = (ListView) findViewById(R.id.file_history);
        for (int i=0; i < fileList.getChildCount(); i++) {
            File _file = (File)fileList.getItemAtPosition(i);
            if (_file.id == file.id) {
                TextView fileName = (TextView) fileList.getChildAt(i).findViewById(R.id.list_row_document_name);
                fileName.setText(file.fileName);

                FileHelper fileHelper = new FileHelper(this);
                fileHelper.SaveItem(file);

                break;
            }
        }

        MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(file.fileName).build();
        file.driveId.asDriveFile().updateMetadata(mGoogleApiClient, changeSet);
    }
}

class listAdapter extends BaseAdapter {

    private Context context;
    private List<File> files;
    private FragmentManager fragmentManager;
    private LayoutInflater inflater = null;

    public listAdapter(Context context, List<File> files, FragmentManager fragmentManager) {
        this.context = context;
        this.files = files;
        this.fragmentManager = fragmentManager;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return this.files.size();
    }

    @Override
    public Object getItem(int position) {
        return this.files.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        //Load view
        view = inflater.inflate(R.layout.content_document_list_row, null);
        final TextView fileName = (TextView) view.findViewById(R.id.list_row_document_name);
        fileName.setText(files.get(position).fileName);
        TextView lastUsed = (TextView) view.findViewById(R.id.list_row_last_used);
        lastUsed.setText("Last viewed: " + files.get(position).dateViewed.toString());

        //On row click
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Opening file with id", Long.toString(files.get(position).id));

                Intent intent = new Intent(context, TextEditor.class);
                intent.putExtra(TextEditor.INTENT_FILE_ID, files.get(position).id);
                context.startActivity(intent);
            }
        });

        //On context button click
        Button button = (Button) view.findViewById(R.id.list_row_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(context, v);
                String[] menuItems = new String[]{"Rename", "Details", "Remove from History", "Delete"};
                for (String menuItem : menuItems) {
                    popupMenu.getMenu().add(menuItem);
                }
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.toString()) {
                            case "Rename":
                                FileRenameFragment alertDialog = FileRenameFragment.newInstance(files.get(position));
                                alertDialog.show(fragmentManager, "fragment_alert");
                                return true;
                            case "Details":
                                return true;
                            case "Remove from History":
                                return true;
                            case "Delete":
                                return true;
                        }
                        return false;
                    }
                });

                popupMenu.show();
            }
        });

        return view;
    }
}
