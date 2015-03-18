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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import com.androidplot.util.ValPixConverter;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.LineAndPointRenderer;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

public abstract class EventRenderer extends LineAndPointRenderer<EventFormatter> {
    protected final Paint paint;
    private boolean visible = true;

    public EventRenderer(XYPlot plot, Paint paint) {
        super(plot);
        this.paint = paint;
    }

    @Override
    protected void drawSeries(Canvas canvas, RectF plotArea, XYSeries xySeries, LineAndPointFormatter formatter) {
        if (!visible) {
            return;
        }

        EventSeries eventSeries = (EventSeries)xySeries;
        for (int i = 0; i < eventSeries.size(); i++) {
            Number x = eventSeries.getX(i);
            Number y = eventSeries.getY(i);
            if (x == null || y == null) {
                continue;
            }

            PointF point = ValPixConverter.valToPix(
                    x,
                    y,
                    plotArea,
                    getPlot().getCalculatedMinX(),
                    getPlot().getCalculatedMaxX(),
                    getPlot().getCalculatedMinY(),
                    getPlot().getCalculatedMaxY());

            drawEvent(canvas, eventSeries.getType(i), eventSeries.getDescription(i), point.x, point.y);
        }
    }

    protected abstract void drawEvent(Canvas canvas, HistoryEvent.Type type, String text, float x, float y);

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
