package com.gmail.walles.johan.batterylogger;

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
            Number[] series1Numbers = {1, 8, 5, 2, 7, 4};
            Number[] series2Numbers = {4, 6, 3, 8, 2, 10};

            // Turn the above arrays into XYSeries':
            XYSeries series1 = new SimpleXYSeries(
                    Arrays.asList(series1Numbers),          // SimpleXYSeries takes a List so turn our array into a List
                    SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // Y_VALS_ONLY means use the element index as the x value
                    "Series1");                             // Set the display title of the series

            // same as above
            XYSeries series2 = new SimpleXYSeries(Arrays.asList(series2Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Series2");

            // Create a formatter to use for drawing a series using LineAndPointRenderer
            // and configure it from xml:
            LineAndPointFormatter series1Format = new LineAndPointFormatter();
            series1Format.setPointLabelFormatter(new PointLabelFormatter());
            series1Format.configure(getActivity(),
                    R.xml.line_point_formatter_with_plf1);

            // add a new series' to the xyplot:
            plot.addSeries(series1, series1Format);

            // same as above:
            LineAndPointFormatter series2Format = new LineAndPointFormatter();
            series2Format.setPointLabelFormatter(new PointLabelFormatter());
            series2Format.configure(getActivity(),
                    R.xml.line_point_formatter_with_plf2);
            plot.addSeries(series2, series2Format);

            // reduce the number of range labels
            plot.setTicksPerRangeLabel(3);
            plot.getGraphWidget().setDomainLabelOrientation(-45);

            return rootView;
        }
    }
}
