package com.gmail.walles.johan.batterylogger;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.util.Arrays;

public class BatteryPlotFragment extends Fragment {
    private GestureDetector gestureDetector;
    private XYPlot plot;
    private PointF minXY;
    private PointF maxXY;
    private PointF originalMinXY;
    private PointF originalMaxXY;

    private void zoom(float scale) {
        float domainSpan = maxXY.x - minXY.x;
        float domainMidPoint = maxXY.x - domainSpan / 2.0f;
        float offset = domainSpan * scale / 2.0f;

        minXY.x = domainMidPoint - offset;
        if (minXY.x < originalMinXY.x) {
            minXY.x = originalMinXY.x;
        }

        maxXY.x = domainMidPoint + offset;
        if (maxXY.x > originalMaxXY.x) {
            maxXY.x = originalMaxXY.x;
        }
    }

    private float pixelsToDomainUnits(float pixels) {
        float domainSpan = maxXY.x - minXY.x;
        float pixelSpan = plot.getWidth();
        return pixels * domainSpan / pixelSpan;
    }

    private void scrollSideways(float nPixels) {
        float offset = pixelsToDomainUnits(nPixels);

        minXY.x += offset;
        if (minXY.x < originalMinXY.x) {
            float adjustment = originalMinXY.x - minXY.x;
            minXY.x += adjustment;
            maxXY.x += adjustment;
        }

        maxXY.x += offset;
        if (maxXY.x > originalMaxXY.x) {
            float adjustment = maxXY.x - originalMaxXY.x;
            minXY.x -= adjustment;
            maxXY.x -= adjustment;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        if (rootView == null) {
            throw new RuntimeException("Got a null root view");
        }

        // initialize our XYPlot reference:
        plot = (XYPlot)rootView.findViewById(R.id.mySimpleXYPlot);

        // Create a couple arrays of y-values to plot:
        Number[] series1Numbers = {
                0, 1,
                1, 8,
                2, 5,
                3, 2,
                4, 7,
                5, 4
        };
        Number[] series2Numbers = {
                10, 4,
                11, 6,
                12, 3,
                13, 8,
                14, 2,
                15, 10
        };

        // Turn the above arrays into XYSeries':
        XYSeries series1 = new SimpleXYSeries(
                Arrays.asList(series1Numbers),          // SimpleXYSeries takes a List so turn our array into a List
                SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED,
                "Series1");                             // Set the display title of the series

        // same as above
        XYSeries series2 = new SimpleXYSeries(
                Arrays.asList(series2Numbers),
                SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED,
                "Series2");

        EventSeries eventSeries = new EventSeries();
        eventSeries.add(7, "Sju sju sju");
        eventSeries.add(8, "Åtta åtta åtta");

        // Create a formatter to use for drawing a series using LineAndPointRenderer
        // and configure it from xml:
        LineAndPointFormatter lineAndPointFormatter = new LineAndPointFormatter();
        lineAndPointFormatter.setPointLabelFormatter(new PointLabelFormatter());
        lineAndPointFormatter.configure(getActivity(), R.xml.line_point_formatter_with_plf1);

        // add a new series' to the xyplot:
        plot.addSeries(series1, lineAndPointFormatter);

        // same as above:
        plot.addSeries(series2, lineAndPointFormatter);

        Paint labelPaint = new Paint();
        labelPaint.setAntiAlias(true);
        labelPaint.setColor(Color.WHITE);
        plot.addSeries(eventSeries, new EventFormatter(labelPaint));

        // reduce the number of range labels
        plot.setTicksPerRangeLabel(3);

        gestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent motionEvent) {
                // Return true since the framework is weird:
                // http://stackoverflow.com/questions/4107565/on-android-do-gesture-events-work-on-the-emulator
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float dx, float dy) {
                float factor;
                if (dy < 0) {
                    factor = 1.0f / (1.0f - dy / plot.getHeight());
                } else {
                    factor = dy / plot.getHeight() + 1.0f;
                }
                zoom(factor);
                scrollSideways(dx);

                plot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.FIXED);
                plot.redraw();
                return true;
            }
        });
        gestureDetector.setIsLongpressEnabled(false);

        plot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent);
            }
        });

        plot.calculateMinMaxVals();
        minXY = new PointF(plot.getCalculatedMinX().floatValue(), plot.getCalculatedMinY().floatValue());
        maxXY = new PointF(plot.getCalculatedMaxX().floatValue(), plot.getCalculatedMaxY().floatValue());
        originalMinXY = new PointF(minXY.x, minXY.y);
        originalMaxXY = new PointF(maxXY.x, maxXY.y);
        plot.setRangeBoundaries(minXY.y, maxXY.y, BoundaryMode.FIXED);

        return rootView;
    }
}
