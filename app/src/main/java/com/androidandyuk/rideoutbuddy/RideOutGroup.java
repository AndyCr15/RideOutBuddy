package com.androidandyuk.rideoutbuddy;

/**
 * Created by AndyCr15 on 11/07/2017.
 */

public class RideOutGroup {
    String ID;
    String name;
    String password;
    Long endTime;
    Boolean live;

    public RideOutGroup(String name, String password) {
        this.name = name;
        this.password = password;
        this.ID = name + System.currentTimeMillis();
        this.live = true;
    }
}
