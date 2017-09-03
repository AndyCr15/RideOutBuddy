package com.androidandyuk.rideoutbuddy;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.androidandyuk.rideoutbuddy.MainActivity.activeGroup;
import static com.androidandyuk.rideoutbuddy.MainActivity.messages;
import static com.androidandyuk.rideoutbuddy.MainActivity.messagesDB;
import static com.androidandyuk.rideoutbuddy.MainActivity.millisToTime;
import static com.androidandyuk.rideoutbuddy.MainActivity.user;
import static com.androidandyuk.rideoutbuddy.MainActivity.userMember;
import static com.androidandyuk.rideoutbuddy.MapsActivity.myChatAdapter2;

/**
 * Created by filipp on 6/28/2016.
 */
public class ChatRoom extends AppCompatActivity {

    private Button btn_send_msg;
    private ListView listView;

    private String user_name, room_name;
    private String temp_key;

    static MyChatAdapter myChatAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        btn_send_msg = (Button) findViewById(R.id.btn_send);
//        input_msg = (EditText) findViewById(R.id.msg_input);
        listView = (ListView) findViewById(R.id.chatListView);

        user_name = user.getDisplayName();
        room_name = activeGroup.name;
        setTitle("Group - " + room_name);

        initiateList();

        checkMessages();

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

                String msg = input_msg.getText().toString();

                newMessage(msg);

                myChatAdapter.notifyDataSetChanged();
                listView.setSelection(myChatAdapter.getCount() - 1);

                input_msg.setText("");

            }
        });
    }

    public void newMessage(String msg) {
        temp_key = messagesDB.push().getKey();

        Map<String, Object> map = new HashMap<String, Object>();
        messagesDB.updateChildren(map);
        DatabaseReference message_root = messagesDB.child(temp_key);
        Map<String, Object> map2 = new HashMap<String, Object>();
        map2.put("name", user_name);
        map2.put("msg", msg);

        String stamp = Long.toString(System.currentTimeMillis());
        map2.put("stamp", stamp);
        message_root.updateChildren(map2);
        ChatMessage thisMessage = new ChatMessage(temp_key, user_name, msg, stamp);
        messages.add(thisMessage);

    }

    private void initiateList() {
        Log.i("initiateList", "listView");
        listView = (ListView) findViewById(R.id.chatListView);
        myChatAdapter = new MyChatAdapter(messages);
        listView.setAdapter(myChatAdapter);
    }

    private void checkMessages() {
        Log.i("checkMessages", "Called");
        messagesDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i("ChatRoom checkMessages", "onDataChange");
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

                    ChatMessage newMessage = new ChatMessage(ID, name, msg, stamp);

                    Log.i("Adding newMessage", "" + newMessage);
                    messages.add(newMessage);
                }
                myChatAdapter.notifyDataSetChanged();

                if (myChatAdapter.getCount() > 0) {
                    listView.setSelection(myChatAdapter2.getCount() - 1);
                }

                checkEmergency();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void checkEmergency() {

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
                        .setAutoCancel(true)
//                        .addAction(android.R.drawable.btn_default, "RETURN TO APP", pendingIntent)
                        .build();

                NotificationManager notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);

                notificationManager.notify(1, notification);

            }
        }

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

            time.setText(millisToTime(s.timestamp));

            TextView userName = (TextView) myView.findViewById(R.id.userName);
            userName.setText(s.name);

            TextView message = (TextView) myView.findViewById(R.id.message);
            String thisMessage = s.message;
            message.setText(s.message);
            if (thisMessage.contains("** EMERGENCY **")) {
                message.setTextColor(getResources().getColor(R.color.colorRed));
            }

            if (s.name != null) {
                if (s.name.equals(userMember.name)) {
                    Log.i("User Made Message", "Found");
                    LinearLayout chat = (LinearLayout) myView.findViewById(R.id.chat);
                    View messageView = myView.findViewById(R.id.messageView);

                    message.setTypeface(null, Typeface.BOLD);
                    message.setGravity(Gravity.RIGHT);

                    final int sdk = android.os.Build.VERSION.SDK_INT;
                    if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        messageView.setBackgroundDrawable(getResources().getDrawable(R.drawable.rounded_corners_grey_accent));
                    } else {
                        messageView.setBackground(getResources().getDrawable(R.drawable.rounded_corners_grey_accent));
                    }

                }
            }

            return myView;
        }
    }

    public static void setMargins(View v, int l, int t, int r, int b) {
        Log.i("setMargins", "" + v);
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(l, t, r, b);
            v.requestLayout();
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