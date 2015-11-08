package rkr.drive.notepad.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.drive.DriveId;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class FileHelper {

    private DBHelper dbHelper;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String[] fileColumns = {
            DBContracts.FileEntry.COLUMN_NAME_DRIVE_ID,
            DBContracts.FileEntry.COLUMN_NAME_CONTENTS,
            DBContracts.FileEntry.COLUMN_NAME_LASTUSED,
            DBContracts.FileEntry.COLUMN_NAME_FILENAME,
            DBContracts.FileEntry.COLUMN_NAME_FILESIZE,
    };

    public FileHelper(Context context)
    {
        dbHelper = new DBHelper(context);
    }

    public boolean AddItem(File file)
    {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DBContracts.FileEntry.COLUMN_NAME_DRIVE_ID, file.driveId.encodeToString());
        values.put(DBContracts.FileEntry.COLUMN_NAME_CONTENTS, file.contents);
        values.put(DBContracts.FileEntry.COLUMN_NAME_LASTUSED, dateFormat.format(file.lastUsed));
        values.put(DBContracts.FileEntry.COLUMN_NAME_FILENAME, file.fileName);
        values.put(DBContracts.FileEntry.COLUMN_NAME_FILESIZE, file.fileSize);

        long newRowId = db.insert(DBContracts.FileEntry.TABLE_NAME, null, values);
        return newRowId > -1;
    }

    public boolean ItemExists(File file)
    {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DBContracts.FileEntry.TABLE_NAME,
                fileColumns,
                DBContracts.FileEntry.COLUMN_NAME_DRIVE_ID + " = ?",
                new String[]{file.driveId.encodeToString()},
                null,
                null,
                null
        );

        return cursor.getCount() > 0;
    }

    public boolean UpdateItem(File file)
    {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DBContracts.FileEntry.COLUMN_NAME_DRIVE_ID, file.driveId.encodeToString());
        values.put(DBContracts.FileEntry.COLUMN_NAME_CONTENTS, file.contents);
        values.put(DBContracts.FileEntry.COLUMN_NAME_LASTUSED, dateFormat.format(file.lastUsed));
        values.put(DBContracts.FileEntry.COLUMN_NAME_FILENAME, file.fileName);
        values.put(DBContracts.FileEntry.COLUMN_NAME_FILESIZE, file.fileSize);

        int rowsUpdated = db.update(
                DBContracts.FileEntry.TABLE_NAME,
                values,
                DBContracts.FileEntry.COLUMN_NAME_DRIVE_ID + " = ?",
                new String[]{file.driveId.encodeToString()}
        );

        return rowsUpdated == 1;
    }

    public ArrayList<File> LoadItems()
    {
        String sortOrder = DBContracts.FileEntry.COLUMN_NAME_LASTUSED + " DESC";

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DBContracts.FileEntry.TABLE_NAME, // The table to query
                fileColumns,                         // The columns to return
                null,                             // The columns for the WHERE clause
                null,                             // The values for the WHERE clause
                null,                             // don't group the rows
                null,                             // don't filter by row groups
                sortOrder                         // The sort order
        );

        ArrayList<File> files = new ArrayList<File>();
        cursor.moveToFirst();
        for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
        {
            File file = new File();
            file.driveId = DriveId.decodeFromString(cursor.getString(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_DRIVE_ID)));
            file.fileName = cursor.getString(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_FILENAME));
            file.fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_FILESIZE));
            file.contents = cursor.getString(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_CONTENTS));
            try {
                file.lastUsed = dateFormat.parse(cursor.getString(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_LASTUSED)));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            files.add(file);
        }
        return  files;
    }
}

