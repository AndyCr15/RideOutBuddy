package com.androidandyuk.rideoutbuddy;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import static com.androidandyuk.rideoutbuddy.MainActivity.activeGroup;
import static com.androidandyuk.rideoutbuddy.MainActivity.addMemberToGoogle;
import static com.androidandyuk.rideoutbuddy.MainActivity.groups;
import static com.androidandyuk.rideoutbuddy.MainActivity.saveGroupToGoogle;
import static com.androidandyuk.rideoutbuddy.MainActivity.saveSettings;
import static com.androidandyuk.rideoutbuddy.MainActivity.userMember;

public class CreateGroup extends AppCompatActivity {

    EditText groupName;
    EditText groupPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        groupName = (EditText) findViewById(R.id.groupNameET);
        groupPassword = (EditText) findViewById(R.id.groupPasswordET);
    }

    public void createGroup(View view) {
        Log.i("createGroup", "Started");
        activeGroup = new RideOutGroup(groupName.getText().toString(), groupPassword.getText().toString());
        saveSettings();
        activeGroup.members.add(userMember);
        groups.add(activeGroup);
        saveGroupToGoogle(activeGroup);
        addMemberToGoogle(userMember, activeGroup);
        finish();
    }




}
