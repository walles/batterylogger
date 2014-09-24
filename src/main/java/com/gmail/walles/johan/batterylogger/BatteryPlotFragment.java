package com.gmail.walles.johan.batterylogger;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.PointLabeler;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.gmail.walles.johan.batterylogger.MainActivity.TAG;

public class BatteryPlotFragment extends Fragment {
    public static final int IN_GRAPH_TEXT_SIZE_SP = 12;

    private static final long ONE_DAY_MS = 86400 * 1000;

    private double minX;
    private double maxX;

    private double originalMinX;
    private double originalMaxX;
    private EventFormatter eventFormatter;

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

    private double pixelsToDomainUnits(final XYPlot plot, double pixels) {
        double domainSpan = maxX - minX;
        double pixelSpan = plot.getWidth();
        return pixels * domainSpan / pixelSpan;
    }

    private void scrollSideways(final XYPlot plot, double nPixels) {
        double offset = pixelsToDomainUnits(plot, nPixels);

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

    /**
     * We only want to show text events if we're zoomed in enough; otherwise the display becomes
     * too cluttered when showing a month of data.
     *
     * @return True if we should show text events, false otherwise.
     */
    private boolean isShowingEvents() {
        long visibleMs = History.toDate(maxX).getTime() - History.toDate(minX).getTime();
        return visibleMs <= 3 * ONE_DAY_MS;
    }

    private void redrawPlot(final XYPlot plot) {
        // FIXME: Call plot.setDomainStep() with some good value

        eventFormatter.setVisible(isShowingEvents());
        plot.redraw();
    }

    private static final DialogInterface.OnClickListener DIALOG_DISMISSER = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
        }
    };

    private void showAlertDialog(String title, String message,
                                 DialogInterface.OnClickListener dismisser)
    {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(title);
        dialogBuilder.setMessage(message);
        dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);
        dialogBuilder.setPositiveButton(android.R.string.ok, dismisser);
        dialogBuilder.show();
    }

    private void showAlertDialogOnce(String title, String message) {
        final String shownTag = title + ": " + message + " shown";
        final SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        if (preferences.getBoolean(shownTag, false)) {
            return;
        }

        showAlertDialog(title, message, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                preferences.edit().putBoolean(shownTag, true).commit();
                dialogInterface.dismiss();
            }
        });
    }

    private void showAlertDialog(String title, String message) {
        showAlertDialog(title, message, DIALOG_DISMISSER);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        long t0 = System.currentTimeMillis();

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        if (rootView == null) {
            throw new RuntimeException("Got a null root view");
        }

        // initialize our XYPlot view reference:
        XYPlot plot = (XYPlot)rootView.findViewById(R.id.mySimpleXYPlot);

        addPlotData(plot);
        plot.setOnTouchListener(getOnTouchListener(plot));
        setUpPlotLayout(plot);
        redrawPlot(plot);

        long t1 = System.currentTimeMillis();
        long dMillis = t1 - t0;
        Log.i(TAG, "Setting up view took " + dMillis + "ms");

        return rootView;
    }

    private float spToPixels(int sp) {
        Resources r = getResources();
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                sp,
                r.getDisplayMetrics());
    }

    private void setUpPlotLayout(final XYPlot plot) {
        final float labelHeightPixels = spToPixels(15);

        // Note that we have to set text size before label text, otherwise the label gets clipped,
        // with AndroidPlot 0.6.2-SNAPSHOT on 2014sep12 /Johan
        plot.getRangeLabelWidget().getLabelPaint().setTextSize(labelHeightPixels);
        plot.setRangeLabel("Battery Drain (%/h)");

        plot.getTitleWidget().setVisible(false);
        plot.getDomainLabelWidget().setVisible(false);
        plot.getLegendWidget().setVisible(false);

        plot.getGraphWidget().getRangeLabelPaint().setTextSize(labelHeightPixels);
        plot.getGraphWidget().getDomainLabelPaint().setTextSize(labelHeightPixels);

        // Tell the widget about how much space we should reserve for the range label widgets
        final float maxRangeLabelWidth = plot.getGraphWidget().getRangeLabelPaint().measureText("25.0");
        plot.getGraphWidget().setRangeLabelWidth(maxRangeLabelWidth);

        // Need room for top scale label
        plot.getGraphWidget().setMarginTop(labelHeightPixels);

        // Need room for domain labels
        plot.getGraphWidget().setMarginBottom(labelHeightPixels);

        // Need room for the range label
        plot.getGraphWidget().setMarginLeft(labelHeightPixels);

        // Prevent the leftmost part of the range labels from being clipped
        // FIXME: I don't know where the clipping comes from, fixing it properly would be better
        plot.getGraphWidget().setClippingEnabled(false);

        // Symmetry with upper and bottom
        //noinspection SuspiciousNameCombination
        plot.getGraphWidget().setMarginRight(labelHeightPixels);

        plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 1);
        plot.setTicksPerRangeLabel(5);

        plot.setTicksPerDomainLabel(1);
        plot.setDomainStep(XYStepMode.SUBDIVIDE, 4);
        plot.setDomainValueFormat(new Format() {
            @Override
            public StringBuffer format(Object o, @NotNull StringBuffer toAppendTo, @NotNull FieldPosition position) {
                Date timestamp = History.toDate((Number) o);
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
            public Object parseObject(String s, @NotNull ParsePosition parsePosition) {
                return null;
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
    }

    public static boolean isRunningOnEmulator() {
        // Inspired by
        // http://stackoverflow.com/questions/2799097/how-can-i-detect-when-an-android-application-is-running-in-the-emulator
        if ("sdk".equals(Build.PRODUCT)) {
            return true;
        }

        if ("google_sdk".equals(Build.PRODUCT)) {
            return true;
        }

        if ("google_sdk_x86".equals(Build.PRODUCT)) {
            return true;
        }

        return false;
    }

    private void addPlotData(final XYPlot plot) {
        LineAndPointFormatter drainFormatter = getDrainFormatter();
        LineAndPointFormatter medianFormatter = getMedianFormatter();

        try {
            // Add battery drain series to the plot
            History history = new History(getActivity());
            if (history.isEmpty() && BuildConfig.DEBUG && isRunningOnEmulator()) {
                history = History.createFakeHistory();
            }

            for (XYSeries drain : history.getBatteryDrain()) {
                plot.addSeries(drain, drainFormatter);
            }

            final List<XYSeries> medians = history.getDrainLines();
            for (XYSeries median : medians) {
                plot.addSeries(median, medianFormatter);
            }

            // Add events to the plot
            Paint labelPaint = new Paint();
            labelPaint.setAntiAlias(true);
            labelPaint.setColor(Color.WHITE);
            labelPaint.setTextSize(spToPixels(IN_GRAPH_TEXT_SIZE_SP));

            eventFormatter = new EventFormatter(labelPaint);
            plot.addSeries(history.getEvents(), eventFormatter);

            if (history.isEmpty()) {
                showAlertDialogOnce(
                        "No Battery History Recorded",
                        "Come back in a few hours to get a graph, or in a week to be able to see patterns.");
            } else if (medians.size() < 5) {
                showAlertDialogOnce(
                        "Very Short Battery History Recorded",
                        "If you come back in a week you'll be able to see patterns much better.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Reading battery history failed", e);
            showAlertDialog("Reading Battery History Failed", e.getMessage());
        }
    }

    private LineAndPointFormatter getMedianFormatter() {
        LineAndPointFormatter medianFormatter = new LineAndPointFormatter();
        medianFormatter.setPointLabelFormatter(new PointLabelFormatter());
        medianFormatter.setPointLabeler(new PointLabeler() {
            @Override
            public String getLabel(XYSeries xySeries, int i) {
                return "";
            }
        });
        medianFormatter.getLinePaint().setStrokeWidth(7);
        medianFormatter.getLinePaint().setColor(Color.GREEN);
        medianFormatter.getVertexPaint().setColor(Color.TRANSPARENT);
        medianFormatter.getFillPaint().setColor(Color.TRANSPARENT);
        return medianFormatter;
    }

    private LineAndPointFormatter getDrainFormatter() {
        LineAndPointFormatter drainFormatter = new LineAndPointFormatter();
        drainFormatter.setPointLabelFormatter(new PointLabelFormatter());
        drainFormatter.setPointLabeler(new PointLabeler() {
            @Override
            public String getLabel(XYSeries xySeries, int i) {
                return "";
            }
        });
        drainFormatter.getLinePaint().setStrokeWidth(0);
        drainFormatter.getLinePaint().setColor(Color.TRANSPARENT);
        drainFormatter.getVertexPaint().setColor(Color.rgb(0x00, 0x44, 0x00));
        drainFormatter.getVertexPaint().setStrokeWidth(4);
        drainFormatter.getFillPaint().setColor(Color.TRANSPARENT);
        drainFormatter.getPointLabelFormatter().getTextPaint().setColor(Color.WHITE);
        return drainFormatter;
    }

    private View.OnTouchListener getOnTouchListener(final XYPlot plot) {
        final GestureDetector gestureDetector = getOneFingerGestureDetector(plot);
        final ScaleGestureDetector scaleGestureDetector = getTwoFingerGestureDetector(plot);
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                boolean returnMe;
                returnMe = scaleGestureDetector.onTouchEvent(motionEvent);
                returnMe |= gestureDetector.onTouchEvent(motionEvent);
                returnMe |= view.onTouchEvent(motionEvent);
                return returnMe;
            }
        };
    }

    private GestureDetector getOneFingerGestureDetector(final XYPlot plot) {
        GestureDetector.SimpleOnGestureListener gestureListener =
                new GestureDetector.SimpleOnGestureListener()
                {
                    @Override
                    public boolean onDown(MotionEvent motionEvent) {
                        // Return true since the framework is weird:
                        // http://stackoverflow.com/questions/4107565/on-android-do-gesture-events-work-on-the-emulator
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        // Reset zoom to max out
                        minX = originalMinX;
                        maxX = originalMaxX;
                        plot.setDomainBoundaries(minX, maxX, BoundaryMode.FIXED);
                        redrawPlot(plot);

                        return true;
                    }

                    @Override
                    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float dx, float dy) {
                        scrollSideways(plot, dx);

                        plot.setDomainBoundaries(minX, maxX, BoundaryMode.FIXED);
                        redrawPlot(plot);
                        return true;
                    }
                };

        final GestureDetector gestureDetector = new GestureDetector(getActivity(), gestureListener);
        gestureDetector.setIsLongpressEnabled(false);
        gestureDetector.setOnDoubleTapListener(gestureListener);

        return gestureDetector;
    }

    private ScaleGestureDetector getTwoFingerGestureDetector(final XYPlot plot) {
        ScaleGestureDetector.SimpleOnScaleGestureListener gestureListener =
                new ScaleGestureDetector.SimpleOnScaleGestureListener()
                {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float factor = detector.getPreviousSpan() / detector.getCurrentSpan();
                        float pixelX = detector.getFocusX();
                        RectF gridRect = plot.getGraphWidget().getGridRect();
                        // getXVal throws IAE if the X value is outside of the rectangle
                        if (gridRect.contains(pixelX, gridRect.top)) {
                            double pivot = plot.getGraphWidget().getXVal(pixelX);
                            zoom(factor, pivot);
                        }

                        plot.setDomainBoundaries(minX, maxX, BoundaryMode.FIXED);
                        redrawPlot(plot);
                        return true;
                    }
                };

        return new ScaleGestureDetector(getActivity(), gestureListener);
    }
}
