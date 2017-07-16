package com.androidandyuk.rideoutbuddy;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.androidandyuk.rideoutbuddy.MainActivity.activeGroup;
import static com.androidandyuk.rideoutbuddy.MainActivity.loadGroupsFromGoogle;
import static com.androidandyuk.rideoutbuddy.MainActivity.messages;
import static com.androidandyuk.rideoutbuddy.MainActivity.timeDifference;

/**
 * Created by filipp on 6/28/2016.
 */
public class ChatRoom extends AppCompatActivity {

    private Button btn_send_msg;
    //    private EditText input_msg;
    private ListView listView;
    private String chat_msg, chat_user_name, ID, stamp;

    private String user_name, room_name;
    private DatabaseReference messagesDB;
    private String temp_key;

    static MyChatAdapter myChatAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        btn_send_msg = (Button) findViewById(R.id.btn_send);
//        input_msg = (EditText) findViewById(R.id.msg_input);
        listView = (ListView) findViewById(R.id.chatListView);

        user_name = MainActivity.user.getDisplayName();
        room_name = activeGroup.name;
        setTitle("Group - " + room_name);

        initiateList();

        messagesDB = FirebaseDatabase.getInstance().getReference().child(activeGroup.ID).child("Messages");

        // press enter to send the message
        final EditText input_msg = (EditText) findViewById(R.id.msg_input);

        input_msg.setFocusableInTouchMode(true);
        input_msg.requestFocus();

        input_msg.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    btn_send_msg.performClick();

                    return true;
                }
                return false;
            }
        });


        // use the button to send the message
        btn_send_msg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                temp_key = messagesDB.push().getKey();

                Map<String, Object> map = new HashMap<String, Object>();
                messagesDB.updateChildren(map);
                DatabaseReference message_root = messagesDB.child(temp_key);
                Map<String, Object> map2 = new HashMap<String, Object>();
                map2.put("name", user_name);
                map2.put("msg", input_msg.getText().toString());

                String stamp = Long.toString(System.currentTimeMillis());
                map2.put("stamp", stamp);
                message_root.updateChildren(map2);
                ChatMessage thisMessage = new ChatMessage(temp_key, user_name, input_msg.getText().toString(), stamp);
                messages.add(thisMessage);
                myChatAdapter.notifyDataSetChanged();



                input_msg.setText("");

            }
        });

        messagesDB.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                loadGroupsFromGoogle();
                myChatAdapter.notifyDataSetChanged();
                // set view to the last message posted
                listView.setSelection(myChatAdapter.getCount() - 1);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void initiateList() {
        Log.i("initiateList", "listView");
        listView = (ListView) findViewById(R.id.chatListView);
        myChatAdapter = new MyChatAdapter(messages);
        listView.setAdapter(myChatAdapter);
    }

    public class MyChatAdapter extends BaseAdapter {
        public ArrayList<ChatMessage> groupChatAdapter;

        public MyChatAdapter(ArrayList<ChatMessage> groupChatAdapter) {
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
            View myView = mInflater.inflate(R.layout.chat_listview, null);

            final ChatMessage s = groupChatAdapter.get(position);

            TextView time = (TextView) myView.findViewById(R.id.timeStamp);

            long minute = (s.timestamp / (1000 * 60)) % 60;
            long hour = ((s.timestamp / (1000 * 60 * 60)) % 24) + timeDifference;
            String msgTime = String.format("%02d:%02d", hour, minute);
            time.setText(msgTime);

            TextView userName = (TextView) myView.findViewById(R.id.userName);
            userName.setText(s.name);

            TextView message = (TextView) myView.findViewById(R.id.message);
            String thisMessage = s.message;
            message.setText(s.message);
            if(thisMessage.contains("** EMERGENCY **")){
                message.setTextColor(getResources().getColor(R.color.colorRed));
            }

            return myView;
        }

    }

//    private void append_chat_conversation(DataSnapshot dataSnapshot) {
//
//        Iterator iter = dataSnapshot.getChildren().iterator();
//
//        while (iter.hasNext()) {
//
//            ID = dataSnapshot.getKey().toString();
//            chat_msg = (String) ((DataSnapshot) iter.next()).getValue();
//            chat_user_name = (String) ((DataSnapshot) iter.next()).getValue();
//            stamp = ((DataSnapshot) iter.next()).getValue().toString();
//        }
//
//        ChatMessage thisMessage = new ChatMessage(ID, chat_user_name, chat_msg, stamp);
//        messages.add(thisMessage);
//        myChatAdapter.notifyDataSetChanged();
//
//        Log.i("listView.setSelection", "size " + messages.size());
//
//
//        // set view to the last message posted
//        listView.setSelection(myChatAdapter.getCount() - 1);
//    }

    @Override
    public void onBackPressed() {
        // this must be empty as back is being dealt with in onKeyDown
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            messages.clear();
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

}