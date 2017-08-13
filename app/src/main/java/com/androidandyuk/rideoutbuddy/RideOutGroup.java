package com.androidandyuk.rideoutbuddy;

import android.support.annotation.NonNull;

import static com.androidandyuk.rideoutbuddy.MainActivity.groupListMethod;

/**
 * Created by AndyCr15 on 11/07/2017.
 */

public class RideOutGroup implements Comparable<RideOutGroup> {
    String ID;
    String name;
    String password;
    String created;
    Long lastUsed;
    String riderCount;

    public RideOutGroup(String name, String password, String created) {
        this.name = name;
        this.password = password;
        this.ID = name + System.currentTimeMillis();
        this.created = created;
        this.lastUsed = System.currentTimeMillis();
        this.riderCount = "0";
    }

    public RideOutGroup(String ID, String name, String password, String riderCount, String created, Long lastUsed) {
        this.name = name;
        this.password = password;
        this.ID = ID;
        this.created = created;
        this.lastUsed = lastUsed;
        this.riderCount = riderCount;
    }


    @Override
    public int compareTo(@NonNull RideOutGroup o) {

        String result1 = o.name;
        String result2 = this.name;

        switch (groupListMethod) {
            case 1:

                return result2.compareTo(result1);

            case 2:

                return result1.compareTo(result2);

            case 3:
                Long resultDate1 = o.lastUsed;
                Long resultDate2 = this.lastUsed;

                if (resultDate1 < resultDate2) {
                    return -1;
                }

                if (resultDate1 > resultDate2) {
                    return 1;
                }

                return 0;
        }
        return 0;
    }
}
