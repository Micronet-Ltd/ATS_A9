/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.dsc.ats;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigurationTest {

    private final static String TAG ="Configuration-T";
    private Config cf;


    @Before
    public void setUp(){
        Context context = ApplicationProvider.getApplicationContext();
        cf = new Config(context);
        cf.open();
        cf.clearAll();
    }

    @Test
    public void testBadSettingIds(){
        assertNull(cf.readSetting(999999));
        assertNull(cf.readSetting(-1));
        assertNull(cf.readParameter(0, 99999));
        assertNull(cf.readParameter(0,-1));

        assertFalse(cf.writeSetting(99999, "xxxxx"));
        assertFalse(cf.writeSetting(-1, "ZZZZZ"));
    }

    @Test
    public void testReadDefaults(){
        String server = cf.readSetting(Config.SETTING_PING);
        for(int i =0; i <= 10 ; i++){
            String testingDefaultsValue = cf.readSetting(i);
            Log.d(TAG, i + " = " + testingDefaultsValue);
        }
        Log.d(TAG, "server = " + server);
        assertEquals("30|50|90|300",server);

        String ip = cf.readParameter(Config.PARAMETER_SERVER_ADDRESS_IP,0);
    }
}
