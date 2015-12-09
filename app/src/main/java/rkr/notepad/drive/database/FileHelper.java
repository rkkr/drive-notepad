package rkr.notepad.drive.database;

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

    private File CursorToFile(Cursor cursor) {
        File file = new File();
        file.id = cursor.getLong(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_ID));
        if (cursor.getString(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_DRIVE_ID)) != null)
            file.driveId = DriveId.decodeFromString(cursor.getString(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_DRIVE_ID)));
        file.fileName = cursor.getString(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_FILENAME));
        try {
            file.dateViewed = dateFormat.parse(cursor.getString(cursor.getColumnIndexOrThrow(DBContracts.FileEntry.COLUMN_NAME_DATEVIEWED)));
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return file;
    }

    private ContentValues FileToContentValues(File file) {
        ContentValues values = new ContentValues();
        if (file.driveId != null)
            values.put(DBContracts.FileEntry.COLUMN_NAME_DRIVE_ID, file.driveId.encodeToString());
        if (file.dateViewed != null)
            values.put(DBContracts.FileEntry.COLUMN_NAME_DATEVIEWED, dateFormat.format(file.dateViewed));
        if (file.fileName != null)
            values.put(DBContracts.FileEntry.COLUMN_NAME_FILENAME, file.fileName);
        return values;
    }

    public File SaveItem(File file) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = FileToContentValues(file);

        if (file.id == -1) {
            Log.d("Adding item to db", values.toString());
            file.id =  db.insert(DBContracts.FileEntry.TABLE_NAME, null, values);
            db.close();
            return file;
        } else {
            Log.d("Updating item to db", values.toString());
            int rows = db.update(
                    DBContracts.FileEntry.TABLE_NAME,
                    values,
                    DBContracts.FileEntry.COLUMN_NAME_ID + " = ?",
                    new String[]{Long.toString(file.id)});
            db.close();
            if (rows == 1)
                return file;
            else
                return null;
        }

    }

    public File GetItem(DriveId driveId) {
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

    public File GetItem(long id) {
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

    public int DeleteItem(File file) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int rows =  db.delete(DBContracts.FileEntry.TABLE_NAME,
                DBContracts.FileEntry.COLUMN_NAME_ID + " = ?",
                new String[]{Long.toString(file.id)});
        db.close();
        return rows;
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

