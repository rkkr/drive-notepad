package rkr.drive.notepad.database;

import android.provider.BaseColumns;

public final class DBContracts {

    public DBContracts() {}

    private static final String TEXT_TYPE = " TEXT";
    private static final String LONG_TYPE = " LONG";
    private static final String COMMA_SEP = ",";

    public static abstract class FileEntry implements BaseColumns {
        public static final String TABLE_NAME = "Files";
        public static final String COLUMN_NAME_DRIVE_ID = "driveid";
        public static final String COLUMN_NAME_CONTENTS = "contents";
        public static final String COLUMN_NAME_LASTUSED = "lastused";
        public static final String COLUMN_NAME_FILENAME = "filename";
        public static final String COLUMN_NAME_FILESIZE = "filesize";
        public static final String COLUMN_NAME_STATE = "state";

        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY" +
                        COMMA_SEP + COLUMN_NAME_DRIVE_ID + TEXT_TYPE +
                        COMMA_SEP + COLUMN_NAME_CONTENTS + TEXT_TYPE +
                        COMMA_SEP + COLUMN_NAME_LASTUSED + TEXT_TYPE +
                        COMMA_SEP + COLUMN_NAME_FILENAME + TEXT_TYPE +
                        COMMA_SEP + COLUMN_NAME_FILESIZE + LONG_TYPE +
                        COMMA_SEP + COLUMN_NAME_STATE + LONG_TYPE +
                " )";

        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

}
