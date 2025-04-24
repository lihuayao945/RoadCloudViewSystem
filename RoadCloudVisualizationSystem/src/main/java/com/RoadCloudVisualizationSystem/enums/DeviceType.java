package com.RoadCloudVisualizationSystem.enums;

import java.util.HashMap;
import java.util.Map;

public enum DeviceType {
    UNKNOWN(0, "未知"),
    FUSION(1, "融合设备"),
    RADAR(2, "摄像头"),
    MILLISECOND(3, "毫秒波雷达"),
    LIDAR(4, "激光雷达"),
    RCU(5, "rcu"),
    OTHER(6,"其他");

    private final int code;
    private final String type;

    // 双向查找映射
    private static final Map<Integer, DeviceType> codeLookup = new HashMap<>();
    private static final Map<String, DeviceType> typeLookup = new HashMap<>();

    static {
        for (DeviceType type : DeviceType.values()) {
            codeLookup.put(type.code, type);
            typeLookup.put(type.type.toUpperCase(), type);
        }
    }

    DeviceType(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public int getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    // 通过数字编码获取枚举
    public static DeviceType fromCode(int code) {
        DeviceType result = codeLookup.get(code);
        if (result == null) {
            throw new IllegalArgumentException("未知设备编码: " + code);
        }
        return result;
    }

    // 通过类型名称获取枚举（兼容原设计）
    public static DeviceType fromType(String type) {
        DeviceType result = typeLookup.get(type.toUpperCase());
        if (result == null) {
            throw new IllegalArgumentException("未知设备类型: " + type);
        }
        return result;
    }
}