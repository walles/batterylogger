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
import com.androidplot.xy.XYPlot;

public class EventFormatter extends HidableLineAndPointFormatter {
    private static class TextEventRenderer extends EventRenderer {
        public TextEventRenderer(XYPlot plot, Paint paint) {
            super(plot, paint);
        }

        /**
         * From http://stackoverflow.com/questions/24091390/androidplot-labels-and-text/24092382#24092382
         * @param text the text to be drawn
         * @param x x-coordinate of where the text should be drawn
         * @param y y-coordinate of where the text should be drawn
         */
        protected void drawEvent(Canvas canvas, HistoryEvent.Type type, String text, float x, float y) {
            if (text == null || text.length() == 0) {
                return;
            }

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
    }

    private final Paint textPaint;

    public EventFormatter(Paint textPaint) {
        this.textPaint = textPaint;
    }

    @Override
    public Class<? extends EventRenderer> getRendererClass() {
        return EventRenderer.class;
    }

    @Override
    protected EventRenderer createRendererInstance(XYPlot plot) {
        return new TextEventRenderer(plot, textPaint);
    }
}
