package com.gmail.walles.johan.batterylogger;

import java.text.ParseException;
import java.util.Date;

class HistoryEvent {
    enum Type {
        BATTERY_LEVEL,
        SYSTEM_SHUTDOWN,
        SYSTEM_BOOT,
        INFO
    }

    private final Date timestamp;
    private final Type type;
    private int percentage;
    private String message;

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

    public String getMessage() {
        if (type != Type.INFO) {
            throw new UnsupportedOperationException(
                    "Message only available for INFO events but I'm a " + type);
        }
        return message;
    }

    private HistoryEvent(Date timestamp, Type type) {
        this.timestamp = timestamp;
        this.type = type;
    }

    public static HistoryEvent createBatteryLevelEvent(Date timestamp, int percentage) {
        HistoryEvent event = new HistoryEvent(timestamp, Type.BATTERY_LEVEL);
        event.percentage = percentage;
        return event;
    }

    public static HistoryEvent createInfoEvent(Date timestamp, String message) {
        HistoryEvent event = new HistoryEvent(timestamp, Type.INFO);
        event.message = message;
        return event;
    }

    public static HistoryEvent createSystemHaltingEvent(Date timestamp) {
        return new HistoryEvent(timestamp, Type.SYSTEM_SHUTDOWN);
    }

    public static HistoryEvent createSystemBootingEvent(Date timestamp) {
        return new HistoryEvent(timestamp, Type.SYSTEM_BOOT);
    }

    @Override
    public String toString() {
        return "HistoryEvent{" +
                "timestamp=" + timestamp +
                ", " + type +
                ", " + percentage + "%" +
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
        }

        return returnMe;
    }

    public String serializeToString() {
        switch (type) {
            case INFO:
                return type.name() + " " + timestamp.getTime() + " " + message;
            case BATTERY_LEVEL:
                return type.name() + " " + timestamp.getTime() + " " + percentage;
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

        if (!timestamp.equals(eventB.timestamp)) {
            return false;
        }

        if (type.equals(Type.BATTERY_LEVEL) && percentage != eventB.percentage) {
            return false;
        }

        if (type.equals(Type.INFO) && !message.equals(eventB.message)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }
}
