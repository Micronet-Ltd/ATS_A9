/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.dsc.ats;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;

import android.content.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by dschmidt on 1/26/17.
 */

public class AtsApplication extends Application {

    public static final String TAG = "ATS-Application";

    public AtsApplication() {
        // this method fires only once per application start.
        // getApplicationContext returns null here
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // this method fires once as well as constructor
        // but also application has context here

        Log.i(TAG, "Created: ATS version " + BuildConfig.VERSION_NAME);

        // if they exist we need to move the alternate configuration files to the primary location here
        //  before anything else in the application tries to access them

        // Check which process this is: MainService, Io, or Watchdog because we don't want to update config files if
        // it is Io or Watchdog
        int pid = android.os.Process.myPid();
        ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);

        List<RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();

        if(processes != null){
            for(RunningAppProcessInfo info: processes){

                if(info.pid == pid && info.processName.equalsIgnoreCase("com.micronet.dsc.ats.obc5")){
                    Log.i(TAG, "MainService is starting. Renaming new configuration and codemap files if present.");
                    copyAlternateConfigFiles();
                }
            }
        }else{
            Log.e(TAG, "Running App Processes was null... alternate config files not copied if they were available.");
        }
    }


    ///////////////////////////////////////////////////////////////////
    // copyAlternateConfigFiles()
    //      copy the config files from the alternate location to the primary, and remember if we did this
    public void copyAlternateConfigFiles() {

        int config_files_updated = Config.init();
        int eventcode_files_updated = CodeMap.init();

        // now, if we did something, we should remember this in the state file so that we can
        //  generate a message the next time the ATS service starts (which could be right away if that is how we got here)

        if ((config_files_updated | eventcode_files_updated) != 0) {
            State state = new State(getApplicationContext());
            state.setFlags(State.PRECHANGED_CONFIG_FILES_BF, config_files_updated | eventcode_files_updated);
        }
    }


} // AtsApplication class
