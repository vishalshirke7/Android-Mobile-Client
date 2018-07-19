package io.intelehealth.client.services;

/**
 * Created by Dexter Barretto on 17/6/18.
 * Github : @dbarretto
 */

import android.app.DownloadManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import io.intelehealth.client.R;
import io.intelehealth.client.database.LocalRecordsDatabaseHelper;

/**
 * Created by Dexter Barretto on 16/6/18.
 * Github : @dbarretto
 */
public class ImageDownloadService extends IntentService {

    NotificationCompat.Builder mBuilder;
    NotificationManager mNotifyManager;
    public int mId = 4;
    Integer files_to_download = 0;
    Integer files_downloaded = 0;

     DownloadManager downloadManager;

    private static final String TAG = ImageDownloadService.class.getSimpleName();

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public ImageDownloadService(String name) {
        super(name);
    }

    public ImageDownloadService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        Toast.makeText(this, getString(R.string.image_downloading_service_message), Toast.LENGTH_SHORT).show();

        mNotifyManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);

        mBuilder.setSmallIcon(R.mipmap.ic_sync)
                .setContentTitle(getString(R.string.image_downloading_service_title))
                .setContentText(getString(R.string.image_downloading_service_message));
        mNotifyManager.notify(mId, mBuilder.build());
        queryAllImagesFromDatabase(this);
        queryAllProfileImages(this);
    }


    private void queryAllImagesFromDatabase(Context context) {
        LocalRecordsDatabaseHelper mDbHelper = new LocalRecordsDatabaseHelper(context);
        SQLiteDatabase localdb = mDbHelper.getWritableDatabase();
        String[] coloumns = {"_id", "parse_id", "image_path", "image_type", "delete_status"};
        Cursor cursor = localdb.query("image_records", coloumns, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            files_to_download = files_to_download + (cursor.getCount());
            do {
                String parse_id = cursor.getString(cursor.getColumnIndexOrThrow("parse_id"));
                String image_file_path = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
                Integer delete_status = cursor.getInt(cursor.getColumnIndexOrThrow("delete_status"));
                String image_type = cursor.getString(cursor.getColumnIndexOrThrow("image_type"));
                Long id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                File image_file = new File(image_file_path);
                if (!image_file.exists()) {
                    image_file.mkdirs();
                    if (delete_status.equals(1) || parse_id == null || !parse_id.isEmpty()) {
                        String selection = "_id =?";
                        String[] selectionArgs = {String.valueOf(id)};
                        //localdb.delete("image_records", selection, selectionArgs);
                    }
                    if (parse_id != null && !parse_id.isEmpty()) {
                        try {
                            downloadImagesFromParse(context, parse_id, image_type, image_file);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }else{
                        if(image_file.exists())image_file.delete();
                        notifyDownload();
                    }
                } else {
                    notifyDownload();
                }
            } while (cursor.moveToNext());
        }
        localdb.close();
    }

    private void downloadImagesFromParse(final Context context, String parse_id, String image_type, final File img_file) throws InterruptedException {
        if (parse_id != null && !parse_id.isEmpty() &&
                image_type != null && !image_type.isEmpty()) {
            String parse_id_clean = parse_id.replace("\n", "").trim();
            switch (image_type) {
                case "AD": {
                    image_type = "AdditionalDocuments";
                    break;
                }
                case "PE": {
                    image_type = "PhysicalExam";
                    break;
                }
                case "MH": {
                    image_type = "MedicalHistory";
                    break;
                }
            }
            ParseQuery<ParseObject> query = ParseQuery.getQuery(image_type);
            query.whereEqualTo("objectId", parse_id_clean);
            query.getFirstInBackground(new GetCallback<ParseObject>() {
                @Override
                public void done(ParseObject object, ParseException e) {
                    if (object == null) {
                        Toast.makeText(context, getString(R.string.error_img_unavailable), Toast.LENGTH_SHORT).show();
                        if(img_file.exists())img_file.delete();
                        notifyDownload();
                    } else {
                        try {
                            Thread.sleep(15000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        final ParseFile file = (ParseFile) object.get("Image");
                        final String url = file.getUrl();
                        Uri Download_Uri = Uri.parse(url);
                        DownloadManager.Request request = new DownloadManager.Request(Download_Uri);
                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                        request.setAllowedOverRoaming(false);
                        request.setVisibleInDownloadsUi(true);
                        request.setDestinationInExternalFilesDir(ImageDownloadService.this,Environment.DIRECTORY_PICTURES,img_file.getAbsolutePath());

                        long refid = downloadManager.enqueue(request);



                    }
                }
            });
        }
    }

    private void queryAllProfileImages(Context context) {
        LocalRecordsDatabaseHelper mDbHelper = new LocalRecordsDatabaseHelper(context);
        SQLiteDatabase localdb = mDbHelper.getWritableDatabase();
        String[] coloumns = {"_id", "openmrs_uuid", "patient_photo"};
        Cursor cursor = localdb.query("patient", coloumns, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            files_to_download = files_to_download + (cursor.getCount());
            do {
                String openmrs_uuid = cursor.getString(cursor.getColumnIndexOrThrow("openmrs_uuid"));
                String image_file_path = cursor.getString(cursor.getColumnIndexOrThrow("patient_photo"));
                Long id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                if (image_file_path != null && !image_file_path.isEmpty()) {
                    File image_file = new File(image_file_path);
                    if (!image_file.exists())
                        if (openmrs_uuid != null || !openmrs_uuid.isEmpty()) {
                            try {
                                downloadProfilePhotoFromParse(this, openmrs_uuid, image_file);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                    }
                } else {
                    notifyDownload();
                }
            } while (cursor.moveToNext());
        }
    }

    private void downloadProfilePhotoFromParse(final Context context, String openmrs_uuid, final File img_file) throws InterruptedException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Profile");
        query.whereEqualTo("PatientID", openmrs_uuid.trim());
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if (object == null) {
                    Toast.makeText(context, getString(R.string.error_img_unavailable), Toast.LENGTH_SHORT).show();
                    notifyDownload();
                } else {
                    try {
                        Thread.sleep(15000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    final ParseFile file = (ParseFile) object.get("Image");
                    final String url = file.getUrl();
                    Uri Download_Uri = Uri.parse(url);
                    DownloadManager.Request request = new DownloadManager.Request(Download_Uri);
                    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                    request.setAllowedOverRoaming(false);
                    request.setVisibleInDownloadsUi(true);
                    request.setDestinationInExternalFilesDir(ImageDownloadService.this,Environment.DIRECTORY_PICTURES,img_file.getAbsolutePath());

                    long refid = downloadManager.enqueue(request);
                }
            }
        });
    }

    private void notifyDownload() {
        files_downloaded++;
        if (files_to_download.equals(files_downloaded)) {
            mBuilder.setSmallIcon(R.mipmap.ic_sync)
                    .setContentTitle(getString(R.string.image_downloading_service_complete_title))
                    .setContentText(getString(R.string.image_downloading_service_complete_message));
        } else {
            mBuilder.setSmallIcon(R.mipmap.ic_sync)
                    .setContentTitle(getString(R.string.image_downloading_service_message))
                    .setContentText(getString(R.string.image_downloading_service_process_message) + " " + files_downloaded + "/" + files_to_download);
        }
        mNotifyManager.notify(mId, mBuilder.build());
    }
}

