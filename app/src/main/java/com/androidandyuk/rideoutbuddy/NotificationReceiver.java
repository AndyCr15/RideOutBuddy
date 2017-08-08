package com.androidandyuk.rideoutbuddy;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.androidandyuk.rideoutbuddy.MainActivity.lastKnownLocation;
import static com.androidandyuk.rideoutbuddy.MainActivity.recordingTrip;
import static com.androidandyuk.rideoutbuddy.MainActivity.trip;
import static com.androidandyuk.rideoutbuddy.MapsActivity.AppConstant.PAUSE_ACTION;

/**
 * Created by AndyCr15 on 08/08/2017.
 */

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i("action",action);

        TripMarker thisMarker = new TripMarker(lastKnownLocation);

        if (PAUSE_ACTION.equals(action)) {
            Toast.makeText(context, "Trip Recording Paused", Toast.LENGTH_SHORT).show();

            recordingTrip = false;
            thisMarker.stop = true;
            thisMarker.stationary = true;
            trip.add(thisMarker);

        }
        else if (MapsActivity.AppConstant.STOP_ACTION.equals(action)) {
            Toast.makeText(context, "Trip Recording stopped", Toast.LENGTH_SHORT).show();

            recordingTrip = false;
            thisMarker.stop = true;
            thisMarker.stationary = true;
            trip.add(thisMarker);

            // clear notification
            NotificationManager nm = null;
            nm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(10);

        }
    }
}
