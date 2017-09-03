package com.androidandyuk.rideoutbuddy;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static android.os.Build.ID;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

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
    public static Location userHome = new Location("0, 0");

    public static LocationManager locationManager;
    public static LocationListener locationListener;
    public static int locationUpdatesTime = 20000;
    public static int locationUpdatesDistance = 100;
    public static int geofenceSizeMeters = 400;
    public static Location lastKnownLocation;
    public static int groupListMethod = 1;
    public static Boolean mapView = false;

    public static String mapType = "Normal";
    public static String GPSrate = "Med";
    public static String geofenceSize = "Med";

    public static SimpleDateFormat sdf = new SimpleDateFormat("dd/MMM/yyyy");
    public static SimpleDateFormat dayOfWeek = new SimpleDateFormat("EEEE");
    public static SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd/MM HH:mm:ss");

    public static final DecimalFormat precision = new DecimalFormat("0.00");
    public static final DecimalFormat oneDecimal = new DecimalFormat("0.#");

    public static int timeDifference;

    public static String jsonLocation = "http://www.lanarchy.co.uk/";

    public static ArrayList<RideOutGroup> groups;
    public static ArrayList<GroupMember> members;
    public static ArrayList<ChatMessage> messages;
    public static ArrayList<TripMarker> trip;

    public static SQLiteDatabase tripDB;
    public static DatabaseReference messagesDB;

    public static String devVersion;
    public static String devRelease;
    public static String devComment;
    public static String devURL;
    public static String appVersion;

    public static PowerManager pm;
//    public static PowerManager.WakeLock wl;

    public static Boolean recordingTrip = false;

    private static ValueEventListener groupListener;

    static MyGroupAdapter myAdapter;
    ListView listView;
    View passwordView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        Log.i("MainActivity", "onCreate");

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

        passwordView = findViewById(R.id.passwordView);

        TextView versionInfoTV = (TextView) findViewById(R.id.versionInfoTV);
        appVersion = "tbc";
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            appVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        versionInfoTV.setText("Version :" + appVersion + " (Go to Settings to check this is the latest)");


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
                    userMember = new GroupMember(user.getUid(), user.getDisplayName());
                    invalidateOptionsMenu();
                    loadGroupsFromGoogle();
                    setToolbarUser();
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

        groupListener = (new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (!mapView) {
                    Log.i("ValueEventListener", "Count :" + dataSnapshot.getChildrenCount());
                    groups.clear();

                    for (final DataSnapshot groupDS : dataSnapshot.getChildren()) {
                        Log.i("DataSnapshot groupDS", "" + groupDS);

                        for (DataSnapshot detailsDS : groupDS.getChildren()) {

                            if (detailsDS.getKey().equals("Details")) {
                                Log.i("detailsDS", "" + detailsDS);
                                if (detailsDS.getChildrenCount() == 6) {
                                    GenericTypeIndicator<Map<String, String>> genericTypeIndicator = new GenericTypeIndicator<Map<String, String>>() {
                                    };

                                    Map<String, String> map = null;
                                    map = detailsDS.getValue(genericTypeIndicator);

                                    String name = map.get("Name");
                                    String password = map.get("Password");
                                    String ID = map.get("ID");
                                    String count = map.get("RiderCount");
                                    String created = map.get("Created");
                                    Long lastUsed = Long.parseLong(map.get("LastUsed"));

                                    // groups not used for 4 days are deleted
                                    if ((lastUsed + 345600000) > System.currentTimeMillis()) {
                                        rootDB.child(groupDS.getKey()).child("Details").child("RiderCount").setValue(Long.toString(dataSnapshot.child(groupDS.getKey()).child("Riders").getChildrenCount()));
                                        RideOutGroup newGroup = new RideOutGroup(ID, name, password, count, created, lastUsed);
                                        groups.add(newGroup);
                                        myAdapter.notifyDataSetChanged();
                                    } else {
                                        //remove group
                                        rootDB.child(ID).removeValue();
                                        Log.i("Removing Old Group", ID);
                                    }
                                }
                            }
                        }
                    }
                    loadSettings();
                    initiateList();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        loadGroupsFromGoogle();

        initiateList();
        loadSettings();
        loadTrip();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                activeGroup = groups.get(position);
                saveSettings();
                Log.i("listView", "Tapped " + position);

                // if you created the group, no more use of the password check
                if (groups.get(position).created.equals(userMember.ID)) {
                    addMemberToGoogle(userMember, activeGroup);
                    rootDB.removeEventListener(groupListener);
                    Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                    startActivity(intent);
                } else {
                    checkPassword();
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                final int thisPosition = position;
                String groupCreator = groups.get(position).created;

                if (groupCreator.equals(userMember.ID)) {
                    Log.i("riderCount", groups.get(position).riderCount);

                    if (groups.get(position).riderCount.equals("0")) {

                        new AlertDialog.Builder(MainActivity.this)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle("Remove this group?")
                                .setMessage("The group will be deleted forever")
                                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        //remove group
                                        rootDB.child(groups.get(thisPosition).ID).removeValue();
                                        groups.remove(thisPosition);
                                        myAdapter.notifyDataSetChanged();
                                        Log.i("Removing Old Group", ID);
                                    }
                                })
                                .setNegativeButton("Keep", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                    }
                                })
                                .show();


                    } else {
                        Toast.makeText(MainActivity.this, "The group must be empty to delete it.", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(MainActivity.this, "You can only remove groups you created.", Toast.LENGTH_SHORT).show();
                }

                return true;
            }
        });

//        new MyAsyncTaskgetNews().execute(jsonLocation + "devnotes.json");

        setToolbar();

    }

    private void setToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }

    public void setToolbarUser(){
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        TextView userName = (TextView) headerView.findViewById(R.id.nav_user);
        if(userMember.name!=null) {
            userName.setText(userMember.name);
        }
    }

    public boolean checkUpdate() {

        Log.i("App " + appVersion, "Dev " + devVersion);
        if (!appVersion.equals(devVersion) && devVersion != null) {
            Toast.makeText(this, "Update your app! Go to Settings", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getApplicationContext(), Settings.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, intent, 0);

            Notification notification = new Notification.Builder(getApplicationContext())
                    .setContentTitle("Update Ride Out Buddy!")
                    .setContentText("There's a new Beta version of Ride Out Buddy. Update in the Settings page.")
                    .setContentIntent(pendingIntent)
                    .addAction(android.R.drawable.btn_default, "GO TO SETTINGS", pendingIntent)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setAutoCancel(true)
                    .build();

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.notify(1, notification);

            return true;
        }
        return false;
    }

    public void versionCheck(View view) {
        Intent intent = new Intent(getApplicationContext(), Settings.class);
        startActivity(intent);
    }

    public void checkPassword() {

        Log.i("checkPassword", "members.size() " + members.size());

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
                    Log.i("checkPassword", thisPassword);
                    if (thisPassword.equals(activeGroup.password)) {
                        Log.i("checkPassword", "Correct");
                        password.setText("");
                        passwordView.setVisibility(View.INVISIBLE);
                        addMemberToGoogle(userMember, activeGroup);
                        rootDB.removeEventListener(groupListener);
                        Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                        startActivity(intent);
                        Log.i("MainActivity", "Calling finish()");
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

//    public void changeListedBy(View view) {
//        groupListMethod += 1;
//        if (groupListMethod > 3) {
//            groupListMethod = 1;
//        }
//
//        updateSortedBy();
//
//    }

//    private void updateSortedBy() {
//        TextView listedByTV = (TextView) findViewById(R.id.listedByTV);
//        switch (groupListMethod) {
//            case 1:
//                listedByTV.setText("Sorted Alphabetically");
//                initiateList();
//                return;
//            case 2:
//                listedByTV.setText("Sorted Reverse Alphabetically");
//                initiateList();
//                return;
//            case 3:
//                listedByTV.setText("Sorted Most Recently Used");
//                initiateList();
//                return;
//        }
//    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_settings) {

            Intent intent = new Intent(getApplicationContext(), Settings.class);
            startActivity(intent);

        } else if (id == R.id.nav_logout) {

            if (user == null) {
                signIn();
            } else {
                signOut();
            }

        } else if (id == R.id.nav_atoz) {

            groupListMethod = 1;
            initiateList();

        } else if (id == R.id.nav_ztoa) {

            groupListMethod = 2;
            initiateList();

        } else if (id == R.id.nav_time) {

            groupListMethod = 3;
            initiateList();

        }else if (id == R.id.nav_exit) {

            finish();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;

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
            if (userMember != null) {
                if (s.created.equals(userMember.ID)) {
                    groupName.setTypeface(null, Typeface.BOLD);
                }
            }

            TextView members = (TextView) myView.findViewById(R.id.membersTV);
            int size = Integer.parseInt(s.riderCount);
            members.setText("Riders :" + size);

            TextView lastUsed = (TextView) myView.findViewById(R.id.lastUsedTV);
            lastUsed.setText("Last Used :" + millisToTime(s.lastUsed));

            return myView;
        }

    }

    private void initiateList() {
        Log.i("initiateList", "listView");
        listView = (ListView) findViewById(R.id.groupsListView);
        myAdapter = new MyGroupAdapter(groups);
        listView.setAdapter(myAdapter);
        Collections.sort(groups);
        myAdapter.notifyDataSetChanged();
    }

    public static String millisToTime(Long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return dateTimeFormatter.format(calendar.getTime());
    }

    public static String millisToHours(Long millis) {
        return String.format("%d hrs, %02d mins",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)));
    }

    // get news from server
    public class MyAsyncTaskgetNews extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            //before works
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                Log.i("Car Shows", "doInBackground");
                String NewsData;
                //define the url we have to connect with
                URL url = new URL(params[0]);
                //make connect with url and send request
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                //waiting for 7000ms for response
                urlConnection.setConnectTimeout(15000);//set timeout to 15 seconds

                try {
                    //getting the response data
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    //convert the stream to string
                    NewsData = ConvertInputToStringNoChange(in);
                    //send to display data
                    publishProgress(NewsData);
                } finally {
                    //end connection
                    urlConnection.disconnect();
                }

            } catch (Exception ex) {
                Log.i("Exception Caught ", "" + ex);
            }
            return null;
        }

        protected void onProgressUpdate(String... progress) {
            try {
                Log.i("Car Shows", "Getting JSON");
                JSONArray json = new JSONArray(progress[0]);
                Log.i("JSON size", "" + json.length());

//                for (int i = 0; i < json.length(); i++) {
                JSONObject notes = json.getJSONObject(0);
                devVersion = notes.getString("version");
                devRelease = notes.getString("release");
                devComment = notes.getString("comment");
                devURL = notes.getString("url");
                Log.i("Adding notes ", devVersion);
//                }
            } catch (Exception ex) {
                Log.i("JSON failed", "" + ex);
            }
        }

        protected void onPostExecute(String result2) {
//            checkUpdate();
        }

    }

    // this method convert any stream to string
    public static String ConvertInputToStringNoChange(InputStream inputStream) {

        BufferedReader bureader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        String linereultcal = "";

        try {
            while ((line = bureader.readLine()) != null) {

                linereultcal += line;

            }
            inputStream.close();


        } catch (Exception ex) {
        }

        return linereultcal;
    }

    //   GOOGLE SIGN IN

    public void signIn() {
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
                Log.i("onActivityResult", "Sign in failed :" + result.getStatus());
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
        userMember = null;
        invalidateOptionsMenu();
        signIn();
    }

    //   GOOGLE SIGN IN END

    public static void loadGroupsFromGoogle() {
        if (user != null) {

            Log.i("loadGroupsFromGoogle", "Loading");

            rootDB.addValueEventListener(groupListener);

        }
    }

    public static void saveGroupToGoogle(RideOutGroup o) {
        Log.i("saveGroupToGoogle", "Starting");

        // save the group
        rootDB.child(o.ID).child("Details").child("Name").setValue(o.name);
        rootDB.child(o.ID).child("Details").child("Password").setValue(o.password);
        rootDB.child(o.ID).child("Details").child("ID").setValue(o.ID);
        rootDB.child(o.ID).child("Details").child("Created").setValue(o.created);
        rootDB.child(o.ID).child("Details").child("LastUsed").setValue(Long.toString(System.currentTimeMillis()));
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

        tripDB.execSQL("CREATE TABLE IF NOT EXISTS trip (lat VARCHAR, lon VARCHAR, time VARCHAR, start INTEGER, stop INTEGER, stationary INTEGER)");

        tripDB.delete("trip", null, null);

        if (trip.size() > 0) {

            try {

                for (TripMarker thisTrip : trip) {

                    // change into String to be saved to the DB
                    String thisLat = Double.toString(thisTrip.location.getLatitude());
                    String thisLon = Double.toString(thisTrip.location.getLongitude());
                    String thisTime = Long.toString(thisTrip.timeStamp);
                    int startInt = (thisTrip.start) ? 1 : 0;
                    int stopInt = (thisTrip.start) ? 1 : 0;
                    int stationaryInt = (thisTrip.start) ? 1 : 0;

                    Log.i("Saving TripMarker", thisLat + " " + thisLon + " " + thisTime);
                    tripDB.execSQL("INSERT INTO trip (lat, lon, time, start, stop, stationary) VALUES ('" + thisLat + "' , '" + thisLon + "' , '" + thisTime + "' , '" + startInt + "' , '" + stopInt + "' , '" + stationaryInt + "')");

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
            int startIndex = c.getColumnIndex("start");
            int stopIndex = c.getColumnIndex("stop");
            int stationaryIndex = c.getColumnIndex("stationary");

            c.moveToFirst();

            do {
                Location thisLocation = new Location("51.5007292,-0.1268194");
                thisLocation.setLatitude(Double.parseDouble(c.getString(latIndex)));
                thisLocation.setLongitude(Double.parseDouble(c.getString(lonIndex)));
                Boolean start = (c.getInt(startIndex) == 1) ? true : false;
                Boolean stop = (c.getInt(stopIndex) == 1) ? true : false;
                Boolean stationary = (c.getInt(stationaryIndex) == 1) ? true : false;
                TripMarker newMarker = new TripMarker(thisLocation, Long.parseLong(c.getString(timeIndex)), start, stop, stationary, "Loading");

                trip.add(newMarker);

            } while (c.moveToNext());

            Log.i("loadTrip", "trip.size " + trip.size());

        } catch (Exception e) {

            Log.i("LoadingDB", "Caught Error");
            e.printStackTrace();

        }
    }

    public static void saveSettings() {
        Log.i("Main Activity", "saveSettings");
        ed.putString("GPSrate", GPSrate).apply();
        ed.putString("mapType", mapType).apply();
        ed.putString("geofenceSize", geofenceSize).apply();
        ed.putString("userLat", String.valueOf(userHome.getLatitude())).apply();
        ed.putString("userLon", String.valueOf(userHome.getLongitude())).apply();
//        ed.putBoolean("mapView", mapView).apply();
        if (activeGroup != null) {
            ed.putString("activeGroupID", activeGroup.ID).apply();
        } else {
            ed.putString("activeGroupID", "null").apply();
        }
    }

    public static void loadSettings() {
        GPSrate = sharedPreferences.getString("GPSrate", "Med");
        mapType = sharedPreferences.getString("mapType", "Normal");
        geofenceSize = sharedPreferences.getString("geofenceSize", "Med");

        String userLat = sharedPreferences.getString("userLat", "0");
        String userLon = sharedPreferences.getString("userLon", "0");
        userHome.setLatitude(Double.parseDouble(userLat));
        userHome.setLongitude(Double.parseDouble(userLon));

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
                }
            }
        }
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
        Log.i("MainActivity", "onPause");
        saveSettings();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        myAdapter.notifyDataSetChanged();
        Log.i("onResume", "activeGroup" + activeGroup);
        loadSettings();
        loadGroupsFromGoogle();
//        checkUpdate();
    }

    @Override
    protected void onDestroy() {
        Log.i("MainActivity", "onDestroy");
        mAuth.removeAuthStateListener(mAuthListener);
        rootDB.removeEventListener(groupListener);
        saveTrip();
        super.onDestroy();
    }


}
