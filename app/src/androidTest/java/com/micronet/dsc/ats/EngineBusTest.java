/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.dsc.ats;

import org.junit.Test;

import static org.junit.Assert.*;

public class EngineBusTest {
    // Most of the functions in EngineBus class are tested in the EngineBus sub-classes (J1587Test and J1939Test)

    @Test
    public void testLittleEndian2Long() {
        byte[] inputBytes = new byte[]{1, -2, 3, -4, 5, -6, 7, -8};

        long resultLong;

        resultLong = EngineBus.littleEndian2Long(inputBytes, 0, 8);
        assertEquals(resultLong, 0xF807FA05FC03FE01L);

        resultLong = EngineBus.littleEndian2Long(inputBytes, 0, 3);
        assertEquals(resultLong, 0x03FE01L);

        resultLong = EngineBus.littleEndian2Long(inputBytes, 5, 3);
        assertEquals(resultLong, 0xF807FAL);

        resultLong = EngineBus.littleEndian2Long(inputBytes, 4, 4);
        assertEquals(resultLong, 0xF807FA05L);

    } // test_littleEndian2Long()

    @Test
    public void testLong2LittleEndian() {
        byte[] resultBytes = new byte[8];

        EngineBus.long2LittleEndian(-1, resultBytes, 0, 8);
        assertEquals(Log.bytesToHex(resultBytes, 8), "FFFFFFFFFFFFFFFF");

        EngineBus.long2LittleEndian(-1, resultBytes, 0, 5);
        assertEquals(Log.bytesToHex(resultBytes, 5), "FFFFFFFFFF");

        EngineBus.long2LittleEndian(0, resultBytes, 0, 4);
        assertEquals(Log.bytesToHex(resultBytes, 4), "00000000");

        // first 4 bytes are already known to be all 0s
        EngineBus.long2LittleEndian(0xF0F102F3, resultBytes, 4, 4);
        assertEquals(Log.bytesToHex(resultBytes, 8), "00000000F302F1F0");

    } // test_long2LittleEndian()
} // class
