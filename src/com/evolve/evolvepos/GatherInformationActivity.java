
package com.evolve.evolvepos;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class GatherInformationActivity extends ActionBarActivity {
    protected static final String FILE_NAME = "FILE_NAME";
    private static final String TAG = "GatherInformationActivity";
    public static final String UPLOAD = "UPLOAD";
    private FTPClient mFtpClient;
    private static final String HOSTNAME = "ftp.drivehq.com";
    private static final String USERNAME = "EvolvePosTest";
    private static final String PASSWORD = "HackSummit";
    private static final int PORT = 21;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gather_information);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DonateFormFragment()).commit();
        }

        boolean oldFileExists = false;
        for (String file : fileList()) {
            if (FILE_NAME.equals(file))
                oldFileExists = true;
        }
        if (!oldFileExists) {
            FileOutputStream fos = null;
            StringBuilder newFileStringBuilder = new StringBuilder();
            newFileStringBuilder.append(getFormName());
            getTags(((ViewGroup) findViewById(android.R.id.content)), newFileStringBuilder);
            try {
                fos = openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
                fos.write(newFileStringBuilder.toString().getBytes());
                fos.close();
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File not found exception in onCreate");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "IOException in onCreate");
                e.printStackTrace();
            }
        }
    }

    /**
     * Get the name of the current form
     * 
     * @return
     */
    private String getFormName() {
        return "Default Form";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gather_information, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so
        // long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                // ignore settings for now
                return true;
            case R.id.action_upload:
                // begin upload service
                upload();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void submitInformation(View v) {

        StringBuilder stringToSave = new StringBuilder();
        // Put the current time
        Time t = new Time();
        t.setToNow();
        stringToSave.append(t.toString());
        stringToSave.append('\n');

        getFields(((ViewGroup) findViewById(android.R.id.content)), stringToSave);
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(FILE_NAME, Context.MODE_APPEND);
            fos.write(stringToSave.toString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException in submitInformation");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "IOException in submitInformation");
            e.printStackTrace();
        }
        Log.d(TAG, "Saved form information: " + stringToSave.toString());

        // Display notification to user that information has been saved and
        // update UI
        Toast toast = Toast.makeText(this, "Information saved", Toast.LENGTH_LONG);
        toast.show();

        clearForm(((ViewGroup) findViewById(android.R.id.content)));
    }

    /**
     * Clears all of the EditText and CheckBoxes in viewGroup
     */
    private void clearForm(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            Object child = viewGroup.getChildAt(i);
            if (child instanceof EditText) {
                ((EditText) child).setText("");
            } else if (child instanceof CheckBox) {
                ((CheckBox) child).setChecked(false);
            } else if (child instanceof ViewGroup) {
                // Recursive call
                clearForm((ViewGroup) child);
            }
        }
    }

    /**
     * Populate the provided StringBuilder with all the tags of the elements in
     * ViewGroup that we are interested in (currently EditText and CheckBox)
     * 
     * @param viewGroup
     * @param stringBuilder
     */
    private void getTags(ViewGroup viewGroup, StringBuilder stringBuilder) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            Object child = viewGroup.getChildAt(i);
            if (child instanceof EditText) {
                stringBuilder.append((String) (((EditText) child).getTag()));
                stringBuilder.append('\n');
            } else if (child instanceof CheckBox) {
                stringBuilder.append((String) (((CheckBox) child).getTag()));
                stringBuilder.append('\n');
            } else if (child instanceof ViewGroup) {
                // Recursive call
                getTags((ViewGroup) child, stringBuilder);
            }
        }
    }

    /**
     * Populate the provided StringBuilder with all the fields of the elements
     * in ViewGroup that we are interested in (currently EditText and CheckBox)
     * 
     * @param viewGroup
     * @param stringBuilder
     */
    private void getFields(ViewGroup v, StringBuilder sb) {
        // Put names and values of fields
        for (int i = 0; i < v.getChildCount(); i++) {
            Object child = v.getChildAt(i);
            if (child instanceof EditText) {
                sb.append(((EditText) child).getText());
                sb.append('\n');
            } else if (child instanceof CheckBox) {
                sb.append(String.valueOf(((CheckBox) child).isChecked()));
                sb.append('\n');
            } else if (child instanceof ViewGroup) {
                // Recursive call
                getFields((ViewGroup) child, sb);
            }
        }

    }

    private void upload() {
        if (mFtpClient == null) {
            mFtpClient = new FTPClient();
        }
        new AsyncUploader().execute();

    }

    private final class AsyncUploader extends AsyncTask<Void, Void, Void> {

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(GatherInformationActivity.this);
            pd.setTitle("Sending Data");
            pd.setMessage("Please wait, data is sending");
            pd.setCancelable(false);
            pd.setIndeterminate(true);
            pd.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            // Connect to the FTP server
            boolean connectionStatus = false;
            boolean uploadStatus = false;

            try {
                mFtpClient = new FTPClient();
                // connecting to the host
                mFtpClient.connect(HOSTNAME, PORT);

                // now check the reply code, if positive mean connection
                // success
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
                Log.e(TAG, "Unable to connect to server!");
            } else {
                try {
                    FileInputStream srcFileStream = openFileInput(GatherInformationActivity.FILE_NAME);

                    Time now = new Time();
                    now.setToNow();
                    String dateAndTime = now.format2445();
                    // TODO Add a unique id for the device uploading the
                    // file
                    uploadStatus = mFtpClient.storeFile(dateAndTime, srcFileStream);

                    srcFileStream.close();
                } catch (Exception e) {
                    Log.d(TAG, "upload failed: " + e);
                }

                if (!uploadStatus) {
                    // FILE NOT UPLOADED
                    // TODO
                    Log.e(TAG, "File not uploaded!");
                } else {
                    // file uploaded
                    // delete file from local content
                    getApplicationContext().deleteFile(GatherInformationActivity.FILE_NAME);
                    Log.d(TAG, "File uploaded and deleted from local content");
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            pd.dismiss();
            Toast toast = Toast.makeText(GatherInformationActivity.this, "File uploaded",
                    Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}
