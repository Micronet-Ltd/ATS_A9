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

        // Send intent to service (and start if needed)
        Intent i = new Intent(context, MainService.class);
        i.putExtra(Power.BOOT_REQUEST_NAME, 1);
        context.startForegroundService(i);
    }
} // class
