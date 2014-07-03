package com.gmail.walles.johan.batterylogger;

import com.androidplot.xy.XYSeries;

import java.util.ArrayList;

public class EventSeries implements XYSeries {
    private final ArrayList<Double> xCoordinates = new ArrayList<Double>();
    private final ArrayList<String> descriptions = new ArrayList<String>();

    public void add(double x, String description) {
        xCoordinates.add(x);
        descriptions.add(description);
    }

    @Override
    public int size() {
        return xCoordinates.size();
    }

    @Override
    public Number getX(int i) {
        return xCoordinates.get(i);
    }

    @Override
    public Number getY(int i) {
        return 0;
    }

    public String getDescription(int i) {
        return descriptions.get(i);
    }

    @Override
    public String getTitle() {
        return "Not expected to be shown";
    }
}
