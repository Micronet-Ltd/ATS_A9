package com.micronet.dsc.ats;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class GSDService {
    final String TAG = "GSD-Service";
    MainService service;

    final String BROADCAST_ACTION_3RD = "com.communitake.android.main.broadcast.3rd";
    final String ACTION_THIRDPARTY_BROADCAST = "com.communitake.ACTION_THIRDPARTY_BROADCAST";

    public GSDService(MainService service){
        this.service = service;
    }

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

    public void start(){ // Todo: Maybe call this in the io class.
        boolean gsdEnabled = getConfigGSDEnabled();
        boolean isNewVersion = isNewGSDVersion();

        try{

            Intent intent3rd = new Intent(BROADCAST_ACTION_3RD);
            intent3rd.setAction(ACTION_THIRDPARTY_BROADCAST);
            intent3rd.putExtra("sync", true);

            intent3rd.setClassName("com.communitake.mdc.micronet","com.communitake.mdc.externalapi.ThirdPartyReceiver");
            service.sendBroadcast(intent3rd);
            Log.d(TAG, "Intent3rd send!");
        }catch(Exception e){
            e.printStackTrace();
        }
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
    }
}
