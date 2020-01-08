/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.dsc.ats;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigTest {
    private static final String TAG = "ConfigTest";
    private Config cf;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        cf = new Config(context);
        cf.open();
        cf.clearAll();
    }

    @Test
    public void testBadSettingIds() {
        assertNull(cf.readSetting(999999));
        assertNull(cf.readSetting(-1));
        assertNull(cf.readParameter(0, 99999));
        assertNull(cf.readParameter(0, -1));

        assertFalse(cf.writeSetting(99999, "XXXXX"));
        assertFalse(cf.writeSetting(-1, "XXXXX"));
    }

    @Test
    public void testReadDefaults() {

        // Read Entire Setting Value

        String server = cf.readSetting(Config.SETTING_PING);
        //assertEquals(Config.SETTING_DEFAULTS[SETTING_SERVER_ADDRESS], res);
        assertEquals("30|50|90|300", server);


        // Read just one parameter from a setting value

        String ip = cf.readParameter(Config.SETTING_SERVER_ADDRESS, 0);
        Log.d(TAG, "ip = " + ip);
        String port = cf.readParameter(Config.SETTING_SERVER_ADDRESS, 1);

        assertEquals("10.0.2.2", ip);
        assertEquals("9999", port);


        // Read as an integer
        int port_num = cf.readParameterInt(Config.SETTING_SERVER_ADDRESS, 1);
        assertEquals(9999, port_num);

        // Read an Array of setting values

        String[] message_type_array = cf.readParameterArray(Config.SETTING_BACKOFF_RETRIES);
        assertEquals(message_type_array.length,7);
        assertEquals("10", message_type_array[0]);
        assertEquals("10", message_type_array[1]);
        assertEquals("15", message_type_array[2]);
        assertEquals("15", message_type_array[3]);
        assertEquals("20", message_type_array[4]);
        assertEquals("20", message_type_array[5]);
        assertEquals("60", message_type_array[6]);

        assertEquals(10, Integer.parseInt(message_type_array[0]));

        // .. etc..


    } // testReadDefaults()

    @Test
    public void testWrite() {

        cf.writeSetting(Config.SETTING_SERVER_ADDRESS, "1.1.1.1|2222");

        String address = cf.readSetting(Config.SETTING_SERVER_ADDRESS);
        //assertEquals(Config.SETTING_DEFAULTS[SETTING_SERVER_ADDRESS], res);
        assertEquals("1.1.1.1|2222", address);

        String port = cf.readParameter(Config.SETTING_SERVER_ADDRESS, 1);

        assertEquals("2222", port);
    } // testReadDefaults()

    @Test
    public void testReadNotANumber() {

        // if we expect a number from a config parameter and we get something else, we should treat it like 0.

        cf.writeSetting(Config.SETTING_SERVER_ADDRESS, "ABCD1.1.1.1|2222");

        int bad_int = cf.readParameterInt(Config.SETTING_SERVER_ADDRESS, Config.PARAMETER_SERVER_ADDRESS_IP);

        assertEquals(0, bad_int);
    }
}
