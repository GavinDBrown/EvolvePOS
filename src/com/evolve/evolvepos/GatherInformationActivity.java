
package com.evolve.evolvepos;

import android.content.Context;
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class GatherInformationActivity extends ActionBarActivity {
    private static final String FILE_NAME = "FILE_NAME";
    private static final String TAG = "GatherInformationActivity";

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
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            // ignore settings for now
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

        // TODO Display notification to user that information has been saved and
        // update UI

        // FOR DEBUGING
        Toast toast = Toast.makeText(this, stringToSave.toString(), Toast.LENGTH_LONG);
        toast.show();

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

    private void exportData() {

    }
}
