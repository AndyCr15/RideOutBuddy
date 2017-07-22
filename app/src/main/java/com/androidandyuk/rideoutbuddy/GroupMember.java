package com.androidandyuk.rideoutbuddy;

import android.location.Location;
import android.util.Log;

/**
 * Created by AndyCr15 on 11/07/2017.
 */

enum GPSrateEnum {
    High(3), Med(2),Low(1);
    private final int value;

    private GPSrateEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

public class GroupMember {
    String ID;
    String name;
    Location location;
    String state;
    String updated;

    public GroupMember(String ID, String name) {
        this.ID = ID;
        this.name = name;
        this.state = "Normal";
        this.location = new Location("1,53");
        this.location.setLatitude(51.5007292);
        this.location.setLongitude(-0.1268194);
        Log.i("GroupMember created","Lat :" + this.location.getLatitude() + " Lon : " + this.location.getLongitude());
    }

    public GroupMember(String ID, String name, Location location, String state, String updated) {
        this.ID = ID;
        this.name = name;
        this.location = location;
        this.state = state;
        this.updated = updated;
    }

    @Override
    public String toString() {
        return name + " ID:" + ID;
    }

    public String getID() {
        return ID;
    }
}
