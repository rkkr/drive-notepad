package rkr.drive.notepad.database;

import com.google.android.gms.drive.DriveId;
import java.util.Date;

public class File {
    public long id = -1;
    public DriveId driveId;
    public String fileName;
    public Date dateModified;
    public Date dateViewed;
    public String contents;
    public long fileSize = -1;
    public long state = -1;
}
