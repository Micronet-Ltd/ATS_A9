package com.micronet.dsc.ats;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;


public class GSDActivity extends Activity {
    private static final String TAG = "GSDActivity";

    private static final String PACKAGE_NAME = "com.communitake.mdc.micronet"; // GSD Package Name.
    /**These are the Contains for ATS to trigger GSD**/
    private static final String ACTION_EXTRA_CODE = "ACTION_EXTRA_CODE";
    private static final String PIN_CODE = "PIN_CODE";
    private static final String GSD_ACTION_SYNC_NOW = "com.micronet.dsc.ats.GSD_ACTION_SYNC_NOW";
    private static final String GSD_ACTION_CHEKC_STATE = "com.micronet.dsc.ats.GSD_ACTION_CHECK_STATE";
    private static final String GSD_ACTION_DO_REGISTER = "com.micronet.dsc.ats.GSD_ACTION_DO_REGISTER";

    /**These are the Contains from CommuniTake**/
    private static final String BROADCAST_ACTION_3RD = "com.communitake.android.main.broadcast.3rd";
    private static final String ACTION_THIRDPARTY_BROADCAST = "com.communitake.ACTION_THIRDPARTY_BROADCAST";

    public static final String ACTION_DO_REGISTER = "com.communitake.mdc.externalapi.DO_REGISTER";
    public static final String ACTION_REGISTER_STATE = "com.communitake.mdc.externalapi.REGISTER_STATE";

    private static final int REQUEST_CODE_REGISTER_STATUS = 1112;
    private static final int REQUEST_CODE_DO_REGISTER = 1122;

    private static final String STATUS_KEY = "STATUS";
    private static final String STATUS_DETAILED_KEY = "STATUS_DETAILED";
    private static final String PINCODE_KEY = "PINCODE";

    private static final int STATUS_DO_REGISTER_SUCCESS = 10;
    private static final int STATUS_DO_REGISTER_ERROR_EMPTY_PINCODE = 11;
    private static final int STATUS_DO_REGISTER_ERROR_ALREADY_REGISTERED = 12;
    private static final int STATUS_DO_REGISTER_ERROR_TIMEOUT = 13;
    private static final int STATUS_DO_REGISTER_ERROR_GENERAL = 14;


    private static Map<Integer,String> lookupMap = new HashMap<>();
    static {
        lookupMap.put(STATUS_DO_REGISTER_SUCCESS, "Register success");
        lookupMap.put(STATUS_DO_REGISTER_ERROR_EMPTY_PINCODE, "Error empty pincode");
        lookupMap.put(STATUS_DO_REGISTER_ERROR_ALREADY_REGISTERED, "Error already registered");
        lookupMap.put(STATUS_DO_REGISTER_ERROR_TIMEOUT, "Error timeout");
        lookupMap.put(STATUS_DO_REGISTER_ERROR_GENERAL, "Error general");

    }

    // These two variables are examine for GSD version name.
    private int packageVersionInt = 0;
    final static private int targetVersion = 11870; // GSD has to be newer that this to trigger the new API.

    /**
     * The reason that I place GSD checking process over here,
     * is because ATS is a always-running app, I'd like to check for the GSD service status every time before we run into it.
     * Just to make sure GSD service status is valid and secure.
     * **/
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getApplicationContext();
        PackageManager packageManager = context.getPackageManager();
        boolean isInstalled = isPackageInstalled(PACKAGE_NAME, packageManager);
        if(isInstalled){
            //Checking GSD Version Name.
            try{
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(PACKAGE_NAME, 0);
                String packageName = packageInfo.versionName;
                packageName = packageName.replace(".", "");
                packageVersionInt = new Integer(packageName);
                Log.d(TAG, "Package Version Int: " + packageVersionInt);
            }catch(Exception e){
                Log.d(TAG, "GSD Version Detection Error: " +e);
            }
        }else{
            finish();
            Log.d(TAG, "Manage Package Not Found, GSD Activity Ended");
        }

        String intentActionCode = getIntent().getStringExtra(ACTION_EXTRA_CODE);
        Log.d(TAG, "intentActionCode: " + intentActionCode);

        if(intentActionCode != null){
            if(packageVersionInt > targetVersion) {
                Log.d(TAG, "New version GSD Manage");
                switch (intentActionCode) {
                    case GSD_ACTION_SYNC_NOW:
                        doSync(this);
                        break;
                    case GSD_ACTION_CHEKC_STATE:

                        Intent outGoingintent = new Intent(ACTION_REGISTER_STATE);
                        startActivityForResult(outGoingintent, REQUEST_CODE_REGISTER_STATUS);
                        android.util.Log.d(TAG, "register intent sent: " + outGoingintent + outGoingintent.getAction() + outGoingintent.getExtras());
                        break;
                    case GSD_ACTION_DO_REGISTER:

                        String pinCode = getIntent().getStringExtra(PIN_CODE);
                        outGoingintent = new Intent(ACTION_DO_REGISTER);
                        outGoingintent.putExtra(PINCODE_KEY, pinCode);

                        startActivityForResult(outGoingintent, REQUEST_CODE_DO_REGISTER);

                        break;
                }
            }else{
                //Handling the old GSD version(<=11.8.70)
                Log.d(TAG, "This is an old version GSD Manage");
                Intent intent3rd = new Intent(BROADCAST_ACTION_3RD);
                intent3rd.setAction(ACTION_THIRDPARTY_BROADCAST);
                intent3rd.putExtra("sync", true);
                intent3rd.setClassName(PACKAGE_NAME, "com.communitake.mdc.externalapi.ThirdPartyReceiver");
                context.sendBroadcast(intent3rd);
                Log.d(TAG, "Old Intent Sent");
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_REGISTER_STATUS && data != null) {
            int status = data.getIntExtra(STATUS_KEY, -1);
            if (resultCode == RESULT_OK) {
                android.util.Log.d(TAG, "onResults1 " + requestCode + " - " + status);
                Toast.makeText(this, status == 1 ? "Device registered" : "Device NOT registered" , Toast.LENGTH_LONG).show();
                this.finish();
            }
            else if (resultCode == RESULT_CANCELED)
            {

                android.util.Log.d(TAG, "onResults2 " + requestCode + " - " + status);
                Toast.makeText(this, "Failed to get device registered status", Toast.LENGTH_LONG).show();
                this.finish();
            }
        }
        else if (requestCode == REQUEST_CODE_DO_REGISTER && data != null)
        {
            int status = data.getIntExtra(STATUS_KEY, -1);
            String statusDetailed = data.getStringExtra(STATUS_DETAILED_KEY);
            if (resultCode == RESULT_OK) {

                android.util.Log.d(TAG, "onResults3 " + requestCode + " - " + status + statusDetailed);
                Toast.makeText(this, lookupMap.get(status), Toast.LENGTH_LONG).show();
                this.finish();
            }
            else if (resultCode == RESULT_CANCELED)
            {

                android.util.Log.d(TAG, "onResults4 " + requestCode + " - " + status + statusDetailed);
                Toast.makeText(this, lookupMap.get(status) + ", " + statusDetailed , Toast.LENGTH_LONG).show();
                this.finish();
            }
        }
    }

    public void doSync(Context context) {
        Intent intent3rd = new Intent(BROADCAST_ACTION_3RD);
        intent3rd.setAction(ACTION_THIRDPARTY_BROADCAST);
        intent3rd.putExtra("sync", true);
        context.sendBroadcast(intent3rd);
        Toast.makeText(this, "GSD SYNC", Toast.LENGTH_LONG).show();
        this.finish();
    }

    /**
     * Checking if targeted package exists in the device.
     * **/
    private boolean isPackageInstalled(String packageName, PackageManager packageManager){

        try{
            packageManager.getPackageInfo(packageName, 0);
            Log.d(TAG, "Package: " + packageName + " FOUND.");
            return true;
        }catch(PackageManager.NameNotFoundException e){
            Log.d(TAG, "Package: " + packageName + " NOT FOUND. " + e);
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
