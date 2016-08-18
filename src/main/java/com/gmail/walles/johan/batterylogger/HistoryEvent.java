/*
 * Copyright 2016 Johan Walles <johan.walles@gmail.com>
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.text.ParseException;
import java.util.Date;

public class HistoryEvent implements Comparable<HistoryEvent> {
    @Override
    public int compareTo(@NonNull HistoryEvent historyEvent) {
        return getTimestamp().compareTo(historyEvent.getTimestamp());
    }

    public enum Type {
        BATTERY_LEVEL,
        SYSTEM_SHUTDOWN,
        SYSTEM_BOOT,
        INFO,
        START_CHARGING,
        STOP_CHARGING
    }

    @Nullable
    private Date timestamp;

    private final Type type;
    private int percentage;
    private String message;
    private boolean charging;

    public Date getTimestamp() {
        if (timestamp == null) {
            throw new IllegalStateException("Must set timestamp first");
        }

        return timestamp;
    }

    public void setTimestamp(@NonNull Date timestamp) {
        if (this.timestamp != null) {
            throw new IllegalStateException("Timestamp already set");
        }

        this.timestamp = timestamp;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isComplete() {
        return timestamp != null;
    }

    public Type getType() {
        return type;
    }

    public int getPercentage() {
        if (type != Type.BATTERY_LEVEL) {
            throw new UnsupportedOperationException(
                    "Percentage only available for BATTERY_LEVEL events but I'm a " + type);
        }
        return percentage;
    }

    public String getMessage() {
        if (type != Type.INFO) {
            throw new UnsupportedOperationException(
                    "Message only available for INFO events but I'm a " + type);
        }
        return message;
    }

    public boolean isCharging() {
        if (type != Type.SYSTEM_BOOT) {
            throw new UnsupportedOperationException(
                    "Charging state only available for SYSTEM_BOOT events, but I'm a " + type);
        }
        return charging;
    }

    private HistoryEvent(@Nullable Date timestamp, Type type) {
        this.timestamp = timestamp;
        this.type = type;
    }

    public static HistoryEvent createBatteryLevelEvent(@Nullable Date timestamp, int percentage) {
        HistoryEvent event = new HistoryEvent(timestamp, Type.BATTERY_LEVEL);
        event.percentage = percentage;
        return event;
    }

    public static HistoryEvent createInfoEvent(@Nullable Date timestamp, String message) {
        HistoryEvent event = new HistoryEvent(timestamp, Type.INFO);
        event.message = message;
        return event;
    }

    public static HistoryEvent createSystemHaltingEvent(@Nullable Date timestamp) {
        return new HistoryEvent(timestamp, Type.SYSTEM_SHUTDOWN);
    }

    public static HistoryEvent createSystemBootingEvent(@Nullable Date timestamp, boolean isCharging) {
        HistoryEvent event = new HistoryEvent(timestamp, Type.SYSTEM_BOOT);
        event.charging = isCharging;
        return event;
    }

    public static HistoryEvent createStartChargingEvent(@Nullable Date timestamp) {
        return new HistoryEvent(timestamp, Type.START_CHARGING);
    }

    public static HistoryEvent createStopChargingEvent(@Nullable Date timestamp) {
        return new HistoryEvent(timestamp, Type.STOP_CHARGING);
    }

    @Override
    public String toString() {
        return "HistoryEvent{" +
                timestamp +
                ", " + type +
                ", " + percentage + "%" +
                ", " + (charging ? "charging" : "discharging") +
                ", '" + message + '\'' +
                '}';
    }

    public static HistoryEvent deserializeFromString(String serialization) throws ParseException {
        int firstSpaceIndex = serialization.indexOf(' ');
        int secondSpaceIndex = serialization.indexOf(' ', firstSpaceIndex + 1);

        if (firstSpaceIndex == -1) {
            throw new ParseException(
                    "Parse failed, firstSpace=" + firstSpaceIndex
                            + ", string: <" + serialization + ">", -1);
        }

        Type type;
        String typeString = serialization.substring(0, firstSpaceIndex);
        try {
            type = Type.valueOf(typeString);
        } catch (IllegalArgumentException e) {
            ParseException throwMe = new ParseException("Parsing type failed: " + serialization, -1);
            throwMe.initCause(e);
            throw throwMe;
        }

        long timestamp;
        String timestampString;
        if (secondSpaceIndex != -1) {
            timestampString = serialization.substring(firstSpaceIndex + 1, secondSpaceIndex);
        } else {
            timestampString = serialization.substring(firstSpaceIndex + 1);
        }
        try {
            timestamp = Long.valueOf(timestampString);
        } catch (NumberFormatException e) {
            ParseException throwMe = new ParseException("Parsing timestamp failed: " + serialization, -1);
            throwMe.initCause(e);
            throw throwMe;
        }

        HistoryEvent returnMe = new HistoryEvent(new Date(timestamp), type);

        if (type == Type.BATTERY_LEVEL) {
            if (secondSpaceIndex == -1) {
                throw new ParseException(
                        "Parse failed, firstSpace=" + firstSpaceIndex
                                + ", secondSpace=" + secondSpaceIndex
                                + ", string: <" + serialization + ">", -1);
            }

            int percentage;
            String percentageString = serialization.substring(secondSpaceIndex + 1);
            try {
                percentage = Integer.valueOf(percentageString);
            } catch (NumberFormatException e) {
                ParseException throwMe = new ParseException("Parsing percentage failed: " + serialization, -1);
                throwMe.initCause(e);
                throw throwMe;
            }

            returnMe.percentage = percentage;
        } else if (type == Type.INFO) {
            if (secondSpaceIndex == -1) {
                throw new ParseException(
                        "Parse failed, firstSpace=" + firstSpaceIndex
                                + ", secondSpace=" + secondSpaceIndex
                                + ", string: <" + serialization + ">", -1);
            }

            returnMe.message = serialization.substring(secondSpaceIndex + 1);
        } else if (type == Type.SYSTEM_BOOT) {
            if (secondSpaceIndex == -1) {
                throw new ParseException(
                        "Parse failed, firstSpace=" + firstSpaceIndex
                                + ", secondSpace=" + secondSpaceIndex
                                + ", string: <" + serialization + ">", -1);
            }

            returnMe.charging = Boolean.valueOf(serialization.substring(secondSpaceIndex + 1));
        }

        return returnMe;
    }

    public String serializeToString() {
        if (timestamp == null) {
            throw new IllegalStateException("Must set timestamp before serializing");
        }

        switch (type) {
            case INFO:
                return type.name() + " " + timestamp.getTime() + " " + message;
            case BATTERY_LEVEL:
                return type.name() + " " + timestamp.getTime() + " " + percentage;
            case SYSTEM_BOOT:
                return type.name() + " " + timestamp.getTime() + " " + charging;
            default:
                return type.name() + " " + timestamp.getTime();
        }
    }

    @Override
    public boolean equals(Object b) {
        if (b.getClass() != getClass()) {
            return false;
        }
        HistoryEvent eventB = (HistoryEvent)b;

        if (!type.equals(eventB.type)) {
            return false;
        }

        if (timestamp == null && eventB.timestamp != null) {
            return false;
        }
        if (timestamp != eventB.timestamp && !timestamp.equals(eventB.timestamp)) {
            return false;
        }

        if (type.equals(Type.BATTERY_LEVEL) && percentage != eventB.percentage) {
            return false;
        }

        if (type.equals(Type.INFO) && !message.equals(eventB.message)) {
            return false;
        }

        if (type.equals(Type.SYSTEM_BOOT) && charging != eventB.charging) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }
}
