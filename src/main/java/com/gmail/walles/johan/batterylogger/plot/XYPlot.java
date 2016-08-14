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

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class XYPlot extends View {
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private boolean showDrainDots = true;
    private boolean showEvents;
    private String yLabel;
    private List<DrainSample> drainDots;
    private List<DrainSample> drainLines;

    public XYPlot(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setXRange(double minX, double maxX) {
        this.minX = minX;
        this.maxX = maxX;
    }

    public void setYRange(double minY, double maxY) {
        this.minY = minY;
        this.maxY = maxY;
    }

    public void setShowDrainDots(boolean yesOrNo) {
        showDrainDots = yesOrNo;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        clear(canvas);
        drawAxes(canvas);
        drawYLabel(canvas);
        drawGridLines(canvas);

        if (showDrainDots) {
            drawDrainDots(canvas);
        }

        if (showEvents) {
            drawRestarts(canvas);
        }

        drawDrainLines(canvas);

        if (showEvents) {
            drawPackagingEvents(canvas);
        }
    }

    private void clear(Canvas canvas) {
        // FIXME: Code missing here
    }

    private void drawAxes(Canvas canvas) {
        // FIXME: Code missing here
    }

    private void drawYLabel(Canvas canvas) {
        // FIXME: Code missing here
    }

    private void drawGridLines(Canvas canvas) {
        // FIXME: Code missing here
    }

    private void drawDrainDots(Canvas canvas) {
        // FIXME: Code missing here
    }

    private void drawRestarts(Canvas canvas) {
        // FIXME: Code missing here
    }

    private void drawDrainLines(Canvas canvas) {
        // FIXME: Code missing here
    }

    private void drawPackagingEvents(Canvas canvas) {
        // FIXME: Code missing here
    }

    /**
     * Convert an X pixel value into a plot X value.
     */
    public double getXVal(float pixelX) {
        // FIXME: Code missing here
    }

    public void setYLabel(String yLabel) {
        this.yLabel = yLabel;
    }

    public void setShowEvents(boolean showEvents) {
        this.showEvents = showEvents;
    }

    public void setDrainDots(List<DrainSample> drainDots) {
        this.drainDots = drainDots;
    }

    public void setDrainLines(List<DrainSample> drainLines) {
        this.drainLines = drainLines;
    }
}
