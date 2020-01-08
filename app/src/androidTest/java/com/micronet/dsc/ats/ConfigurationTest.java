/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.dsc.ats;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.net.InterfaceAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ConfigurationTest {

    private final static String TAG ="Configuration-T";
    private Config cf;
    private Config.ECR ecr;


    @Before
    public void setUp(){
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
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
        assertTrue(cf.writeSetting(1, "192.0.0.1|8550"));
        cf.writeSetting(1,"This is not OK");
        String testingCf = cf.readSetting(1);
        Log.d(TAG, "testingCf = " + testingCf);

        cf.writeSetting(1,"192.0.0.23|2550");
        String ip = cf.readParameter(Config.SETTING_SERVER_ADDRESS, 0);
        String port = cf.readParameter(Config.SETTING_SERVER_ADDRESS, 1);
        Log.d(TAG, "IP = " + ip + " / Port = " + port);
    }

    @Test
    public void testReadWrite(){
        String server = cf.readSetting(Config.SETTING_PING);
        for(int i =0; i <= 10 ; i++){
            String testingDefaultsValue = cf.readSetting(i);
            Log.d(TAG, i + " = " + testingDefaultsValue);
        }
        Log.d(TAG, "server = " + server);
        assertEquals("30|50|90|300",server);

        //Testing defaults value on SETTING_SERVER_ADDRESS
        String ip = cf.readParameter(Config.SETTING_SERVER_ADDRESS,0);
        String port = cf.readParameter(Config.SETTING_SERVER_ADDRESS, 1);
        Log.d(TAG, "DefaultsValue - IP= "+ ip + " /Port= " +port);
        assertEquals("10.0.2.2", ip);
        assertEquals("9999", port);

        //Testing custom value on SETTING_SERVER_ADDRESS
        cf.writeSetting(Config.SETTING_SERVER_ADDRESS, "123.123.123.123|2550");
        ip = cf.readParameter(Config.SETTING_SERVER_ADDRESS, 0);
        port = cf.readParameter(Config.SETTING_SERVER_ADDRESS, 1);
        Log.d(TAG, "CustomValue - IP= "+ip+" /Port= " +port);
        assertEquals("123.123.123.123", ip);
        assertEquals("2550", port);

        //Writing new value into SETTING_BACKOFF_RETRIES(3)
        cf.writeSetting(3, "7|6|5|4|3|2|1");
        assertTrue(cf.writeSetting(3, "7|6|5|4|3|2|1"));

        //Testing if the cf could read value with parameter correctly.
        String[] testing_array = cf.readParameterArray(3);
        assertEquals(7, testing_array.length);
        int targetNumber = 7;
        for(int i = 0; i<testing_array.length; i++){
            assertEquals(Integer.toString(targetNumber), testing_array[i]);
            Log.d(TAG, "testing_array:[" + i + "] = " + targetNumber);
            targetNumber --;
            Log.d(TAG, "Next targetNumber = " + targetNumber);
        }
    }

    @Test
    public void testECRandOTA(){
        ecr = new Config.ECR(20,10,1,1);
        assertTrue(cf.shouldSendOTA(20));
        assertTrue(cf.shouldSendOTA(30));
        /**
         * Something to note here:
         * The following 2 tests will pass, even the event_type_id is not stored in the list.
         * The reason for that is, we'd like to allow user to be able to customize their own event id,
         * therefore, shouldSendOTA basically accepting any number you type in.
         * **/
        assertTrue(cf.shouldSendOTA(-10));
        assertTrue(cf.shouldSendOTA(250));

    }
}
