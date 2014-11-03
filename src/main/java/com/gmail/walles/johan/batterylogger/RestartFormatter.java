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

public class RestartFormatter extends HidableLineAndPointFormatter {
    private static class RestartEventRenderer extends EventRenderer {
        public RestartEventRenderer(XYPlot plot, Paint paint) {
            super(plot, paint);
        }

        protected void drawEvent(Canvas canvas, HistoryEvent.Type type, String text, float x, float y) {
            if (type != HistoryEvent.Type.SYSTEM_BOOT && type != HistoryEvent.Type.SYSTEM_SHUTDOWN) {
                return;
            }

            canvas.drawLine(x, 0, x, canvas.getHeight(), paint);
        }
    }

    private final Paint paint;

    public RestartFormatter(Paint paint) {
        this.paint = paint;
    }

    @Override
    public Class<? extends EventRenderer> getRendererClass() {
        return RestartEventRenderer.class;
    }

    @Override
    public EventRenderer createRendererInstance(XYPlot plot) {
        return new RestartEventRenderer(plot, paint);
    }
}
