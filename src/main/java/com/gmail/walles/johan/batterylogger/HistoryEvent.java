package com.gmail.walles.johan.batterylogger;

import java.text.ParseException;
import java.util.Date;

class HistoryEvent {
    enum Type {
        BATTERY_LEVEL,
        CHARGING_START,
        CHARGING_STOP,
        SYSTEM_SHUTDOWN,
        SYSTEM_BOOT_CHARGING,
        SYSTEM_BOOT_UNPLUGGED
    }

    private final Date timestamp;
    private final Type type;
    private int percentage;

    public Date getTimestamp() {
        return timestamp;
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

    private HistoryEvent(Date timestamp, Type type) {
        this.timestamp = timestamp;
        this.type = type;
    }

    public static HistoryEvent createBatteryLevelEvent(int percentage, Date timestamp) {
        HistoryEvent event = new HistoryEvent(timestamp, Type.BATTERY_LEVEL);
        event.percentage = percentage;
        return event;
    }

    public static HistoryEvent createStartChargingEvent(Date timestamp) {
        return new HistoryEvent(timestamp, Type.CHARGING_START);
    }

    public static HistoryEvent createStopChargingEvent(Date timestamp) {
        return new HistoryEvent(timestamp, Type.CHARGING_STOP);
    }

    public static HistoryEvent createSystemHaltingEvent(Date timestamp) {
        return new HistoryEvent(timestamp, Type.SYSTEM_SHUTDOWN);
    }

    public static HistoryEvent createSystemBootingEvent(Date timestamp, boolean isCharging) {
        if (isCharging) {
            return new HistoryEvent(timestamp, Type.SYSTEM_BOOT_CHARGING);
        } else {
            return new HistoryEvent(timestamp, Type.SYSTEM_BOOT_UNPLUGGED);
        }
    }

    public static HistoryEvent deserializeFromString(String serialization) throws ParseException {
        int firstSpaceIndex = serialization.indexOf(' ');
        int secondSpaceIndex = serialization.indexOf(' ', firstSpaceIndex + 1);

        if (firstSpaceIndex == -1 || secondSpaceIndex == -1) {
            throw new ParseException(
                    "Parse failed, firstSpace=" + firstSpaceIndex
                            + ", secondSpace=" + secondSpaceIndex
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
        String timestampString = serialization.substring(firstSpaceIndex + 1, secondSpaceIndex);
        try {
            timestamp = Long.valueOf(timestampString);
        } catch (NumberFormatException e) {
            ParseException throwMe = new ParseException("Parsing timestamp failed: " + serialization, -1);
            throwMe.initCause(e);
            throw throwMe;
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

        HistoryEvent returnMe = new HistoryEvent(new Date(timestamp), type);
        returnMe.percentage = percentage;

        return returnMe;
    }

    public String serializeToString() {
        return type.name() + " " + timestamp.getTime() + " " + percentage;
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

        if (!timestamp.equals(eventB.timestamp)) {
            return false;
        }

        if (type.equals(Type.BATTERY_LEVEL) && percentage != eventB.percentage) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }
}
