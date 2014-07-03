package com.gmail.walles.johan.batterylogger;

import android.graphics.Paint;
import com.androidplot.ui.SeriesRenderer;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;

public class EventFormatter extends LineAndPointFormatter {
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
        return new EventRenderer(plot, textPaint);
    }
}
