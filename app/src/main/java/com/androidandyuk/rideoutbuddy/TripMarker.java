package com.androidandyuk.rideoutbuddy;

import android.location.Location;

/**
 * Created by AndyCr15 on 16/07/2017.
 */

public class TripMarker {
    Location location;
    Long timeStamp;

    public TripMarker(Location location) {
        this.location = location;
        this.timeStamp = System.currentTimeMillis();
    }

    public TripMarker(Location location, Long millis, String loading) {
        this.location = location;
        this.timeStamp = millis;
    }
}
