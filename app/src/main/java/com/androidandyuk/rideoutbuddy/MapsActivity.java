package com.androidandyuk.rideoutbuddy;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static android.util.Log.i;
import static com.androidandyuk.rideoutbuddy.MainActivity.activeGroup;
import static com.androidandyuk.rideoutbuddy.MainActivity.lastKnownLocation;
import static com.androidandyuk.rideoutbuddy.MainActivity.loadGroupsFromGoogle;
import static com.androidandyuk.rideoutbuddy.MainActivity.loadSettings;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationListener;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationManager;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationUpdatesDistance;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationUpdatesTime;
import static com.androidandyuk.rideoutbuddy.MainActivity.mapView;
import static com.androidandyuk.rideoutbuddy.MainActivity.members;
import static com.androidandyuk.rideoutbuddy.MainActivity.messages;
import static com.androidandyuk.rideoutbuddy.MainActivity.myRef;
import static com.androidandyuk.rideoutbuddy.MainActivity.recordingTrip;
import static com.androidandyuk.rideoutbuddy.MainActivity.removeMemberFromGoogle;
import static com.androidandyuk.rideoutbuddy.MainActivity.saveSettings;
import static com.androidandyuk.rideoutbuddy.MainActivity.timeDifference;
import static com.androidandyuk.rideoutbuddy.MainActivity.trip;
import static com.androidandyuk.rideoutbuddy.MainActivity.user;
import static com.androidandyuk.rideoutbuddy.MainActivity.userMember;
import static com.androidandyuk.rideoutbuddy.MainActivity.wl;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static GoogleMap mMap;

    TextView currentUser;
    private ListView listView2;
    static MyChatAdapter2 myChatAdapter2;
    private DatabaseReference messagesDB;

    private static int viewing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        viewing = 0;

        initiateList();

        messagesDB = FirebaseDatabase.getInstance().getReference().child(activeGroup.ID).child("Messages");

        messagesDB.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                loadGroupsFromGoogle();
                myChatAdapter2.notifyDataSetChanged();
                // set view to the last message posted
                listView2.setSelection(myChatAdapter2.getCount() - 1);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        listView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Log.i("listView2", "Tapped " + position);
                goToChat();
            }
        });

        startService(new Intent(this, MyService.class));

    }

    public void updateCurrentView() {
        Log.i("members.size", "" + members.size());
        currentUser = (TextView) findViewById(R.id.currentView);
        if (members.size() > 0) {
            String thisUser = members.get(viewing).name + " - Status '" + members.get(viewing).state + "' - Last Updated " + members.get(viewing).updated;
            currentUser.setText(thisUser);
        }
    }

    private void initiateList() {
        Log.i("initiateList", "listView2");
        listView2 = (ListView) findViewById(R.id.chatListView2);
        myChatAdapter2 = new MyChatAdapter2(messages);
        listView2.setAdapter(myChatAdapter2);
        if (myChatAdapter2.getCount() > 0) {
            listView2.setSelection(myChatAdapter2.getCount() - 1);
        }
    }

    public void recordTrip(View view) {
        Button recordButton = (Button) findViewById(R.id.recordButton);
        if (!recordingTrip) {
            recordingTrip = true;
            recordButton.setText("StopRec");
        } else {
            recordingTrip = false;
            recordButton.setText(" Record ");
        }
    }

    public void clearTrip(final View view) {
        new AlertDialog.Builder(MapsActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Clear Trip Data?")
                .setMessage("Are you sure you want to clear the trip data? This cannot be recovered")
                .setPositiveButton("Clear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        trip.clear();
                        showRiders(members);
                        centerMapOnUser(view);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .show();


    }

    public void viewTrip(View view) {
        Log.i("viewTrip", "Locations :" + trip.size());
        mMap.clear();
        for (TripMarker thisLocation : trip) {
            Log.i("Marking", "" + thisLocation);
            LatLng thisLatLng = new LatLng(thisLocation.location.getLatitude(), thisLocation.location.getLongitude());
            Marker thisMarker = mMap.addMarker(new MarkerOptions()
                    .position(thisLatLng)
                    .title(millisToTime(thisLocation.timeStamp))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

            thisMarker.showInfoWindow();
        }
        LatLng thisLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(thisLatLng, 11));
    }

    public String millisToTime(Long millis) {
        return "tbc";
    }

    public class MyChatAdapter2 extends BaseAdapter {
        public ArrayList<ChatMessage> groupChatAdapter;

        public MyChatAdapter2(ArrayList<ChatMessage> groupChatAdapter) {
            this.groupChatAdapter = groupChatAdapter;
        }

        @Override
        public int getCount() {
            return groupChatAdapter.size();
        }

        @Override
        public String getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater mInflater = getLayoutInflater();
            View myView = mInflater.inflate(R.layout.chat_listview, null);

            final ChatMessage s = groupChatAdapter.get(position);

            TextView time = (TextView) myView.findViewById(R.id.timeStamp);

            long minute = (s.timestamp / (1000 * 60)) % 60;
            long hour = ((s.timestamp / (1000 * 60 * 60)) % 24) + timeDifference;
            String msgTime = String.format("%02d:%02d", hour, minute);
            time.setText(msgTime);

            TextView userName = (TextView) myView.findViewById(R.id.userName);
            userName.setText(s.name);

            TextView message = (TextView) myView.findViewById(R.id.message);
            message.setText(s.message);

            return myView;
        }

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
                centerMapOnLocation(location);
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
                centerMapOnLocation(lastKnownLocation);

            } else {

                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            }

        }

        loadGroupsFromGoogle();
        showRiders(members);
        updateCurrentView();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationUpdatesTime, locationUpdatesDistance, locationListener);

                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateLocationGoogle(lastKnownLocation, userMember);

                centerMapOnLocation(lastKnownLocation);
            }
        }
    }

    public void updateLocationGoogle(Location thisLocation, GroupMember user) {
        Log.i("updateLocationGoogle", "thisLocation :" + thisLocation);
        if (thisLocation != null) {

            if (recordingTrip) {
                TripMarker thisTrip = new TripMarker(thisLocation);
                trip.add(thisTrip);
            }

            String thisLat = Double.toString(thisLocation.getLatitude());
            String thisLon = Double.toString(thisLocation.getLongitude());
            Log.i("thisLat " + thisLat, "thisLon " + thisLon);
            myRef.child(activeGroup.ID).child("Riders").child(user.ID).child("Lat").setValue(thisLat);
            myRef.child(activeGroup.ID).child("Riders").child(user.ID).child("Lon").setValue(thisLon);

            // using time for testing purposes, change to milliseconds for actual use
            Calendar now = new GregorianCalendar();
            String nowString = now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE);
            myRef.child(activeGroup.ID).child("Riders").child(user.ID).child("LastUpdate").setValue(nowString);

            centerMapOnLocation(thisLocation);
        }
    }

    public void centerMapOnUser(View view) {
        i("centerMapOnUser", "called");

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationUpdatesTime, locationUpdatesDistance, locationListener);

            i("Center View on User", "LK Location updated");
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            updateLocationGoogle(lastKnownLocation, userMember);
            centerMapOnLocation(lastKnownLocation);
        }
    }

    public void centerMapOnLocation(Location location) {

        LatLng selectedLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 17));

    }

    public static void showRiders(List<GroupMember> members) {
        Log.i("showRiders", "GroupSize :" + members.size());
        mMap.clear();
        for (GroupMember thisMember : members) {
            Log.i("Marking", "" + thisMember.location);
            LatLng memberLatLng = new LatLng(thisMember.location.getLatitude(), thisMember.location.getLongitude());
            Marker thisMarker = mMap.addMarker(new MarkerOptions()
                    .position(memberLatLng)
                    .title(thisMember.name)
                    .snippet("Status : '" + thisMember.state + "' - Last Update " + thisMember.updated)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

            thisMarker.showInfoWindow();
        }
        LatLng thisLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(thisLatLng, 11));
    }

    public void viewPrevious(View view) {
        viewing--;
        Log.i("viewPrevious","Viewing " + viewing);
        if (viewing < 0) {
            viewing = members.size() - 1;
        }
        showRiders(members);
        centerMapOnLocation(members.get(viewing).location);
        updateCurrentView();
    }

    public void viewNext(View view) {
        viewing++;
        Log.i("viewNext","Viewing " + viewing);
        if (viewing >= members.size()) {
            viewing = 0;
        }
        showRiders(members);
        centerMapOnLocation(members.get(viewing).location);
        updateCurrentView();
    }

    @Override
    public void onBackPressed() {
        // this must be empty as back is being dealt with in onKeyDown
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.i("onKeyDown", "" + activeGroup);
            new AlertDialog.Builder(MapsActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Leave the current group?")
                    .setMessage("You're about to leave your current group?")
                    .setPositiveButton("Leave", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            removeMemberFromGoogle(user.getUid(), activeGroup);
                            activeGroup = null;
                            mapView = false;
                            wl.release();
                            finish();
                        }
                    })
                    .setNegativeButton("Stay", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    })
                    .show();

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
        // if mapView then likely returning from screen off
        if (mapView) {
            loadGroupsFromGoogle();
            showRiders(members);
            viewing = 0;
            centerMapOnLocation(members.get(viewing).location);
            updateCurrentView();
        }
        initiateList();
        mapView = true;
    }

    public void goToChat() {
        Intent intent = new Intent(getApplicationContext(), ChatRoom.class);
        startActivity(intent);
    }

}
