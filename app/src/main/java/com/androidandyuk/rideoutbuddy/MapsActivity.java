package com.androidandyuk.rideoutbuddy;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.util.Log.i;
import static com.androidandyuk.rideoutbuddy.MainActivity.activeGroup;
import static com.androidandyuk.rideoutbuddy.MainActivity.database;
import static com.androidandyuk.rideoutbuddy.MainActivity.geofenceSizeMeters;
import static com.androidandyuk.rideoutbuddy.MainActivity.lastKnownLocation;
import static com.androidandyuk.rideoutbuddy.MainActivity.loadSettings;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationListener;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationManager;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationUpdatesDistance;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationUpdatesTime;
import static com.androidandyuk.rideoutbuddy.MainActivity.mapType;
import static com.androidandyuk.rideoutbuddy.MainActivity.mapView;
import static com.androidandyuk.rideoutbuddy.MainActivity.members;
import static com.androidandyuk.rideoutbuddy.MainActivity.messages;
import static com.androidandyuk.rideoutbuddy.MainActivity.messagesDB;
import static com.androidandyuk.rideoutbuddy.MainActivity.millisToTime;
import static com.androidandyuk.rideoutbuddy.MainActivity.recordingTrip;
import static com.androidandyuk.rideoutbuddy.MainActivity.removeMemberFromGoogle;
import static com.androidandyuk.rideoutbuddy.MainActivity.rootDB;
import static com.androidandyuk.rideoutbuddy.MainActivity.saveSettings;
import static com.androidandyuk.rideoutbuddy.MainActivity.saveTrip;
import static com.androidandyuk.rideoutbuddy.MainActivity.trip;
import static com.androidandyuk.rideoutbuddy.MainActivity.tripDB;
import static com.androidandyuk.rideoutbuddy.MainActivity.user;
import static com.androidandyuk.rideoutbuddy.MainActivity.userHome;
import static com.androidandyuk.rideoutbuddy.MainActivity.userMember;
import static java.lang.Double.parseDouble;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private static GoogleMap mMap;

    private static Boolean viewingTrip = false;

    TextView currentUser;
    private ListView listView2;
    static MyChatAdapter2 myChatAdapter2;

    public static DatabaseReference ridersDB;

    MyReceiver myReceiver;

    private static int viewing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Log.i("locationUpdatesTime " + locationUpdatesTime, "locationUpdatesDistance " + locationUpdatesDistance);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        viewing = 0;

        messagesDB = FirebaseDatabase.getInstance().getReference().child(activeGroup.ID).child("Messages");

        //set this to be a new 'last used' for the group
        rootDB.child(activeGroup.ID).child("Details").child("LastUsed").setValue(Long.toString(System.currentTimeMillis()));

        checkMessages();
        initiateList();

        if (myChatAdapter2.getCount() > 0) {
            listView2.setSelection(myChatAdapter2.getCount() - 1);
        }

        listView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Log.i("listView2", "Tapped " + position);
                goToChat();
            }
        });

        checkRecording();

        new ChatRoom().newMessage("**" + userMember.name + " joined the group **");

        // this is supposed to start checking GPS as a service, I have no evidence that it does
        // but I keep GPS through a wakelock instead!
        Intent intent = new Intent(this, MyService.class);
        intent.putExtra("Time", locationUpdatesTime);
        intent.putExtra("Dist", locationUpdatesDistance);
        startService(intent);

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

    public void checkMessages() {
        Log.i("checkMessages", "Called");
        messagesDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i("MapsAct checkMessages", "onDataChange");
                messages.clear();
                for (DataSnapshot messagesDS : dataSnapshot.getChildren()) {
                    GenericTypeIndicator<Map<String, String>> genericTypeIndicator = new GenericTypeIndicator<Map<String, String>>() {
                    };

                    Map<String, String> map = null;
                    map = messagesDS.getValue(genericTypeIndicator);
                    Log.i("messagesDS.getKey", "" + messagesDS.getKey());

                    String ID = messagesDS.getKey();
                    String msg = map.get("msg");
                    String name = map.get("name");
                    String stamp = map.get("stamp");

                    ChatMessage newMessage = new ChatMessage(ID, name, msg, stamp);

                    Log.i("Adding newMessage", "" + newMessage);
                    messages.add(newMessage);
                }
                myChatAdapter2.notifyDataSetChanged();
                checkEmergency();

                if (myChatAdapter2.getCount() > 0) {
                    listView2.setSelection(myChatAdapter2.getCount() - 1);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void checkEmergency() {

        Log.i("checkEmergency", "messages.size " + messages.size());

        for (ChatMessage thisMessage : messages) {

            // check for an emergency
            if (thisMessage.message.contains("** EMERGENCY **")) {

                Log.i("checkEmergency", "Emergency Found");

                Intent intent = new Intent(this, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, intent, 0);

                String text = thisMessage.name + " : " + thisMessage.message.substring(19, thisMessage.message.length());

                Notification notification = new Notification.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setContentTitle("** EMERGENCY **")
                        .setContentText(text)
                        .setContentIntent(pendingIntent)
                        .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                        .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                        .setAutoCancel(true)
//                        .addAction(android.R.drawable.btn_default, "RETURN TO APP", pendingIntent)
                        .build();

                NotificationManager notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);

                notificationManager.notify(1, notification);

            }
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

    public void checkRecording() {
        Button recordButton = (Button) findViewById(R.id.recordButton);
        if (recordingTrip) {
            recordButton.setText("StopRec");
        } else {
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
                        saveTrip();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .show();

    }

    public static void checkMembers(RideOutGroup thisGroup) {
        ridersDB = database.getReference().child(thisGroup.ID).child("Riders");
        Log.i("checkMembers", "ridersDB.getKey " + ridersDB.getKey());
        ridersDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i("MapsAct checkRiders", "onDataChange");
                members.clear();
                for (DataSnapshot riderDS : dataSnapshot.getChildren()) {
                    // check all the rider data has been added to Google
                    if (riderDS.getChildrenCount() == 5) {
                        GenericTypeIndicator<Map<String, String>> genericTypeIndicator = new GenericTypeIndicator<Map<String, String>>() {
                        };

                        Map<String, String> map = null;
                        map = riderDS.getValue(genericTypeIndicator);
                        Log.i("riderDS.getKey", "" + riderDS.getKey());

                        String riderName = map.get("riderName");
                        String riderState = map.get("riderState");
                        String riderLat = map.get("Lat");
                        String riderLon = map.get("Lon");
                        String riderUpdated = map.get("LastUpdate");

                        Log.i("Name " + riderName, "State " + riderState);
                        Log.i("googleLat" + parseDouble(riderLat), "googleLon" + parseDouble(riderLon));
                        Location riderLocation = new Location("1,20");
                        riderLocation.setLatitude(parseDouble(riderLat));
                        riderLocation.setLongitude(parseDouble(riderLon));

                        GroupMember newRider = new GroupMember(riderDS.getKey(), riderName, riderLocation, riderState, riderUpdated);

                        members.add(newRider);
                        Log.i("Adding newRider" + newRider, "members.size " + members.size());
                    }
                }
//                myAdapter.notifyDataSetChanged();
                if (mapView) {
                    Log.i("Updated Data", "You're in MapView");
                    showRiders(members);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i("checkMembers", "onCancelled");
            }
        });
    }

    public void viewTrip(View view) {
        Log.i("viewTrip", "Locations :" + trip.size());
        if (trip.size() > 1) {
            viewingTrip = true;
            mMap.clear();
            drawHome();

            LatLngBounds.Builder builder = new LatLngBounds.Builder();


            for(int i = 1; i < trip.size(); i++){

                LatLng first = new LatLng(trip.get(i-1).location.getLatitude(), trip.get(i-1).location.getLongitude());
                LatLng second = new LatLng(trip.get(i).location.getLatitude(), trip.get(i).location.getLongitude());

                builder.include(second);

                mMap.addPolyline(new PolylineOptions()
                        .add(first,second)
                        .width(15)
                        .color(Color.GRAY)
                        .geodesic(true));
            }

            LatLngBounds bounds = builder.build();

            int padding = 50; // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

            mMap.animateCamera(cu);

//        LatLng thisLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(thisLatLng, 12));
        }
    }

    public void emergency(View view) {
        Intent intent = new Intent(getApplicationContext(), EmergencyActivity.class);
        startActivity(intent);
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
            View myView = mInflater.inflate(R.layout.small_chat_listview, null);

            final ChatMessage s = groupChatAdapter.get(position);

//            TextView time = (TextView) myView.findViewById(R.id.timeStamp);
//
//            long minute = (s.timestamp / (1000 * 60)) % 60;
//            long hour = ((s.timestamp / (1000 * 60 * 60)) % 24) + timeDifference;
//            String msgTime = String.format("%02d:%02d", hour, minute);
//            time.setText(msgTime);

            TextView userName = (TextView) myView.findViewById(R.id.userName);
            userName.setText(s.name);

            TextView message = (TextView) myView.findViewById(R.id.message);
            String thisMessage = s.message;
            message.setText(s.message);
            if (thisMessage.contains("** EMERGENCY **")) {
                message.setTextColor(getResources().getColor(R.color.colorRed));
            }
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

        mMap.setOnMapLongClickListener(this);

        mapView = true;

        // zoom in on user's location
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                Log.i("onLocationChanged", "User has moved");
//                centerMapOnLocation(location);
                updateLocationGoogle(location);

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

                lastKnownLocation.setLatitude(1d);
                lastKnownLocation.setLongitude(50d);

                Location thisLocation = new Location("1, 50");
                thisLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (thisLocation != null) {
                    lastKnownLocation = thisLocation;
                }

                updateLocationGoogle(lastKnownLocation);
                centerMapOnLocation(lastKnownLocation);

            } else {

                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            }

        }

        checkMembers(activeGroup);
        updateCurrentView();

        switch (mapType) {
            case "Normal":
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                return;
            case "Hybrid":
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                return;
            case "Satellite":
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                return;
            case "Terrain":
                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                return;
        }

    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.i("onMapLongClick", "LatLng" + latLng);

        userHome.setLatitude(latLng.latitude);
        userHome.setLongitude(latLng.longitude);

        saveSettings();

        mMap.clear();
        showRiders(members);
        drawHome();

        Toast.makeText(this, "Home Location Set. Leave and rejoin to remove your position if you're home now.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationUpdatesTime, locationUpdatesDistance, locationListener);

                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateLocationGoogle(lastKnownLocation);

                centerMapOnLocation(lastKnownLocation);
            }
        }
    }

    public void updateLocationGoogle(Location thisLocation) {
        Log.i("updateLocationGoogle", "thisLocation :" + thisLocation);
        if (thisLocation != null) {

            if (recordingTrip) {
                TripMarker thisTrip = new TripMarker(thisLocation);
                trip.add(thisTrip);

                //save it direct into the database
                tripDB.execSQL("CREATE TABLE IF NOT EXISTS trip (lat VARCHAR, lon VARCHAR, time VARCHAR)");

                // change into String to be saved to the DB
                String thisLat = Double.toString(thisTrip.location.getLatitude());
                String thisLon = Double.toString(thisTrip.location.getLongitude());
                String thisTime = Long.toString(thisTrip.timeStamp);

                tripDB.execSQL("INSERT INTO trip (lat, lon, time) VALUES ('" + thisLat + "' , '" + thisLon + "' , '" + thisTime + "')");


            }

            // check distance from home, only procede if greater than selected meters
            if (distanceFromHome(thisLocation) > geofenceSizeMeters) {
                String thisLat = Double.toString(thisLocation.getLatitude());
                String thisLon = Double.toString(thisLocation.getLongitude());
                try {
                    Log.i("thisLat " + thisLat, "thisLon " + thisLon);
                    Log.i("user.ID ", " " + userMember.ID);

                    rootDB.child(activeGroup.ID).child("Riders").child(userMember.ID).child("Lat").setValue(thisLat);
                    rootDB.child(activeGroup.ID).child("Riders").child(userMember.ID).child("Lon").setValue(thisLon);

                    Long nowMillis = System.currentTimeMillis();
                    String nowString = millisToTime(nowMillis);
                    nowString = nowString.substring(0, nowString.length() - 3);

                    rootDB.child(activeGroup.ID).child("Riders").child(userMember.ID).child("LastUpdate").setValue(nowString);
                } catch (Exception e) {
                    Log.e("updateLocationGoogle", "Error Getting Location");
                    e.printStackTrace();
                }
            } else {
                Log.i("Location Not Updated", "Within Home Geofence");
            }
//            centerMapOnLocation(thisLocation);
        }
    }

    public double distanceFromHome(Location o) {
        Log.i("distanceFromHome", "Location " + o);
        Log.i("HomeLocation", "Location " + userHome);

        if (o != null) {
            double lat1 = userHome.getLatitude();
            double lng1 = userHome.getLongitude();
            double lat2 = o.getLatitude();
            double lng2 = o.getLongitude();

            int r = 6371; // average radius of the earth in km
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lng2 - lng1);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double d = (r * c) * 1000;
            Log.i("distanceFromHome", "Distance " + d);
            return d;
        }
        return 0;
    }

    public void centerMapOnUser(View view) {
        i("centerMapOnUser", "called");

        viewingTrip = false;

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationUpdatesTime, locationUpdatesDistance, locationListener);

            i("Center View on User", "LK Location updated");
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            updateLocationGoogle(lastKnownLocation);
            centerMapOnLocation(lastKnownLocation);
        }
    }

    public void centerMapOnLocation(Location location) {

        LatLng selectedLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (selectedLatLng.latitude != 0 && selectedLatLng.longitude != 0) {
            Log.i("centerMapOnLocation", "" + selectedLatLng);

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15));
        } else {
            Toast.makeText(this, "User within home geolocation, location hidden", Toast.LENGTH_LONG).show();
        }
    }

    public void viewOtherRider(Location location) {

        LatLng selectedLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (selectedLatLng.latitude != 0 && selectedLatLng.longitude != 0) {
            Log.i("centerMapOnLocation", "" + selectedLatLng);

            try {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                LatLng userLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                builder.include(selectedLatLng);
                builder.include(userLocation);
                Log.i("centerMapOnLocation", "U " + userLocation + "T " + selectedLatLng);
                LatLngBounds bounds = builder.build();
                int padding = 400;
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                mMap.animateCamera(cu);
            } catch (Exception e) {
                e.printStackTrace();
            }

//            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15));
        } else {
            Toast.makeText(this, "User within home geolocation, location hidden", Toast.LENGTH_LONG).show();
        }
    }

    public static void showRiders(List<GroupMember> members) {

        if (mMap != null && !viewingTrip) {
            Log.i("showRiders", "GroupSize :" + members.size());
            mMap.clear();
            drawHome();
            for (GroupMember thisMember : members) {
                LatLng memberLatLng = new LatLng(thisMember.location.getLatitude(), thisMember.location.getLongitude());
                Log.i("memberLatLng", "" + memberLatLng);
                if (memberLatLng.latitude != 0 && memberLatLng.longitude != 0) {
                    Marker thisMarker = mMap.addMarker(new MarkerOptions()
                            .position(memberLatLng)
                            .title(thisMember.name)
                            .snippet("Status : '" + thisMember.state + "' - Last Update " + thisMember.updated)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.bike_icon)));
                    thisMarker.showInfoWindow();
                }
//                LatLng thisLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
//                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(thisLatLng, 10));
            }
        } else {
            Log.i("showRiders", "mMap null");
        }
    }

    public void goToChat() {
        Intent intent = new Intent(getApplicationContext(), ChatRoom.class);
        startActivity(intent);
    }

    public void viewPrevious(View view) {
        viewingTrip = false;
        if (members.size() > 0) {
            viewing--;
            Log.i("viewPrevious", "Viewing " + viewing);
            if (viewing < 0) {
                viewing = members.size() - 1;
            }
            showRiders(members);
            viewOtherRider(members.get(viewing).location);
            updateCurrentView();
        }
    }

    public void viewNext(View view) {
        viewingTrip = false;
        if (members.size() > 0) {
            viewing++;
            Log.i("viewNext", "Viewing " + viewing);
            if (viewing >= members.size()) {
                viewing = 0;
            }
            showRiders(members);
            viewOtherRider(members.get(viewing).location);
            updateCurrentView();
        }
    }

    public static void drawHome(){
        LatLng home = new LatLng(userHome.getLatitude(),userHome.getLongitude());
        mMap.addCircle(new CircleOptions()
                .center(home)
                .radius(geofenceSizeMeters)
                .strokeColor(Color.LTGRAY)
                .fillColor(Color.LTGRAY));
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

                            //set this to be a new 'last used' for the group
                            rootDB.child(activeGroup.ID).child("Details").child("LastUsed").setValue(Long.toString(System.currentTimeMillis()));

                            mapView = false;
                            new ChatRoom().newMessage("** " + userMember.name + " has left the group **");
                            removeMemberFromGoogle(user.getUid(), activeGroup);
                            activeGroup = null;
                            saveSettings();
                            Log.i("LeavingGroup", "activeGroup" + activeGroup);
                            Intent intent = new Intent(getApplicationContext(), MyService.class);
                            stopService(intent);
                            unregisterReceiver(myReceiver);
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
//        saveTrip();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
        checkRecording();
        // if mapView then likely returning from screen off
        if (mapView) {
            checkMembers(activeGroup);
            showRiders(members);
            checkMessages();
            viewing = 0;
            centerMapOnLocation(members.get(viewing).location);
            updateCurrentView();
        }
        initiateList();
        mapView = true;
    }

//    private class MyReceiver extends BroadcastReceiver {
//
//        @Override
//        public void onReceive(Context arg0, Intent arg1) {
//
//            String thisLat = arg1.getStringExtra("Lat");
//            String thisLon = arg1.getStringExtra("Lon");
//
//            Log.i("thisLat(s)" + thisLat,"thisLon(s)" + thisLon);
//
//        }
//
//    }

    @Override
    protected void onStart() {

        //Register BroadcastReceiver
        //to receive event from our service
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        registerReceiver(myReceiver, intentFilter);

        super.onStart();
    }
}
