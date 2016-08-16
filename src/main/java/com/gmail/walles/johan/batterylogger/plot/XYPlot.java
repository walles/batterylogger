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
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class XYPlot extends View {
    private static final Paint BACKGROUND; static {
        BACKGROUND = new Paint();
        BACKGROUND.setColor(Color.BLACK);
    }

    private double minX;
    private double maxX;
    private double minY;
    private double maxY;

    private int screenMinX;
    private int screenMaxX;

    /**
     * Lower values are further down on the screen.
     */
    private int screenMinY;

    /**
     * Higher values are further up on the screen.
     */
    private int screenMaxY;

    private boolean showDrainDots = true;
    private boolean showEvents;
    private String yLabel;
    private List<DrainSample> drainDots;
    private List<DrainSample> drainLines;
    private List<PlotEvent> events;

    public XYPlot(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

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
        doLayout(canvas);

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

    /**
     * Set up {@link #screenMinX}, {@link #screenMaxX}, {@link #screenMinY} and {@link #screenMaxY}
     */
    private void doLayout(Canvas canvas) {
        screenMinX = 0;
        screenMaxX = canvas.getWidth() - 1;

        screenMinY = 0;
        screenMaxY = canvas.getHeight() - 1;
    }

    private void clear(Canvas canvas) {
        canvas.drawPaint(BACKGROUND);
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
        int screenWidthPixels = screenMaxX - screenMaxY;
        double valueWidth = maxX - minX;
        return (pixelX - screenMinX) * valueWidth / screenWidthPixels;
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

    public void setEvents(List<PlotEvent> events) {
        this.events = events;
    }

    private CharSequence getXValueLabel(double x) {
        Date timestamp = new Date((long)x);
        long domainWidthSeconds = (long)((maxX - minX) / 1000);
        SimpleDateFormat format;
        if (domainWidthSeconds < 5 * 60) {
            format = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        } else if (domainWidthSeconds < 86400) {
            format = new SimpleDateFormat("HH:mm", Locale.getDefault());
        } else if (domainWidthSeconds < 86400 * 7) {
            format = new SimpleDateFormat("EEE HH:mm", Locale.getDefault());
        } else {
            format = new SimpleDateFormat("MMM d", Locale.getDefault());
        }
        return format.format(timestamp);
    }
}
