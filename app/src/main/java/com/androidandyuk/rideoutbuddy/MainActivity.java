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
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
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
    public static DatabaseReference myRef;
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

    public static PowerManager pm;
    public static PowerManager.WakeLock wl;

    public static Boolean recordingTrip = false;

    static MyGroupAdapter myAdapter;
    ListView listView;
    View passwordView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        myRef = database.getReference();

        groups = new ArrayList<>();
        members = new ArrayList<>();
        messages = new ArrayList<>();
        trip = new ArrayList<>();

        tripDB = this.openOrCreateDatabase("trip", MODE_PRIVATE, null);

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");

        loadGroupsFromGoogle();
        initiateList();
        loadSettings();
        loadTrip();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // this is for editing a fueling, it stores the info in itemLongPressed
                activeGroup = groups.get(position);
                saveSettings();
                Log.i("listView", "Tapped " + position);
                addMemberToGoogle(userMember, activeGroup);
                checkPassword(activeGroup);

            }
        });

    }

    public void checkPassword(final RideOutGroup thisGroup) {
        passwordView = (View) findViewById(R.id.passwordView);
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
                    if (thisPassword.equals(thisGroup.password)) {
                        wl.acquire();
                        passwordView.setVisibility(View.INVISIBLE);
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
            int size = Integer.parseInt(s.riderCount);

            TextView groupName = (TextView) myView.findViewById(R.id.groupName);
            groupName.setText(s.name);

            TextView members = (TextView) myView.findViewById(R.id.membersTV);
            members.setText("Riders :" + size);

            return myView;
        }

    }

    private void initiateList() {
        Log.i("initiateList", "listView");
        listView = (ListView) findViewById(R.id.groupsListView);
        myAdapter = new MyGroupAdapter(groups);
        listView.setAdapter(myAdapter);
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

            final DatabaseReference thisRef = database.getReference();

            thisRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    Log.i("loadGroupsFromGoogle", "Count :" + dataSnapshot.getChildrenCount());
                    groups.clear();
                    members.clear();

                    for (DataSnapshot groupDS : dataSnapshot.getChildren()) {
                        Log.i("DataSnapshot", "" + groupDS);
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

                                RideOutGroup newGroup = new RideOutGroup(ID, name, password, count);

                                groups.add(newGroup);
                            }

                            if (detailsDS.getKey().equals("Riders")) {
                                // counts the children of the Riders node to get the rider count in each group
                                myRef.child(groupDS.getKey()).child("Details").child("RiderCount").setValue(Long.toString(detailsDS.getChildrenCount()));
                                Log.i("detailsDS", "riderCount " + detailsDS.getChildrenCount());
                                if (activeGroup != null) {
                                    Log.i("detailsDS", "activeGroup.ID " + activeGroup.ID);
                                    Log.i("detailsDS", "groupDS.getKey " + groupDS.getKey());
                                    if (groupDS.getKey().equals(activeGroup.ID)) {
                                        for (DataSnapshot riderDS : detailsDS.getChildren()) {
                                            // check all the rider data has been added to Google
                                            if (riderDS.getChildrenCount() == 5) {
                                                Log.i("riderDS", "riderDS " + riderDS);
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

                                                Log.i("Adding newRider", "" + newRider);
                                                members.add(newRider);
                                            }
                                        }
                                    }
                                }
                            }

                            if (detailsDS.getKey().equals("Messages")) {
                                if (activeGroup != null) {
                                    if (groupDS.getKey().equals(activeGroup.ID)) {
                                        messages.clear();
                                        for (DataSnapshot messagesDS : detailsDS.getChildren()) {
                                            // check all the rider data has been added to Google
                                            if (messagesDS.getChildrenCount() == 3) {
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
                                        }
                                    }
                                }
                            }
                        }
                    }
//                    Collections.sort(groups);
                    myAdapter.notifyDataSetChanged();
                    if (mapView) {
                        Log.i("Updated Date", "You're in MapView");
                        MapsActivity.showRiders(members);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    public static void saveGroupToGoogle(RideOutGroup o) {
        Log.i("saveGroupToGoogle", "Starting");

        // save the group
        myRef.child(o.ID).child("Details").child("Name").setValue(o.name);
        myRef.child(o.ID).child("Details").child("Password").setValue(o.password);
        myRef.child(o.ID).child("Details").child("ID").setValue(o.ID);
        myRef.child(o.ID).child("Details").child("Created").setValue(o.created.toString());
        myRef.child(o.ID).child("Details").child("Live").setValue(o.live.toString());
        myRef.child(o.ID).child("Details").child("RiderCount").setValue(o.riderCount);
    }

    public static void addMemberToGoogle(GroupMember m, RideOutGroup o) {

        Log.i("addMemberToGoogle", "" + m);
        myRef.child(o.ID).child("Riders").child(m.ID).child("riderState").setValue(m.state);
        myRef.child(o.ID).child("Riders").child(m.ID).child("riderName").setValue(m.name);
        myRef.child(o.ID).child("Riders").child(m.ID).child("Lat").setValue("1");
        myRef.child(o.ID).child("Riders").child(m.ID).child("Lon").setValue("50");
    }

    public static void removeMemberFromGoogle(String uid, RideOutGroup o) {
        myRef.child(o.ID).child("Riders").child(uid).removeValue();
    }

    public static void saveTrip() {
        Log.i("saveTrip", "size " + trip.size());
        ed.putInt("tripSize", trip.size()).apply();
        if (trip.size() > 0) {

            try {

                tripDB.execSQL("CREATE TABLE IF NOT EXISTS trip (lat VARCHAR, lon VARCHAR, time VARCHAR)");

                tripDB.delete("trip", null, null);

                for (TripMarker thisTrip : trip) {

                    // change into String to be saved to the DB
                    String thisLat = Double.toString(thisTrip.location.getLatitude());
                    String thisLon = Double.toString(thisTrip.location.getLongitude());
                    String thisTime = Long.toString(thisTrip.timeStamp);

                    Log.i("Sacing TripMarker", thisLat + " " + thisLon + " " + thisTime);
                    tripDB.execSQL("INSERT INTO trip (lat, lon, time) VALUES ('" + thisLat + "' , '" + thisLon + "' , '" + thisTime + "')");

                }

            } catch (Exception e) {

                e.printStackTrace();

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
                Location thisLocation = new Location("50,1");
                thisLocation.setLatitude(Double.parseDouble(c.getString(latIndex)));
                thisLocation.setLongitude(Double.parseDouble(c.getString(lonIndex)));
                TripMarker newMarker = new TripMarker(thisLocation, Long.parseLong(c.getString(timeIndex)), "Loading");

                trip.add(newMarker);

            } while (c.moveToNext());


        } catch (Exception e) {

            Log.i("LoadingDB", "Caught Error");
            e.printStackTrace();

        }
    }


    public static void saveSettings() {
        Log.i("Main Activity", "saveSettings");
        if (activeGroup != null) {
            ed.putString("activeGroupID", activeGroup.ID).apply();
        }
    }

    public static void loadSettings() {
        Log.i("Main Activity", "loadSettings");
        String activeGroupID = sharedPreferences.getString("activeGroupID", "null");
        if (activeGroupID.equals("null")) {
            activeGroup = null;
            Log.i("activeGroup not found", "" + activeGroup);
        } else {
            // find active group from the ID
            for (RideOutGroup thisGroup : groups) {
                if (thisGroup.ID.equals(activeGroupID)) {
                    activeGroup = thisGroup;
                    Log.i("activeGroup found", "" + activeGroup);
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
        saveSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        myAdapter.notifyDataSetChanged();
        if (activeGroup != null) {
            Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("onDestroy", "Starting");
        mAuth.removeAuthStateListener(mAuthListener);
        saveTrip();
    }


}
