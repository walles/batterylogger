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

import android.graphics.Paint;
import com.androidplot.ui.SeriesRenderer;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;

import java.util.HashSet;
import java.util.Set;

public class EventFormatter extends LineAndPointFormatter {
    private final Set<EventRenderer> eventRenderers = new HashSet<EventRenderer>();
    private final Paint textPaint;

    public EventFormatter(Paint textPaint) {
        super();
        this.textPaint = textPaint;
    }

    @Override
    public Class<? extends SeriesRenderer> getRendererClass() {
        return EventRenderer.class;
    }

    @Override
    public SeriesRenderer getRendererInstance(XYPlot plot) {
        EventRenderer eventRenderer = new EventRenderer(plot, textPaint);
        eventRenderers.add(eventRenderer);
        return eventRenderer;
    }

    public void setVisible(boolean visible) {
        for (EventRenderer eventRenderer : eventRenderers) {
            eventRenderer.setVisible(visible);
        }
    }
}
