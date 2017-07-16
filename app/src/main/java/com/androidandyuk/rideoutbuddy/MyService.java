package com.androidandyuk.rideoutbuddy;

/**
 * Created by AndyCr15 on 15/07/2017.
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static com.androidandyuk.rideoutbuddy.MainActivity.activeGroup;
import static com.androidandyuk.rideoutbuddy.MainActivity.myRef;
import static com.androidandyuk.rideoutbuddy.MainActivity.userMember;

public class MyService extends Service
{
    private static final String TAG = "LOCATIONSERVICE";
    private LocationManager mLocationManager = null;

    private class LocationListener implements android.location.LocationListener
    {
//        Location mLastLocation;

        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);
            MainActivity.lastKnownLocation = new Location(provider);

        }

        @Override
        public void onLocationChanged(Location location)
        {
            Log.e(TAG, "onLocationService: " + location);
            Log.i(TAG, "onLocationService: " + location);
            MainActivity.lastKnownLocation.set(location);

            String thisLat = Double.toString(location.getLatitude());
            String thisLon = Double.toString(location.getLongitude());
            Log.i("thisLat " + thisLat, "thisLon " + thisLon);
            myRef.child(activeGroup.ID).child("Riders").child(userMember.ID).child("Lat").setValue(thisLat);
            myRef.child(activeGroup.ID).child("Riders").child(userMember.ID).child("Lon").setValue(thisLon);

            // using time for testing purposes, change to milliseconds for actual use
            Calendar now = new GregorianCalendar();
            String nowString = now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE);
            myRef.child(activeGroup.ID).child("Riders").child(userMember.ID).child("LastUpdate").setValue(nowString);

        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        Log.e(TAG, "onCreate");
        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, MainActivity.locationUpdatesTime, MainActivity.locationUpdatesDistance,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, MainActivity.locationUpdatesTime, MainActivity.locationUpdatesDistance,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }
}