package com.androidandyuk.rideoutbuddy;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class CreateGroup extends AppCompatActivity {

    EditText groupName;
    EditText groupPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        groupName = (EditText)findViewById(R.id.groupNameET);
        groupPassword = (EditText)findViewById(R.id.groupPasswordET);
    }

    public void createGroup(View view){
        Log.i("createGroup","Started");
        RideOutGroup thisGroup = new RideOutGroup(groupName.getText().toString(), groupPassword.getText().toString());
    }
}
