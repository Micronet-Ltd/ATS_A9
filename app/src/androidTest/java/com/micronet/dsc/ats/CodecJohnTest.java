package com.micronet.dsc.ats;

import android.content.Context;
import android.os.Looper;
import android.telephony.TelephonyManager;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class CodecJohnTest {
    private static final String TAG = "codec-john";

    private static final int ATS_TEST_VERSION = BuildConfig.VERSION_CODE;
    private static MainService service;
    private static Codec codec;

    private byte[] defaultExpectedMessage = {
      8,
      48, 48, 48,48, 48, 48, 48, 48,
            (byte) 0x80,
            0x79, 0x01,
            10,
            0x28, (byte) 0xC4, 0x3,
            93,
            13,
            (byte) 0xC4, (byte) 0xBE, (byte) 0xF0, 0x54,
            (byte) 136,
            0x43,
            0x44, 0, (byte) 0x88, 0x15,
            0x4C, (byte) 0x90, (byte) 0xEC, (byte) 0xB9,
            (byte) 0xF8, 0x0D,
            0x39, 1,
            0x61,
            0, 0,
            5,
            (byte) 0xA4, 0x7C, 0x22, 0x20,
            (byte) 0xC3, 0x01,
            0x00, 0x00,
            0x00
    };

    @BeforeClass
    public static void setUp(){
        Context conetxt = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if(Looper.myLooper() == null){
            Looper.prepare();
        }

        CodeMap codeMap = new CodeMap(conetxt);
        codeMap.open();
        codeMap.clearAll();

        service = new MainService(conetxt);
        codec = new Codec(service);
    }

    @Test
    public void testWeirdInternal(){
        Codec.ByteContainer bc = new Codec.ByteContainer();
        bc.thebyte = -128;
        codec.testWeird(bc);
    }

    @Test
    public void testEncode(){
        Codec.OutgoingMessage message;

        Ota.ConnectInfo connectinfo = setupDefaultConnection();
        QueueItem queueItem = setupDefaultQueueItem(connectinfo);

        message = codec.encodeMessage(queueItem, connectinfo);
        Log.d(TAG, Arrays.toString(Arrays.copyOf(message.data, message.length)));

        assertEquals(defaultExpectedMessage.length, message.length);
        assertEquals(Arrays.toString(defaultExpectedMessage), Arrays.toString(Arrays.copyOf(message.data, message.length)));

        service.codemap.writeMoEventCode(10, 0x99);

        message = codec.encodeMessage(queueItem, connectinfo);
        defaultExpectedMessage[12] = (byte) 0x99;

        assertEquals(defaultExpectedMessage.length, message.length);
        assertEquals(Arrays.toString(defaultExpectedMessage), Arrays.toString(Arrays.copyOf(message.data, message.length)));

        // Trying system boot message.
        queueItem.event_type_id = EventType.EVENT_TYPE_REBOOT;
        Codec codec = new Codec(service);
        queueItem.additional_data_bytes = Codec.dataForSystemBoot(7, null, 5);

        message = codec.encodeMessage(queueItem, connectinfo);
        defaultExpectedMessage[12] = (byte) EventType.EVENT_TYPE_REBOOT;
        byte[] expectedRebootData = {
                0x05, 0x00,
                0x07,
                (byte) (ATS_TEST_VERSION & 0xFF), ((ATS_TEST_VERSION >>8)&0xFF),
                0x05
        };

        byte[] expectedData = expectedRebootData;
        verifyEncodeData(message, expectedData);
    }

    private void verifyEncodeData(Codec.OutgoingMessage message, byte[] expectedData) {
        assertEquals(defaultExpectedMessage.length + expectedData.length-2, message.length);

        assertEquals(Arrays.toString(Arrays.copyOf(message.data, defaultExpectedMessage.length - 3 )),
                Arrays.toString(Arrays.copyOf(message.data, defaultExpectedMessage.length -3)));

        assertEquals(Arrays.toString(expectedData), Arrays.toString(Arrays.copyOfRange(message.data, message.length - expectedData.length-1, message.length -1)));

        assertEquals(defaultExpectedMessage[defaultExpectedMessage.length - 1], message.data[message.data.length -1]);
    }

    private QueueItem setupDefaultQueueItem(Ota.ConnectInfo connectinfo) {
        QueueItem queueItem = new QueueItem();

        queueItem.signal_strength = (byte)connectinfo.signalStrength;
        queueItem.network_type = (byte)connectinfo.networkType;
        queueItem.is_roaming = connectinfo.isRoaming;
        queueItem.carrier_id = Integer.parseInt(connectinfo.networkOperator);

        queueItem.trigger_dt = 1425063620L;

        queueItem.sequence_id = 377;
        queueItem.event_type_id = EventType.EVENT_TYPE_HEARTBEAT;
        queueItem.input_bitfield = 0x43;
        queueItem.battery_voltage = 136;
        queueItem.is_fix_historic = true;
        queueItem.fix_accuracy = 5;
        queueItem.fix_type = Position.FIX_TYPE_FLAG_LOC_GNSS | Position.FIX_TYPE_FLAG_TIME_EARTH;
        queueItem.sat_count = 5;
        queueItem.continuous_idle = 451;
        queueItem.latitude = 36.12345;
        queueItem.longitude = -117.56789;
        queueItem.odometer = 539131044L;
        queueItem.speed = 3576;
        queueItem.heading = 313;

        return queueItem;
    }

    private Ota.ConnectInfo setupDefaultConnection() {
        Ota.ConnectInfo connectInfo = new Ota.ConnectInfo();
        connectInfo.isRoaming = false;
        connectInfo.networkOperator = "246824";
        connectInfo.networkType = TelephonyManager.NETWORK_TYPE_LTE;
        connectInfo.dataState = TelephonyManager.DATA_CONNECTED;
        connectInfo.phoneType = TelephonyManager.PHONE_TYPE_GSM;
        connectInfo.signalStrength = -93;
        return connectInfo;
    }
}
