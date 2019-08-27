/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */


package com.micronet.dsc.ats;

import android.content.Context;
import android.os.Looper;
import android.telephony.TelephonyManager;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class CodecTest {
    private static final int ATS_TEST_VERSION = BuildConfig.VERSION_CODE; // test that version information returned in udp packets matches this

    private static MainService service;
    private static Codec codec;

    private byte[] defaultExpectedMessage = { // If the udp protocol changes then this will also need to change
            8,                // length of device ID
            48, 48, 48, 48, 48, 48, 48, 48,  // device ID
            (byte) 0x80,                // requires an ACK, application source ID = 0
            0x79, 0x01,       // sequence number 377
            10,               // Heartbeat
            0x28, (byte) 0xC4, 0x3, // carrier ID "246824"
            93,              // signal strength
            13,               // LTE
            (byte) 0xC4, (byte) 0xBE, (byte) 0xF0, 0x54,   // time
            (byte) 136,            // Battery voltage
            0x43,              // Input bitfield
            0x44, 0, (byte) 0x88, 0x15, //lat
            0x4C, (byte) 0x90, (byte) 0xEC, (byte) 0xB9, //lon
            (byte) 0xF8, 0x0D,       // speed
            0x39, 1,        // heading
            0x61,             // historic GPS, earth time
            0, 0,                // HDOP
            5,                // satelite count
            (byte) 0xA4, 0x7C, 0x22, 0x20, // odom
            (byte) 0xC3, 0x01, // idle
            0x00, 0x00, // data length
            0x00 // Dock state (this can change)
    };

    @BeforeClass
    public static void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        CodeMap codeMap = new CodeMap(context);
        codeMap.open();
        codeMap.clearAll();

        service = new MainService(context);

        codec = new Codec(service);
    }

    @Test
    public void testWeirdInternal() {
        Codec.ByteContainer bc = new Codec.ByteContainer();
        bc.thebyte = -93;
        codec.testWeird(bc);
    }

    @Test
    public void testEncode() {
        Codec.OutgoingMessage message;

        //setup
        Ota.ConnectInfo connectInfo = setupDefaultConnectInfo();
        QueueItem queueItem = setupDefaultQueueItem(connectInfo);

        ////////////////////////////////////
        // encode a basic (default) message
        message = codec.encodeMessage(queueItem, connectInfo);
        Log.d("TEST", Arrays.toString(Arrays.copyOf(message.data, message.length)));

        assertEquals(defaultExpectedMessage.length, message.length);
        assertEquals(Arrays.toString(defaultExpectedMessage), Arrays.toString(Arrays.copyOf(message.data, message.length)));

        ////////////////////////////////////
        // now add  re-mapping and re-encode
        service.codemap.writeMoEventCode(10, 0x99); // make all 10s appear as 99s.

        message = codec.encodeMessage(queueItem, connectInfo);

        defaultExpectedMessage[12] = (byte) 0x99;

        assertEquals(defaultExpectedMessage.length, message.length);
        assertEquals(Arrays.toString(defaultExpectedMessage), Arrays.toString(Arrays.copyOf(message.data, message.length)));

        ////////////////////////////////////
        // try the system boot message, since it contains more data

        queueItem.event_type_id = EventType.EVENT_TYPE_REBOOT;
        Codec codec = new Codec(service);
        queueItem.additional_data_bytes = Codec.dataForSystemBoot(7, null, 5); // boot_reason

        message = codec.encodeMessage(queueItem, connectInfo);

        defaultExpectedMessage[12] = (byte) EventType.EVENT_TYPE_REBOOT;

        byte[] expectedRebootData = {
                0x05, 0x00,
                0x07,  // io_boot_reason
                (byte) (ATS_TEST_VERSION & 0xFF), ((ATS_TEST_VERSION >> 8) & 0xFF), // ATS Version
                (byte) 0xFF,
                0x05
        };

        byte[] expectedData = expectedRebootData;
        // Make sure expected and received message lengths are the same
        // Minus 2 because the double 0 in the default message gets overwritten by the new data length
        verifyEncodeData(message, expectedData);


        ////////////////////////////////////
        // try the system restart message, since it contains more data

        queueItem.event_type_id = EventType.EVENT_TYPE_RESTART;
        codec = new Codec(service);
        queueItem.additional_data_bytes = Codec.dataForServiceRestart("unknown"); // boot_reason

        message = codec.encodeMessage(queueItem, connectInfo);

        defaultExpectedMessage[12] = (byte) EventType.EVENT_TYPE_RESTART;

        byte[] expectedRestartData = {
                0x03, 0x00,
                (byte) (ATS_TEST_VERSION & 0xFF), ((ATS_TEST_VERSION >> 8) & 0xFF),
                0 // unknown reason
        };

        expectedData = expectedRestartData;
        // Verify that data is equal
        verifyEncodeData(message, expectedData);

        ////////////////////////////////////
        // try the system shutdown message, since it contains more data

        // 07/18/2016 @ 7:19pm (UTC)
        // = 1468869546 seconds
        // = 24481159.1 minutes = x01758D87
        service.state.writeStateLong(State.NEXT_HEARTBEAT_TIME_S, 1468869546L);


        queueItem.event_type_id = EventType.EVENT_TYPE_SHUTDOWN;
        codec = new Codec(service);
        queueItem.additional_data_bytes = codec.dataForSystemShutdown(3); // shutdown_reason

        message = codec.encodeMessage(queueItem, connectInfo);

        defaultExpectedMessage[12] = (byte) EventType.EVENT_TYPE_SHUTDOWN;

        byte[] expectedShutdownData = {
                0x05, 0x00,
                0x03,  // shutdown_reason
                (byte) 0x87, (byte) 0x8D, 0x75, 0x1
        };

        expectedData = expectedShutdownData;
        // Verify that data is equal
        verifyEncodeData(message, expectedData);
    } // test_encode()

    @Test
    public void testDecode() {

        byte[] newMessageData = {
                8,                // length of device ID
                48, 48, 48, 48, 48, 48, 48, 48,  // device ID
                (byte) 0x80,                // requires an ACK
                0x79, 0x01,       // sequence number 377
                100,                 // Config Write
                4, 0,                // data length
                18,               // Idling
                0x33, 0x32, 0x31 // new value: 321
        };

        Codec.IncomingMessage incomingMessage = new Codec.IncomingMessage();


        incomingMessage.data = Arrays.copyOf(newMessageData, newMessageData.length);
        incomingMessage.length = newMessageData.length;

        QueueItem queueItem;
        queueItem = codec.decodeMessage(incomingMessage);


        assertEquals(EventType.EVENT_TYPE_CONFIGW, queueItem.event_type_id);
        assertEquals(377, queueItem.sequence_id);

        byte[] additionalData = {18, 0x33, 0x32, 0x31};
        assertNotNull(queueItem.additional_data_bytes);
        assertEquals(additionalData.length, queueItem.additional_data_bytes.length);
        assertEquals(Arrays.toString(additionalData), Arrays.toString(queueItem.additional_data_bytes));


        ////////////////////////////////////
        // now add  re-mapping and re-decode
        service.codemap.writeMtEventCode(0x98, 100); // make all 0x98s appear as type 100s

        newMessageData[12] = (byte) 0x98;
        incomingMessage = new Codec.IncomingMessage();


        incomingMessage.data = Arrays.copyOf(newMessageData, newMessageData.length);
        incomingMessage.length = newMessageData.length;

        queueItem = codec.decodeMessage(incomingMessage);

        assertEquals(EventType.EVENT_TYPE_CONFIGW, queueItem.event_type_id);
        assertEquals(377, queueItem.sequence_id);

    } // test_decode()

    @Test
    public void testDataForSystemBoot() {
        Codec codec = new Codec(service);
        Power.RTCRebootStatusClass rtcRebootStatus = new Power.RTCRebootStatusClass();
        rtcRebootStatus.hasRTCcleared = true;
        rtcRebootStatus.hasRTCtriggered = false;

        byte[] data = Codec.dataForSystemBoot(7, rtcRebootStatus, 5);
        byte[] expected = {7, (byte) (ATS_TEST_VERSION & 0xFF), (byte) ((ATS_TEST_VERSION >> 8) & 0xFF), (byte) 0x02, (byte) 0x05};

        assertEquals(expected.length, data.length);
        assertEquals(Arrays.toString(expected), Arrays.toString(data));
    }

    @Test
    public void testDataForSystemShutdown() {
        // 07/18/2016 @ 7:19pm (UTC)
        // = 1468869546 seconds
        // = 24481159.1 minutes = x01758D87
        service.state.writeStateLong(State.NEXT_HEARTBEAT_TIME_S, 1468869546L);

        Codec codec = new Codec(service);
        byte[] data = codec.dataForSystemShutdown(2);

        byte[] expected = {0x2, (byte) 0x087, (byte) 0x8D, 0x75, 0x1};

        assertEquals(expected.length, data.length);
        assertEquals(Arrays.toString(expected), Arrays.toString(data));
    }

    //////////////////////////////////////////////////
    // Helper functions
    //////////////////////////////////////////////////

    // If the udp protocol changes then this will also need to change
    private void verifyEncodeData(Codec.OutgoingMessage message, byte[] expectedData) {
        // Compare total message length
        assertEquals(defaultExpectedMessage.length + expectedData.length - 2, message.length);
        // Make sure that everything up until the data is the same
        assertEquals(Arrays.toString(Arrays.copyOf(defaultExpectedMessage, defaultExpectedMessage.length - 3)),
                Arrays.toString(Arrays.copyOf(message.data, defaultExpectedMessage.length - 3)));
        // Make sure the data is the same
        assertEquals(Arrays.toString(expectedData), Arrays.toString(Arrays.copyOfRange(message.data, message.length - expectedData.length - 1, message.length - 1)));
        // Make sure the dock state is the same
        assertEquals(defaultExpectedMessage[defaultExpectedMessage.length - 1], message.data[message.data.length - 1]);
    }

    private Ota.ConnectInfo setupDefaultConnectInfo() {
        Ota.ConnectInfo connectInfo = new Ota.ConnectInfo();

        connectInfo.isRoaming = false;
        connectInfo.networkOperator = "246824";
        connectInfo.networkType = TelephonyManager.NETWORK_TYPE_LTE;
        connectInfo.dataState = TelephonyManager.DATA_CONNECTED;
        connectInfo.phoneType = TelephonyManager.PHONE_TYPE_GSM;
        connectInfo.signalStrength = -93; // Ota.convertGSMStrengthtoDBM(10);

        return connectInfo;

    }

    private QueueItem setupDefaultQueueItem(Ota.ConnectInfo connectInfo) {
        QueueItem queueItem = new QueueItem();

        queueItem.signal_strength = (byte) connectInfo.signalStrength;
        queueItem.network_type = (byte) connectInfo.networkType;
        queueItem.is_roaming = connectInfo.isRoaming;
        queueItem.carrier_id = Integer.parseInt(connectInfo.networkOperator);


        queueItem.trigger_dt = 1425063620L;

        queueItem.sequence_id = 377;
        queueItem.event_type_id = EventType.EVENT_TYPE_HEARTBEAT;
        queueItem.input_bitfield = 0x43; // ignition on, some inputs on
        queueItem.battery_voltage = 136;
        queueItem.is_fix_historic = true;
        queueItem.fix_accuracy = 5; // really good accuracy (5m)
        queueItem.fix_type = Position.FIX_TYPE_FLAG_LOC_GNSS | Position.FIX_TYPE_FLAG_TIME_EARTH;
        queueItem.sat_count = 5;
        queueItem.continuous_idle = 451;
        queueItem.latitude = 36.12345;
        queueItem.longitude = -117.56789;
        queueItem.odometer = 539131044L; // 539,131,044 meters = 335000.5 miles
        queueItem.speed = 3576; // 3,576 cm/s = 80 mph
        queueItem.heading = 313;

        return queueItem;
    }

} // CodecTest
