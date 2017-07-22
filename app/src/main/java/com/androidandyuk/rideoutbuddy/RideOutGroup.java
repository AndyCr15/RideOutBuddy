package com.androidandyuk.rideoutbuddy;

import android.support.annotation.NonNull;

/**
 * Created by AndyCr15 on 11/07/2017.
 */

public class RideOutGroup implements Comparable<RideOutGroup> {
    String ID;
    String name;
    String password;
    Long created;
    Boolean live;
    String riderCount;

    public RideOutGroup(String name, String password) {
        this.name = name;
        this.password = password;
        this.ID = name + System.currentTimeMillis();
        this.created = System.currentTimeMillis();
        this.live = true;
        this.riderCount = "0";
    }

    public RideOutGroup(String ID, String name, String password, String riderCount, Long created) {
        this.name = name;
        this.password = password;
        this.ID = ID;
        this.created = created;
        this.live = true;
        this.riderCount = riderCount;
    }


    @Override
    public int compareTo(@NonNull RideOutGroup o) {

        Long resultDate1 = o.created;
        Long resultDate2 = this.created;

        if (resultDate1 < resultDate2) {
            return -1;
        }

        if (resultDate2 > resultDate1) {
            return 1;
        }

        return 0;
    }
}
