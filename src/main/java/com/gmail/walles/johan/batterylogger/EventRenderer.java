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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import com.androidplot.util.ValPixConverter;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.LineAndPointRenderer;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

public class EventRenderer extends LineAndPointRenderer<EventFormatter> {
    private final Paint textPaint;
    private boolean visible = true;

    public EventRenderer(XYPlot plot, Paint textPaint) {
        super(plot);
        this.textPaint = textPaint;
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

            String text = eventSeries.getDescription(i);
            if (text == null || text.length() == 0) {
                continue;
            }

            drawVerticalText(canvas, textPaint, text, point.x, point.y);
        }
    }

    /**
     * From http://stackoverflow.com/questions/24091390/androidplot-labels-and-text/24092382#24092382
     * @param paint paint used to draw the text
     * @param text the text to be drawn
     * @param x x-coordinate of where the text should be drawn
     * @param y y-coordinate of where the text should be drawn
     */
    private static void drawVerticalText(Canvas canvas, Paint paint, String text, float x, float y) {
        // record the state of the canvas before the draw:
        canvas.save(Canvas.ALL_SAVE_FLAG);

        // center the canvas on our drawing coordinates:
        canvas.translate(x, y);

        // rotate into the desired "vertical" orientation:
        canvas.rotate(-90);

        // draw the text; note that we are drawing at 0, 0 and *not* x, y.
        canvas.drawText(text, 0, 0, paint);

        // restore the canvas state:
        canvas.restore();
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
