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

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;

import java.util.HashSet;
import java.util.Set;

public abstract class HidableLineAndPointFormatter extends LineAndPointFormatter {
    private final Set<EventRenderer> eventRenderers = new HashSet<>();

    @Override
    public Class<? extends EventRenderer> getRendererClass() {
        return EventRenderer.class;
    }

    /**
     * Override {@link #createRendererInstance(com.androidplot.xy.XYPlot)} instead.
     */
    @Override
    public final EventRenderer getRendererInstance(XYPlot plot) {
        EventRenderer eventRenderer = createRendererInstance(plot);
        eventRenderers.add(eventRenderer);
        return eventRenderer;
    }

    protected abstract EventRenderer createRendererInstance(XYPlot plot);

    public void setVisible(boolean visible) {
        for (EventRenderer eventRenderer : eventRenderers) {
            eventRenderer.setVisible(visible);
        }
    }
}
