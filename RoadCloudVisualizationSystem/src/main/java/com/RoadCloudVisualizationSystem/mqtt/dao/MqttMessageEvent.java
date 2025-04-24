package com.RoadCloudVisualizationSystem.mqtt.dao;

import lombok.Data;

// 事件类
@Data
public class MqttMessageEvent {
    private final String topic;
    private final String payload;

    public MqttMessageEvent(String topic, String payload) {
        this.topic = topic;
        this.payload = payload;
    }

    // Getters
}