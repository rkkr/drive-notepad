package rkr.drive.notepad.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.android.gms.drive.DriveId;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class FileHelper {

    private DBHelper dbHelper;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public FileHelper(Context context){
        dbHelper = new DBHelper(context);
    }

    private File CursorToFile(Cursor cursor){
        File file = new File();
        file.id = cursor.getLong(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_ID));
        if (cursor.getString(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_DRIVE_ID)) != null)
            file.driveId = DriveId.decodeFromString(cursor.getString(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_DRIVE_ID)));
        file.fileName = cursor.getString(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_FILENAME));
        file.fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_FILESIZE));
        file.contents = cursor.getString(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_CONTENTS));
        file.state = cursor.getLong(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_STATE));
        try {
            file.dateModified = dateFormat.parse(cursor.getString(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_DATEMODIFIED)));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        try {
            file.dateViewed = dateFormat.parse(cursor.getString(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_DATEVIEWED)));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return file;
    }

    private ContentValues FileToContentValues(File file){
        ContentValues values = new ContentValues();
        if (file.driveId != null)
            values.put(DBContracts.FileEntry.COLUMN_NAME_DRIVE_ID, file.driveId.encodeToString());
        if (file.contents != null)
            values.put(DBContracts.FileEntry.COLUMN_NAME_CONTENTS, file.contents);
        if (file.dateModified != null)
            values.put(DBContracts.FileEntry.COLUMN_NAME_DATEMODIFIED, dateFormat.format(file.dateModified));
        if (file.dateViewed != null)
            values.put(DBContracts.FileEntry.COLUMN_NAME_DATEVIEWED, dateFormat.format(file.dateViewed));
        if (file.fileName != null)
            values.put(DBContracts.FileEntry.COLUMN_NAME_FILENAME, file.fileName);
        if (file.fileSize != -1)
            values.put(DBContracts.FileEntry.COLUMN_NAME_FILESIZE, file.fileSize);
        if (file.state != -1)
            values.put(DBContracts.FileEntry.COLUMN_NAME_STATE, file.state);
        return values;
    }

    public long SaveItem(File file){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = FileToContentValues(file);

        if (file.id == -1) {
            Log.d("Adding item to db", values.toString());
            return db.insert(DBContracts.FileEntry.TABLE_NAME, null, values);
        } else {
            Log.d("Updating item to db", values.toString());
            if (db.update(
                    DBContracts.FileEntry.TABLE_NAME,
                    values,
                    DBContracts.FileEntry.COLUMN_NAME_ID + " = ?",
                    new String[]{Long.toString(file.id)}
            ) != 1)
                return -1;
            return file.id;
        }

    }

    public File GetItem(DriveId driveId){
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DBContracts.FileEntry.TABLE_NAME,
                null,
                DBContracts.FileEntry.COLUMN_NAME_DRIVE_ID + " = ?",
                new String[]{driveId.encodeToString()},
                null,
                null,
                null
        );

        if (cursor.getCount() != 1)
            return null;

        cursor.moveToFirst();
        File file = CursorToFile(cursor);
        cursor.close();

        return file;
    }

    public File GetItem(long id){
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DBContracts.FileEntry.TABLE_NAME,
                null,
                DBContracts.FileEntry.COLUMN_NAME_ID + " = ?",
                new String[]{Long.toString(id)},
                null,
                null,
                null
        );

        if (cursor.getCount() != 1)
            return null;

        cursor.moveToFirst();
        File file = CursorToFile(cursor);
        cursor.close();

        return file;
    }

    public ArrayList<File> GetItems()
    {
        String sortOrder = DBContracts.FileEntry.COLUMN_NAME_DATEVIEWED + " DESC";

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DBContracts.FileEntry.TABLE_NAME, // The table to query
                null,                             // The columns to return
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
            files.add(CursorToFile(cursor));
        }
        cursor.close();
        return files;
    }
}

