package com.gmail.walles.johan.batterylogger;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
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
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.io.IOException;

import static com.gmail.walles.johan.batterylogger.MainActivity.TAG;

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

        // Create a formatter to use for drawing a series using LineAndPointRenderer
        // and configure it from xml:
        LineAndPointFormatter lineAndPointFormatter = new LineAndPointFormatter();
        lineAndPointFormatter.setPointLabelFormatter(new PointLabelFormatter());
        lineAndPointFormatter.configure(getActivity(), R.xml.line_point_formatter_with_plf1);

        // add a new series' to the xyplot:
        try {
            History history = new History(getActivity());
            for (XYSeries drain : history.getBatteryDrain()) {
                plot.addSeries(drain, lineAndPointFormatter);
            }

            Paint labelPaint = new Paint();
            labelPaint.setAntiAlias(true);
            labelPaint.setColor(Color.WHITE);
            plot.addSeries(history.getEvents(), new EventFormatter(labelPaint));
        } catch (IOException e) {
            Log.e(TAG, "Reading battery history failed", e);
            showAlertDialog(getActivity(), "Failed to read battery history");
        }

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
        minXY = new PointF(plot.getCalculatedMinX().floatValue(), 0);
        maxXY = new PointF(plot.getCalculatedMaxX().floatValue(), Math.max(plot.getCalculatedMaxY().floatValue(), 9f));
        originalMinXY = new PointF(minXY.x, minXY.y);
        originalMaxXY = new PointF(maxXY.x, maxXY.y);
        plot.setRangeBoundaries(minXY.y, maxXY.y, BoundaryMode.FIXED);

        return rootView;
    }
}
