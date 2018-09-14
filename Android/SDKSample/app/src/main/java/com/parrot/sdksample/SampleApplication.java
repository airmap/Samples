package com.parrot.sdksample;

import android.content.Context;
import android.support.multidex.MultiDexApplication;

import com.airmap.airmapsdk.networking.services.AirMap;

public class SampleApplication extends MultiDexApplication {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    public void onCreate() {
        super.onCreate();

        AirMap.init(this);
        AirMap.enableLogging();
    }
}
