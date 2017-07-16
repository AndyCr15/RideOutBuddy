package com.androidandyuk.rideoutbuddy;

/**
 * Created by AndyCr15 on 11/07/2017.
 */

import android.app.Application;
import android.content.Context;

public class App extends Application {

    private static Application sApplication;
    private static Context context;

    public static Application getApplication() {
        return sApplication;
    }

    public static Context getAppContext() {
        return App.context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
    }
}