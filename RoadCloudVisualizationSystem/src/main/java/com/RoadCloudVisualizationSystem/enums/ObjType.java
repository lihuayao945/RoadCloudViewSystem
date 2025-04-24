package com.RoadCloudVisualizationSystem.enums;

import java.util.HashMap;
import java.util.Map;

public enum ObjType {
    PEOPLE(0, "行人"),
    BICYCLE(1, "自行车"),
    PASSENGER_CAR(2, "乘用车"),
    MOTORCYCLE(3, "摩托车"),
    SPECIAL_VEHICLE(4, "特殊用车辆"),
    BUS(5, "公交车"),
    RAIL_VEHICLE(6, "有轨道车"),
    TRUCK(7, "卡车"),
    TRICYCLE(8, "三轮车"),
    UNKNOWN(0, "未知");

    private final int code;
    private final String type;

    // 双向查找映射
    private static final Map<Integer, ObjType> codeLookup = new HashMap<>();
    private static final Map<String, ObjType> typeLookup = new HashMap<>();

    static {
        for (ObjType type : ObjType.values()) {
            codeLookup.put(type.code, type);
            typeLookup.put(type.type.toUpperCase(), type);
        }
    }

    ObjType(int code, String type) {
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
    public static ObjType fromCode(int code) {
        ObjType result = codeLookup.get(code);
        if (result == null) {
            return UNKNOWN;
        }
        return result;
    }

    // 通过类型名称获取枚举
    public static ObjType fromType(String type) {
        if (type == null) {
            return UNKNOWN;
        }
        ObjType result = typeLookup.get(type.toUpperCase());
        if (result == null) {
            return UNKNOWN;
        }
        return result;
    }
}
