package com.androidandyuk.rideoutbuddy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import static com.androidandyuk.rideoutbuddy.MainActivity.GPSrate;
import static com.androidandyuk.rideoutbuddy.MainActivity.appVersion;
import static com.androidandyuk.rideoutbuddy.MainActivity.devComment;
import static com.androidandyuk.rideoutbuddy.MainActivity.devRelease;
import static com.androidandyuk.rideoutbuddy.MainActivity.devURL;
import static com.androidandyuk.rideoutbuddy.MainActivity.devVersion;
import static com.androidandyuk.rideoutbuddy.MainActivity.geofenceSize;
import static com.androidandyuk.rideoutbuddy.MainActivity.geofenceSizeMeters;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationUpdatesDistance;
import static com.androidandyuk.rideoutbuddy.MainActivity.locationUpdatesTime;
import static com.androidandyuk.rideoutbuddy.MainActivity.mapType;
import static com.androidandyuk.rideoutbuddy.MainActivity.saveSettings;
import static com.androidandyuk.rideoutbuddy.MainActivity.user;
import static com.androidandyuk.rideoutbuddy.MainActivity.userMember;

public class Settings extends AppCompatActivity {

    Spinner spinnerGPSrate;
    Spinner mapTypeSpinner;
    Spinner geofenceSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setTitle("Settings");

        spinnerGPSrate = (Spinner) findViewById(R.id.spinnerGPS);
        spinnerGPSrate.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, GPSrateEnum.values()));

        mapTypeSpinner = (Spinner) findViewById(R.id.mapTypeSpinner);
        mapTypeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, MapTypeEnum.values()));

        geofenceSpinner = (Spinner) findViewById(R.id.geofenceSpinner);
        geofenceSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, GeofenceEnum.values()));

        setSpinners();

        TextView loggedInTV = (TextView) findViewById(R.id.loggedInTV);
        TextView yourVersionTV = (TextView) findViewById(R.id.yourVersionTV);
        TextView latestVersionTV = (TextView) findViewById(R.id.latestVersionTV);
        TextView releaseNotesTV = (TextView) findViewById(R.id.releaseNotesTV);
        TextView commentsTV = (TextView) findViewById(R.id.commentsTV);

        yourVersionTV.setText("Your App : " + appVersion);
        latestVersionTV.setText("Latest : " + devVersion);
        releaseNotesTV.setText("Release Notes : " + devRelease);
        commentsTV.setText("Dev Comments : " + devComment);

        if(userMember!=null) {
            loggedInTV.setText("Logged in as " + user.getDisplayName());
        } else {
            loggedInTV.setText("You must log in to Google to use this app.");
        }
    }

    public void loadUpdateFolder(View view){
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(devURL)));
    }

    private void setSpinners() {
        Log.i("SettingSpinners GPSrate", GPSrate);
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
        Log.i("SettingSpinner mapType", mapType);
        switch (mapType) {
            case "Normal":
                mapTypeSpinner.setSelection(0);
                break;
            case "Hybrid":
                mapTypeSpinner.setSelection(1);
                break;
            case "Satellite":
                mapTypeSpinner.setSelection(2);
                break;
            case "Terrain":
                mapTypeSpinner.setSelection(3);
                break;
        }
        Log.i("SetSpinner geofenceSize", geofenceSize);
        switch (geofenceSize) {
            case "Large":
                geofenceSpinner.setSelection(0);
                break;
            case "Med":
                geofenceSpinner.setSelection(1);
                break;
            case "Small":
                geofenceSpinner.setSelection(2);
                break;
        }
    }

    public static void checkSpinners() {
        Log.i("checkSpinners ", GPSrate);
        switch (GPSrate) {
            case "High":
                locationUpdatesTime = 5000;
                locationUpdatesDistance = 40;
                break;
            case "Med":
                locationUpdatesTime = 20000;
                locationUpdatesDistance = 100;
                break;
            case "Low":
                locationUpdatesTime = 60000;
                locationUpdatesDistance = 500;
                break;
        }
        Log.i("checkSpinners ", "" + geofenceSize);
        switch (geofenceSize) {
            case "Large":
                geofenceSizeMeters = 1000;
                break;
            case "Med":
                geofenceSizeMeters = 400;
                break;
            case "Small":
                geofenceSizeMeters = 100;
                break;
        }
    }

    public void SendFeedbackMail(View view) {

        //send file using email
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        // Set type to "email"
        emailIntent.setType("vnd.android.cursor.dir/email");
        String to[] = {"AndyCr15@gmail.com"};
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
        // the mail subject
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Ride Out Buddy Feedback");
        startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }

    public void watchRideOutGuide(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=Hak3egkcRDo")));
    }

    @Override
    protected void onPause() {
        spinnerGPSrate = (Spinner) findViewById(R.id.spinnerGPS);
        GPSrate = spinnerGPSrate.getSelectedItem().toString();
        geofenceSpinner = (Spinner) findViewById(R.id.geofenceSpinner);
        geofenceSize = geofenceSpinner.getSelectedItem().toString();
        checkSpinners();
        mapTypeSpinner = (Spinner) findViewById(R.id.mapTypeSpinner);
        mapType = mapTypeSpinner.getSelectedItem().toString();
        Log.i("onPause mapType", mapType);
        saveSettings();
        super.onPause();
    }
}
