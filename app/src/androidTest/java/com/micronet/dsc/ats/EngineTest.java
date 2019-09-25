/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.dsc.ats;

import android.content.Context;
import android.os.Looper;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class EngineTest {

    private static TestCommon test;
    private static MainService service;
    private static Engine engine;
    private static Config config;
    private static State state;

    @BeforeClass
    public static void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }


        config = new Config(context);
        state = new State(context);

        // clear config and state info to the default before init'ing IO
        config.open();
        config.clearAll();
        state.clearAll();

        service = new MainService(context);
        engine = service.engine;

        service.queue.clearAll();
        service.clearEventSequenceIdNow();

        test = new TestCommon(service.queue);
    } // setup

    @Before
    public void beforeTest() {
        config.clearAll();
        state.clearAll();

        service.queue.clearAll();
        service.clearEventSequenceIdNow();

        test = new TestCommon(service.queue);
    }

    @Test
    public void testHasBusPriority() {
        // priority is simple, just J1939 bus has higher priority than J1587 has higher priority than none.

        assertTrue(Engine.hasBusPriority(Engine.BUS_TYPE_J1939, Engine.BUS_TYPE_NONE));
        assertTrue(Engine.hasBusPriority(Engine.BUS_TYPE_J1587, Engine.BUS_TYPE_NONE));
        assertTrue(Engine.hasBusPriority(Engine.BUS_TYPE_J1939, Engine.BUS_TYPE_J1587));
        assertTrue(Engine.hasBusPriority(Engine.BUS_TYPE_J1939, Engine.BUS_TYPE_J1939));
        assertTrue(Engine.hasBusPriority(Engine.BUS_TYPE_J1587, Engine.BUS_TYPE_J1587));

        assertFalse(Engine.hasBusPriority(Engine.BUS_TYPE_J1587, Engine.BUS_TYPE_J1939));
    } // test_hasBusPriority()

    @Test
    public void testCheckParkingBrake() {

        // Set the config for parking brake

        service.config.writeSetting(Config.SETTING_PARKING_BRAKE, "3|1"); // all messages, conflict-default is on


        // because input poll period is slower (500ms), really this takes wither 5 or 6 polls, not 30 or 40

        //assertFalse((engine.savedIo.input_bitfield & Io.INPUT_BITVALUE_IGNITION) != 0);
        service.queue.clearAll();

        int i;

        // no matter how long, if ignition is off then we are off
        for (i = 1; i < 10; i++) {
            assertFalse(engine.checkParkingBrake(Engine.BUS_TYPE_J1939, false));
        }

        // it must be high for longer than the debounce time to be considered on
        //  debounce time is always two polls

        assertNull(service.queue.getFirstItem(Queue.SERVER_ID_PRIMARY));
        //assertFalse(engine.checkParkingBrake(true));
        assertTrue(engine.checkParkingBrake(Engine.BUS_TYPE_J1939, true));

        // conflicting info from another bus does nothing

        assertTrue(engine.checkParkingBrake(Engine.BUS_TYPE_J1587, false));


        // other things that should have happened when ignition turned on:
        assertTrue(service.state.readStateBool(State.FLAG_PARKING_BRAKE_STATUS));
        assertEquals(EventType.EVENT_TYPE_PARKBRAKE_ON, service.queue.getFirstItem(Queue.SERVER_ID_PRIMARY).event_type_id);


        // and ignition turns off just as easily (two poll periods)
        service.queue.clearAll();
        //assertTrue(engine.checkParkingBrake(false));
        assertFalse(engine.checkParkingBrake(Engine.BUS_TYPE_J1939, false));

        // other things that should have happened when ignition turned off:
        assertFalse(service.state.readStateBool(State.FLAG_PARKING_BRAKE_STATUS));
        assertEquals(EventType.EVENT_TYPE_PARKBRAKE_OFF, service.queue.getFirstItem(Queue.SERVER_ID_PRIMARY).event_type_id);

    } // test_checkParkingBrake()

    @Test
    public void testCheckDTCs() {

        // there should be no outstanding DTCs to start

        assertEquals(0, engine.current_dtcs.size());

        service.queue.clearAll();
        service.config.writeSetting(Config.SETTING_FAULT_CODES, "3"); // messages


        long[] dtclist2 = new long[]{0x12345678, 0xABCDEF01};
        long[] dtclist1 = new long[]{0x12345678};
        long[] dtclist0 = new long[]{};


        int[] dtcsource2 = new int[]{0, 0x17};
        int[] dtcsource1 = new int[]{0};
        int[] dtcsource0 = new int[]{};

        byte[] state_code_array; // expected state info
        byte[] message_code_array; // expected for messages
        byte[] message_code_array1; // expected for messages
        byte[] message_code_array2; // expected for messages

        int lamp_status_bf = 0x55;

        // checkDTCs responds with 0xAADD (AA = num added, DD = num deleted)

        // add a DTCs
        assertEquals(0x0100, engine.checkDtcs(Engine.BUS_TYPE_J1587, dtclist1, dtcsource1, lamp_status_bf));

        // check for messages and state changes
        state_code_array = new byte[]{Engine.BUS_TYPE_J1587, 0x78, 0x56, 0x34, 0x12, 0x00};
        message_code_array = new byte[]{Engine.BUS_TYPE_J1587, 0x78, 0x56, 0x34, 0x12, 0x00, 0x55};

        assertArrayEquals(state_code_array, service.state.readStateArray(State.ARRAY_FAULT_CODES));
        assertEquals(EventType.EVENT_TYPE_FAULTCODE_ON, service.queue.getFirstItem(Queue.SERVER_ID_PRIMARY).event_type_id);
        assertEquals(Arrays.toString(message_code_array), Arrays.toString(service.queue.getFirstItem(Queue.SERVER_ID_PRIMARY).additional_data_bytes));


        // add another DTC
        service.queue.clearAll();
        assertEquals(0x0100, engine.checkDtcs(Engine.BUS_TYPE_J1587, dtclist2, dtcsource2, lamp_status_bf));


        // check for messages and state changes
        state_code_array = new byte[]{Engine.BUS_TYPE_J1587, 0x78, 0x56, 0x34, 0x12, 0x00,
                Engine.BUS_TYPE_J1587, 0x01, (byte) 0xEF, (byte) 0xCD, (byte) 0xAB, 0x17};
        message_code_array = new byte[]{Engine.BUS_TYPE_J1587, 0x01, (byte) 0xEF, (byte) 0xCD, (byte) 0xAB, 0x17, 0x55};


        assertArrayEquals(state_code_array, service.state.readStateArray(State.ARRAY_FAULT_CODES));
        assertEquals(EventType.EVENT_TYPE_FAULTCODE_ON, service.queue.getFirstItem(Queue.SERVER_ID_PRIMARY).event_type_id);
        assertArrayEquals(message_code_array, service.queue.getFirstItem(Queue.SERVER_ID_PRIMARY).additional_data_bytes);


        // add two DTCs from a different bus (these should be kept separate from the other bus DTCs)
        service.queue.clearAll();
        assertEquals(0x0200, engine.checkDtcs(Engine.BUS_TYPE_J1939, dtclist2, dtcsource2, lamp_status_bf));

        // check for messages and state changes
        state_code_array = new byte[]{Engine.BUS_TYPE_J1587, 0x78, 0x56, 0x34, 0x12, 0x00,
                Engine.BUS_TYPE_J1587, 0x01, (byte) 0xEF, (byte) 0xCD, (byte) 0xAB, 0x17,
                Engine.BUS_TYPE_J1939, 0x78, 0x56, 0x34, 0x12, 0x00,
                Engine.BUS_TYPE_J1939, 0x01, (byte) 0xEF, (byte) 0xCD, (byte) 0xAB, 0x17
        };
        message_code_array1 = new byte[]{
                Engine.BUS_TYPE_J1939, 0x78, 0x56, 0x34, 0x12, 0x00, 0x55};
        message_code_array2 = new byte[]{
                Engine.BUS_TYPE_J1939, 0x01, (byte) 0xEF, (byte) 0xCD, (byte) 0xAB, 0x17, 0x55
        };

        assertArrayEquals(state_code_array, service.state.readStateArray(State.ARRAY_FAULT_CODES));
        assertEquals(EventType.EVENT_TYPE_FAULTCODE_ON, service.queue.getFirstItem(Queue.SERVER_ID_PRIMARY).event_type_id);
        assertArrayEquals(message_code_array1, service.queue.getFirstItem(Queue.SERVER_ID_PRIMARY).additional_data_bytes);

        assertEquals(EventType.EVENT_TYPE_FAULTCODE_ON, service.queue.getAllItems().get(1).event_type_id);
        assertEquals(Arrays.toString(message_code_array2), Arrays.toString(service.queue.getAllItems().get(1).additional_data_bytes));

        // now remove one dtc

        service.queue.clearAll();
        assertEquals(0x0200, engine.checkDtcs(Engine.BUS_TYPE_J1939, dtclist1, dtcsource1, lamp_status_bf));

        // check for messages and state changes
        state_code_array = new byte[]{Engine.BUS_TYPE_J1587, 0x78, 0x56, 0x34, 0x12, 0x00,
                Engine.BUS_TYPE_J1587, 0x01, (byte) 0xEF, (byte) 0xCD, (byte) 0xAB, 0x17,
                Engine.BUS_TYPE_J1939, 0x78, 0x56, 0x34, 0x12, 0x00,

        };
        message_code_array1 = new byte[]{
                Engine.BUS_TYPE_J1939, 0x01, (byte) 0xEF, (byte) 0xCD, (byte) 0xAB, 0x00, 0x55
        };

        assertArrayEquals(state_code_array, service.state.readStateArray(State.ARRAY_FAULT_CODES));
        assertEquals(EventType.EVENT_TYPE_FAULTCODE_OFF, service.queue.getFirstItem(Queue.SERVER_ID_PRIMARY).event_type_id);
        assertArrayEquals(message_code_array1, service.queue.getFirstItem(Queue.SERVER_ID_PRIMARY).additional_data_bytes);


        // now remove two dtcs


        service.queue.clearAll();
        assertEquals(0x0200, engine.checkDtcs(Engine.BUS_TYPE_J1587, dtclist0, dtcsource0, lamp_status_bf));

        // check for messages and state changes
        state_code_array = new byte[]{
                Engine.BUS_TYPE_J1939, 0x78, 0x56, 0x34, 0x12, 0x00,
                Engine.BUS_TYPE_J1939, 0x01, (byte) 0xEF, (byte) 0xCD, (byte) 0xAB, 0x17,
        };
        message_code_array1 = new byte[]{
                Engine.BUS_TYPE_J1587, 0x78, 0x56, 0x34, 0x12, 0x00, 0x55
        };
        message_code_array2 = new byte[]{
                Engine.BUS_TYPE_J1587, 0x01, (byte) 0xEF, (byte) 0xCD, (byte) 0xAB, 0x17, 0x55
        };

        assertArrayEquals(state_code_array, service.state.readStateArray(State.ARRAY_FAULT_CODES));
        assertEquals(EventType.EVENT_TYPE_FAULTCODE_OFF, service.queue.getFirstItem(Queue.SERVER_ID_PRIMARY).event_type_id);
        assertArrayEquals(message_code_array1, service.queue.getFirstItem(Queue.SERVER_ID_PRIMARY).additional_data_bytes);

        assertEquals(EventType.EVENT_TYPE_FAULTCODE_OFF, service.queue.getAllItems().get(1).event_type_id);
        assertArrayEquals(message_code_array2, service.queue.getAllItems().get(1).additional_data_bytes);


    } // check DTCs


} // class EngineTest
