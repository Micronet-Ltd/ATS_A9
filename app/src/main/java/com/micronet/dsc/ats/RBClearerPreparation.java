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
import android.support.annotation.Nullable;

public class RBClearerPreparation extends IntentService {
    private static final String TAG = "RBClearerPreparation";
    public static final String RESET_RB_SHARED_PREFERENCES = "resetRBSharedPref";
    public static final String SYSTEM_CURRENT_TIME = "systemCurrentTime";
    public static final String LAST_CLEAN_UP_TIME = "lastCleanUpTime";
    Context context;
    MainService service;

    public RBClearerPreparation(){
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
        if(checkInternetConnection()){
            //getConfigResetRBEnable();
            //Config config = new Config(context);
            //String testReading = service.config.readSetting(Config.SETTING_RESET_RB);
            //Log.d(TAG, "testReading: " + testReading);
            //SharedPreferences sharedPreferences = getSharedPreferences("configuration", MODE_PRIVATE);
            //Log.d(TAG, "Testing sp: "  + sharedPreferences);
            //String testString = config.readSetting(35);
            //Log.d(TAG, "testString : " + testString);

            SharedPreferences sp = getSharedPreferences(RESET_RB_SHARED_PREFERENCES, Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = sp.edit();
            long currentTime = System.currentTimeMillis();
                    Log.d(TAG, "currentTime: " + currentTime);
            long lastCleanUpTime = context.getSharedPreferences(RESET_RB_SHARED_PREFERENCES, Context.MODE_PRIVATE).getLong(LAST_CLEAN_UP_TIME, 0);
                    Log.d(TAG, "lastCleanUpTime: " + lastCleanUpTime);
                    //Todo: Do something to handle the first time user(When ATS running for the first time, lastCleanTime will be 0);
            if(lastCleanUpTime == 0){
                //Todo: Save current time as the lastCleanUpTime for future comparison.
                context.getSharedPreferences(RESET_RB_SHARED_PREFERENCES, Context.MODE_PRIVATE).edit().putLong(LAST_CLEAN_UP_TIME, currentTime).apply();
            }
            if (RBCClearer.isTimeForPeriodicCleaning(lastCleanUpTime, currentTime)){ // Todo: Modify this to fit for comparing lastCleanUpTime and ConfiguredCleanUpTim.
                Log.d(TAG, "NOW CLEANING");
                context.getSharedPreferences(RESET_RB_SHARED_PREFERENCES, Context.MODE_PRIVATE).edit().putLong(LAST_CLEAN_UP_TIME, currentTime).apply();
                RBCClearer.clearRedbendFiles(true);
            }
            /*boolean testingBoolean = true;
            if(testingBoolean == true){
                long testing = sp.getLong(SYSTEM_CURRENT_TIME, 000000L);
                Log.d(TAG, "testing: " + testing);
                RBCClearer.clearRedbendFiles(testingBoolean);
            }*/
        }else{
            Log.d(TAG, "Cleaning Process Stopped");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy..");
    }

    boolean getConfigResetRBEnable(){
        String resetRB_config = service.config.readParameterString(Config.SETTING_RESET_RB, Config.PARAMETER_AOLLOW_RESET);
        boolean resetRB_enabled = false;
        if(!resetRB_config.toUpperCase().equals("OFF")){
            resetRB_enabled = true;
        }
        Log.d(TAG, "resetRB Enable? " + resetRB_enabled);
        return resetRB_enabled;
    }
    /**
     * Make sure we have a internet connection before cleaning process
     * **/
    private boolean checkInternetConnection(){
        Log.d(TAG, "Checking internet connection..");
        boolean results = false;

        if(context == null){
            return results;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if(cm != null){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if(capabilities != null){
                    if(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){
                        Log.d(TAG, "CELLULAR");
                        results = true;
                    } else if(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                        Log.d(TAG, "WIFI");
                        results = true;
                    }else if(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){
                        Log.d(TAG, "ETHERNET");
                        results = true;
                    }
                }
            }
            else{
                try{
                    NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
                    if(activeNetworkInfo != null && activeNetworkInfo.isConnected()){
                        Log.d(TAG, "Network is available");
                        results = true;
                        return results;
                    }
                }catch(Exception e){
                    Log.d(TAG, e.toString());
                }
            }
        }
        Log.d(TAG, "checkInternetConnection result: " + results);
        return results;
    }
}
