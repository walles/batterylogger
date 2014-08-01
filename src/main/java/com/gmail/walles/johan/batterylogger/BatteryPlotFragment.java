package com.gmail.walles.johan.batterylogger;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.PointLabeler;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.gmail.walles.johan.batterylogger.MainActivity.TAG;

public class BatteryPlotFragment extends Fragment {
    private GestureDetector gestureDetector;
    private XYPlot plot;

    private double minX;
    private double maxX;

    private double originalMinX;
    private double originalMaxX;

    private void zoom(double factor, double pivot) {
        double leftSpan = pivot - minX;
        minX = pivot - leftSpan * factor;
        if (minX < originalMinX) {
            minX = originalMinX;
        }

        double rightSpan = maxX - pivot;
        maxX = pivot + rightSpan * factor;
        if (maxX > originalMaxX) {
            maxX = originalMaxX;
        }
    }

    private double pixelsToDomainUnits(double pixels) {
        double domainSpan = maxX - minX;
        double pixelSpan = plot.getWidth();
        return pixels * domainSpan / pixelSpan;
    }

    private void scrollSideways(double nPixels) {
        double offset = pixelsToDomainUnits(nPixels);

        minX += offset;
        if (minX < originalMinX) {
            double adjustment = originalMinX - minX;
            minX += adjustment;
            maxX += adjustment;
        }

        maxX += offset;
        if (maxX > originalMaxX) {
            double adjustment = maxX - originalMaxX;
            minX -= adjustment;
            maxX -= adjustment;
        }
    }

    private void redrawPlot() {
        // FIXME: Call plot.setDomainStep() with some good value

        plot.redraw();
    }

    private static final DialogInterface.OnClickListener DIALOG_DISMISSER = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
        }
    };

    private static void showAlertDialog(Context context, CharSequence message) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setMessage(message);
        dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);
        dialogBuilder.setPositiveButton(android.R.string.ok, DIALOG_DISMISSER);
        dialogBuilder.show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        if (rootView == null) {
            throw new RuntimeException("Got a null root view");
        }

        // initialize our XYPlot view reference:
        plot = (XYPlot)rootView.findViewById(R.id.mySimpleXYPlot);

        LineAndPointFormatter drainFormatter = new LineAndPointFormatter();
        drainFormatter.setPointLabelFormatter(new PointLabelFormatter());
        drainFormatter.setPointLabeler(new PointLabeler() {
            @Override
            public String getLabel(XYSeries xySeries, int i) {
                return "";
            }
        });
        drainFormatter.configure(getActivity(), R.xml.drain_formatter);

        LineAndPointFormatter medianFormatter = new LineAndPointFormatter();
        medianFormatter.setPointLabelFormatter(new PointLabelFormatter());
        medianFormatter.setPointLabeler(new PointLabeler() {
            @Override
            public String getLabel(XYSeries xySeries, int i) {
                return "";
            }
        });
        medianFormatter.configure(getActivity(), R.xml.median_formatter);

        try {
            // Add battery drain series to the plot
            History history = new History(getActivity());
            for (XYSeries drain : history.getBatteryDrain()) {
                plot.addSeries(drain, drainFormatter);
            }

            for (XYSeries median : history.getDrainMedians()) {
                plot.addSeries(median, medianFormatter);
            }

            // Add events to the plot
            Paint labelPaint = new Paint();
            labelPaint.setAntiAlias(true);
            labelPaint.setColor(Color.WHITE);
            plot.addSeries(history.getEvents(), new EventFormatter(labelPaint));
        } catch (IOException e) {
            Log.e(TAG, "Reading battery history failed", e);
            showAlertDialog(getActivity(), "Failed to read battery history");
        }

        plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 1);
        plot.setTicksPerRangeLabel(5);

        plot.setTicksPerDomainLabel(1);
        plot.setDomainStep(XYStepMode.SUBDIVIDE, 4);
        plot.setDomainValueFormat(new Format() {
            @Override
            public StringBuffer format(Object o, StringBuffer toAppendTo, FieldPosition position) {
                Date timestamp = History.toDate((Number)o);
                long domainWidthSeconds = History.toDate(maxX - minX).getTime() / 1000;
                SimpleDateFormat format;
                if (domainWidthSeconds < 5 * 60) {
                    format = new SimpleDateFormat("HH:mm:ss");
                } else if (domainWidthSeconds < 86400) {
                    format = new SimpleDateFormat("HH:mm");
                } else if (domainWidthSeconds < 86400 * 7) {
                    format = new SimpleDateFormat("EEE HH:mm");
                } else {
                    format = new SimpleDateFormat("MMM d");
                }
                return format.format(timestamp, toAppendTo, position);
            }

            @Override
            @Nullable
            public Object parseObject(String s, ParsePosition parsePosition) {
                return null;
            }
        });

        GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent motionEvent) {
                // Return true since the framework is weird:
                // http://stackoverflow.com/questions/4107565/on-android-do-gesture-events-work-on-the-emulator
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                minX = originalMinX;
                maxX = originalMaxX;
                plot.setDomainBoundaries(minX, maxX, BoundaryMode.FIXED);
                redrawPlot();

                return true;
            }

            @Override
            public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float dx, float dy) {
                // Tuned from real-world testing, change the number if it's too high or too low
                final double ZOOM_SPEED = 7;

                double factor;
                if (dy < 0) {
                    factor = 1.0f / (1.0f - dy * ZOOM_SPEED / plot.getHeight());
                } else {
                    factor = dy * ZOOM_SPEED / plot.getHeight() + 1.0f;
                }
                RectF gridRect = plot.getGraphWidget().getGridRect();
                // getXVal throws IAE if the X value is outside of the rectangle
                if (gridRect.contains(motionEvent2.getX(), gridRect.top)) {
                    double pivot = plot.getGraphWidget().getXVal(motionEvent2.getX());
                    zoom(factor, pivot);
                }
                scrollSideways(dx);

                plot.setDomainBoundaries(minX, maxX, BoundaryMode.FIXED);
                redrawPlot();
                return true;
            }
        };

        gestureDetector = new GestureDetector(getActivity(), gestureListener);
        gestureDetector.setIsLongpressEnabled(false);
        gestureDetector.setOnDoubleTapListener(gestureListener);

        plot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent);
            }
        });

        plot.calculateMinMaxVals();
        minX = plot.getCalculatedMinX().doubleValue();
        maxX = plot.getCalculatedMaxX().doubleValue();
        Date now = new Date();
        if (maxX < History.toDouble(now)) {
            maxX = History.toDouble(now);
        }
        Date fiveMinutesAgo = new Date(now.getTime() - History.FIVE_MINUTES_MS);
        if (minX > History.toDouble(fiveMinutesAgo)) {
            minX = History.toDouble(fiveMinutesAgo);
        }

        originalMinX = minX;
        originalMaxX = maxX;

        plot.setDomainBoundaries(minX, maxX, BoundaryMode.FIXED);

        double maxY = plot.getCalculatedMaxY().doubleValue();
        if (maxY < 5) {
            maxY = 5;
        }
        if (maxY > 25) {
            // We sometimes get unreasonable outliers, clamp them so they don't make the graph unreadable
            maxY = 25;
        }

        plot.setRangeBoundaries(0, maxY, BoundaryMode.FIXED);
        redrawPlot();

        return rootView;
    }
}
