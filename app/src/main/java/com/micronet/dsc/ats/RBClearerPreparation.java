package com.micronet.dsc.ats;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.Nullable;

public class RBClearerPreparation extends IntentService {
    private static final String TAG = "ResetRB-prep";

    //Constants for SharedPreferences.
    public static final String RESET_RB_SHARED_PREFERENCES = "resetRBSharedPref";
    public static final String SYSTEM_CURRENT_TIME = "systemCurrentTime";
    public static final String LAST_CLEAN_UP_TIME = "lastCleanUpTime";

    //Constants for intent extra.
    public static final String RESET_RB_ALLOW = "resetRBAllow";
    public static final String RESET_RB_DAYS = "resetRBDays";
    public static final String RESET_RB_FORCE_SYNC = "resetRBForceSync";
    public static final String RESET_RB_REBOOT = "resetRBReboot";

    //Objects.
    Context context;
    MainService service;



    public RBClearerPreparation() {
        super("RBClearerPreparation");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        service = new MainService();
        Log.d(TAG, "onCreate..");
    }

    @Override
    public ComponentName startService(Intent service) {
        return super.startService(service);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

            SharedPreferences sp = getSharedPreferences(RESET_RB_SHARED_PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();

            //Collecting required elements from the intent extra.
            String resetRBAllow = intent.getStringExtra(RESET_RB_ALLOW);
            String resetRBDays = intent.getStringExtra(RESET_RB_DAYS);
            String resetRBForceSync = intent.getStringExtra(RESET_RB_FORCE_SYNC);
            String resetRBReboot = intent.getStringExtra(RESET_RB_REBOOT);
            Log.d(TAG, "Testing intent extra: " + resetRBAllow + "|" + resetRBDays + "|" + resetRBForceSync + "|" + resetRBReboot);

            //Preparing information for calculation.
            int targetCleaningDays = Integer.parseInt(resetRBDays);
            long currentTime = System.currentTimeMillis();
            long lastCleanUpTime = context.getSharedPreferences(RESET_RB_SHARED_PREFERENCES, Context.MODE_PRIVATE).getLong(LAST_CLEAN_UP_TIME, 0);
            Log.d(TAG, "lastCleanUpTime = " + lastCleanUpTime + " | CurrentTime =" + currentTime + " | targetCleaningDays = " + targetCleaningDays);

            if (lastCleanUpTime == 0) { // When lastCleanUpTime = 0, which means system has never trigger RestRB before, set currentSystemTime to be lastCleanUpTime for later comparison.

                editor.putLong(LAST_CLEAN_UP_TIME, currentTime);
                editor.commit();
                lastCleanUpTime = currentTime;
            }
            if (RBCClearer.isTimeForPeriodicCleaning(lastCleanUpTime, currentTime, targetCleaningDays)) {
                Log.d(TAG, "NOW CLEANING");

                context.getSharedPreferences(RESET_RB_SHARED_PREFERENCES, Context.MODE_PRIVATE).edit().putLong(LAST_CLEAN_UP_TIME, currentTime).apply();
                RBCClearer.clearRedbendFiles(false);

                //Check if we need to force-sync with Redbend.
                if (resetRBForceSync.toUpperCase().equals("ON")) {
                    if(checkInternetConnection()){
                        RBCClearer.manualRedbendCheckIn();
                    }else{
                        Log.d(TAG, "Network Connectivity unavailable, please check device's connection.");
                    }
                }

                //Check if we need to reboot the device.
                if (resetRBReboot.toUpperCase().equals("ON")) {
                    long systemSleepTime = 120000;
                    // Waiting
                    Log.d(TAG, "Device reboot in " + (systemSleepTime/1000) + " seconds");
                    SystemClock.sleep(systemSleepTime);
                    Log.d(TAG, "Device reboot now");

                    try {
                        Log.d(TAG, "Entering Reboot State");

                        Intent rebootIntent = new Intent(Intent.ACTION_REBOOT);
                        rebootIntent.putExtra(Intent.EXTRA_KEY_EVENT, false);
                        rebootIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(rebootIntent);

                    } catch (Exception e) {
                        Log.d(TAG, "Reboot process fail: " + e);
                    }
                }
            }
        }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy..");
    }

    /**
     * Make sure we have a internet connection before cleaning process
     **/
    private boolean checkInternetConnection() {
        boolean results = false;

        if (context == null) {
            return results;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        Log.d(TAG, "Network Connection: CELLULAR");
                        results = true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        Log.d(TAG, "Network Connection: WIFI");
                        results = true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        Log.d(TAG, "Network Connection: ETHERNET");
                        results = true;
                    }
                }
            } else {
                try {
                    NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
                    if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                        Log.d(TAG, "Network is available");
                        results = true;
                        return results;
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                }
            }
        }
        return results;
    }
}
