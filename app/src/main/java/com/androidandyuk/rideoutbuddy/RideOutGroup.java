package com.androidandyuk.rideoutbuddy;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by AndyCr15 on 11/07/2017.
 */

public class RideOutGroup implements Comparable<RideOutGroup> {
    String ID;
    String name;
    String password;
    Date created;
    Boolean live;
    String riderCount;
    ArrayList<GroupMember> members = new ArrayList<>();

    public RideOutGroup(String name, String password) {
        this.name = name;
        this.password = password;
        this.ID = name + System.currentTimeMillis();
        this.created = new Date();
        this.live = true;
        this.riderCount = "0";
    }

    public RideOutGroup(String ID, String name, String password, String riderCount) {
        this.name = name;
        this.password = password;
        this.ID = ID;
        this.created = new Date();
        this.live = true;
        this.riderCount = riderCount;
    }


    @Override
    public int compareTo(@NonNull RideOutGroup o) {

        Date resultDate1 = o.created;
        Date resultDate2 = this.created;

        if (resultDate1.before(resultDate2)) {
            return -1;
        }

        if (resultDate2.before(resultDate1)) {
            return 1;
        }

        return 0;
    }
}
