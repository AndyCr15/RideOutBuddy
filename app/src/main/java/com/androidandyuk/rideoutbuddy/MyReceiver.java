package com.androidandyuk.rideoutbuddy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context arg0, Intent arg1) {

        Log.i("MyReceiver","Called");

        String thisLat = arg1.getStringExtra("Lat");
        String thisLon = arg1.getStringExtra("Lon");

        Log.i("thisLat(s)" + thisLat,"thisLon(s)" + thisLon);
        Location thisLocation = new Location("50,1");
        thisLocation.setLatitude(Double.parseDouble(thisLat));
        thisLocation.setLongitude(Double.parseDouble(thisLon));
        new MapsActivity().updateLocationGoogle(thisLocation);
//        new MapsActivity().centerMapOnUser();

    }
}
