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
import android.util.TypedValue;
import android.view.View;

import com.gmail.walles.johan.batterylogger.HistoryEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class XYPlot extends View {
    private final Paint BACKGROUND;
    private final Paint DRAINLINE;
    private final Paint DRAINDOTS;
    private final Paint RESTART;

    private double minX;
    private double maxX;
    private double minY;
    private double maxY;

    private int screenLeftX;
    private int screenRightX;
    private int screenBottomY;
    private int screenTopY;

    private boolean showDrainDots = true;
    private boolean showEvents;
    private String yLabel;
    private List<DrainSample> drainDots;
    private List<DrainSample> drainLines;
    private List<PlotEvent> events;

    public XYPlot(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public XYPlot(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        BACKGROUND = new Paint();
        BACKGROUND.setColor(Color.BLACK);

        DRAINLINE = new Paint();
        DRAINLINE.setColor(Color.GREEN);
        DRAINLINE.setStrokeWidth(mmToPixels(0.5f, context));

        DRAINDOTS = new Paint();
        DRAINDOTS.setColor(Color.DKGRAY);
        DRAINDOTS.setStrokeWidth(mmToPixels(0.25f, context));

        RESTART = new Paint();
        RESTART.setColor(Color.RED);
        RESTART.setStrokeWidth(mmToPixels(0.25f, context));
    }

    private static float mmToPixels(float mm, Context context) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, mm,
            context.getResources().getDisplayMetrics());
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
            drawSamples(canvas, drainDots, DRAINDOTS);
        }

        if (showEvents) {
            drawRestarts(canvas);
        }

        drawSamples(canvas, drainLines, DRAINLINE);

        if (showEvents) {
            drawPackagingEvents(canvas);
        }
    }

    /**
     * Set up {@link #screenLeftX}, {@link #screenRightX}, {@link #screenBottomY} and {@link #screenTopY}
     */
    private void doLayout(Canvas canvas) {
        screenLeftX = 0;
        screenRightX = canvas.getWidth() - 1;

        screenTopY = 0;
        screenBottomY = canvas.getHeight() - 1;
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

    /**
     * Clip the drawing region to the plot area.
     */
    private void prepareForPlotting(Canvas canvas) {
        canvas.clipRect(screenLeftX, screenTopY, screenRightX, screenBottomY);
    }

    private void drawRestarts(Canvas canvas) {
        prepareForPlotting(canvas);

        for (PlotEvent event : events) {
            if (event.type != HistoryEvent.Type.SYSTEM_BOOT) {
                // FIXME: Draw shutdowns as well? Or shutdown->startup together somehow?
                continue;
            }

            if (event.msSinceEpoch < minX) {
                continue;
            }
            if (event.msSinceEpoch > maxX) {
                continue;
            }

            canvas.drawLine(
                toScreenX(event.msSinceEpoch), screenBottomY,
                toScreenX(event.msSinceEpoch), screenTopY,
                RESTART);
        }
    }

    private void drawSamples(Canvas canvas, Iterable<DrainSample> samples, Paint paint) {
        prepareForPlotting(canvas);

        for (DrainSample sample : samples) {
            if (sample.startMsSinceEpoch > maxX) {
                continue;
            }
            if (sample.endMsSinceEpoch < minX) {
                continue;
            }
            canvas.drawLine(
                toScreenX(sample.startMsSinceEpoch), toScreenY(sample.drainSpeed),
                toScreenX(sample.endMsSinceEpoch),   toScreenY(sample.drainSpeed),
                paint);
        }
    }

    private void drawPackagingEvents(Canvas canvas) {
        prepareForPlotting(canvas);

        // FIXME: Code missing here
    }

    private int toScreenX(double x) {
        double xWidth = maxX - minX;
        int screenWidth = screenRightX - screenLeftX;
        return (int)(((x - minX) / xWidth) * screenWidth) + screenLeftX;
    }

    private int toScreenY(double y) {
        double yHeight = maxY - minY;
        int screenHeight = screenBottomY - screenTopY;
        return (int)(((y - minY) / yHeight) * -screenHeight) + screenBottomY;
    }

    /**
     * Convert an X pixel value into a plot X value.
     */
    public double getXValue(float pixelX) {
        int screenWidthPixels = screenRightX - screenLeftX;
        double valueWidth = maxX - minX;
        return ((pixelX - screenLeftX) / (double)screenWidthPixels) * valueWidth + minX;
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

    public boolean isEmpty() {
        if (!drainDots.isEmpty()) {
            return false;
        }
        if (!drainLines.isEmpty()) {
            return false;
        }
        if (!events.isEmpty()) {
            return false;
        }
        return true;
    }

    public double getLeftmostX() {
        double leftmostX = Double.MAX_VALUE;
        if (!drainDots.isEmpty()) {
            leftmostX = Math.min(leftmostX, drainDots.get(0).startMsSinceEpoch);
        }
        if (!drainLines.isEmpty()) {
            leftmostX = Math.min(leftmostX, drainLines.get(0).startMsSinceEpoch);
        }
        if (!events.isEmpty()) {
            leftmostX = Math.min(leftmostX, events.get(0).msSinceEpoch);
        }

        if (leftmostX == Double.MAX_VALUE) {
            throw new IllegalStateException("No data, leftmost X undefined");
        }

        return leftmostX;
    }
}
