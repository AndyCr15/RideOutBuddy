package com.androidandyuk.rideoutbuddy;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static com.androidandyuk.rideoutbuddy.MainActivity.oneDecimal;
import static com.androidandyuk.rideoutbuddy.MainActivity.trip;

public class EditTrip extends AppCompatActivity {

    static MyListAdapter myListAdapter;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_trip);

        initiateList();

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                final int markerPosition = position;
                final Context context = getApplicationContext();

                new AlertDialog.Builder(EditTrip.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Are you sure?")
                        .setMessage("You're about to delete this waypoint forever...")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.i("Removing", "Log " + markerPosition);
                                trip.remove(markerPosition);
                                initiateList();
                                Toast.makeText(context, "Deleted!", Toast.LENGTH_SHORT).show();

                            }
                        })
                        .setNegativeButton("No", null)
                        .show();

                return true;
            }


        });

    }

    private class MyListAdapter extends BaseAdapter {
        public ArrayList<TripMarker> groupDataAdapter;

        public MyListAdapter(ArrayList<TripMarker> groupDataAdapter) {
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
            View myView = mInflater.inflate(R.layout.trip_listview, null);

            final TripMarker s = groupDataAdapter.get(position);

            TextView number = (TextView) myView.findViewById(R.id.number);
            number.setText(Integer.toString(position));

            TextView status = (TextView) myView.findViewById(R.id.status);
            status.setText("");
            if (s.stationary) {
                status.setText("(Stationary)");
            }
            if (s.start) {
                status.setText("(Start)");
            }
            if (s.stop) {
                status.setText("(Stop)");
            }

            Log.i("Start " + s.start, "Stationary " + s.stationary);


            TextView time = (TextView) myView.findViewById(R.id.time);
            time.setText(MainActivity.millisToTime(s.timeStamp));

            TextView aveSpeed = (TextView) myView.findViewById(R.id.aveSpeed);
            Double thisAve = 0d;
            if (position > 0) {
                Double thisDistance = MapsActivity.getDistance(trip.get(position - 1).location, trip.get(position).location);
                Long thisMillis = trip.get(position).timeStamp - trip.get(position - 1).timeStamp;
                Double thisHours = (double) thisMillis / 3600000L;
                thisAve = (double) thisDistance / thisHours;
            }
            aveSpeed.setText(oneDecimal.format(thisAve) + "mph");

            return myView;
        }

    }

    private void initiateList() {
        Log.i("initiateList", "listView");
        listView = (ListView) findViewById(R.id.tripList);
        myListAdapter = new MyListAdapter(trip);
        listView.setAdapter(myListAdapter);
        myListAdapter.notifyDataSetChanged();
    }

}
