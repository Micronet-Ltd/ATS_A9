/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.dsc.ats;

import android.app.Application;

/**
 * This class subclasses Application. More on this here: https://developer.android.com/reference/android/app/Application.
 */
public class AtsApplication extends Application {
    public static final String TAG = "ATS-Application";

    public AtsApplication() {}

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Created: ATS version " + BuildConfig.VERSION_NAME);

        // In ATS built for OBC5 and Tab5 (Android 5), this class was called for every different process that was made for this app.
        // This is different on the Tab8 with Android 9. It only calls this class once.
        // This app has three different processes: MainService, Io, and Watchdog.
    }
} // AtsApplication class
