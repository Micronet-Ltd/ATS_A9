package com.micronet.dsc.ats;

import android.app.IntentService;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RBCClearer{

    public static final String TAG = "ResetRB-RBCClearer";


    static final String TREE_XML_FILE_PATH = "/data/data/com.redbend.client/files/tree.xml";

    //////////////////////////////////////////////////////////////////
    // clearRedbendFiles()
    //  clears out the redbend client files required to reset the client
    //  /data/misc/rb/* and /data/data/com.redbend.client
    //////////////////////////////////////////////////////////////////
    static void clearRedbendFiles(boolean manualCheck) {

        android.util.Log.d(TAG, "Clearing redbend client files");
        // since this has a wildcard it must be run in a shell

        // Force stop Redbend client before cleaning files
        android.util.Log.d(TAG, "Force stopping Redbend client");
        stopRedbendClient();

        String command = "";

        command = "rm -r /data/misc/rb/*";
        android.util.Log.d(TAG, "Running " + command);

        try {
            Runtime.getRuntime().exec(new String[] { "sh", "-c", command } ).waitFor();
        } catch (Exception e) {
            android.util.Log.d(TAG, "Exception exec: " + command + ": " + e.getMessage());
        }

        // Remove result file from prior UA operations
        command = "rm /data/redbend/result";
        android.util.Log.d(TAG, "Running " + command);

        try {
            Runtime.getRuntime().exec(new String[] { "sh", "-c", command } ).waitFor();
        } catch (Exception e) {
            android.util.Log.d(TAG, "Exception exec: " + command + ": " + e.getMessage());
        }


        command = "pm clear com.redbend.client";
        android.util.Log.d(TAG, "Running " + command);

        try {
            Runtime.getRuntime().exec(command).waitFor();
        } catch (Exception e) {
            android.util.Log.d(TAG, "Exception exec " + command + ": " +  e.getMessage());
        }

        // Start Redbend Client
        android.util.Log.d(TAG, "Starting Redbend client");
        startRedbendClient();

        if(manualCheck){
            // Send broadcast to do a manual check in
            android.util.Log.d(TAG, "Manually checking in on Redbend");
            manualRedbendCheckIn();
        }
    } // clearRedbendFiles

    static void manualRedbendCheckIn(){
        try {
            Runtime.getRuntime().exec("am broadcast -a SwmClient.CHECK_FOR_UPDATES_NOW".split(" "));
        } catch (Exception e) {
            android.util.Log.e(TAG, "Exception exec: am force-stop com.redbend.client: " + e.getMessage());
        }
    }

    static void stopRedbendClient(){
        try {
            Runtime.getRuntime().exec("am force-stop com.redbend.client".split(" ")).waitFor();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Exception exec: am force-stop com.redbend.client: " + e.getMessage());
        }
    }

    static void startRedbendClient(){
        // -W means to wait for launch to complete
        try {
            Runtime.getRuntime().exec("am start -W com.redbend.client/.StartActivity".split(" ")).waitFor();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Exception exec: am start com.redbend.client/.StartActivity: " + e.getMessage());
        }
    }

    static boolean isTimeForPeriodicCleaning(long lastCleaning, long currentMilliseconds){
        double difference = (double) currentMilliseconds - (double) lastCleaning;
        // Convert to months to see if a month has elapsed
        double seconds = difference/1000.0;
        double minutes = seconds/60.0;
        double hours = minutes/60.0;
        double days = hours/24.0;
        double months = days/30.0;

        return months >= 1.0;
    }

    static boolean isTimeForPeriodicCleaning(long lastCleaningTime, long currentMilliseconds, int targetCleaningDays){
       boolean isTimeToClean = false;
       long targetCleaningTimeMillionSeconds = lastCleaningTime + ((long)targetCleaningDays * 86400000);
       Log.d(TAG, "currentMillionSeconds= " + currentMilliseconds +
               " | lastCleaningTime= " + lastCleaningTime +
               " | targetCleaningTimeMillionSeconds=" + targetCleaningTimeMillionSeconds);

       double difference = (double) currentMilliseconds - (double) targetCleaningTimeMillionSeconds;
       Log.d(TAG, "difference= " + difference);

       double seconds = difference/1000.0;
        double minutes = seconds/60.0;
        double hours = minutes/60.0;
        double days = hours/24.0;

        if(days >= 1.0){
            isTimeToClean = true;
        }

       return isTimeToClean;
    }

    /**
     * Does /data/data/com.redbend.client/files/tree.xml exist with a zero byte length?
     *      (this is a known problem condition)
     * @return
     */
    static boolean isTreeXmlZeroBytes() {

        // This is done via ls command (and not java) because we need to su first

        android.util.Log.d(TAG, "Checking " + TREE_XML_FILE_PATH + " for a zero-byte length");

        String command;
        command = "su -c 'ls -s " + TREE_XML_FILE_PATH + "'";
        // returns <size_in_blocks><space><file_name>


        String result = null;
        int exitCode = -1;

        Process p;
        try {
            p = Runtime.getRuntime().exec(new String[] { "sh", "-c", command } );
            try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                result = in.readLine();
                exitCode = p.waitFor();
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Exception running command: " + command + " : " + e.getMessage());
        }

        if (exitCode != 0) {
            android.util.Log.w(TAG, TREE_XML_FILE_PATH + " does not exist");
            return false; // it is not existing with zero bytes because it does not yet exist.
        }

        if ((result != null) && (!result.isEmpty())) {

            String[] splits = result.split(" ", 2);
            int filesize = -1; // non-zero value
            try {
                filesize = Integer.parseInt(splits[0]);
            } catch (Exception e) {
                android.util.Log.e(TAG, "command " + command + " returns '" + result + "' which is not parseable as integer");
                return false; //assume we are not zero-byte length
            }

            if (filesize == 0) {
                android.util.Log.w(TAG, TREE_XML_FILE_PATH + " has zero bytes length!");
                return true;
            }
        }

        Log.d(TAG, TREE_XML_FILE_PATH + " has non-zero length.");
        return false; // does not have a zero length
    } // isTreeXmlZeroBytes()

}
