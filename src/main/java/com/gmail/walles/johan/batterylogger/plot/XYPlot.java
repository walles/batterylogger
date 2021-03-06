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
    private static final int EVENT_TEXT_SIZE_SP = 11;

    private final Paint BACKGROUND;
    private final Paint DRAINLINE;
    private final Paint DRAINDOTS;
    private final Paint RESTART;
    private final Paint AXES;
    private final Paint YLABEL;
    private final Paint YTICK;
    private final Paint XTICK;
    private final Paint EVENTS;

    private double minX;
    private double maxX;
    private double minY;
    private double maxY;

    // Here's the screen coordinates of the plot
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
        DRAINLINE.setStrokeWidth(mmToPixels(0.5, context));

        DRAINDOTS = new Paint();
        DRAINDOTS.setColor(Color.DKGRAY);
        DRAINDOTS.setStrokeWidth(mmToPixels(0.25, context));

        RESTART = new Paint();
        RESTART.setColor(Color.RED);
        RESTART.setStrokeWidth(mmToPixels(0.25, context));

        AXES = new Paint();
        AXES.setColor(Color.WHITE);
        AXES.setStrokeWidth(mmToPixels(0.25, context));

        YLABEL = new Paint();
        YLABEL.setColor(Color.WHITE);
        YLABEL.setTextAlign(Paint.Align.CENTER);
        YLABEL.setTextSize(spToPixels(14, context));

        YTICK = new Paint();
        YTICK.setColor(Color.WHITE);
        YTICK.setTextAlign(Paint.Align.RIGHT);
        YTICK.setTextSize(spToPixels(11, context));

        XTICK = new Paint();
        XTICK.setColor(Color.WHITE);
        XTICK.setTextAlign(Paint.Align.CENTER);
        XTICK.setTextSize(YTICK.getTextSize());

        EVENTS = new Paint();
        EVENTS.setColor(Color.WHITE);
        EVENTS.setTextSize(spToPixels(EVENT_TEXT_SIZE_SP, context));
        EVENTS.setTextAlign(Paint.Align.LEFT);
    }

    private static float mmToPixels(double mm, Context context) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, (float)mm,
            context.getResources().getDisplayMetrics());
    }

    private static float spToPixels(double sp, Context context) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, (float)sp,
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

        drawAxes(canvas);
        drawYLabel(canvas);
    }

    /**
     * Set up {@link #screenLeftX}, {@link #screenRightX}, {@link #screenBottomY} and {@link #screenTopY}
     */
    private void doLayout(Canvas canvas) {
        // Make room for the Y axis label and the tick labels
        screenLeftX = Math.round(2.5f * YLABEL.getTextSize());

        screenRightX = canvas.getWidth() - 1;

        screenTopY = 0;

        // Make room for the X axis labels
        screenBottomY = canvas.getHeight() - 1 - Math.round(2 * XTICK.getTextSize());
    }

    private void withPlotClip(Canvas canvas, Runnable runnable) {
        try {
            canvas.save();
            canvas.clipRect(screenLeftX, screenTopY, screenRightX, screenBottomY);
            runnable.run();
        } finally {
            canvas.restore();
        }
    }

    private void clear(Canvas canvas) {
        canvas.drawPaint(BACKGROUND);
    }

    private void drawAxes(Canvas canvas) {
        // Horizontal axis
        canvas.drawLine(screenLeftX, screenBottomY, screenRightX, screenBottomY, AXES);

        final int N_XTICKS = 2;

        float belowScreenHeight = canvas.getHeight() - screenBottomY;
        float screenY = screenBottomY + belowScreenHeight * 2 / 3;

        double width = maxX - minX;
        for (int i = 1; i <= N_XTICKS; i++) {
            double x = width / (N_XTICKS + 1) * i + minX;
            float screenX = toScreenX(x);
            String label = getXValueLabel(x).toString();
            drawText(canvas, screenX, screenY, label, 0, XTICK);
        }

        // Vertical axis
        canvas.drawLine(screenLeftX, screenBottomY, screenLeftX, screenTopY, AXES);
        for (int i = 5; i < maxY; i += 5) {
            float x = screenLeftX;
            float y = toScreenY(i);
            drawText(canvas, x, y, i + " ",0, YTICK);
        }
    }

    private void drawText(
        Canvas canvas, float x0, float y0, String text, float rotationDegrees, Paint paint)
    {
        try {
            canvas.save();

            canvas.translate(x0, y0);
            if (rotationDegrees != 0) {
                canvas.rotate(rotationDegrees);
            }
            canvas.drawText(text, 0, 0, paint);
        } finally {
            canvas.restore();
        }
    }

    private void drawYLabel(Canvas canvas) {
        int x = screenLeftX / 3;
        int y = (screenBottomY + screenTopY) / 2;
        drawText(canvas, x, y, yLabel, -90, YLABEL);
    }

    private void drawRestarts(final Canvas canvas) {
        withPlotClip(canvas, new Runnable() {
            @Override
            public void run() {
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
        });
    }

    private void drawSamples(final Canvas canvas, final Iterable<DrainSample> samples, final Paint paint) {
        withPlotClip(canvas, new Runnable() {
            @Override
            public void run() {
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
        });
    }

    private void drawPackagingEvents(final Canvas canvas) {
        withPlotClip(canvas, new Runnable() {
            @Override
            public void run() {
                double width = maxX - minX;
                double leftQuickClip = minX - width;
                double rightQuickClip = maxX + width;
                for (PlotEvent plotEvent : events) {
                    if (plotEvent.msSinceEpoch > rightQuickClip) {
                        continue;
                    }
                    if (plotEvent.msSinceEpoch < leftQuickClip) {
                        continue;
                    }
                    drawText(
                        canvas,
                        toScreenX(plotEvent.msSinceEpoch), screenBottomY,
                        plotEvent.description,
                        -90, EVENTS);
                }
            }
        });
    }

    private float toScreenX(double x) {
        double xWidth = maxX - minX;
        int screenWidth = screenRightX - screenLeftX;
        return (float)(((x - minX) / xWidth) * screenWidth) + screenLeftX;
    }

    private float toScreenY(double y) {
        double yHeight = maxY - minY;
        int screenHeight = screenBottomY - screenTopY;
        return (float)(((y - minY) / yHeight) * -screenHeight) + screenBottomY;
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
