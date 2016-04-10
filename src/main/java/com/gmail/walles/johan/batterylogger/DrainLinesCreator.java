/*
 * Copyright 2015 Johan Walles <johan.walles@gmail.com>
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

import android.support.annotation.Nullable;

import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import timber.log.Timber;

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

    @Nullable
    static Double average(Collection<Double> numbers) {
        if (numbers.size() == 0) {
            return null;
        }

        double sum = 0.0;
        for (double number : numbers) {
            sum += number;
        }

        return sum / numbers.size();
    }

    @Nullable
    private Double getDrainLineLevel() {
        if (currentDrainEvents == null) {
            throw new IllegalStateException("No drain events => level undefined");
        }
        if (currentDrainEvents.size() < 2) {
            throw new IllegalStateException("Need at least two drain events to compute a level, got "
                    + currentDrainEvents.size());
        }

        List<Double> numbers = new ArrayList<>(currentDrainEvents.size() - 1);

        HistoryEvent previous = null;
        for (HistoryEvent drainEvent : currentDrainEvents) {
            if (previous != null) {
                long dMilliseconds =
                        drainEvent.getTimestamp().getTime() - previous.getTimestamp().getTime();
                double dHours = dMilliseconds / (3600 * 1000.0);

                int percentDischarge = previous.getPercentage() - drainEvent.getPercentage();
                double percentPerHour = percentDischarge / dHours;

                if (percentPerHour <= 0) {
                    // This is an edge case that we choose to ignore. You get here by charging
                    // your phone short enough time that both the before and after samples says
                    // "draining", but long enough that the after sample has higher charge
                    // percentage than the before one.
                    continue;
                }

                numbers.add(percentPerHour);
            }
            previous = drainEvent;
        }

        // No matter how much I like medians, the input here is quantized and averages will give
        // us a much better representation of what the drain has actually been over a period.
        return average(numbers);
    }

    @Nullable
    private XYSeries createDrainLine(Date lineEnd) {
        if (charging == null) {
            // Don't know whether we're charging, don't draw anything
            Timber.v("No charging state => no line");
            return null;
        }

        if (lineStart == null) {
            // Don't know when the current run started, don't draw anything
            Timber.v("No line start => no line");
            return null;
        }

        if (charging) {
            // Draw a line at y=0
            Timber.v("Charging => line at y=0");
            SimpleXYSeries line = new SimpleXYSeries("don't show this string");
            line.addLast(History.toDouble(lineStart), 0);
            line.addLast(History.toDouble(lineEnd), 0);
            drainLines.add(line);
        }

        if (currentDrainEvents == null) {
            Timber.v("No drain events => no line");
            // No drain events, can't draw anything
            return null;
        }

        if (currentDrainEvents.size() < 2) {
            Timber.v("Too few drain events => no line");
            // Too few drain events to be able to compute a drain speed, don't draw anything
            return null;
        }

        // We're draining
        Double y = getDrainLineLevel();
        if (y == null) {
            // No we aren't
            return null;
        }

        Timber.v("Drawing drain line at " + y);
        SimpleXYSeries line = new SimpleXYSeries("don't show this string");
        line.addLast(History.toDouble(lineStart), y);
        line.addLast(History.toDouble(lineEnd), y);
        return line;
    }

    private void finishLine(Date lineEnd) {
        XYSeries drainLine = createDrainLine(lineEnd);
        if (drainLine != null) {
            drainLines.add(drainLine);
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

            case SYSTEM_BOOT:
                finishLine(event.getTimestamp());
                charging = event.isCharging();
                break;

            case BATTERY_LEVEL:
                if (currentDrainEvents == null) {
                    currentDrainEvents = new LinkedList<>();
                }
                currentDrainEvents.add(event);
                break;
        }
    }

    public List<XYSeries> getDrainLines() {
        if (drainLines != null) {
            return drainLines;
        }
        drainLines = new LinkedList<>();

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
