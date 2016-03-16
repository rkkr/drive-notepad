package rkr.notepad.drive;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import rkr.notepad.drive.database.File;
import rkr.notepad.drive.database.FileHelper;


public class DocumentList extends BaseDriveActivity implements
        FileRenameFragment.EditNameDialogListener {

    FileHelper fileHelper;

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

        fileHelper = new FileHelper(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LoadHistory();

        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (driveService == null || driveService.getApiClient() == null || !driveService.getApiClient().isConnected()) {
                    ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        }, 1000);*/

    }

    @Override
    protected void ServiceConnected () {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        /*new Thread(new Runnable() {
            public void run() {
                boolean changesFound = false;

                ListView fileList = (ListView) findViewById(R.id.file_history);
                for (int i=0; i < fileList.getCount(); i++) {
                    //Separators and other decorations
                    if (!fileList.getItemAtPosition(i).getClass().equals(File.class))
                        continue;

                    File file = (File) fileList.getItemAtPosition(i);

                    //TODO: Handle deleted file
                    //TODO: Predownload file contents?
                    DriveResource.MetadataResult metaResult = file.driveId.asDriveFile().getMetadata(driveService.getApiClient()).await(30, TimeUnit.SECONDS);
                    if (!metaResult.getStatus().isSuccess()) {
                        Log.e("DocumentList", "Metadata update failed: " + metaResult.getStatus().getStatusMessage());
                        continue;
                    }

                    if (metaResult.getMetadata().isTrashed()) {
                        fileHelper.DeleteItem(file);
                        changesFound = true;
                        continue;
                    }

                    if (!metaResult.getMetadata().getTitle().equals(file.fileName)) {
                        file.fileName = metaResult.getMetadata().getTitle();
                        fileHelper.SaveItem(file);
                        changesFound = true;
                        continue;
                    }
                }

                if (changesFound) {
                    Log.i("DocumentList", "Changes were made outside this application, will refresh");
                    LoadHistory();
                } else {
                    Log.d("DocumentList", "No changes found outside application");
                }
            }
        }).start();*/
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

    private String GetCategory(Date date) {
        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);
        if (cal.getTime().before(date))
            return "Today";

        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        if (cal.getTime().before(date))
            return "This week";

        cal.set(Calendar.DAY_OF_MONTH, 1);
        if (cal.getTime().before(date))
            return "This month";

        cal.set(Calendar.DAY_OF_YEAR, 1);
        if (cal.getTime().before(date))
            return "This year";

        return "Older";
    }


    private void LoadHistory() {
        List<File> files = fileHelper.GetItems();
        ListView fileList = (ListView) findViewById(R.id.file_history);
        TextView helpText = (TextView) findViewById(R.id.helpText);

        if (files.size() == 0) {
            helpText.setVisibility(View.VISIBLE);
            return;
        }

        String currentHeader = "";
        ArrayList items = new ArrayList();
        for(File file : files) {
            if (!GetCategory(file.dateViewed).equals(currentHeader)) {
                currentHeader = GetCategory(file.dateViewed);
                items.add(new ListViewSeparator(currentHeader));
            }
            items.add(file);
        }
        items.add(new ListViewFooter());

        fileList.setAdapter(new listAdapter(this, items, getSupportFragmentManager()));
        helpText.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_document_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_open) {
            if (!driveService.getApiClient().isConnected()) {
                driveService.getApiClient().connect();
                Log.e("DocumentList", "Not connected to drive");
                Toast.makeText(this, "Not connected to Drive", Toast.LENGTH_SHORT).show();
                return false;
            }

            IntentSender intent = Drive.DriveApi.newOpenFileActivityBuilder()
                    .setMimeType(new String[]{
                            "text/html",
                            "text/x-asm",
                            "text/asp",
                            "text/plain",
                            "text/x-c",
                            "text/x-script.csh",
                            "text/css",
                            "text/x-script.elisp",
                            "text/x-setext",
                            "text/x-fortran",
                            "text/vnd.fmi.flexstor",
                            "text/x-h",
                            "text/x-script",
                            "text/x-component",
                            "text/webviewhtml",
                            "text/x-java-source",
                            "text/javascript",
                            "text/ecmascript",
                            "text/x-script.ksh",
                            "text/x-script.lisp",
                            "text/x-la-asf",
                            "text/x-m",
                            "text/x-pascal",
                            "text/pascal",
                            "text/x-script.perl",
                            "text/x-script.perl-module",
                            "text/x-script.phyton",
                            "text/x-script.rexx",
                            "text/richtext",
                            "text/mcf",
                            "text/vnd.rn-realtext",
                            "text/x-asm",
                            "text/x-script.guile",
                            "text/x-script.scheme",
                            "text/sgml",
                            "text/x-sgml",
                            "text/x-script.sh",
                            "text/x-server-parsed-html",
                            "text/x-speech",
                            "text/x-script.tcl",
                            "text/x-script.tcsh",
                            "text/tab-separated-values",
                            "text/x-uil",
                            "text/uri-list",
                            "text/x-uuencode",
                            "text/x-vcalendar",
                            "text/vnd.wap.wml",
                            "text/vnd.wap.wmlscript",
                            "text/scriplet",
                            "text/xml",
                    })
                    .build(driveService.getApiClient());

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
                fileHelper.SaveItem(file);

                break;
            }
        }

        MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(file.fileName).build();
        file.driveId.asDriveFile().updateMetadata(driveService.getApiClient(), changeSet);
    }
}

class listAdapter extends BaseAdapter {

    private DocumentList context;
    private ArrayList items;
    private FragmentManager fragmentManager;
    private LayoutInflater inflater = null;

    public listAdapter(DocumentList context, ArrayList items, FragmentManager fragmentManager) {
        this.context = context;
        this.items = items;
        this.fragmentManager = fragmentManager;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return this.items.size();
    }

    @Override
    public Object getItem(int position) {
        return this.items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, final ViewGroup parent) {
        if (items.get(position).getClass() == ListViewFooter.class) {
            view = inflater.inflate(R.layout.content_document_list_footer, null);
            return view;
        }

        if (items.get(position).getClass() == ListViewSeparator.class) {
            view = inflater.inflate(R.layout.content_document_list_category, null);
            TextView header = (TextView) view.findViewById(R.id.list_row_document_category);
            ListViewSeparator separator = (ListViewSeparator) items.get(position);
            header.setText(separator.title);
            return view;
        }

        final File file = (File)items.get(position);
        view = inflater.inflate(R.layout.content_document_list_row, null);
        //Load view

        final TextView fileName = (TextView) view.findViewById(R.id.list_row_document_name);
        fileName.setText(file.fileName);
        TextView lastUsed = (TextView) view.findViewById(R.id.list_row_last_used);
        if (file.dateViewed != null)
            lastUsed.append(DateUtils.getRelativeTimeSpanString(context, file.dateViewed.getTime(), false));

        if (getCount() == position + 1) {
            float pad = 100 * context.getResources().getDisplayMetrics().density;
            view.setPadding(0, 0, 0, (int)pad);
        }

        //On row click
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Opening file with id", Long.toString(file.id));

                Intent intent = new Intent(context, TextEditor.class);
                intent.putExtra(TextEditor.INTENT_FILE_ID, file.id);
                context.startActivity(intent);
            }
        });

        //On context button click
        Button button = (Button) view.findViewById(R.id.list_row_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popupMenu = new PopupMenu(context, v);
                String[] menuItems = new String[]{"Rename", /*"Details",*/ "Remove from History", "Delete"};
                for (String menuItem : menuItems) {
                    popupMenu.getMenu().add(menuItem);
                }
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.toString()) {
                            case "Rename":
                                FileRenameFragment alertDialog = FileRenameFragment.newInstance(file);
                                alertDialog.show(fragmentManager, "fragment_alert");
                                return true;
                            //case "Details":
                            //    return true;
                            case "Remove from History":
                                context.fileHelper.DeleteItem(file);
                                items.remove(file);
                                notifyDataSetChanged();

                                /*new AlertDialog.Builder(context)
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .setTitle("Remove from History?")
                                        .setMessage("File will not be deleted")
                                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                context.fileHelper.DeleteItem(file);
                                                items.remove(file);
                                                notifyDataSetChanged();
                                            }

                                        })
                                        .setNegativeButton("Cancel", null)
                                        .show();*/
                                return true;
                            case "Delete":
                                new AlertDialog.Builder(context)
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .setTitle("Delete?")
                                        .setMessage("File will be moved to trash")
                                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                context.fileHelper.DeleteItem(file);
                                                items.remove(file);
                                                notifyDataSetChanged();

                                                file.driveId.asDriveFile().trash(context.driveService.getApiClient());
                                            }

                                        })
                                        .setNegativeButton("Cancel", null)
                                        .show();
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

class ListViewSeparator {
    public String title;

    public ListViewSeparator(String title) {
        this.title = title;
    }
}

class ListViewFooter {
    public ListViewFooter() {
    }
}
