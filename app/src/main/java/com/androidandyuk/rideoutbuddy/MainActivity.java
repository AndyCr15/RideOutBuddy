package com.androidandyuk.rideoutbuddy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

import static java.lang.Double.parseDouble;

public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    public static FirebaseDatabase database;
    public static FirebaseUser user;
    public static DatabaseReference rootDB;
    String signInOut = "Sign In";
    private static final int RC_SIGN_IN = 9001;
    private GoogleApiClient mGoogleApiClient;

    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor ed;

    public static RideOutGroup activeGroup;
    public static GroupMember userMember;

    public static LocationManager locationManager;
    public static LocationListener locationListener;
    public static int locationUpdatesTime = 5000;
    public static int locationUpdatesDistance = 1;
    public static Location lastKnownLocation;
    public static Boolean mapView = false;

    public static SimpleDateFormat sdf = new SimpleDateFormat("dd/MMM/yyyy");
    public static SimpleDateFormat dayOfWeek = new SimpleDateFormat("EEEE");
    public static int timeDifference;

    public static ArrayList<RideOutGroup> groups;
    public static ArrayList<GroupMember> members;
    public static ArrayList<ChatMessage> messages;
    public static ArrayList<TripMarker> trip;

    public static SQLiteDatabase tripDB;
    public static DatabaseReference messagesDB;
    public static DatabaseReference ridersDB;

    public static PowerManager pm;
//    public static PowerManager.WakeLock wl;

    public static Boolean recordingTrip = false;

    static MyGroupAdapter myAdapter;
    ListView listView;
    View passwordView;
    Spinner spinnerGPSrate;
    public static SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd/MM HH:mm:ss");

    public static String GPSrate = "Med";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("MainActivity","onCreate");

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mAuth = FirebaseAuth.getInstance();

        sharedPreferences = this.getSharedPreferences("com.androidandyuk.rideoutbuddy", Context.MODE_PRIVATE);
        ed = sharedPreferences.edit();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_LONG).show();
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        lastKnownLocation = new Location("1,50");

        spinnerGPSrate = (Spinner) findViewById(R.id.spinnerGPS);
        spinnerGPSrate.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, GPSrateEnum.values()));

        passwordView = findViewById(R.id.passwordView);

        // attempt to find the difference of now to GMT
        Calendar now = new GregorianCalendar();
        Log.i("GregorianCalendar", "" + now);
        timeDifference = (now.get(Calendar.DST_OFFSET) + now.get(Calendar.ZONE_OFFSET)) / 3600000;

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d("Login", "onAuthStateChanged:signed_in:" + user.getUid());
                    Toast.makeText(MainActivity.this, "Signed in as " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                    Log.i("SignedIn", "onAuthStateChanged");
                    userMember = new GroupMember(user.getUid(), user.getDisplayName());
                    invalidateOptionsMenu();
                    loadGroupsFromGoogle();
                } else {
                    // User is signed out
                    Log.d("Login", "onAuthStateChanged:signed_out");
                    invalidateOptionsMenu();
                }
            }
        };

        mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(mAuthListener);

        database = FirebaseDatabase.getInstance();
        rootDB = database.getReference();

        groups = new ArrayList<>();
        members = new ArrayList<>();
        messages = new ArrayList<>();
        trip = new ArrayList<>();

        tripDB = this.openOrCreateDatabase("trip", MODE_PRIVATE, null);

//        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");

        if (user == null) {
            signIn();
        }

        loadGroupsFromGoogle();
        initiateList();
        loadSettings();
        loadTrip();
        setSpinner();

//        if(mapView && activeGroup != null){
//            Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
//            startActivity(intent);
//        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                activeGroup = groups.get(position);
                saveSettings();
                Log.i("listView", "Tapped " + position);

                checkPassword();

            }
        });

    }

    private void setSpinner() {
        Log.i("SettingSpinner GPSrate", GPSrate);
        switch (GPSrate) {
            case "High":
                spinnerGPSrate.setSelection(0);
                break;
            case "Med":
                spinnerGPSrate.setSelection(1);
                break;
            case "Low":
                spinnerGPSrate.setSelection(2);
                break;
        }
    }

    public void checkPassword() {
        Log.i("checkPassword", "thisGroup.riderCount " + Integer.parseInt(activeGroup.riderCount));

        Log.i("checkPassword", "members.size() " + members.size());
        Boolean alreadyInGroup = false;
        for (GroupMember thisMember : members) {
            Log.i("thisMember.ID " + thisMember.ID, "user.getUid() " + user.getUid());
            if (thisMember.ID.equals(user.getUid())) {
                Log.i("checkPassword", "Already in Group");
                alreadyInGroup = true;
            }
        }

        if (!alreadyInGroup) {

            passwordView.setVisibility(View.VISIBLE);

            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInputFromWindow(passwordView.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);

            final EditText password = (EditText) findViewById(R.id.groupPassword);

            password.setFocusableInTouchMode(true);
            password.requestFocus();

            password.setOnKeyListener(new View.OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    // If the event is a key-down event on the "enter" button
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                            (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        // Perform action on key press
                        String thisPassword = password.getText().toString();
                        if (thisPassword.equals(activeGroup.password)) {
//                            wl.acquire();
                            password.setText("");
                            passwordView.setVisibility(View.INVISIBLE);
                            addMemberToGoogle(userMember, activeGroup);
                            Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(MainActivity.this, "Incorrect password", Toast.LENGTH_LONG).show();
                            password.setText("");
                            activeGroup = null;
                            passwordView.setVisibility(View.INVISIBLE);
                        }
                        return true;
                    }
                    return false;
                }
            });
        } else {
//            wl.acquire();
            passwordView.setVisibility(View.INVISIBLE);
            addMemberToGoogle(userMember, activeGroup);
            Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
            startActivity(intent);
        }
    }

    public void createGroupClicked(View view) {
        Log.i("createGroupClicked", "Started");
        Intent intent = new Intent(getApplicationContext(), CreateGroup.class);
        startActivity(intent);
    }

    private class MyGroupAdapter extends BaseAdapter {
        public ArrayList<RideOutGroup> groupDataAdapter;

        public MyGroupAdapter(ArrayList<RideOutGroup> groupDataAdapter) {
            this.groupDataAdapter = groupDataAdapter;
        }

        @Override
        public int getCount() {
            return groupDataAdapter.size();
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
            View myView = mInflater.inflate(R.layout.groups_listview, null);

            final RideOutGroup s = groupDataAdapter.get(position);

            TextView groupName = (TextView) myView.findViewById(R.id.groupName);
            groupName.setText(s.name);

            TextView members = (TextView) myView.findViewById(R.id.membersTV);
            int size = Integer.parseInt(s.riderCount);
            members.setText("Riders :" + size);

            TextView created = (TextView) myView.findViewById(R.id.createdTV);
            Log.i("s.created", "" + s.created);
            created.setText("Created :" + millisToTime(s.created));

            return myView;
        }

    }

    private void initiateList() {
        Log.i("initiateList", "listView");
        listView = (ListView) findViewById(R.id.groupsListView);
        myAdapter = new MyGroupAdapter(groups);
        listView.setAdapter(myAdapter);
    }

    public static String millisToTime(Long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return dateTimeFormatter.format(calendar.getTime());
    }

    //   GOOGLE SIGN IN

    private void signIn() {
        Log.i("signIn", "Starting");

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("onActivityResult", "Starting");
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
                Log.i("SignedIn", "OnActivityResult");
            } else {
                // Google Sign In failed, update UI appropriately
                Log.i("onActivityResult", "Sign in failed");
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.i("GoogleSignIn", "signInWithCredential:success");
                            user = mAuth.getCurrentUser();
//                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.i("GoogleSignIn", "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
        Log.i("SignedIn", "firebaseAuthWithGoogle");
    }

    public void signOut() {
        mAuth.signOut();
        Auth.GoogleSignInApi.signOut(mGoogleApiClient);
        Log.i("signOut", "Complete");
        Toast.makeText(MainActivity.this, "Signed Out", Toast.LENGTH_SHORT).show();
        invalidateOptionsMenu();
    }

    //   GOOGLE SIGN IN END

    public static void loadGroupsFromGoogle() {
        Log.i("loadGroupsFromGoogle", "Starting");
        if (user != null) {

            Log.i("loadGroupsFromGoogle", "Loading");

//            final DatabaseReference thisRef = database.getReference();

            rootDB.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    Log.i("loadGroupsFromGoogle", "Count :" + dataSnapshot.getChildrenCount());
                    groups.clear();


                    for (final DataSnapshot groupDS : dataSnapshot.getChildren()) {
                        Log.i("DataSnapshot groupDS", "" + groupDS);

                        ridersDB = database.getReference().child(groupDS.getKey()).child("Riders");
                        ridersDB.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                rootDB.child(groupDS.getKey()).child("Details").child("RiderCount").setValue(Long.toString(dataSnapshot.getChildrenCount()));
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
//                        rootDB.child(groupDS.getKey()).child("Details").child("RiderCount").setValue(Long.toString(ridersDB.));

                        for (DataSnapshot detailsDS : groupDS.getChildren()) {

                            if (detailsDS.getKey().equals("Details")) {
                                Log.i("detailsDS", "" + detailsDS);
                                GenericTypeIndicator<Map<String, String>> genericTypeIndicator = new GenericTypeIndicator<Map<String, String>>() {
                                };

                                Map<String, String> map = null;
                                map = detailsDS.getValue(genericTypeIndicator);

                                String name = map.get("Name");
                                String password = map.get("Password");
                                String ID = map.get("ID");
                                String count = map.get("RiderCount");
                                String created = map.get("Created");

                                RideOutGroup newGroup = new RideOutGroup(ID, name, password, count, Long.parseLong(created));

                                groups.add(newGroup);
                                myAdapter.notifyDataSetChanged();

                            }
                        }
                    }
                    loadSettings();
//                    myAdapter.notifyDataSetChanged();
//                    if (mapView) {
//                        Log.i("Updated Data", "You're in MapView");
//                        MapsActivity.showRiders(members);
//                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    public static void checkMembers(RideOutGroup thisGroup) {
        ridersDB = database.getReference().child(thisGroup.ID).child("Riders");
        Log.i("checkMembers", "ridersDB.getKey " + ridersDB.getKey());
        ridersDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i("MainAct checkRiders", "onDataChange");
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
                    MapsActivity.showRiders(members);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i("checkMembers", "onCancelled");
            }
        });
    }


    public static void saveGroupToGoogle(RideOutGroup o) {
        Log.i("saveGroupToGoogle", "Starting");

        // save the group
        rootDB.child(o.ID).child("Details").child("Name").setValue(o.name);
        rootDB.child(o.ID).child("Details").child("Password").setValue(o.password);
        rootDB.child(o.ID).child("Details").child("ID").setValue(o.ID);
        rootDB.child(o.ID).child("Details").child("Created").setValue(o.created.toString());
        rootDB.child(o.ID).child("Details").child("Live").setValue(o.live.toString());
        rootDB.child(o.ID).child("Details").child("RiderCount").setValue(o.riderCount);
    }

    public static void addMemberToGoogle(GroupMember m, RideOutGroup o) {

        Log.i("addMemberToGoogle", "" + m);
        rootDB.child(o.ID).child("Riders").child(m.ID).child("riderState").setValue(m.state);
        rootDB.child(o.ID).child("Riders").child(m.ID).child("riderName").setValue(m.name);
        rootDB.child(o.ID).child("Riders").child(m.ID).child("Lat").setValue("51.5007292");
        rootDB.child(o.ID).child("Riders").child(m.ID).child("Lon").setValue("-0.1268194");
        if (m.location != null) {
            rootDB.child(o.ID).child("Riders").child(m.ID).child("Lat").setValue(Double.toString(m.location.getLatitude()));
            rootDB.child(o.ID).child("Riders").child(m.ID).child("Lon").setValue(Double.toString(m.location.getLongitude()));
        }
        // using time for testing purposes, change to milliseconds for actual use
        Calendar now = new GregorianCalendar();
        String nowMins = Integer.toString(now.get(Calendar.MINUTE));
        if (now.get(Calendar.MINUTE) < 10) {
            nowMins = "0" + nowMins;
        }
        String nowString = now.get(Calendar.HOUR_OF_DAY) + ":" + nowMins;
        rootDB.child(o.ID).child("Riders").child(m.ID).child("LastUpdate").setValue(nowString);
    }

    public static void removeMemberFromGoogle(String uid, RideOutGroup o) {
        rootDB.child(o.ID).child("Riders").child(uid).removeValue();
    }

    public static void saveTrip() {
        Log.i("saveTrip", "trip.size " + trip.size());
        ed.putInt("tripSize", trip.size()).apply();

        tripDB.execSQL("CREATE TABLE IF NOT EXISTS trip (lat VARCHAR, lon VARCHAR, time VARCHAR)");

        tripDB.delete("trip", null, null);

        if (trip.size() > 0) {

            try {

                for (TripMarker thisTrip : trip) {

                    // change into String to be saved to the DB
                    String thisLat = Double.toString(thisTrip.location.getLatitude());
                    String thisLon = Double.toString(thisTrip.location.getLongitude());
                    String thisTime = Long.toString(thisTrip.timeStamp);

                    Log.i("thisTrip.timeStamp ", "" + thisTrip.timeStamp);
                    Log.i("Saving TripMarker", thisLat + " " + thisLon + " " + thisTime);
                    tripDB.execSQL("INSERT INTO trip (lat, lon, time) VALUES ('" + thisLat + "' , '" + thisLon + "' , '" + thisTime + "')");

                }

            } catch (Exception e) {

                e.printStackTrace();
                Log.i("saveTrip", "Error Saving");

            }
        }
    }

    public static void loadTrip() {
        int tripSize = sharedPreferences.getInt("tripSize", 0);
        Log.i("loadTrip", "size " + tripSize);

        trip.clear();

        try {

            Cursor c = tripDB.rawQuery("SELECT * FROM trip", null);

            int latIndex = c.getColumnIndex("lat");
            int lonIndex = c.getColumnIndex("lon");
            int timeIndex = c.getColumnIndex("time");

            c.moveToFirst();

            do {
                Location thisLocation = new Location("51.5007292,-0.1268194");
                thisLocation.setLatitude(Double.parseDouble(c.getString(latIndex)));
                thisLocation.setLongitude(Double.parseDouble(c.getString(lonIndex)));
                TripMarker newMarker = new TripMarker(thisLocation, Long.parseLong(c.getString(timeIndex)), "Loading");

                trip.add(newMarker);

            } while (c.moveToNext());

            Log.i("loadTrip", "trip.size " + trip.size());

        } catch (Exception e) {

            Log.i("LoadingDB", "Caught Error");
            e.printStackTrace();

        }
    }

    public static void checkGPSrate() {

        switch (GPSrate) {
            case "High":
                locationUpdatesTime = 10000;
                locationUpdatesDistance = 20;
                break;
            case "Med":
                locationUpdatesTime = 40000;
                locationUpdatesDistance = 100;
                break;
            case "Low":
                locationUpdatesTime = 120000;
                locationUpdatesDistance = 500;
                break;
        }


    }

    public static void saveSettings() {
        Log.i("Main Activity", "saveSettings");
        ed.putString("GPSrate", GPSrate).apply();
//        ed.putBoolean("mapView", mapView).apply();
        if (activeGroup != null) {
            ed.putString("activeGroupID", activeGroup.ID).apply();
        } else {
            ed.putString("activeGroupID", "null").apply();
        }
    }

    public static void loadSettings() {
        GPSrate = sharedPreferences.getString("GPSrate", "Med");
//        mapView  = sharedPreferences.getBoolean("mapView", false);
        String activeGroupID = sharedPreferences.getString("activeGroupID", "null");
        Log.i("loadSettings", "activeGroupID" + activeGroupID);
        if (activeGroupID.equals("null")) {
            activeGroup = null;
            Log.i("activeGroup not found", "" + activeGroup);
        } else {
            // find active group from the ID
            for (RideOutGroup thisGroup : groups) {
                if (thisGroup.ID.equals(activeGroupID)) {
                    activeGroup = thisGroup;
                    Log.i("activeGroup found", "" + activeGroup);
//                    checkMembers(activeGroup);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        super.onCreateOptionsMenu(menu);

        if (user == null) {
            signInOut = "Sign In";
        } else {
            signInOut = "Sign Out";
        }

        menu.add(0, 0, 0, signInOut).setShortcut('3', 'c');

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menu_choice = item.getItemId();
        switch (menu_choice) {
            case 0:
                Log.i("Option", "0");
                if (user == null) {
                    signIn();
                } else {
                    signOut();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // this must be empty as back is being dealt with in onKeyDown
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (passwordView.isShown()) {
                passwordView.setVisibility(View.INVISIBLE);
            } else {
                finish();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();

        spinnerGPSrate = (Spinner) findViewById(R.id.spinnerGPS);
        GPSrate = spinnerGPSrate.getSelectedItem().toString();
        checkGPSrate();
        saveSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        myAdapter.notifyDataSetChanged();
        Log.i("onResume", "activeGroup" + activeGroup);
        if (activeGroup != null) {
            Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
            startActivity(intent);
        }
        loadSettings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("onDestroy", "Starting");
        mAuth.removeAuthStateListener(mAuthListener);
        saveTrip();
    }


}
