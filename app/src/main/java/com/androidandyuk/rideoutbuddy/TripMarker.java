package com.androidandyuk.rideoutbuddy;

import android.location.Location;

/**
 * Created by AndyCr15 on 16/07/2017.
 */

public class TripMarker {
    Location location;
    Long timeStamp;
    Boolean start;
    Boolean stop;
    Boolean stationary;

    public TripMarker(Location location) {
        this.location = location;
        this.timeStamp = System.currentTimeMillis();
        this.start = false;
        this.stop = false;
        this.stationary = false;
    }

    public TripMarker(Location location, Long millis, Boolean start, Boolean stop, Boolean stationary, String loading) {
        this.location = location;
        this.timeStamp = millis;
        this.start = start;
        this.stop = stop;
        this.stationary = stationary;
    }
}
