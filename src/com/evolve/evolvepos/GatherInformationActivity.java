
package com.evolve.evolvepos;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

    private boolean mIsUploading = false;
    private StringBuilder mInvalidFields = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gather_information);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DonateFormFragment()).commit();
        }

    }

    private boolean validate(EditText et) {
        String[] viewTag = ((String) et.getTag()).split(":");
        // TODO check that a valid format is supplied and that it's a regex
        String validFormat = viewTag[1];
        return et.getText().toString().matches(validFormat);
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

    /**
     * Check if there is a save file in existence
     * 
     * @return
     */
    private boolean saveFileExists() {
        boolean fileExists = false;
        for (String file : fileList()) {
            if (FILE_NAME.equals(file))
                fileExists = true;
        }
        return fileExists;
    }

    private boolean saveInformation() {
        // Check if there is already a save file and create one if needed.
        if (!saveFileExists()) {
            FileOutputStream fos = null;
            StringBuilder newFileStringBuilder = new StringBuilder();
            newFileStringBuilder.append(getFormName());
            newFileStringBuilder.append("\n");
            int numFields = getTags(((ViewGroup) findViewById(android.R.id.content)),
                    newFileStringBuilder);

            try {
                fos = openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
                fos.write((Integer.toString(numFields) + "\n" + (newFileStringBuilder.toString()))
                        .getBytes());
                fos.close();
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File not found exception in onCreate");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "IOException in onCreate");
                e.printStackTrace();
            }
        }

        // Create the string to save.
        StringBuilder stringToSave = new StringBuilder();
        // Put the current time
        Time t = new Time();
        t.setToNow();
        stringToSave.append(t.toString());
        stringToSave.append('\n');

        StringBuilder invalids = new StringBuilder();

        boolean allValid = getFieldsAndValidate(((ViewGroup) findViewById(android.R.id.content)),
                stringToSave, invalids);

        // check if all fields have valid information before saving
        if (!allValid) {
            // this field is invalid
            mInvalidFields = invalids;

        } else {
            FileOutputStream fos = null;
            // TODO make sure user doesn't try to upload and save information at
            // the
            // same time corrupting the file.
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
        }
        return allValid;
    }

    /**
     * Called when the user clicks the submit button. Displays a confirmation
     * dialog and then saves the form data before clearing the form.
     * 
     * @param v
     */
    public void submitInformation(View v) {
        // Confirm that the user wants to submit their information
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_submit_message).setTitle(R.string.dialog_submit_title);
        builder.setPositiveButton(R.string.dialog_submit_positive_button,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked Submit button
                        boolean allValid = saveInformation();

                        if (allValid) {
                            clearForm(((ViewGroup) findViewById(android.R.id.content)));

                            // Thank the user for their submission
                            AlertDialog.Builder thanksBuilder = new AlertDialog.Builder(
                                    GatherInformationActivity.this);
                            thanksBuilder.setMessage(R.string.dialog_thanks_message);
                            thanksBuilder.create().show();
                        } else {
                            // not all fields were valid
                            AlertDialog.Builder builder = new AlertDialog.Builder(
                                    GatherInformationActivity.this);
                            builder.setMessage(
                                    "Please correct the following information: "
                                            + mInvalidFields.toString()).setTitle(
                                    R.string.dialog_invalid_title);
                            builder.create().show();

                        }
                    }
                });
        builder.setNegativeButton(R.string.dialog_submit_negative_button,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the submission process
                    }
                });
        builder.create().show();

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
     * ViewGroup that we are interested in (currently EditText and CheckBox).
     * 
     * @param viewGroup
     * @param stringBuilder
     */
    private int getTags(ViewGroup viewGroup, StringBuilder stringBuilder) {
        int numFields = 0;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            Object child = viewGroup.getChildAt(i);
            if (child instanceof EditText) {
                stringBuilder.append(((String) (((EditText) child).getTag())).split(":")[0]);
                stringBuilder.append('\n');
                numFields++;
            } else if (child instanceof CheckBox) {
                stringBuilder.append((String) (((CheckBox) child).getTag()));
                stringBuilder.append('\n');
                numFields++;
            } else if (child instanceof ViewGroup) {
                // Recursive call
                numFields += getTags((ViewGroup) child, stringBuilder);
            }
        }
        return numFields;
    }

    /**
     * Populate the provided StringBuilder with all the fields of the elements
     * in ViewGroup that we are interested in (currently EditText and CheckBox)
     * 
     * @param viewGroup
     * @param stringBuilder
     */
    private boolean getFieldsAndValidate(ViewGroup v, StringBuilder sb, StringBuilder invalids) {
        boolean allValid = true;
        // Get names and values of fields
        for (int i = 0; i < v.getChildCount(); i++) {
            Object child = v.getChildAt(i);
            if (child instanceof EditText) {
                if (!validate((EditText) child)) {
                    invalids.append(((EditText) child).getHint());
                    invalids.append("; ");
                    allValid = false;
                }
                sb.append(((EditText) child).getText());
                sb.append('\n');
            } else if (child instanceof CheckBox) {
                sb.append(String.valueOf(((CheckBox) child).isChecked()));
                sb.append('\n');
            } else if (child instanceof ViewGroup) {
                // Recursive call
                allValid = getFieldsAndValidate((ViewGroup) child, sb, invalids) ? allValid : false;
            }
        }

        return allValid;

    }

    private void upload() {

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (mFtpClient == null) {
            mFtpClient = new FTPClient();
        }

        boolean hasData = saveFileExists();

        if (!isConnected) {// check if there is no network connection
            // No network connection!
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.dialog_no_connection_message).setTitle(
                    R.string.dialog_no_connection_title);
            builder.setPositiveButton(R.string.dialog_no_connection_positive_button,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked retry button
                            upload();
                        }
                    });
            builder.setNegativeButton(R.string.dialog_no_connection_negative_button,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the submission process
                        }
                    });
            builder.create().show();
        } else if (mIsUploading) { // check if there is a current upload in
                                   // progress. (i.e. user press button twice
                                   // in
                                   // quick succesion.)
            Toast toast = Toast.makeText(this, R.string.currently_uploading, Toast.LENGTH_SHORT);
            toast.show();
        } else if (!hasData) { // check if there is data that needs uploaded
            Toast toast = Toast
                    .makeText(this, R.string.toast_no_data_to_upload, Toast.LENGTH_SHORT);
            toast.show();
        } else { // Upload the data!
            new AsyncUploader().execute();
        }

    }

    private final class AsyncUploader extends AsyncTask<Void, Void, Void> {

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mIsUploading = true;

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
                // TODO fail gracefully or retry
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
                    // TODO fail gracefully or retry
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
            Toast toast = Toast.makeText(GatherInformationActivity.this,
                    R.string.file_upload_successful, Toast.LENGTH_SHORT);
            toast.show();
            mIsUploading = false;
        }
    }
}
