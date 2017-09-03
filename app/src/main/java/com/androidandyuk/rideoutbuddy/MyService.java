package com.androidandyuk.rideoutbuddy;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class MyService extends Service
{
    private static final String TAG = "LOCATIONSERVICE";
    public static final String ACTION = "com.androidandyuk.rideoutbuddy";
    private LocationManager mLocationManager = null;

    int gpsTime;
    int gpsDist;

    private class LocationListener implements android.location.LocationListener
    {
//        Location mLastLocation;

        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);

        }

        @Override
        public void onLocationChanged(Location location)
        {
            Log.i(TAG, "onLocationService: " + location);

            String thisLat = Double.toString(location.getLatitude());
            String thisLon = Double.toString(location.getLongitude());
            Log.i("thisLat(inS) " + thisLat, "thisLon(inS) " + thisLon);

            Intent intent = new Intent(ACTION);
            intent.putExtra("Lat", thisLat);
            intent.putExtra("Lon", thisLon);

            sendBroadcast(intent);

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
//            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");

        gpsTime = intent.getIntExtra("Time", 20000);
        gpsDist = intent.getIntExtra("Dist", 100);
        Log.i("gpsTime " + gpsTime,"gpsDist " + gpsDist);

        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, gpsTime, gpsDist,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }


        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");

        Intent notificationIntent = new Intent(this, MapsActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle("Monitoring Location")
                .setContentText("Tap here to return to the Map view")
                .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);

        initializeLocationManager();
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
                    Log.i(TAG, "fail to remove location listeners, ignore", ex);
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