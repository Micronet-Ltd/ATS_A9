/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.dsc.ats;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class StateTest {

    private static State st;

    @BeforeClass
    public static void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        st = new State(context);
    }

    @Before
    public void beforeTest() {
        st.clearAll();
    }

    @Test
    public void testReadDefault() {

        // Read Entire State Value

        int engine_status = st.readState(State.FLAG_ENGINE_STATUS);

        assertEquals(0, engine_status);

    } // testReadDefault()

    @Test
    public void testWrite() {

        st.writeState(State.FLAG_ENGINE_STATUS, 1);

        int engine_status = st.readState(State.FLAG_ENGINE_STATUS);
        //assertEquals(Config.SETTING_DEFAULTS[SETTING_SERVER_ADDRESS], res);
        assertEquals(1, engine_status);


    } // testWrite()

    @Test
    public void testReadString() {


        String vin = st.readStateString(State.STRING_VIN);

        assertEquals("", vin);

    } // testReadDefault()

    @Test
    public void testWriteString() {

        // Read Entire State Value
        String write_vin = "TEST123456";
        st.writeStateString(State.STRING_VIN, write_vin);

        String read_vin = st.readStateString(State.STRING_VIN);
        assertEquals(write_vin, read_vin);

    }


} // class StateTest
