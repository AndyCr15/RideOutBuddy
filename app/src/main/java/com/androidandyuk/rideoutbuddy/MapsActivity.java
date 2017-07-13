package com.androidandyuk.rideoutbuddy;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

import static android.util.Log.i;
import static com.androidandyuk.rideoutbuddy.MainActivity.activeGroup;
import static com.androidandyuk.rideoutbuddy.MainActivity.lastKnownLocation;
import static com.androidandyuk.rideoutbuddy.MainActivity.loadSettings;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationListener;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationManager;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationUpdatesDistance;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationUpdatesTime;
import static com.androidandyuk.rideoutbuddy.MainActivity.mapView;
import static com.androidandyuk.rideoutbuddy.MainActivity.members;
import static com.androidandyuk.rideoutbuddy.MainActivity.myRef;
import static com.androidandyuk.rideoutbuddy.MainActivity.removeMemberFromGoogle;
import static com.androidandyuk.rideoutbuddy.MainActivity.saveSettings;
import static com.androidandyuk.rideoutbuddy.MainActivity.user;
import static com.androidandyuk.rideoutbuddy.MainActivity.userMember;
import static com.androidandyuk.rideoutbuddy.MainActivity.wl;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // zoom in on user's location
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                Log.i("onLocationChanged", "User has moved");
                centerMapOnLocation(location, "Your location");
                updateLocationGoogle(location, userMember);

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        if (Build.VERSION.SDK_INT < 23) {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationUpdatesTime, locationUpdatesDistance, locationListener);

        } else {

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationUpdatesTime, locationUpdatesDistance, locationListener);

                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateLocationGoogle(lastKnownLocation, userMember);
                centerMapOnLocation(lastKnownLocation, "Your location");

            } else {

                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            }

        }

        showRiders(members);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationUpdatesTime, locationUpdatesDistance, locationListener);

                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateLocationGoogle(lastKnownLocation, userMember);

                centerMapOnLocation(lastKnownLocation, "Your location");
            }
        }
    }

    public void updateLocationGoogle(Location thisLocation, GroupMember user) {
        Log.i("updateLocationGoogle", "thisLocation :" + thisLocation);
        String thisLat = Double.toString(thisLocation.getLatitude());
        String thisLon = Double.toString(thisLocation.getLongitude());
        Log.i("thisLat " + thisLat, "thisLon " + thisLon);
        myRef.child(activeGroup.ID).child("Riders").child(user.ID).child("Lat").setValue(thisLat);
        myRef.child(activeGroup.ID).child("Riders").child(user.ID).child("Lon").setValue(thisLon);
    }

    public void centerMapOnUser(View view) {
        i("centerMapOnUser", "called");

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationUpdatesTime, locationUpdatesDistance, locationListener);

            i("Center View on User", "LK Location updated");
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            updateLocationGoogle(lastKnownLocation, userMember);
            centerMapOnLocation(lastKnownLocation, "Your location");
        }
    }

    public void centerMapOnLocation(Location location, String title) {

        i("MapsActivity", "Center on map " + title);

        LatLng selectedLatLng = new LatLng(location.getLatitude(), location.getLongitude());

//        mMap.addMarker(new MarkerOptions().position(selectedLatLng).title(title));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 14));

    }

    public static void showRiders(List<GroupMember> members) {
        Log.i("showRiders", "called");
        mMap.clear();
        for (GroupMember thisMember : members) {
            Log.i("Marking", "" + thisMember.location);
            LatLng memberLatLng = new LatLng(thisMember.location.getLatitude(), thisMember.location.getLongitude());
            Marker thisMarker = mMap.addMarker(new MarkerOptions()
                    .position(memberLatLng)
                    .title(thisMember.name)
                    .snippet("Status : " + thisMember.state)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

            thisMarker.showInfoWindow();
        }
        LatLng thisLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(thisLatLng, 11));
    }

    @Override
    public void onBackPressed() {
        // this must be empty as back is being dealt with in onKeyDown
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            i("onKeyDown", "" + activeGroup);
            removeMemberFromGoogle(user.getUid(), activeGroup);
            activeGroup = null;
            wl.release();
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSettings();
        mapView = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
        mapView = true;
    }
}
