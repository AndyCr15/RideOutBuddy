package com.androidandyuk.rideoutbuddy;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by AndyCr15 on 11/07/2017.
 */

public class GroupMember {
    String ID;
    String name;
    LatLng location;
    String state;

    public GroupMember(String ID, String name) {
        this.ID = ID;
        this.name = name;
    }

}
