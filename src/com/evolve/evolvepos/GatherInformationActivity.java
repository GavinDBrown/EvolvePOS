
package com.evolve.evolvepos;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gather_information);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DonateFormFragment()).commit();
        }
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void submitInformation(View v) {
        // TODO Change filename
        // WARNING: Previous instances of this file will be overwritten
        String FILENAME = "hello_file";
        String stringToWrite = generateStringToSaveRecersiveSearch(((ViewGroup) findViewById(android.R.id.content)));
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(stringToWrite.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // FOR DEBUGING
        Toast toast = Toast.makeText(this, stringToWrite, Toast.LENGTH_LONG);
        toast.show();

    }

    /**
     * Process current state of all user input into a String that will be saved
     * 
     * @return The data to be saved.
     */
    private String generateStringToSaveRecersiveSearch(ViewGroup v) {
        StringBuilder sb = new StringBuilder();
        // Put names and values of fields
        for (int i = 0; i < v.getChildCount(); i++) {
            Object child = v.getChildAt(i);
            if (child instanceof EditText) {
                sb.append((String) (((EditText) child).getTag()));
                sb.append(',');
                sb.append(((EditText) child).getText());
                sb.append('\n');
            } else if (child instanceof CheckBox) {
                sb.append((String) (((CheckBox) child).getTag()));
                sb.append(',');
                sb.append(String.valueOf(((CheckBox) child).isChecked()));
                sb.append('\n');
            } else if (child instanceof ViewGroup) {
                // Recursive call
                sb.append(generateStringToSaveRecersiveSearch((ViewGroup) child));
            }
        }
        return sb.toString();
    }
}
