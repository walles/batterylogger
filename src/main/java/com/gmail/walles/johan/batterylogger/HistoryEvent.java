package com.gmail.walles.johan.batterylogger;

import java.util.Date;

class HistoryEvent {
    enum Type {
        BATTERY_LEVEL,
        CHARGING_START,
        CHARGING_STOP,
        SYSTEM_SHUTDOWN,
        SYSTEM_BOOT
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

    public static HistoryEvent createSystemBootingEvent(Date timestamp) {
        return new HistoryEvent(timestamp, Type.SYSTEM_BOOT);
    }
}
