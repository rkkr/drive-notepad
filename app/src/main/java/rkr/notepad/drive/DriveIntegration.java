package rkr.notepad.drive;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DriveIntegration extends AppCompatActivity {

    private static final int PERMISSION_CALLBACK = 1;
    private Uri dataUri;
    private String contents;
    //private Boolean readOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_text_editor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_textEditor);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dataUri = getIntent().getData();
        Log.d("DriveNotepad", dataUri.toString());
        //readOnly = getIntent().getAction().equals(Intent.ACTION_VIEW);
        //if (readOnly) {
        //    EditText textField = (EditText) findViewById(R.id.editText);
        //    textField.setFocusable(false);
        //}

        if (dataUri.getScheme().equals("file") ||
                PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CALLBACK);
        } else {
            LoadFile();
        }
    }

    @Override
    protected void onPause() {
        SaveFile();
        super.onPause();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode ==  PERMISSION_CALLBACK) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LoadFile();
            } else {
                Toast.makeText(this, "Application needs External Storage permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void LoadFile() {
        Log.d("DriveNotepad", "Opening local file");
        try {
            InputStream inputStream = getContentResolver().openInputStream(dataUri);
            contents = Utils.readFromInputStream(inputStream);

            EditText textField = (EditText) findViewById(R.id.editText);
            textField.setText(contents);

        } catch (FileNotFoundException e) {
            Log.e("DriveNotepad", "File not found");
            finish();
        }

        //Get file name
        if (dataUri.getScheme().equals("file")) {
            getSupportActionBar().setTitle(dataUri.getLastPathSegment());
        } else { //content
            try {
                Cursor returnCursor = getContentResolver().query(dataUri, null, null, null, null);
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                getSupportActionBar().setTitle(returnCursor.getString(nameIndex));
                returnCursor.close();
            } catch (NullPointerException e) {
                Log.e("DriveNotepad", "File name not found");
            }
        }
    }

    private void SaveFile() {
        //if (readOnly)
        //    return;

        //file not loaded
        if (contents == null)
            return;

        //no changes made
        EditText textField = (EditText) findViewById(R.id.editText);
        String newContents = textField.getText().toString();
        if (contents.equals(newContents))
            return;

        Log.d("DriveNotepad", "Saving changes");

        try {
            OutputStream outputStream = getContentResolver().openOutputStream(dataUri);
            outputStream.write(newContents.getBytes());
        } catch (IOException e) {
            Log.e("DriveNotepad", "Failed to save file");
            Toast.makeText(this, "Permission denied to save file", Toast.LENGTH_LONG).show();
        }
    }

}
