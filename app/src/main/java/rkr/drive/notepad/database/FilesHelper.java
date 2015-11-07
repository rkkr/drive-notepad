package rkr.drive.notepad.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.DateUtils;

import com.google.android.gms.drive.DriveId;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FilesHelper {

    private DBHelper dbHelper;

    public FilesHelper(Context context)
    {
        dbHelper = new DBHelper(context);
    }

    public void AddItem(DriveId driveId, String contents)
    {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();

        ContentValues values = new ContentValues();
        values.put(DBContracts.FileEntry.COLUMN_NAME_DRIVE_ID, driveId.encodeToString());
        values.put(DBContracts.FileEntry.COLUMN_NAME_CONTENTS, contents);
        values.put(DBContracts.FileEntry.COLUMN_NAME_LASTUSED, dateFormat.format(date));

        long newRowId = db.insert(DBContracts.FileEntry.TABLE_NAME, null, values);
    }
}
