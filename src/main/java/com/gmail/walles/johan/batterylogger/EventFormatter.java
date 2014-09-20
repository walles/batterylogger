package com.gmail.walles.johan.batterylogger;

import android.graphics.Paint;
import com.androidplot.ui.SeriesRenderer;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;

import java.util.HashSet;
import java.util.Set;

public class EventFormatter extends LineAndPointFormatter {
    private final Set<EventRenderer> eventRenderers = new HashSet<EventRenderer>();
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
        EventRenderer eventRenderer = new EventRenderer(plot, textPaint);
        eventRenderers.add(eventRenderer);
        return eventRenderer;
    }

    public void setVisible(boolean visible) {
        for (EventRenderer eventRenderer : eventRenderers) {
            eventRenderer.setVisible(visible);
        }
    }
}
