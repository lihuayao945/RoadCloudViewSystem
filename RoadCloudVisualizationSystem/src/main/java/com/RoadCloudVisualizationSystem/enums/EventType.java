package com.RoadCloudVisualizationSystem.enums;

public enum EventType {
    REVERSE_DRIVING(0, "倒车/逆行"),
    SLOW_DRIVING(1, "慢行"),
    FAST_DRIVING(2, "快行"),
    EMERGENCY_BRAKE(3, "紧急制动"),
    ABNORMAL_PARKING(4, "异常停车");

    private final int code;
    private final String description;

    EventType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static EventType getByCode(int code) {
        for (EventType type : EventType.values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("无效的事件类型编码: " + code);
    }

    public static String getDescriptionByCode(int code) {
        return getByCode(code).getDescription();
    }
}