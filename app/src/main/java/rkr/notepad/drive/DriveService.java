package rkr.notepad.drive;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;

public class DriveService extends Service {

    public DriveService() {
    }

    private final IBinder mBinder = new MyBinder();
    private GoogleApiClient googleApiClient;
    private static final String TAG = "DriveService";
    private boolean activeOperations = false;

    @Override
    public void onCreate() {
        super.onCreate();

        //Should never be true
        if (googleApiClient != null)
            return;

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .build();

        Log.d(TAG, "Service is created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service is bound");

        if (!googleApiClient.isConnected())
            googleApiClient.connect();

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Application is now closed");
        DelayDestroy();

        return true;
    }

    private void DelayDestroy() {
        //In case something bad has happened
        if (!googleApiClient.isConnected())
            stopSelf();

        //Service is doing work. Don't stop now. Check after timeout.
        if (activeOperations) {
            Log.e(TAG, "Unbind called during active file operation, service will not stop");
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    DelayDestroy();
                }
            }, 1000 * 5);
            return;
        }

        stopSelf();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service is stopped");
        googleApiClient.disconnect();
        super.onDestroy();
    }

    class MyBinder extends Binder {
        DriveService getService() {
            return DriveService.this;
        }
    }

    public GoogleApiClient getApiClient() {
        return googleApiClient;
    }

    interface OnFileDownloaded {
        void onFileDownloaded(String contents);
    }

    public void DownloadFile(DriveId driveId, final OnFileDownloaded callback) {
        activeOperations = true;
        PendingResult<DriveApi.DriveContentsResult> contentsResult = driveId.asDriveFile().open(getApiClient(), DriveFile.MODE_READ_ONLY, null);
        contentsResult.setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
            @Override
            public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
                Log.d("DocumentList", "In file open");
                if (driveContentsResult.getDriveContents() == null) {
                    //There is no file
                    callback.onFileDownloaded(null);
                    activeOperations = false;
                    return;
                }
                callback.onFileDownloaded(Utils.readFromInputStream(driveContentsResult.getDriveContents().getInputStream()));
                activeOperations = false;
            }
        });
    }

    interface OnFileUploaded {
        void onFileUploaded(DriveId driveId);
    }

    public void UploadFile(DriveId driveId, String contents, String fileName, OnFileUploaded callback) {
        activeOperations = true;
        if (driveId == null)
            UploadFileNew(contents, fileName, callback);
        else
            UploadFileExisting(driveId, contents, fileName, callback);
    }

    private void UploadFileExisting(final DriveId driveId, final String contents, final String fileName, final OnFileUploaded callback) {
        driveId.asDriveFile().open(getApiClient(), DriveFile.MODE_WRITE_ONLY, null).setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
            @Override
            public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
                Utils.writeToOutputStream(driveContentsResult.getDriveContents().getOutputStream(), contents);

                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle(fileName)
                        .setPinned(true)
                        .build();

                driveContentsResult.getDriveContents().commit(getApiClient(), changeSet).setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status result) {
                        if (result.getStatus().isSuccess())
                            callback.onFileUploaded(driveId);
                        else
                            callback.onFileUploaded(null);
                        activeOperations = false;
                    }
                });
            }
        });
    }

    private void UploadFileNew(final String contents, final String fileName, final OnFileUploaded callback) {
        Drive.DriveApi.newDriveContents(getApiClient()).setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
            @Override
            public void onResult(DriveApi.DriveContentsResult driveContentsResult) {
                Utils.writeToOutputStream(driveContentsResult.getDriveContents().getOutputStream(), contents);

                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle(fileName)
                        .setMimeType("text/plain")
                        .setPinned(true)
                        .build();

                Drive.DriveApi.getRootFolder(getApiClient())
                        .createFile(getApiClient(), changeSet, driveContentsResult.getDriveContents())
                        .setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {
                            @Override
                            public void onResult(DriveFolder.DriveFileResult driveFileResult) {
                                if (driveFileResult.getStatus().isSuccess())
                                    callback.onFileUploaded(driveFileResult.getDriveFile().getDriveId());
                                else
                                    callback.onFileUploaded(null);
                                activeOperations = false;
                            }
                        });
            }
        });
    }
}
