package com.gmail.walles.johan.batterylogger;

import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class DrainLinesCreator {
    private final List<HistoryEvent> history;
    private List<XYSeries> drainLines;

    @Nullable
    private Date lineStart;

    @Nullable
    private Boolean charging;

    @Nullable
    private List<HistoryEvent> currentDrainEvents;

    public DrainLinesCreator(List<HistoryEvent> history) {
        this.history = history;
    }

    static double median(List<Double> numbers) {
        if (numbers.size() == 0) {
            throw new IllegalArgumentException("Must get at least one number to compute median");
        }

        Double sortedNumbers[] = new Double[numbers.size()];
        numbers.toArray(sortedNumbers);
        Arrays.sort(sortedNumbers);

        double median;
        if (sortedNumbers.length % 2 == 1) {
            median = sortedNumbers[sortedNumbers.length / 2];
        } else {
            median =
                    sortedNumbers[sortedNumbers.length / 2]
                            + sortedNumbers[sortedNumbers.length / 2 - 1];
            median /= 2.0;
        }

        return median;
    }

    private double getMedianDrain() {
        if (currentDrainEvents == null) {
            throw new IllegalStateException("No drain events => median undefined");
        }
        if (currentDrainEvents.size() < 2) {
            throw new IllegalStateException("Need at least two drain events to compute median, got "
                    + currentDrainEvents.size());
        }

        List<Double> numbers = new ArrayList<Double>(currentDrainEvents.size() - 1);

        HistoryEvent previous = null;
        for (HistoryEvent drainEvent : currentDrainEvents) {
            if (previous != null) {
                long dMilliseconds =
                        drainEvent.getTimestamp().getTime() - previous.getTimestamp().getTime();
                double dHours = dMilliseconds / (3600 * 1000);

                int percentDischarge = previous.getPercentage() - drainEvent.getPercentage();
                double percentPerHour = percentDischarge / dHours;

                if (percentPerHour <= 0) {
                    throw new IllegalStateException("Impo");
                }

                numbers.add(percentPerHour);
            }
            previous = drainEvent;
        }

        return median(numbers);
    }

    // FIXME: Test with charging = false and zero currentDrainEvents
    // FIXME: Test with charging = false and one currentDrainEvent
    // FIXME: Test with charging = false and two currentDrainEvents
    private void finishLine(Date lineEnd) {
        if (charging == null || lineStart == null) {
            // Don't draw anything
        } else if (charging) {
            // Draw a line at y=0
            SimpleXYSeries line = new SimpleXYSeries("don't show this string");
            line.addLast(History.toDouble(lineStart), 0);
            line.addLast(History.toDouble(lineEnd), 0);
            drainLines.add(line);
        } else {
            // We're draining
            double y = getMedianDrain();
            SimpleXYSeries line = new SimpleXYSeries("don't show this string");
            line.addLast(History.toDouble(lineStart), y);
            line.addLast(History.toDouble(lineEnd), y);
            drainLines.add(line);
        }

        currentDrainEvents = null;
        lineStart = lineEnd;
    }

    private void handleEvent(HistoryEvent event) {
        switch (event.getType()) {
            case INFO:
                // This event type intentionally ignored
                break;

            case START_CHARGING:
                finishLine(event.getTimestamp());
                charging = true;
                break;

            case STOP_CHARGING:
                finishLine(event.getTimestamp());
                charging = false;
                break;

            case SYSTEM_SHUTDOWN:
                finishLine(event.getTimestamp());
                charging = null;
                break;

            case  SYSTEM_BOOT:
                finishLine(event.getTimestamp());
                charging = event.isCharging();
                break;

            case BATTERY_LEVEL:
                if (currentDrainEvents == null) {
                    currentDrainEvents = new LinkedList<HistoryEvent>();
                }
                currentDrainEvents.add(event);
                break;
        }
    }

    // FIXME: Test with one event
    public List<XYSeries> getDrainLines() {
        if (drainLines != null) {
            return drainLines;
        }
        drainLines = new LinkedList<XYSeries>();

        if (history.isEmpty()) {
            return drainLines;
        }

        for (HistoryEvent event : history) {
            handleEvent(event);
        }
        finishLine(history.get(history.size() - 1).getTimestamp());

        return drainLines;
    }
}
