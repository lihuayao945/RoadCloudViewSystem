package com.RoadCloudVisualizationSystem.entity;

public class Rcumenu {
    private String rcuId;

    private String deviceType;

    private String receiveTime;

    public String getRcuId() {
        return rcuId;
    }

    public void setRcuId(String rcuId) {
        this.rcuId = rcuId == null ? null : rcuId.trim();
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType == null ? null : deviceType.trim();
    }

    public String getReceiveTime() {
        return receiveTime;
    }

    public void setReceiveTime(String receiveTime) {
        this.receiveTime = receiveTime == null ? null : receiveTime.trim();
    }
}