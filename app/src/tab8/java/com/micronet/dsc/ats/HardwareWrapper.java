/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

/////////////////////////////////////////////////////////////
// IoServiceHardwareWrapper:
//  Handles communications with hardware regarding I/O and Device Info.
/////////////////////////////////////////////////////////////

package com.micronet.dsc.ats;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.PowerManager;
import android.os.SystemClock;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Hardware wrapper for the Tab8.
 */
public class HardwareWrapper extends IoServiceHardwareWrapper {
    static public final String TAG = "ATS-IOS-Wrap-Tab8";
    static public final String HW_SHUTDOWN_DEVICE = "reboot";

    // No HW schemes implemented
    static int IO_SCHEME_DEFAULT = 0;

    static void shutdownDevice(PowerManager powerManager) {
        powerManager.reboot(HW_SHUTDOWN_DEVICE);
    }

    static int restartRILDriver() {
        Log.i(TAG, "Restarting RIL Driver ");

        String command;
        command = "su -c 'setprop ctl.restart ril-daemon'";

        int exitCode = -1; // error

        try {
            exitCode = Runtime.getRuntime().exec(new String[]{"sh", "-c", command}).waitFor();
        } catch (Exception e) {
            Log.d(TAG, "Exception exec: " + command + ": " + e.getMessage());
        }

        Log.d(TAG, command + " returned " + exitCode);

        // exitCode should be 0 if it completed successfully
        return exitCode;
    }

    static void wakeupDevice(AlarmManager alarmManager, long triggerAtMillis, PendingIntent operation) {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation);
    }

    //////////////////////////////////////////////////////////
    // getHardwareIoScheme()
    //  decides which I/O scheme to use for reading inputs.
    //  earlier pcbs used a digital scheme, later pcbs use an analog scheme to allow float-defaults
    //////////////////////////////////////////////////////////
    public static int getHardwareIoScheme() { return IO_SCHEME_DEFAULT; }


    public static int remapBootInputMask(int boot_input_mask) {

        // Original bitmap:
        // b0: ignition
        // b1: input 1
        // b2: input 2
        // b3: input 3

        // Final bitmap:
        // b0: ignition
        // b1: input 1
        // b2: input 2
        // b3: input 3
        // b4: wiggle
        // b5: arm lockup
        // b6: watchdog

        return boot_input_mask & 0x0F;
    }


    public static void setUntrustworthyShutdown(int io_scheme, HardwareInputResults hardwareInputResults) {
        // Inputs  are untrustworthy if they are received from analog and they report a ground during the shutdown window
        if (hardwareInputResults.input1 == 0)
            hardwareInputResults.input1 = IoServiceHardwareWrapper.HW_INPUT_UNKNOWN;
        if (hardwareInputResults.input2 == 0)
            hardwareInputResults.input2 = IoServiceHardwareWrapper.HW_INPUT_UNKNOWN;
        if (hardwareInputResults.input3 == 0)
            hardwareInputResults.input3 = IoServiceHardwareWrapper.HW_INPUT_UNKNOWN;
        if (hardwareInputResults.input4 == 0)
            hardwareInputResults.input4 = IoServiceHardwareWrapper.HW_INPUT_UNKNOWN;
        if (hardwareInputResults.input5 == 0)
            hardwareInputResults.input5 = IoServiceHardwareWrapper.HW_INPUT_UNKNOWN;
        if (hardwareInputResults.input6 == 0)
            hardwareInputResults.input6 = IoServiceHardwareWrapper.HW_INPUT_UNKNOWN;
        if (hardwareInputResults.input7 == 0)
            hardwareInputResults.input7 = IoServiceHardwareWrapper.HW_INPUT_UNKNOWN;
    }

    //////////////////////////////////////////////////////////
    // getAllHardwareInputs()
    //  Calls the hardware API
    //  This is called from timer to get the state of the hardware inputs
    // Returns HardwareInputResults (or null if not found)
    //////////////////////////////////////////////////////////
    public static HardwareInputResults getAllHardwareInputs(int io_scheme) {
        Log.vv(TAG, "getAllHardwareInputs()");
        long start_now = SystemClock.elapsedRealtime();

        HardwareInputResults hardwareInputResults = new HardwareInputResults();
        hardwareInputResults.savedTime = start_now;

        try {
            Log.vv(TAG, "getAllInputs()");
            int[] allInputs = getAllPinInState();

            if (allInputs == null) {
                Log.e(TAG, "Could not read analog inputs; getAllInputs() returns null");
            } else if (allInputs.length < 1) { // should be at least 1 entries
                Log.e(TAG, "Could not read analog inputs; getAllInputs() returns < 1 entry = " + Arrays.toString(allInputs));
            } else {
                Log.v(TAG, " Input results " + Arrays.toString(allInputs));

                // TODO: Change default input voltage to cradle
                hardwareInputResults.voltage = 12.0;

                hardwareInputResults.input1 = getHardwareInput(allInputs[1], 1);
                hardwareInputResults.input2 = getHardwareInput(allInputs[2], 2);
                hardwareInputResults.input3 = getHardwareInput(allInputs[3], 3);
                hardwareInputResults.input4 = getHardwareInput(allInputs[4], 4);
                hardwareInputResults.input5 = getHardwareInput(allInputs[5], 5);
                hardwareInputResults.input6 = getHardwareInput(allInputs[6], 6);
                hardwareInputResults.input7 = getHardwareInput(allInputs[7], 7);

                // Get ignition
                int ignitionInput = getHardwareInput(allInputs[0], 0);
//                int ignitionInput = Io.ignitionState.get();
                Log.d(TAG, "Hardware Wrapper ignition state: " + ignitionInput);
                if (ignitionInput != -1) {
                    hardwareInputResults.ignition_valid = true;
                    hardwareInputResults.ignition_input = ignitionInput == 1;
                }
            } // inputs returned something

        } catch (Exception e) {
            Log.e(TAG, "Exception when trying to get Inputs from vinputs");
            Log.e(TAG, "Exception = " + e.toString(), e);
            return null;
        }

        long end_now = SystemClock.elapsedRealtime();
        Log.vv(TAG, "getHardwareInputs() END: " + (end_now - start_now) + " ms");

        return hardwareInputResults;

    } // getAllHardwareInputs()

    private static int getHardwareInput(int inputVal, int inputNum) {
        if (inputVal < 0) {
            Log.w(TAG, "reading Input" + inputNum + " returned error (-1), trying again");
            inputVal = getInputState(inputNum);
            if (inputVal < 0) {
                Log.e(TAG, "reading Input" + inputNum + " returned error (-1) on retry, aborting read");
            }
        }
        return inputVal;
    }

    //////////////////////////////////////////////////////////
    // getHardwareVoltageOnly()
    //  Calls the hardware API
    //  This is called from timer to get the state of the voltage
    // Returns the voltage of the analog input, used during init
    //////////////////////////////////////////////////////////
    public static HardwareInputResults getHardwareVoltageOnly() {
        Log.vv(TAG, "getHardwareVoltage()");
        long start_now = SystemClock.elapsedRealtime();

        HardwareInputResults hardwareVoltageResults = new HardwareInputResults();
        hardwareVoltageResults.savedTime = start_now;
        // TODO: Change to default input voltage value
        hardwareVoltageResults.voltage = 12.0;
        return hardwareVoltageResults;

//        if (HardwareWrapper.getIoInstance() == null) {
//            Log.e(TAG, "NULL result when trying to getInstance() of Micronet Hardware API");
//            return null;
//        } else {
//
//            try {
//
//                int inputVal;
//
//                inputVal = HardwareWrapper.getAnalogInput(MicronetHardware.TYPE_ANALOG_INPUT1);
//                if (inputVal == -1) {
//                    Log.w(TAG, "reading Analog1 returned error (-1), trying again");
//                    inputVal = HardwareWrapper.getAnalogInput(MicronetHardware.TYPE_ANALOG_INPUT1);
//                    if (inputVal == -1) {
//                        Log.e(TAG, "reading Analog1 returned error (-1) on retry, aborting read");
//                    }
//                }
//                if (inputVal != -1) {
//                    hardwareVoltageResults.voltage = inputVal / 1000.0; // convert to volts from mvolts
//                }
//
//
//            } catch (Exception e) {
//                Log.e(TAG, "Exception when trying getAnalogInput() from Micronet Hardware API ");
//                Log.e(TAG, "Exception = " + e.toString(), e);
//                return null;
//            }
//        } // no null
//
//        long end_now = SystemClock.elapsedRealtime();
//        //Log.v(TAG, "getHardwareVoltage() END: " + (end_now - start_now) + " ms");
//
//        return hardwareVoltageResults;

    } // getHardwareVoltage()

    static int getAnalogInput(int analog_type) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    static int[] getAllAnalogInput() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    static int[] getAllPinInState() {
         beginCall();

        // Initialize all to -1
        int[] inputs = new int[8];
        for (int i = 0; i < 8; i++) {
            inputs[i] = -1;
        }

        // Get all inputs
        for (int i = 0; i < 8; i++) {
            inputs[i] = getInputState(i);
        }

        endCall("getAllPinInState()");
        return inputs;
    }

    /**
     * Returns the input value for the desired input.
     * @param digital_type should be between 0 and 7.
     * @return whether the input is currently high (>0) or low (0). If there is an error (<0) is returned.
     */
    static int getInputState(int digital_type) {
        beginCall();

        int inputValue = -1;
        try {
            @SuppressWarnings("rawtypes")
            Class InputOutputService = Class.forName("com.android.server.vinputs.InputOutputService");
            Method readInput = InputOutputService.getMethod("readInput", int.class);
            Constructor<?> constructor = InputOutputService.getConstructor();
            Object ioServiceInstance = constructor.newInstance();
            inputValue = (int) readInput.invoke(ioServiceInstance, digital_type);
        } catch (IllegalArgumentException iAE) {
            throw iAE;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (java.lang.InstantiationException e) {
            e.printStackTrace();
        }

        if (inputValue > 1) {
            inputValue = 1;
        }

        endCall("getInputState()");
        return inputValue;
    }

    public static int getHardwareBootState() {
        beginCall();
        // TODO: Not sure we can get boot state on Tab8
        endCall("getHardwareBootState()");
        return 0;
    }

} // HardwareWrapper

