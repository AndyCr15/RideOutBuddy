package com.androidandyuk.rideoutbuddy;

import android.Manifest;
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
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.util.Log.i;
import static com.androidandyuk.rideoutbuddy.MainActivity.activeGroup;
import static com.androidandyuk.rideoutbuddy.MainActivity.database;
import static com.androidandyuk.rideoutbuddy.MainActivity.geofenceSizeMeters;
import static com.androidandyuk.rideoutbuddy.MainActivity.lastKnownLocation;
import static com.androidandyuk.rideoutbuddy.MainActivity.loadSettings;
import static com.androidandyuk.rideoutbuddy.MainActivity.loadTrip;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationListener;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationManager;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationUpdatesDistance;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationUpdatesTime;
import static com.androidandyuk.rideoutbuddy.MainActivity.mapType;
import static com.androidandyuk.rideoutbuddy.MainActivity.mapView;
import static com.androidandyuk.rideoutbuddy.MainActivity.members;
import static com.androidandyuk.rideoutbuddy.MainActivity.messages;
import static com.androidandyuk.rideoutbuddy.MainActivity.messagesDB;
import static com.androidandyuk.rideoutbuddy.MainActivity.millisToHours;
import static com.androidandyuk.rideoutbuddy.MainActivity.millisToTime;
import static com.androidandyuk.rideoutbuddy.MainActivity.oneDecimal;
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
import static com.androidandyuk.rideoutbuddy.R.id.map;
import static java.lang.Double.parseDouble;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener {

    private static GoogleMap mMap;

    private static Boolean viewingTrip = false;
    public static Location topSpeedLocation;
    public static Boolean importingDB = true;

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

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(map);
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

                    // messages over 3 days old are deleted
                    if ((Long.valueOf(stamp) + 259200000) > System.currentTimeMillis()) {
                        ChatMessage newMessage = new ChatMessage(ID, name, msg, stamp);
                        Log.i("Adding newMessage", "" + newMessage);
                        messages.add(newMessage);
                        myChatAdapter2.notifyDataSetChanged();
                        checkEmergency();
                    } else {
                        //remove old message
                        rootDB.child(activeGroup.ID).child("Messages").child(ID).removeValue();
                        Log.i("Removing Old Group", ID);
                    }

                }


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

    @Override
    public boolean onMarkerClick(Marker marker) {

        LatLng thisLatLng = new LatLng(marker.getPosition().latitude, marker.getPosition().longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(thisLatLng, 18));
        Log.i("onMarkerClick", "memberLatLng " + thisLatLng);

        return true;
    }

    public class AppConstant {
        public static final String PAUSE_ACTION = "PAUSE_ACTION";
        public static final String STOP_ACTION = "STOP_ACTION";
    }


    public void recordTrip(View view) {
        Button recordButton = (Button) findViewById(R.id.recordButton);

        Notification.Builder notif = null;
        NotificationManager nm = null;

        if (!recordingTrip) {

            recordingTrip = true;
            recordButton.setText("StopRec");
            TripMarker thisMarker = new TripMarker(lastKnownLocation);
            thisMarker.start = true;
            trip.add(thisMarker);
            addToTripDB(thisMarker);

            notif = new Notification.Builder(getApplicationContext());
            notif.setSmallIcon(R.drawable.ic_stat_name);
            notif.setContentTitle("");
            Uri path = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            notif.setContentTitle("Ride Out Buddy");
            notif.setContentText("You are currently recording your trip.");
            notif.setSound(path);

            Intent pauseReceive = new Intent();
            pauseReceive.setAction(AppConstant.PAUSE_ACTION);
            PendingIntent pendingIntentPause = PendingIntent.getBroadcast(this, 12345, pauseReceive, PendingIntent.FLAG_UPDATE_CURRENT);
            notif.addAction(android.R.drawable.ic_media_pause, "PAUSE/RESTART", pendingIntentPause);

            Intent stopReceive = new Intent();
            stopReceive.setAction(AppConstant.STOP_ACTION);
            PendingIntent pendingIntentStop = PendingIntent.getBroadcast(this, 12345, stopReceive, PendingIntent.FLAG_UPDATE_CURRENT);
            notif.addAction(android.R.drawable.ic_media_pause, "STOP", pendingIntentStop);

            nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.notify(10, notif.getNotification());

        } else {

            recordingTrip = false;
            recordButton.setText(" Record ");
            trip.get(trip.size() - 1).stop = true;

            nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(10);

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
                        centerMapOnUser();
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

    public void viewTripClicked(View view) {
        viewTrip();
    }

    public void viewTrip() {
        Log.i("viewTrip", "Locations :" + trip.size());
        viewingTrip = true;

        View topInfo = findViewById(R.id.topInfo);
        View bottomInfo = findViewById(R.id.bottomInfo);
        View tripInfo = findViewById(R.id.tripInfo);
        View lowerTripInfo = findViewById(R.id.lowerTripInfo);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.floatingActionButton);

        topInfo.setVisibility(View.INVISIBLE);
        bottomInfo.setVisibility(View.INVISIBLE);
        fab.setVisibility(View.INVISIBLE);
        tripInfo.setVisibility(View.VISIBLE);
        lowerTripInfo.setVisibility(View.VISIBLE);

        TextView tripDistance = (TextView) findViewById(R.id.tripDistance);
        TextView tripTime = (TextView) findViewById(R.id.tripTime);
        TextView tripAverage = (TextView) findViewById(R.id.tripAverage);
        TextView tripTop = (TextView) findViewById(R.id.tripTop);

        if (trip.size() > 1) {

            mMap.clear();
            drawHome();

            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            Double distance = 0d;
            Long totalMillis = 0L;
            Double topSpeed = 0d;

            for (int i = 1; i < trip.size(); i++) {
                if (!trip.get(i).start && !trip.get(i).stop) {
                    Log.i("TripCounter", "" + i);

                    LatLng first = new LatLng(trip.get(i - 1).location.getLatitude(), trip.get(i - 1).location.getLongitude());
                    LatLng second = new LatLng(trip.get(i).location.getLatitude(), trip.get(i).location.getLongitude());

                    Double thisDistance = getDistance(trip.get(i - 1).location, trip.get(i).location);
                    Long thisMillis = trip.get(i).timeStamp - trip.get(i - 1).timeStamp;

                    Log.i("thisDistance " + thisDistance, "thisMillis " + thisMillis);

                    distance += thisDistance;
                    totalMillis += thisMillis;

                    Double thisHours = (double) thisMillis / 3600000L;
                    Double thisSpeed = (double) thisDistance / thisHours;

                    if (thisSpeed > topSpeed && thisSpeed < 200) {
                        topSpeed = thisSpeed;
                        topSpeedLocation = trip.get(i).location;
                    }

                    builder.include(second);

                    int polyColour = Color.GRAY;
                    if (thisSpeed > 20) {
                        polyColour = Color.rgb(141, 179, 139);
                    }
                    if (thisSpeed > 30) {
                        polyColour = Color.rgb(91, 202, 85);
                    }
                    if (thisSpeed > 40) {
                        polyColour = Color.rgb(100, 221, 23);
                    }
                    if (thisSpeed > 50) {
                        polyColour = Color.rgb(205, 220, 57);
                    }
                    if (thisSpeed > 60) {
                        polyColour = Color.rgb(233, 117, 40);
                    }
                    if (thisSpeed > 70) {
                        polyColour = Color.rgb(233, 69, 40);
                    }
                    if (thisSpeed > 80) {
                        polyColour = Color.rgb(198, 40, 40);
                    }


                    mMap.addPolyline(new PolylineOptions()
                            .add(first, second)
                            .width(15)
                            .color(polyColour)
                            .geodesic(true));
                }
            }

            LatLngBounds bounds = builder.build();

            int padding = 50; // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

            mMap.animateCamera(cu);

            Double totalHours = (double) totalMillis / 3600000L;
            Double aveSpeed = distance / totalHours;

            Log.i("distance " + distance, "totalHours " + totalHours);

            tripDistance.setText("Distance : " + oneDecimal.format(distance) + " Miles");
            tripTime.setText("Time : " + millisToHours(totalMillis));
            tripAverage.setText("Ave Speed : " + oneDecimal.format(aveSpeed) + "mph");
            tripTop.setText("Top Speed : " + oneDecimal.format(topSpeed) + "mph");


        } else {
            Toast.makeText(this, "No trip to view", Toast.LENGTH_SHORT).show();
        }
    }

    public void showTopSpeed(View view) {
        centerMapOnLocation(topSpeedLocation);
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

    public static double getDistance(Location aa, Location bb) {
        double lat1 = bb.getLatitude();
        double lng1 = bb.getLongitude();
        double lat2 = aa.getLatitude();
        double lng2 = aa.getLongitude();

        int r = 6371; // average radius of the earth in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = r * c;
        d = d * 0.621;
        return d;
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

        Log.i("MapsActivity", "onMapReady");

        startLocationService();

        mMap.setOnMapLongClickListener(this);

        mMap.setOnMarkerClickListener(this);

        mapView = true;


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

    public void setupLocationManager() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                Log.i("onLocationChanged", "User has moved");

                // location change dealt with by the service

                centerMapOnLocation(location);
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

                centerMapOnLocation(lastKnownLocation);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                Log.i("onRequest", "Write Storage permission granted");

                if (importingDB) {
                    importDB();
                } else {
                    exportDB();
                }


            }

        }

    }

    public void updateLocationGoogle(Location thisLocation) {
        Log.i("updateLocationGoogle", "thisLocation :" + thisLocation);

        lastKnownLocation = thisLocation;

        if (thisLocation != null) {

            if (recordingTrip) {
                TripMarker thisTrip = new TripMarker(thisLocation);
                Log.i("TripMarker", "" + thisTrip);
                trip.add(thisTrip);

                if (getDistance(thisLocation, trip.get(trip.size() - 2).location) < 10) {
                    thisTrip.stationary = true;
                }

                addToTripDB(thisTrip);

            }

            // check distance from home, only proceed if greater than selected meters
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

    public void addToTripDB(TripMarker thisTrip) {
        Log.i("addToTripDB", "" + thisTrip);
        //save it direct into the database
        tripDB.execSQL("CREATE TABLE IF NOT EXISTS trip (lat VARCHAR, lon VARCHAR, time VARCHAR, start INTEGER, stop INTEGER, stationary INTEGER)");

        // change into String to be saved to the DB
        String thisLat = Double.toString(thisTrip.location.getLatitude());
        String thisLon = Double.toString(thisTrip.location.getLongitude());
        String thisTime = Long.toString(thisTrip.timeStamp);
        int thisStart = (thisTrip.start) ? 1 : 0;
        int thisStop = (thisTrip.stop) ? 1 : 0;
        int stationaryInt = (thisTrip.stationary) ? 1 : 0;

        tripDB.execSQL("INSERT INTO trip (lat, lon, time, start, stop, stationary) VALUES ('" + thisLat + "' , '" + thisLon + "' , '" + thisTime + "' , '" + thisStart + "' , '" + thisStop + "' , '" + stationaryInt + "')");

    }

    public void editTrip(View view) {
        Intent intent = new Intent(getApplicationContext(), EditTrip.class);
        startActivity(intent);
    }

    public void importTrip(View view) {
        importingDB = true;
        int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
        int storage = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (storage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            Log.i("importTrip", "storage !=");
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray
                    (new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            Log.i("importTrip", "listPermissionsNeeded !=");
        } else {
            Log.i("importTrip", "importing!");
            importDB();
        }
    }

    public void importDB() {
        Log.i("ImportDB", "Started");
        try {
            String DB_PATH = "/data/data/com.androidandyuk.rideoutbuddy/databases/trip";

            File sdcard = Environment.getExternalStorageDirectory();
            String yourDbFileNamePresentInSDCard = sdcard.getAbsolutePath() + File.separator + "RideOutBuddy/Trip.db";

            Log.i("ImportDB", "SDCard File " + yourDbFileNamePresentInSDCard);

            File file = new File(yourDbFileNamePresentInSDCard);
            // Open your local db as the input stream
            InputStream myInput = new FileInputStream(file);

            // Path to created empty db
            String outFileName = DB_PATH;

            // Opened assets database structure
            OutputStream myOutput = new FileOutputStream(outFileName);

            // transfer bytes from the inputfile to the outputfile
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }

            // Close the streams
            myOutput.flush();
            myOutput.close();
            myInput.close();
        } catch (Exception e) {
            Log.i("ImportDB", "Exception Caught" + e);
        }
        loadTrip();
        viewTrip();
    }

    public void exportTrip(View view) {
        importingDB = false;
        int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
        int storage = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (storage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            Log.i("exportTrip", "storage !=");
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray
                    (new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            Log.i("exportTrip", "listPermissionsNeeded !=");
        } else {
            Log.i("exportTrip", "exporting!");
            exportDB();
        }
    }

    public void exportDB() {
        Log.i("exportDB", "Starting");
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        FileChannel source = null;
        FileChannel destination = null;

        File dir = new File(Environment.getExternalStorageDirectory() + "/RideOutBuddy/");
//        Log.i("dir is ", "" + dir);
//        dir.mkdir();
        try {
            if (dir.mkdir()) {
                System.out.println("Directory created");
            } else {
                System.out.println("Directory is not created");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("Creating Dir Error", "" + e);
        }

        String currentDBPath = "/data/com.androidandyuk.rideoutbuddy/databases/trip";
        String backupDBPath = "RideOutBuddy/Trip.db";
        File currentDB = new File(data, currentDBPath);
        File backupDB = new File(sd, backupDBPath);
        try {
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            Toast.makeText(this, "DB Exported!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Exported Failed!", Toast.LENGTH_LONG).show();
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

    public void centreMapOnUserButton(View view) {
        viewingTrip = false;
        centerMapOnLocation(lastKnownLocation);
    }

    public void centerMapOnUser() {
        i("centerMapOnUser", "called");

        viewingTrip = false;

//        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//
//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationUpdatesTime, locationUpdatesDistance, locationListener);
//
//            i("Center View on User", "LK Location updated");
//            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//            updateLocationGoogle(lastKnownLocation);
        centerMapOnLocation(lastKnownLocation);
//        }
    }

    public void centerMapOnLocation(Location location) {

        LatLng selectedLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (selectedLatLng.latitude != 0 && selectedLatLng.longitude != 0) {
            Log.i("centerMapOnLocation", "" + selectedLatLng);

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15));
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
        if (members.size() > 1) {
            viewing--;
            Log.i("viewPrevious", "Viewing " + viewing);
            if (viewing < 0) {
                viewing = members.size() - 1;
            }
            if (members.get(viewing).ID == userMember.ID) {
                viewPrevious(view);
            }
            showRiders(members);
            viewOtherRider(members.get(viewing).location);
            updateCurrentView();
        }
    }

    public void viewNext(View view) {
        viewingTrip = false;
        if (members.size() > 1) {
            viewing++;
            Log.i("viewNext", "Viewing " + viewing);
            if (viewing >= members.size()) {
                viewing = 0;
            }
            if (members.get(viewing).ID == userMember.ID) {
                viewNext(view);
            }
            showRiders(members);
            viewOtherRider(members.get(viewing).location);
            updateCurrentView();
        }
    }

    public void returnClicked(View view) {
        returnFromViewTrip();
    }

    public void returnFromViewTrip() {
        View topInfo = findViewById(R.id.topInfo);
        View bottomInfo = findViewById(R.id.bottomInfo);
        View tripInfo = findViewById(R.id.tripInfo);
        View lowerTripInfo = findViewById(R.id.lowerTripInfo);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.floatingActionButton);

        topInfo.setVisibility(View.VISIBLE);
        bottomInfo.setVisibility(View.VISIBLE);
        tripInfo.setVisibility(View.INVISIBLE);
        lowerTripInfo.setVisibility(View.INVISIBLE);
        fab.setVisibility(View.VISIBLE);

        TextView tripDistance = (TextView) findViewById(R.id.tripDistance);
        TextView tripTime = (TextView) findViewById(R.id.tripTime);
        TextView tripAverage = (TextView) findViewById(R.id.tripAverage);
        TextView tripTop = (TextView) findViewById(R.id.tripTop);

        tripDistance.setText("0");
        tripTime.setText("0");
        tripAverage.setText("0");
        tripTop.setText("0");


        viewingTrip = false;
        showRiders(members);
        centerMapOnLocation(lastKnownLocation);
    }

    public static void drawHome() {
        LatLng home = new LatLng(userHome.getLatitude(), userHome.getLongitude());
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

            View lowerTripInfo = findViewById(R.id.lowerTripInfo);
            if (lowerTripInfo.isShown()) {
                returnFromViewTrip();
            } else {
                new AlertDialog.Builder(MapsActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Leave the current group?")
                        .setMessage("You're about to leave your current group?")
                        .setPositiveButton("Leave", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                leavingGroup();

                                finish();
                            }
                        })
                        .setNegativeButton("Stay", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        })
                        .show();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void leavingGroup() {
        //set this to be a new 'last used' for the group
        rootDB.child(activeGroup.ID).child("Details").child("LastUsed").setValue(Long.toString(System.currentTimeMillis()));

        mapView = false;
        new ChatRoom().newMessage("** " + userMember.name + " has left the group **");
        removeMemberFromGoogle(user.getUid(), activeGroup);
        activeGroup = null;
        saveSettings();

        stopLocationService();

        Log.i("leavingGroup", "activeGroup" + activeGroup);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSettings();
    }

    public void startLocationService() {

        Log.i("startLocationService", "" + lastKnownLocation);

        if (Build.VERSION.SDK_INT < 23) {

//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationUpdatesTime, locationUpdatesDistance, locationListener);

            Intent intent = new Intent(this, MyService.class);
            intent.putExtra("Time", locationUpdatesTime);
            intent.putExtra("Dist", locationUpdatesDistance);
            startService(intent);

            lastKnownLocation.setLatitude(1d);
            lastKnownLocation.setLongitude(50d);

        } else {

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                Intent intent = new Intent(this, MyService.class);
                intent.putExtra("Time", locationUpdatesTime);
                intent.putExtra("Dist", locationUpdatesDistance);
                startService(intent);

                lastKnownLocation.setLatitude(1d);
                lastKnownLocation.setLongitude(50d);

            } else {

                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            }

        }

    }

    public void startLocationManager() {
        // this was used for tracking location when the app was open.
        // I then realised it was starting the service and tracking twice
        // so now just use the service.
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationUpdatesTime, locationUpdatesDistance, locationListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopLocationService() {
        try {
            Intent intent = new Intent(getApplicationContext(), MyService.class);
            stopService(intent);
            unregisterReceiver(myReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {

        Log.i("MapsActivity", "onResume");

        loadSettings();
        checkRecording();


        if (viewingTrip) {
            viewTrip();
        } else if (mapView) {
            // if mapView then likely returning from screen off
            checkMembers(activeGroup);
            showRiders(members);
            checkMessages();
            viewing = 0;
            centerMapOnLocation(members.get(viewing).location);
            updateCurrentView();
        }
        initiateList();
        mapView = true;
        super.onResume();
    }

    @Override
    protected void onStart() {

        //Register BroadcastReceiver
        //to receive event from our service
        myReceiver = new MyReceiver();
        IntentFilter iff = new IntentFilter(MyService.ACTION);
        this.registerReceiver(myReceiver, iff);

        super.onStart();
    }

    @Override
    protected void onDestroy() {

        Log.i("MapsActivity", "onDestroy");

        super.onDestroy();
    }
}
