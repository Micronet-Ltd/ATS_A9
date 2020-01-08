package com.micronet.dsc.ats;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
public class UdpTest {
    private Udp udpTesting;

    @Before
    public void setup(){
        udpTesting = new Udp(false, "testingSocket");

    }

    @Test
    public void messageList(){
    }
}
