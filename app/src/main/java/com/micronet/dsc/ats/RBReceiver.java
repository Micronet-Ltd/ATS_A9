package com.micronet.dsc.ats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.Buffer;
import java.util.Scanner;

public class RBReceiver extends BroadcastReceiver {
    private static final String TAG = "ResetRB-Receiver";

    // Name of the intent for 3rd party apps to use.
    public static final String RB_RECEIVER = "com.micronet.dsc.ats.RESET_RB_RECEIVER";

    // Extra of the intent, to define specific action.
    private static final String RB_ACTION_EXTRA = "com.micronet.dsc.ast.RESET_RB_ACTION_EXTRA";
    private static final String RB_CLEAN_CHECK = "RESET_RB_CLEAN_CONFIG_CHECK";
    private static final String RB_CLEAN = "RESET_RB_CLEAN_CONFIG";
    private static final String RB_FORCE_SYNC = "RESET_RB_FORCE_SYNC";
    private static final String RB_CHANGE_CONFIGURATION = "RESET_RB_CHANGE_CONFIG";
    private static final String RB_CONFIGURATION_VALUE = "RESET_RB_CONFIG_VALUE";

    public static final String[] FILENAME_ALTERNATE_PATHS = {
            "/sdcard/ATS", // directory on the Android 4.0 A-317 series
            "/storage/sdcard0/ATS" // directory on the Android 5.0 OBC series
    };

    MainService mainService;
    Config config;
    /**
     * 1. Two CLEAN Actions, on will manual check-in
     * 2. Sample intent sender app
     * 3. ATS suppose to be running all the time, does the intent needs the ability to wake up ATS as well?
     * 4.
     * **/
    @Override
    public void onReceive(Context context, Intent intent) {

        mainService = new MainService(context);

        if(RB_RECEIVER.equals(intent.getAction())){
            Log.d(TAG, "Intent received!" + intent.getPackage());

            String action = intent.getStringExtra(RB_ACTION_EXTRA);
            switch(action){

                case RB_CLEAN: //Trigger ResetRB Cleaner now.
                    Log.d(TAG, RB_CLEAN);
                    String manualCheck = intent.getStringExtra(RB_CLEAN_CHECK); //Should we force sync after clean-up.
                    if(manualCheck.toUpperCase().equals("TRUE")){
                        RBCClearer.clearRedbendFiles(true);
                        Log.d(TAG, "ManualCheck after clean = TRUE");
                    }else if(manualCheck.toUpperCase().equals("FALSE")){
                        RBCClearer.clearRedbendFiles(false);
                        Log.d(TAG, "ManualCheck after clean = FALSE");
                    }else{
                        RBCClearer.clearRedbendFiles(false); //Default is set to be false.
                        Log.d(TAG, "ManualCheck after clean = DEFAULT");
                    }
                    break;

                case RB_FORCE_SYNC: //Force sync with RedBend server NOW.
                    Log.d(TAG, RB_FORCE_SYNC);

                    if(checkInternetConnection(context)){ // Check for internet before running process.
                        RBCClearer.manualRedbendCheckIn();
                    }else{
                        Log.d(TAG, "Internet Connectivity Unavailable, please check your network.");
                    }
                    break;

                case RB_CHANGE_CONFIGURATION: //Update ResetRB configuration setting.
                    String newValue = intent.getStringExtra(RB_CONFIGURATION_VALUE); //Receiving new setting value. Format has to be - "boolean|int|boolean|boolean"
                    Log.d(TAG, RB_CHANGE_CONFIGURATION + " - " + newValue);
                    boolean updatePass = updateSharedPreference(newValue); //Update received value into SharedPreference.
                    Log.d(TAG, "updatePass: " + updatePass);
                    if(updatePass){
                        //Todo: update the bak file

                        String oldValue = mainService.config.readSetting(Config.SETTING_RESET_RB);
                        updateBakFile(oldValue,newValue);
                        //Todo: Trigger the resetRB
                        try {
                            Intent rbClearerPreparation = new Intent(context, RBClearerPreparation.class);
                            Log.d(TAG, "STAGE 1");
                            String allowResetRB = mainService.config.readParameter(Config.SETTING_RESET_RB, Config.PARAMETER_AOLLOW_RESET);
                            String resetRBTargetDays = mainService.config.readParameter(Config.SETTING_RESET_RB, Config.PARAMETER_RESET_PERIOD);
                            String resetRBForceSync = mainService.config.readParameter(Config.SETTING_RESET_RB, Config.PARAMETER_RESET_FORCE_SYNE);
                            String resetRBReboot = mainService.config.readParameter(Config.SETTING_RESET_RB, Config.PARAMETER_RESET_REBOOT);
                            Log.d(TAG, "testingConfig: " + allowResetRB); //
                            rbClearerPreparation.putExtra(mainService.RESET_RB_ALLOW, allowResetRB);
                            rbClearerPreparation.putExtra(mainService.RESET_RB_DAYS, resetRBTargetDays);
                            rbClearerPreparation.putExtra(mainService.RESET_RB_FORCE_SYNC, resetRBForceSync);
                            rbClearerPreparation.putExtra(mainService.RESET_RB_REBOOT, resetRBReboot);
                            context.startService(rbClearerPreparation);
                            Log.d(TAG, "rbClearerPreparation intent sent..");
                        }catch(Exception e){
                            Log.d(TAG, "Error: " + e);
                        }
                    }else{
                        Log.d(TAG, "Update SharedPreference Fail");
                    }
                    break;
            }
        }
    }

    /**
     * Update the Bak file in ATS folder,
     * This won't affect the system's behavior.
     * It's just a reference for user to examining the ResetRB Setting.
     * **/
    private void updateBakFile(String oldValue, String newValue) {

        FileInputStream inStream = null;
        FileOutputStream outputStream = null;
        File src = null;
        File tempSrc = null;
        String fileName = "configuration.xml.bak";
        String tempFileName = "temp.txt";
        String oldContent = "";
        BufferedReader reader = null;
        FileWriter writer = null;

        //locate the bak file
        int i;
        for(i = 0; i< FILENAME_ALTERNATE_PATHS.length; i++){
            if(new File(FILENAME_ALTERNATE_PATHS[i]).exists()){
                break;
            }
        }
        String file_path = FILENAME_ALTERNATE_PATHS[i];
        Log.d(TAG, "file_path: " + file_path);
        //open the bak file
        try{
            src = new File(file_path, fileName);
            tempSrc = new File(file_path, tempFileName);
            Log.d(TAG, "Got the file: " + src + " / and temp file: " + tempSrc);
        }catch(Exception e){
            Log.d(TAG, "STAGE 1 ERROR: " + e);
        }

        try{
            BufferedReader bReader = new BufferedReader(new FileReader(src));
            BufferedWriter bWriter = new BufferedWriter(new FileWriter(tempSrc));


            String targetString = "name=\"37\"";
            String newLine = "<string name=\"37\">"+newValue+"</string>\n";
            String currentLine;
            while((currentLine = bReader.readLine())!=null){
                String trimmedLine = currentLine.trim();
                if(trimmedLine.contains(targetString)){
                    bWriter.write(newLine + System.getProperty("line.separator"));
                    continue;
                }
                bWriter.write(currentLine + System.getProperty("line.separator"));
            }
            bWriter.close();
            bReader.close();
            tempSrc.renameTo(src);


        }catch(Exception e){
            Log.d(TAG, "STAGE 2 ERROR: " + e);
        }

       
    }

    /**
     * Updating the SharedPreferences for ResetRB.
     * **/
    private boolean updateSharedPreference(String value) {
        boolean updatePass = false;
        try {
            mainService.config.writeSetting(Config.SETTING_RESET_RB, value);
            String wholeSetting = mainService.config.readSetting(Config.SETTING_RESET_RB);
            Log.d(TAG, "SETTING_RESET_RB = " + wholeSetting);

            String allowResetRB = mainService.config.readParameter(Config.SETTING_RESET_RB, Config.PARAMETER_AOLLOW_RESET);
            String resetRBTargetDays = mainService.config.readParameter(Config.SETTING_RESET_RB, Config.PARAMETER_RESET_PERIOD);
            String resetRBForceSync = mainService.config.readParameter(Config.SETTING_RESET_RB, Config.PARAMETER_RESET_FORCE_SYNE);
            String resetRBReboot = mainService.config.readParameter(Config.SETTING_RESET_RB, Config.PARAMETER_RESET_REBOOT);

            Log.d(TAG, "Detail Value: Allow = " + allowResetRB +
                    " | days = " + resetRBTargetDays +
                    " | forceSync = "+ resetRBForceSync +
                    " | reboot = "+resetRBReboot);
            updatePass = true;
        }catch(Exception e){
            Log.d(TAG, "updateSharedPreference Fail: " + e);
        }
        return updatePass;
    }

    private boolean checkInternetConnection(Context context){
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
