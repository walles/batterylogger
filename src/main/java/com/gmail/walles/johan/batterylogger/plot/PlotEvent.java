/*
 * Copyright 2016 Johan Walles <johan.walles@gmail.com>
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

package com.gmail.walles.johan.batterylogger.plot;

import com.gmail.walles.johan.batterylogger.HistoryEvent;

import java.util.Date;

public class PlotEvent {
    public final double msSinceEpoch;
    public final HistoryEvent.Type type;
    public final String description;

    public PlotEvent(Date timestamp, String description, HistoryEvent.Type type) {
        this.msSinceEpoch = timestamp.getTime();
        this.description = description;
        this.type = type;
    }
}
