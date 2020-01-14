/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

/////////////////////////////////////////////////////////////
// BootReceiver:
//  Receives the Boot Message from the System
/////////////////////////////////////////////////////////////
package com.micronet.dsc.ats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import java.util.Locale;
import java.util.concurrent.TimeUnit;


// This is registered in the Manifest
public class BootReceiver extends BroadcastReceiver {

    // Here is where we are receiving our boot message.
    //  Information about this should also be put in the manifest.

    public static final String TAG = "ATS-BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "System Boot Notification");

        // Display the time in minutes and seconds since device boot
        long elapsed = SystemClock.elapsedRealtime();
        String timeSinceBoot = String.format(Locale.getDefault(), "%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(elapsed),
                TimeUnit.MILLISECONDS.toSeconds(elapsed) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed)));
        Log.d(TAG, "Elapsed time since boot: " + timeSinceBoot);

        // Check for updated config files.
        copyAlternateConfigFiles(context);

        // Send intent to service (and start if needed)
        Intent i = new Intent(context, MainService.class);
        i.putExtra(Power.BOOT_REQUEST_NAME, 1);
        context.startForegroundService(i);
    }

    public boolean copyAlternateConfigFiles(Context context) {
        // Reset isNewConfig back to false in case activity is run again.
        int config_files_updated = Config.init();
        int eventcode_files_updated = CodeMap.init();

        // now, if we did something, we should remember this in the state file so that we can
        // generate a message the next time the ATS service starts (which could be right away if that is how we got here)
        if ((config_files_updated | eventcode_files_updated) != 0) {
            State state = new State(context);
            Log.d(TAG, "New Configuration file detected.");
            state.setFlags(State.PRECHANGED_CONFIG_FILES_BF, config_files_updated | eventcode_files_updated);
            return true;
        }else{
            return false;
        }
    }
} // class
