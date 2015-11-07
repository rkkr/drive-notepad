package rkr.drive.notepad.database;

import com.google.android.gms.drive.DriveId;
import java.util.Date;

public class File {
    public DriveId driveId;
    public String fileName;
    public Date lastUsed;
    public String contents;
    public long fileSize;
}
