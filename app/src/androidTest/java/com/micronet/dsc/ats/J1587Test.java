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

public class J1587Test {
    private static TestCommon test;
    private static MainService service;
    private static J1587 j1587;
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

        j1587 = new J1587(service.engine, true);

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

    /////////////////////////////////////////////////////////
    // isInJ1708Queue()
    //  Returns true if the given frame is in the can queue
    /////////////////////////////////////////////////////////
    public boolean isInJ1708Queue(int frameId, byte[] data) {

        if (j1587.outgoingList.isEmpty()) return false;
        J1587.J1708Frame f;
        for (int i = 0; i < j1587.outgoingList.size(); i++) {
            f = j1587.outgoingList.get(i);
            if (((f.id & 0xFFFFFF) == (frameId & 0xFFFFFF)) &&
                    (Arrays.equals(f.data, data))
            )
                return true; // it's here!
        }
        return false;

    } // isInJ1708Queue()

    @Test
    public void testReceiveOdometer() {
        // test the ability of the code to receive an Odometer value

        // Setup
        J1587.J1708Frame frame;

        j1587.start();
        j1587.outgoingList.clear();

        assertTrue(j1587.outgoingList.isEmpty());

        // test vehicle distance
        // Vehicle Distance is PID 245
        // Data:
        //      Bytes 1 : = 4 (4 bytes)
        //      Bytes 2-5 : Total Distance Bit Resolution: 0.161 km (0.1 mi)


        frame = new J1587.J1708Frame();
        frame.priority = 5;
        frame.id = 0x80;
        frame.data = new byte[]{(byte) J1587.PID_ODOMETER, 4, (byte) 0xAC, 0x50, 0x60, (byte) 0x82};

        int[] results = j1587.receiveJ1708Frame(frame);
        assertNotNull(results);
        assertEquals(results.length, 1);
        assertEquals(results[0], J1587.PID_ODOMETER); // 2 means parsed as control packet
        assertTrue(j1587.outgoingList.isEmpty()); // No Response needed

        assertEquals(0x826050ACL * 161, j1587.engine.status.odometer_m); // conversion to meters


    } // test_receiveOdometer()

    @Test
    public void testReceiveFuelConsumption() {
        // test the ability of the code to receive an Odometer value

        // Setup
        J1587.J1708Frame frame;

        j1587.start();
        j1587.outgoingList.clear();
        assertTrue(j1587.outgoingList.isEmpty());

        // test fuel consumption
        // Fuel Consumption is PID 250
        // Data:
        //      Bytes 1 : = 4 (4 bytes)
        //      Bytes 2-5 : Total Fuel Bit Resolution: 0.473 L (0.125 gal)


        frame = new J1587.J1708Frame();
        frame.priority = 5;
        frame.id = 0x80;
        frame.data = new byte[]{(byte) J1587.PID_FUEL_CONSUMPTION, 4, (byte) 0xFF, 0x30, 0x40, (byte) 0x80};

        int[] results = j1587.receiveJ1708Frame(frame);
        assertNotNull(results);
        assertEquals(results.length, 1);
        assertEquals(results[0], J1587.PID_FUEL_CONSUMPTION); // 2 means parsed as control packet
        assertTrue(j1587.outgoingList.isEmpty()); // No Response needed

        assertEquals(0x804030FFL * 473, j1587.engine.status.fuel_mL); // conversion to mL


    } // test_receiveFuelConsumption()

    @Test
    public void testReceiveDiagnostics() {
        // test the ability of the code to receive Diagnostic Codes

        // Setup
        J1587.J1708Frame frame;
        int[] results;

        j1587.start();
        j1587.outgoingList.clear();
        j1587.clearCollectedDtcs();
        assertTrue(j1587.outgoingList.isEmpty());

        // test diagnostics
        // Data:
        //      Bytes 1 : Total bytes
        //      Bytes 2-n : DTCs


        frame = new J1587.J1708Frame();
        frame.priority = 5;
        frame.id = 0x80;
        frame.data = new byte[]{(byte) J1587.PID_DIAGNOSTICS, 2, 0x64, (byte) 0x35};

        results = j1587.receiveJ1708Frame(frame);
        assertNotNull(results);
        assertEquals(results.length, 1);
        assertEquals(results[0], J1587.PID_DIAGNOSTICS);
        assertTrue(j1587.outgoingList.isEmpty()); // No Response needed


        assertEquals(j1587.collectedDtcs.size(), 1);

        // J1587 DTCs
        //  DTCs are MID, PID, DCC

        // DTC #1
        assertEquals(j1587.collectedDtcs.get(0).dtc_value, (long) 0x00358064L);
        assertEquals(j1587.collectedDtcs.get(0).occurence_count, 0); // no occurrence count in a 2 byte DTC
        assertEquals(j1587.collectedDtcs.get(0).source_address, 0xFF); // no source address ever


        //////////////////////////////////////////////////////////
        // Add one from a different MID, more than one at the same time, and add a 3-byte one

        frame = new J1587.J1708Frame();
        frame.priority = 5;
        frame.id = 0x81;
        frame.data = new byte[]{(byte) J1587.PID_DIAGNOSTICS, 7, 0x64, 0x35, 0x62, (byte) 0xB3, 0x05, 0x61, 0x34};

        results = j1587.receiveJ1708Frame(frame);
        assertNotNull(results);
        assertEquals(results.length, 1);
        assertEquals(results[0], J1587.PID_DIAGNOSTICS);
        assertTrue(j1587.outgoingList.isEmpty()); // No Response neede


        assertEquals(j1587.collectedDtcs.size(), 4);
        // DTC #2
        assertEquals(j1587.collectedDtcs.get(1).dtc_value, (long) 0x00358164L);
        assertEquals(j1587.collectedDtcs.get(1).occurence_count, 0);
        assertEquals(j1587.collectedDtcs.get(1).source_address, 0xFF); // no source address ever
        // DTC #3
        assertEquals(j1587.collectedDtcs.get(2).dtc_value, (long) 0x00338162L);
        assertEquals(j1587.collectedDtcs.get(2).occurence_count, 5);
        assertEquals(j1587.collectedDtcs.get(2).source_address, 0xFF); // no source address ever
        // DTC #4
        assertEquals(j1587.collectedDtcs.get(3).dtc_value, (long) 0x00348161L);
        assertEquals(j1587.collectedDtcs.get(3).occurence_count, 0);
        assertEquals(j1587.collectedDtcs.get(3).source_address, 0xFF); // no source address ever


        /////////////
        // Give a duplicate (different occurrence count) and make sure that it is not added as new item.

        frame = new J1587.J1708Frame();
        frame.priority = 5;
        frame.id = 0x81;
        frame.data = new byte[]{(byte) J1587.PID_DIAGNOSTICS, 2, 0x62, 0x33};

        results = j1587.receiveJ1708Frame(frame);
        assertNotNull(results);
        assertEquals(results.length, 1);
        assertEquals(results[0], J1587.PID_DIAGNOSTICS);
        assertTrue(j1587.outgoingList.isEmpty()); // No Response neede

        assertEquals(j1587.collectedDtcs.size(), 4);

        // DTC #3
        assertEquals(j1587.collectedDtcs.get(2).dtc_value, (long) 0x00338162L);
        assertEquals(j1587.collectedDtcs.get(2).occurence_count, 0);

        /////////////
        // Now give a new DTC from original MID and see that it is added


        frame = new J1587.J1708Frame();
        frame.priority = 5;
        frame.id = 0x80;
        frame.data = new byte[]{(byte) J1587.PID_DIAGNOSTICS, 2, 0x10, 0x20};

        results = j1587.receiveJ1708Frame(frame);
        assertNotNull(results);
        assertEquals(results.length, 1);
        assertEquals(results[0], J1587.PID_DIAGNOSTICS);
        assertTrue(j1587.outgoingList.isEmpty()); // No Response needed


        assertEquals(j1587.collectedDtcs.size(), 5);

        // DTC #5
        assertEquals(j1587.collectedDtcs.get(4).dtc_value, (long) 0x00208010L);
        assertEquals(j1587.collectedDtcs.get(4).occurence_count, 0); // no occurrence count in a 2 byte DTC
        assertEquals(j1587.collectedDtcs.get(4).source_address, 0xFF); // no source address ever

        /////////////
        // Add no DTCs reported by a module

        frame = new J1587.J1708Frame();
        frame.priority = 5;
        frame.id = 0x80;
        frame.data = new byte[]{(byte) J1587.PID_DIAGNOSTICS, 0};

        results = j1587.receiveJ1708Frame(frame);
        assertNotNull(results);
        assertEquals(results.length, 1);
        assertEquals(results[0], J1587.PID_DIAGNOSTICS);
        assertTrue(j1587.outgoingList.isEmpty()); // No Response needed


        assertEquals(j1587.collectedDtcs.size(), 5); // unchanged


    } // test_receiveDiagnostics()

    @Test
    public void testReceiveLamps() {
        // test the ability of the code to receive Lamp Status

        // Setup
        J1587.J1708Frame frame;
        int[] results;

        j1587.start();
        j1587.outgoingList.clear();
        j1587.clearCollectedDtcs();
        assertTrue(j1587.outgoingList.isEmpty());

        assertEquals(j1587.collectedLampsBf, 0);

        // test lamp status
        // Data:
        //      Bytes 1 : Lamp Bitfield
        //      2 bits each lamp: MSb - Reserved (1s), Protect, Amber, Red - LSb
        //          everything except value "01" is considered off

        // Add Protect lamp On
        // 0xD8 = b11010010

        frame = new J1587.J1708Frame();
        frame.priority = 5;
        frame.id = 0x80;
        frame.data = new byte[]{(byte) J1587.PID_LAMPS, (byte) 0xD8};

        results = j1587.receiveJ1708Frame(frame);
        assertNotNull(results);
        assertEquals(results.length, 1);
        assertEquals(results[0], J1587.PID_LAMPS);
        assertTrue(j1587.outgoingList.isEmpty()); // No Response needed


        assertEquals(j1587.collectedLampsBf, 1); // 1 is the bit for Normalized Protect Lamp


        // Now add one without any lamps on
        // 0xFF

        frame = new J1587.J1708Frame();
        frame.priority = 5;
        frame.id = 0x81;
        frame.data = new byte[]{(byte) J1587.PID_LAMPS, (byte) 0xFF};

        results = j1587.receiveJ1708Frame(frame);
        assertNotNull(results);
        assertEquals(results.length, 1);
        assertEquals(results[0], J1587.PID_LAMPS);
        assertTrue(j1587.outgoingList.isEmpty()); // No Response neede


        assertEquals(j1587.collectedLampsBf, 1); // still the same

        // Now add one with the Red Lamp On
        // 0xF1 = b11110001

        frame = new J1587.J1708Frame();
        frame.priority = 5;
        frame.id = 0x81;
        frame.data = new byte[]{(byte) J1587.PID_LAMPS, (byte) 0xF1};

        results = j1587.receiveJ1708Frame(frame);
        assertNotNull(results);
        assertEquals(results.length, 1);
        assertEquals(results[0], J1587.PID_LAMPS);
        assertTrue(j1587.outgoingList.isEmpty()); // No Response neede


        assertEquals(j1587.collectedLampsBf, 1 | 4); // 1 = Protect, 4 = Red
    } // test_receiveLamps()

    @Test
    public void testReceiveVIN() {
        // test the ability of the code to receive an Odometer value

        // Setup
        J1587.J1708Frame frame;

        j1587.start();
        j1587.outgoingList.clear();
        assertTrue(j1587.outgoingList.isEmpty());

        // test VIN
        // VIN is PID 250
        // Data:
        //      Bytes 1 : = PID length
        //      Bytes 2-.. : VIN (Ascii)


        frame = new J1587.J1708Frame();
        frame.priority = 8;
        frame.id = 0x80;
        frame.data = new byte[]{(byte) 237, 17, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50, 0x51};

        int[] results = j1587.receiveJ1708Frame(frame);
        assertNotNull(results);
        assertEquals(results.length, 1);
        assertEquals(results[0], J1587.PID_VIN); // 2 means parsed as control packet
        assertTrue(j1587.outgoingList.isEmpty()); // No Response needed

        assertEquals(j1587.engine.vin, "ABCDEFGHIJKLMNOPQ");


    } // test_receiveVIN()


} // class J1587 Test
