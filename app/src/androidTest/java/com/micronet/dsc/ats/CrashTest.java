/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.dsc.ats;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CrashTest {

    Crash cr;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        cr = new Crash(context);
        cr.clearAll();
    }


    @Test
    public void testRestorable() {

        // Since we were cleared, we are not restorable

        assertFalse(cr.isRestoreable());

        // After committing, we are restorable

        cr.edit();
        cr.commit();
        assertTrue(cr.isRestoreable());


        cr.edit();
        cr.editor.putLong("SaveTime", SystemClock.elapsedRealtime() - Crash.MAX_ELAPSED_RESTORE_TIME_MS); // to far in past
        cr.editor.commit();

        assertFalse(cr.isRestoreable());

        cr.edit();
        cr.editor.putLong("SaveTime", SystemClock.elapsedRealtime() + 1000); // one second in future
        cr.editor.commit();

        assertFalse(cr.isRestoreable());

        cr.edit();
        cr.editor.putLong("SaveTime", SystemClock.elapsedRealtime()); // just right
        cr.editor.commit();

        assertTrue(cr.isRestoreable());

        cr.edit();
        cr.editor.putString("Version", "XXX"); // wrong version
        cr.editor.commit();

        assertFalse(cr.isRestoreable());

        cr.edit();
        cr.editor.putString("Version", BuildConfig.VERSION_NAME); // right version
        cr.editor.commit();


        assertTrue(cr.isRestoreable());

        cr.clearAll();
        assertFalse(cr.isRestoreable());


    } // testRestorable()

    @Test
    public void testWriteArrayInt() {


        // test writing the int array

        int[] iarr = {20, 30, 40};

        cr.edit();
        cr.writeStateArrayInt(1, iarr);
        cr.commit();


        String[] sarr = cr.readStateArray(1);

        assertEquals(sarr.length, 3);
        assertEquals("20", sarr[0]);
        assertEquals("30", sarr[1]);
        assertEquals("40", sarr[2]);


    } // testWriteArrayInt()

    @Test
    public void testWriteArrayLong() {


        // test writing the int array

        long[] larr = {20, 30, 5000000000L};

        cr.edit();
        cr.writeStateArrayLong(1, larr);
        cr.commit();


        String[] sarr = cr.readStateArray(1);

        assertEquals(sarr.length, 3);
        assertEquals("20", sarr[0]);
        assertEquals("30", sarr[1]);
        assertEquals("5000000000", sarr[2]);


    } // testWriteArrayLong()


} // class
