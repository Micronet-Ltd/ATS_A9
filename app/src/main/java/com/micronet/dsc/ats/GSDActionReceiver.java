package com.micronet.dsc.ats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GSDActionReceiver extends BroadcastReceiver {
    private static final String TAG = "GSDActionReceiver";

    public static final String GSD_COMMON_CONTROL = "com.micronet.dsc.ats.GSD_COMMON_CONTROL";

    public static final String ACTION_EXTRA_CODE = "ACTION_EXTRA_CODE";
    public static final String PIN_CODE = "PIN_CODE";
    public static final String GSD_ACTION_SYNC_NOW = "com.micronet.dsc.ats.GSD_ACTION_SYNC_NOW";
    public static final String GSD_ACTION_CHECK_STATE = "com.micronet.dsc.ats.GSD_ACTION_CHECK_STATE";
    public static final String GSD_ACTION_DO_REGISTER = "com.micronet.dsc.ats.GSD_ACTION_DO_REGISTER";

    @Override
    public void onReceive(Context context, Intent intent) {

        String actionCode = intent.getStringExtra(ACTION_EXTRA_CODE);
        if (actionCode!= null){
            Intent gsdActivityIntent;
            Log.d(TAG, "testing broadcast received!" + intent.getExtras());
            switch(actionCode){
                case GSD_ACTION_SYNC_NOW:
                     gsdActivityIntent = new Intent(context, GSDActivity.class);
                    gsdActivityIntent.putExtra(ACTION_EXTRA_CODE, GSD_ACTION_SYNC_NOW);
                    gsdActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(gsdActivityIntent);
                    Log.d(TAG, "gsdActivityIntent Sent: " + gsdActivityIntent);
                    break;
                case GSD_ACTION_CHECK_STATE:
                    gsdActivityIntent = new Intent(context, GSDActivity.class);
                    gsdActivityIntent.putExtra(ACTION_EXTRA_CODE, GSD_ACTION_CHECK_STATE);
                    gsdActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(gsdActivityIntent);
                    Log.d(TAG, "gsdActivityIntent Sent: " + gsdActivityIntent);

                    break;
                case GSD_ACTION_DO_REGISTER:
                    String pincode = intent.getStringExtra(PIN_CODE);
                    gsdActivityIntent = new Intent(context, GSDActivity.class);
                    gsdActivityIntent.putExtra(ACTION_EXTRA_CODE, GSD_ACTION_DO_REGISTER);
                    gsdActivityIntent.putExtra(PIN_CODE, pincode);
                    gsdActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(gsdActivityIntent);
                    Log.d(TAG, "gsdActivityIntent Sent: " + gsdActivityIntent);
                    break;
            }
        }
    }
}
