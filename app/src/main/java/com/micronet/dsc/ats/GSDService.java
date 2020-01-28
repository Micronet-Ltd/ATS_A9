package com.micronet.dsc.ats;

import android.app.IntentService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static android.support.v4.app.ActivityCompat.startActivityForResult;

public class GSDService extends IntentService {
    private static final String TAG = "GSD-Service";
    MainService service;
    Intent intent;

    final String BROADCAST_ACTION_3RD = "com.communitake.android.main.broadcast.3rd";
    final String ACTION_THIRDPARTY_BROADCAST = "com.communitake.ACTION_THIRDPARTY_BROADCAST";

    public static final String MDM_PKG_NAME = "com.communitake.mdc.micronet";

    public static final String ACTION_DO_REGISTER = "com.communitake.mdc.externalapi.DO_REGISTER";
    public static final String ACTION_REGISTER_STATE = "com.communitake.mdc.externalapi.REGISTER_STATE";

    private static final int REQUEST_CODE_REGISTER_STATUS = 1112;
    private static final int REQUEST_CODE_DO_REGISTER = 1122;

    private static final String STATUS_KEY = "STATUS";
    private static final String STATUS_DETAILED_KEY = "STATUS_DETAILED";
    private static final String PINCODE_KEY = "PINCODE";

    //11871 and UP register code:
    private static final int STATUS_DO_REGISTER_SUCCESS = 10;
    private static final int STATUS_DO_REGISTER_ERROR_EMPTY_PINCODE = 11;
    private static final int STATUS_DO_REGISTER_ERROR_TIMEOUT = 13;
    private static final int STATUS_DO_REGISTER_ERROR_GENERAL = 14;

    String testingPinCode = "6995266527"; // Testing PinCode.

    private static Map<Integer, String> lookupMap = new HashMap<>();
    static{
        lookupMap.put(STATUS_DO_REGISTER_SUCCESS, "Register success");
        lookupMap.put(STATUS_DO_REGISTER_ERROR_EMPTY_PINCODE, "Error empty pincode");
        lookupMap.put(STATUS_DO_REGISTER_ERROR_TIMEOUT, "Error Timeout");
        lookupMap.put(STATUS_DO_REGISTER_ERROR_GENERAL, "Error General");
    }

    public GSDService(){
        super("GSDService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        /*
        intent  = new Intent(ACTION_DO_REGISTER);
        intent.putExtra(PINCODE_KEY, testingPinCode);
        */
        Log.d(TAG, "START");

        intent.setAction(ACTION_DO_REGISTER);
        if(intent.getAction() != null && intent.getAction() == ACTION_DO_REGISTER){
            Log.d(TAG, "Testing: got here");
            try {
                registerMDM(getApplicationContext());
            }catch(IOException e){
                e.printStackTrace();
            }
        }else{
            Log.d(TAG, "Not Working");
        }

    }

    private static void registerMDM(Context context) throws IOException {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(MDM_PKG_NAME);
        if(launchIntent != null){
            launchIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
            Log.i(TAG, "Send intent to communiTake");
        }
    }


/*
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
    */

    /**
     * Grep the version of GSD Manage app,
     * check if it is newer then 11.8.70 or not.
     * if yes, return true; it not, return false;
     * **/
     public boolean isNewGSDVersion(){
        boolean newVersion = false;
        int versionNumber = 0;
        try{
            PackageManager pm = service.getPackageManager();
            PackageInfo pInfo = pm.getPackageInfo("com.communitake.mdc.micronet",0);
            String gsdVersion = pInfo.versionName;
            Log.d(TAG, "GSD Manage Version : " + gsdVersion);
            gsdVersion = gsdVersion.replace(".", "");
            versionNumber = Integer.parseInt(gsdVersion);
            Log.d(TAG, "Version Code we need is : " + versionNumber);
        }catch(PackageManager.NameNotFoundException e){
            Log.d(TAG, "Cannot find GSD Manage" + e);
        }

        if(versionNumber >= 11871){ //Here is the target version number to differ how we are going to register the device on CommuniTake.
            newVersion = true;
        }
        Log.d(TAG, "testing newVersion: " + newVersion);
        return newVersion;
     }

     /**
      * Check configuration.xml if GSD Service is enable.
      * **/
    boolean getConfigGSDEnabled(){
        String gsd_config = service.config.readParameterString(Config.SETTING_GSD_SETTING, Config.PARAMETER_GSD_ACTIVE);
        boolean gsd_enabled = false;
        if(!gsd_config.toUpperCase().equals("OFF")){
            gsd_enabled = true;
        }
        return gsd_enabled;
    }

   /* public void start(){ // Todo: Maybe call this in the io class.
        boolean gsdEnabled = getConfigGSDEnabled();
        boolean isNewVersion = isNewGSDVersion();

        String testingPinCode = "6995266527";
        intent  = new Intent(ACTION_DO_REGISTER);
        intent.putExtra(PINCODE_KEY, testingPinCode);


        if(gsdEnabled == true){
            if(isNewVersion == true){
                // Do the new version things..
                Intent intent3rd = new Intent(BROADCAST_ACTION_3RD);
                intent3rd.setAction(ACTION_THIRDPARTY_BROADCAST);
                intent3rd.putExtra("sync", true);

                intent3rd.setClassName("com.communitake.mdc.micronet","com.communitake.mdc.externalapi.ThirdPartyReceiver");
                service.sendBroadcast(intent3rd);
                Log.d(TAG, "Intent3rd send!");
            }else{
                // Do the old version things..
            }
        }else{
            //Un-register CommuniTake for the device..
        }
    }*/


}
