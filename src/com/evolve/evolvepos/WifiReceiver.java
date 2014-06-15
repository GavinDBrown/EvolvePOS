// Copied from Stack overflow answer http://stackoverflow.com/questions/6362314/wifi-connect-disconnect-listener

package com.evolve.evolvepos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class WifiReceiver extends BroadcastReceiver {
    private static final String TAG = "WiFiReceiver";
    public static final String UPLOAD = "UPLOAD";

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager conMan = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            Log.d(TAG, "Have Wifi Connection");
            // Start a service to upload file
            Intent uploadIntent = new Intent(context, FtpService.class);
            uploadIntent.putExtra(UPLOAD, true);
            context.startService(uploadIntent);

            // TODO Notify our activity to not access the file we are uploading.
        } else
            Log.d(TAG, "No longer have Wifi Connection");
    }
};
