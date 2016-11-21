package rkr.notepad.drive;

import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
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

        final RecyclerView fileList = (RecyclerView) findViewById(R.id.file_history);

        fileList.setHasFixedSize(true);
        fileList.setLayoutManager(new LinearLayoutManager(this));

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                listAdapter.ViewHolder view = (listAdapter.ViewHolder) viewHolder;

                ((listAdapter)fileList.getAdapter()).remove(view.file);

                if (view.file.driveId != null) {
                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setPinned(false)
                            .build();

                    view.file.driveId.asDriveFile().updateMetadata(driveService.getApiClient(), changeSet);
                }
            }

            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (viewHolder instanceof listAdapter.ViewHolder)
                    return super.getSwipeDirs(recyclerView, viewHolder);
                return 0;
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(fileList);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LoadHistory();
    }

    @Override
    protected void ServiceConnected () {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
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
        if (date == null)
            return "Never";

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
        RecyclerView fileList = (RecyclerView) findViewById(R.id.file_history);
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

        fileList.setAdapter(new listAdapter(this, items, getFragmentManager()));
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
                    /*.setMimeType(new String[]{
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
                    })*/
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
        fileHelper.SaveItem(file);
        LoadHistory();

        MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(file.fileName).build();
        file.driveId.asDriveFile().updateMetadata(driveService.getApiClient(), changeSet);
    }
}

class listAdapter extends RecyclerView.Adapter {

    private DocumentList context;
    private ArrayList items;
    private FragmentManager fragmentManager;

    public listAdapter(DocumentList context, ArrayList items, FragmentManager fragmentManager) {
        this.context = context;
        this.items = items;
        this.fragmentManager = fragmentManager;
    }

    private void remove(int position) {
        Log.d("DocumentList", "Remove item at: " + position);
        context.fileHelper.DeleteItem((File) items.get(position));
        items.remove(position);
        notifyItemRemoved(position);

        if (items.get(position).getClass() != File.class && items.get(position - 1).getClass() == ListViewSeparator.class) {
            items.remove(position - 1);
            notifyItemRemoved(position - 1);
            if (items.size() == 1) {
                items.remove(0);
                notifyItemRemoved(0);
                TextView helpText = (TextView)context.findViewById(R.id.helpText);
                helpText.setVisibility(View.VISIBLE);
            }
        }
    }

    public void remove(File file) {
        remove(items.indexOf(file));
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public class FooterViewHolder extends RecyclerView.ViewHolder {
        public FooterViewHolder(View view) {
            super(view);
        }
    }

    public class CategoryViewHolder extends RecyclerView.ViewHolder {
        public TextView header;

        public CategoryViewHolder(View view) {
            super(view);
            header = (TextView) view.findViewById(R.id.list_row_document_category);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView fileName;
        public TextView lastUsed;
        public File file;

        public ViewHolder(View view) {
            super(view);
            fileName = (TextView) view.findViewById(R.id.list_row_document_name);
            lastUsed = (TextView) view.findViewById(R.id.list_row_last_used);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("Opening file with id", Long.toString(file.id));

                    if (file.driveId == null) {
                        Log.d("DocumentList", "Tried to open file that is not yet saved");
                        file = context.fileHelper.GetNewFile(file);

                        if (file.driveId == null) {
                            Toast.makeText(context, "File not yet saved to Drive", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    Intent intent = new Intent(v.getContext(), TextEditor.class);
                    intent.putExtra(TextEditor.INTENT_FILE_ID, file.id);
                    v.getContext().startActivity(intent);
                }
            });

            Button button = (Button) view.findViewById(R.id.list_row_button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                    String[] menuItems = new String[]{"Rename", "Delete"};
                    for (String menuItem : menuItems) {
                        popupMenu.getMenu().add(menuItem);
                    }
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            if (file.driveId == null) {
                                Log.d("DocumentList", "Tried to open file that is not yet saved");
                                file = context.fileHelper.GetNewFile(file);

                                if (file.driveId == null) {
                                    Toast.makeText(context, "File not yet saved to Drive", Toast.LENGTH_LONG).show();
                                    return false;
                                }
                            }

                            switch (item.toString()) {
                                case "Rename":
                                    FileRenameFragment alertDialog = FileRenameFragment.newInstance(file);
                                    alertDialog.show(fragmentManager, "fragment_alert");
                                    return true;
                                case "Delete":
                                    new AlertDialog.Builder(context)
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .setTitle("Delete?")
                                            .setMessage("File will be moved to trash")
                                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    remove(file);
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
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position).getClass() == ListViewFooter.class)
            return 2;
        if (items.get(position).getClass() == ListViewSeparator.class)
            return 1;
        return 0;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 2)
            return new FooterViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.content_document_list_footer, parent, false));
        if (viewType == 1)
            return new CategoryViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.content_document_list_category, parent, false));
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.content_document_list_row, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == 2) {
            return;
        }

        if (getItemViewType(position) == 1) {
            CategoryViewHolder viewHolder = (CategoryViewHolder)holder;
            ListViewSeparator separator = (ListViewSeparator) items.get(position);
            viewHolder.header.setText(separator.title);
            return;
        }

        ViewHolder viewHolder = (ViewHolder)holder;
        viewHolder.file = (File)items.get(position);

        viewHolder.fileName.setText(viewHolder.file.fileName);
        if (viewHolder.file.dateViewed != null)
            viewHolder.lastUsed.setText("Opened by me: " + DateUtils.getRelativeTimeSpanString(context, viewHolder.file.dateViewed.getTime(), false));
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
