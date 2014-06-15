
package com.evolve.evolvepos;

import android.app.IntentService;
import android.content.Intent;
import android.text.format.Time;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.FileInputStream;

public class FtpService extends IntentService {
    private static final String TAG = "FtpService";
    private static final String HOSTNAME = "ftp.drivehq.com";
    private static final String USERNAME = "EvolvePos";
    private static final String PASSWORD = "HackSummit";
    private static final int PORT = 21;

    private FTPClient mFtpClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mFtpClient = new FTPClient();
    }

    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public FtpService() {
        super("UploadService");
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns,
     * IntentService stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getBooleanExtra(WifiReceiver.UPLOAD, false)) {
            // Connect to the FTP server
            boolean connectionStatus = false;
            boolean uploadStatus = false;

            try {
                mFtpClient = new FTPClient();
                // connecting to the host
                mFtpClient.connect(HOSTNAME, PORT);

                // now check the reply code, if positive mean connection success
                if (FTPReply.isPositiveCompletion(mFtpClient.getReplyCode())) {
                    // login using username & password
                    connectionStatus = mFtpClient.login(USERNAME, PASSWORD);

                    /*
                     * Set File Transfer Mode To avoid corruption issue you must
                     * specified a correct transfer mode, such as
                     * ASCII_FILE_TYPE, BINARY_FILE_TYPE, EBCDIC_FILE_TYPE .etc.
                     * Here, I use BINARY_FILE_TYPE for transferring text,
                     * image, and compressed files.
                     */
                    mFtpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    mFtpClient.enterLocalPassiveMode();
                }
            } catch (Exception e) {
                Log.d(TAG, "Error: could not connect to host " + HOSTNAME);
            }

            if (!connectionStatus) {
                // NOT CONNECTED to ftp server
                // TODO
            } else {
                try {
                    FileInputStream srcFileStream = getApplicationContext().openFileInput(
                            GatherInformationActivity.FILE_NAME);

                    Time now = new Time();
                    now.setToNow();
                    String dateAndTime = now.format2445();
                    // TODO Add a unique id for the device uploading the file
                    uploadStatus = mFtpClient.storeFile(dateAndTime, srcFileStream);
                    // }

                    srcFileStream.close();
                } catch (Exception e) {
                    Log.d(TAG, "upload failed: " + e);
                }

                if (!uploadStatus) {
                    // FILE NOT UPLOADED
                    // TODO
                } else {
                    // file uploaded
                    // delete file from local content
                    getApplicationContext().deleteFile(GatherInformationActivity.FILE_NAME);
                }
            }
        }
    }
}
