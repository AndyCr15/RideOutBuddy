package com.androidandyuk.rideoutbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import static com.androidandyuk.rideoutbuddy.MainActivity.activeGroup;
import static com.androidandyuk.rideoutbuddy.MainActivity.addMemberToGoogle;
import static com.androidandyuk.rideoutbuddy.MainActivity.groups;
import static com.androidandyuk.rideoutbuddy.MainActivity.messages;
import static com.androidandyuk.rideoutbuddy.MainActivity.saveGroupToGoogle;
import static com.androidandyuk.rideoutbuddy.MainActivity.saveSettings;
import static com.androidandyuk.rideoutbuddy.MainActivity.user;
import static com.androidandyuk.rideoutbuddy.MainActivity.userMember;
//import static com.androidandyuk.rideoutbuddy.MainActivity.wl;

public class CreateGroup extends AppCompatActivity {

    EditText groupName;
    EditText groupPassword;

    private DatabaseReference messagesDB;
    private String temp_key;

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
//        wl.acquire();
        saveSettings();
//        activeGroup.members.add(userMember);
        groups.add(activeGroup);
        saveGroupToGoogle(activeGroup);
        addMemberToGoogle(userMember, activeGroup);

        groupPassword.setText("");

        // add a message so it can be clicked to get to the chatroom

        messagesDB = FirebaseDatabase.getInstance().getReference().child(activeGroup.ID).child("Messages");

        temp_key = messagesDB.push().getKey();

        Map<String, Object> map = new HashMap<String, Object>();
        messagesDB.updateChildren(map);
        DatabaseReference message_root = messagesDB.child(temp_key);
        Map<String, Object> map2 = new HashMap<String, Object>();
        map2.put("name", user.getDisplayName());
        map2.put("msg", "Welcome to " + groupName.getText().toString() + " group. Tap here to join chat!");
        String stamp = Long.toString(System.currentTimeMillis());
        map2.put("stamp", stamp);
        message_root.updateChildren(map2);
        ChatMessage thisMessage = new ChatMessage(temp_key, user.getDisplayName(), "Welcome to " + groupName.getText().toString() + " group. Tap here to join chat!", stamp);
        messages.add(thisMessage);
        addMemberToGoogle(userMember, activeGroup);
        Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
        startActivity(intent);
    }


}
