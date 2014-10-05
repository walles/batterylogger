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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.PointLabeler;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.animation.ValueAnimator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import static com.gmail.walles.johan.batterylogger.MainActivity.TAG;

public class BatteryPlotFragment extends Fragment {
    public static final int IN_GRAPH_TEXT_SIZE_SP = 12;

    private static final long ONE_DAY_MS = 86400 * 1000;
    public static final int LEGEND_WIDTH_LANDSCAPE_SP = 300;
    public static final int ANIMATION_DURATION_MS = 1000;

    private ValueAnimator animator;
    private double minX;
    private double maxX;

    private double originalMinX;
    private double originalMaxX;
    private EventFormatter eventFormatter;

    @Nullable
    private AlertDialog visibleDialog;

    // Cache shown dialogs so we don't flood SharedPreferences with calls while zooming
    private final Set<String> shownDialogs = new HashSet<String>();

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
        // First time we hide events here we display an alert about that you can get them back by
        // two-finger zooming in
        if (!isShowingEvents()) {
            showAlertDialogOnce("Events Hidden", "Zoom in with two fingers to see hidden events");
        }

        eventFormatter.setVisible(isShowingEvents());
        plot.redraw();
    }

    private static final DialogInterface.OnClickListener DIALOG_DISMISSER = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
        }
    };

    @Nullable
    private AlertDialog showAlertDialog(String title, String message,
                                 DialogInterface.OnClickListener dismisser)
    {
        final Activity activity = getActivity();
        if (activity == null) {
            return null;
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle(title);
        dialogBuilder.setMessage(message);
        dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);
        dialogBuilder.setPositiveButton(android.R.string.ok, dismisser);

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
        return dialog;
    }

    private void showAlertDialogOnce(String title, String message) {
        // Don't show more than one dialog at a time
        if (visibleDialog != null && visibleDialog.isShowing()) {
            return;
        }

        final String shownTag = title + ": " + message + " shown";
        if (shownDialogs.contains(shownTag)) {
            return;
        }

        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        final SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
        if (preferences.getBoolean(shownTag, false)) {
            shownDialogs.add(shownTag);
            return;
        }

        visibleDialog = showAlertDialog(title, message, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                preferences.edit().putBoolean(shownTag, true).commit();
                shownDialogs.add(shownTag);
                dialogInterface.dismiss();
            }
        });
    }

    private void showAlertDialog(String title, String message) {
        showAlertDialog(title, message, DIALOG_DISMISSER);
    }

    // From: http://stackoverflow.com/a/10187511/473672
    private static CharSequence trimTrailingWhitespace(CharSequence source) {
        int i = source.length();

        // loop back to the first non-whitespace character
        while(--i >= 0 && Character.isWhitespace(source.charAt(i))) {
            // This block intentionally left blank
        }

        return source.subSequence(0, i+1);
    }

    private void initializeLegend(final TextView legend) {
        // From: http://stackoverflow.com/questions/6068197/utils-read-resource-text-file-to-string-java#answer-18897411
        String html = new Scanner(this.getClass().getResourceAsStream("/legend.html"), "UTF-8").useDelimiter("\\A").next();
        legend.setText(trimTrailingWhitespace(Html.fromHtml(html)));

        // Check MainActivity.PREF_SHOW_LEGEND and set legend visibility from that
        SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        boolean showLegend = preferences.getBoolean(MainActivity.PREF_SHOW_LEGEND, true);
        legend.setVisibility(showLegend ? View.VISIBLE : View.GONE);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            int width = (int)spToPixels(LEGEND_WIDTH_LANDSCAPE_SP);

            // Put an upper bound on the legend width at 40% landscape screen width
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            //noinspection deprecation
            int landscapeWidth = Math.max(display.getWidth(), display.getHeight());
            if (width > landscapeWidth * 0.4) {
                width = (int)(landscapeWidth * 0.4);
            }

            legend.getLayoutParams().width = width;
        }
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

        // Initialize our WebView legend
        TextView legend = (TextView)rootView.findViewById(R.id.legend);
        initializeLegend(legend);

        // Initialize our XYPlot view reference:
        final XYPlot plot = (XYPlot)rootView.findViewById(R.id.mySimpleXYPlot);

        addPlotData(plot);
        plot.setOnTouchListener(getOnTouchListener(plot));
        setUpPlotLayout(plot);

        // Animate to max zoomed out
        minX = maxX - History.deltaMsToDouble(86400 * 1000);
        if (minX < originalMinX) {
            minX = originalMinX;
        }
        // Delaying startup animation 250ms makes it smoother in the simulator at least; my guess is
        // the delay makes it not interfere with other startup tasks.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                animateXrange(plot, originalMinX, originalMaxX);
            }
        }, 250);

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
        //noinspection SuspiciousNameCombination
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

            plot.addSeries(history.getBatteryDrain(), drainFormatter);

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

    /**
     * Animate minX and maxX to new values.
     *
     * @return true if an animation was started, false if we're already at the target values
     */
    private boolean animateXrange(final XYPlot plot, double targetMinX, double targetMaxX) {
        // Cancel any running animation
        if (animator != null) {
            animator.cancel();
        }

        if (targetMinX < originalMinX) {
            targetMinX = originalMinX;
        }
        if (targetMaxX > originalMaxX) {
            targetMaxX = originalMaxX;
        }
        if (targetMaxX <= targetMinX) {
            throw new IllegalArgumentException("Max target must be > min target");
        }
        if (targetMaxX == maxX && targetMinX == minX) {
            // We're already there, nothing to animate
            return false;
        }

        animator = ValueAnimator.ofPropertyValuesHolder(
                PropertyValuesHolder.ofFloat("minX", (float) minX, (float) targetMinX),
                PropertyValuesHolder.ofFloat("maxX", (float) maxX, (float) targetMaxX));
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(ANIMATION_DURATION_MS);
        final int nFrames[] = new int[1];
        final long longestGapMs[] = new long[1];
        final int longestGapBeforeFrame[] = new int[1];
        final long lastFrameStart[] = new long[1];
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                long frameStart = SystemClock.elapsedRealtime();
                if (lastFrameStart[0] > 0) {
                    long gapMs = frameStart - lastFrameStart[0];
                    if (gapMs > longestGapMs[0]) {
                        longestGapMs[0] = gapMs;
                        longestGapBeforeFrame[0] = nFrames[0];
                    }
                }
                lastFrameStart[0] = frameStart;

                nFrames[0]++;
                minX = (double) (Float) animation.getAnimatedValue("minX");
                maxX = (double) (Float) animation.getAnimatedValue("maxX");

                plot.setDomainBoundaries(minX, maxX, BoundaryMode.FIXED);
                redrawPlot(plot);
            }
        });
        final double finalMinX = targetMinX;
        final double finalMaxX = targetMaxX;
        animator.addListener(new AnimatorListenerAdapter() {
            boolean cancelled = false;
            long t0;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                t0 = SystemClock.elapsedRealtime();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (cancelled) {
                    return;
                }

                long t1 = SystemClock.elapsedRealtime();
                long durationMs = t1 - t0;
                if (nFrames[0] == 0 || durationMs == 0) {
                    Log.w(TAG,
                            "Animation had "
                                    + nFrames[0] + " frames, done in "
                                    + durationMs + "ms, longest gap was "
                                    + longestGapMs[0] + "ms at frames "
                                    + (longestGapBeforeFrame[0] - 1)
                                    + "-"
                                    + longestGapBeforeFrame[0]);
                } else {
                    double fps = nFrames[0] / (durationMs / 1000.0);
                    double msPerFrame = durationMs / ((double)nFrames[0]);
                    Log.i(TAG, String.format("Animation of %d frames took %dms at %.1ffps or %.1fms/frame, longest time was %dms at frames %d-%d",
                            nFrames[0],
                            durationMs,
                            fps,
                            msPerFrame,
                            longestGapMs[0],
                            (longestGapBeforeFrame[0] - 1),
                            longestGapBeforeFrame[0]));
                }

                // Avoid any float -> double rounding errors
                minX = finalMinX;
                maxX = finalMaxX;
            }
        });
        animator.start();

        return true;
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
                        double targetMinX;
                        double targetMaxX;
                        if (minX == originalMinX && maxX == originalMaxX) {
                            // Reset zoom to two most recent days
                            targetMaxX = originalMaxX;
                            targetMinX = targetMaxX - History.deltaMsToDouble(86400 * 1000 * 2);
                        } else {
                            // Reset zoom to max out
                            targetMinX = originalMinX;
                            targetMaxX = originalMaxX;
                        }

                        animateXrange(plot, targetMinX, targetMaxX);

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
