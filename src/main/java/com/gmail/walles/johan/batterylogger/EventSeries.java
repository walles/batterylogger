/*
 * Copyright 2014 Johan Walles <johan.walles@gmail.com>
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

import com.androidplot.xy.XYSeries;

import java.util.ArrayList;

public class EventSeries implements XYSeries {
    private final ArrayList<Double> xCoordinates = new ArrayList<Double>();
    private final ArrayList<String> descriptions = new ArrayList<String>();
    private final ArrayList<HistoryEvent.Type> types = new ArrayList<HistoryEvent.Type>();

    public void add(double x, String description, HistoryEvent.Type type) {
        xCoordinates.add(x);
        descriptions.add(description);
        types.add(type);
    }

    @Override
    public int size() {
        return xCoordinates.size();
    }

    @Override
    public Number getX(int i) {
        return xCoordinates.get(i);
    }

    @Override
    public Number getY(int i) {
        return 0;
    }

    public String getDescription(int i) {
        return descriptions.get(i);
    }

    public HistoryEvent.Type getType(int i) {
        return types.get(i);
    }

    @Override
    public String getTitle() {
        return "Not expected to be shown";
    }
}
