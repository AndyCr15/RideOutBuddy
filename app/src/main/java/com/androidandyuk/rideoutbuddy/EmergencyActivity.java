package com.androidandyuk.rideoutbuddy;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import static com.androidandyuk.rideoutbuddy.MainActivity.activeGroup;
import static com.androidandyuk.rideoutbuddy.MainActivity.messages;
import static com.androidandyuk.rideoutbuddy.MainActivity.user;
import static com.androidandyuk.rideoutbuddy.MapsActivity.myChatAdapter2;

public class EmergencyActivity extends AppCompatActivity {

    private DatabaseReference messagesDB;
    private String temp_key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        Log.i("EmergencyOnCreate","Started");

        messagesDB = FirebaseDatabase.getInstance().getReference().child(activeGroup.ID).child("Messages");

    }

    public void emergencySelected(View view) {
        String reason = "** EMERGENCY **  -  " + view.getTag().toString();
        Log.i("EmergencySelected", reason);

        temp_key = messagesDB.push().getKey();

        Map<String, Object> map = new HashMap<String, Object>();
        messagesDB.updateChildren(map);
        DatabaseReference message_root = messagesDB.child(temp_key);
        Map<String, Object> map2 = new HashMap<String, Object>();
        map2.put("name", user.getDisplayName());
        map2.put("msg", reason);
        String stamp = Long.toString(System.currentTimeMillis());
        map2.put("stamp", stamp);
        message_root.updateChildren(map2);
        ChatMessage thisMessage = new ChatMessage(temp_key, user.getDisplayName(), reason, stamp);
        messages.add(thisMessage);

        finish();
    }

    public void clearEmergencies(View view){

        new AlertDialog.Builder(EmergencyActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Remove ALL Emergencies?")
                .setMessage("Would you like to remove ALL emergencies, or just your own?")
                .setPositiveButton("All", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        for(ChatMessage thisMessage : messages){
                            if(thisMessage.message.contains("** EMERGENCY **")){
                                Log.i("Emergency Found", "thisMessage.ID " + thisMessage.ID);
                                messagesDB.child(thisMessage.ID).removeValue();
                                messages.remove(thisMessage);
                                myChatAdapter2.notifyDataSetChanged();
                                Toast.makeText(EmergencyActivity.this, "Emergency found and removed", Toast.LENGTH_SHORT).show();
                            }
                        }
                        finish();

                    }
                })
                .setNegativeButton("Mine", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        for(ChatMessage thisMessage : messages){
                            if(thisMessage.message.contains("** EMERGENCY **") && thisMessage.name.equals(user.getDisplayName())){
                                Log.i("Emergency Found", "thisMessage.ID " + thisMessage.ID);
                                messagesDB.child(thisMessage.ID).removeValue();
                                messages.remove(thisMessage);
                                myChatAdapter2.notifyDataSetChanged();
                                Toast.makeText(EmergencyActivity.this, "Emergency found and removed", Toast.LENGTH_SHORT).show();
                            }
                        }
                        finish();

                    }
                })
                .show();
    }

    @Override
    public void onBackPressed() {
        // this must be empty as back is being dealt with in onKeyDown
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
