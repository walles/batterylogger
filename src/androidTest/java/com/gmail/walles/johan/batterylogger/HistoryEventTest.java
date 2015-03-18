/*
 * Copyright 2015 Johan Walles <johan.walles@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gmail.walles.johan.batterylogger;

import junit.framework.TestCase;

import java.util.Date;

public class HistoryEventTest extends TestCase {
    private void assertRecycling(HistoryEvent event) throws Exception {
        HistoryEvent recycled = HistoryEvent.deserializeFromString(event.serializeToString());
        assertEquals(event, recycled);
    }

    public void testCreateBatteryLevelEvent() throws Exception {
        assertRecycling(HistoryEvent.createBatteryLevelEvent(new Date(12345678), 42));
    }

    public void testCreateInfoEvent() throws Exception {
        assertRecycling(HistoryEvent.createInfoEvent(new Date(12345678), "some message"));
    }

    public void testCreateSystemHaltingEvent() throws Exception {
        assertRecycling(HistoryEvent.createSystemHaltingEvent(new Date(12345678)));
    }

    public void testCreateSystemBootingEvent() throws Exception {
        assertRecycling(HistoryEvent.createSystemBootingEvent(new Date(12345678), true));
        assertRecycling(HistoryEvent.createSystemBootingEvent(new Date(12345678), false));
    }

    public void testCreateStartChargingEvent() throws Exception {
        assertRecycling(HistoryEvent.createStartChargingEvent(new Date(12345678)));
    }

    public void testCreateStopChargingEvent() throws Exception {
        assertRecycling(HistoryEvent.createStopChargingEvent(new Date(12345678)));
    }
}
