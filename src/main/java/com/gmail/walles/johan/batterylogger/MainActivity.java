package com.gmail.walles.johan.batterylogger;

import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.util.Arrays;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        private XYPlot plot;

        public PlaceholderFragment() {
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

            return rootView;
        }
    }
}
