package com.micronet.dsc.ats;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;


public class GSDActivity extends Activity {
    private static final String TAG = "GSDActivity";

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String intentActionCode = getIntent().getStringExtra(ACTION_EXTRA_CODE);
        Log.d(TAG, "intentActionCode: " + intentActionCode);

        if(intentActionCode != null){
            switch(intentActionCode){
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
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
