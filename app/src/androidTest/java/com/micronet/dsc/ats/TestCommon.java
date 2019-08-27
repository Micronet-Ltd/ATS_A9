/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

/////////////////////////////////////////////////////////////
// TestCommon
//  methods which are common to all tests
/////////////////////////////////////////////////////////////

package com.micronet.dsc.ats;


import java.util.List;

public class TestCommon {

    Queue queue;

    TestCommon(Queue queue) {
        this.queue = queue;

    }

    public boolean isInQueue(int event_type_id) {
        List<QueueItem> queueList = queue.getAllItems();

        if (queueList.isEmpty()) return false;
        for (int i = 0; i < queueList.size(); i++) {
            if (queueList.get(i).event_type_id == event_type_id) return true; // it's here!
        }
        return false;

    } // isInQueue()


} // TestCommon
