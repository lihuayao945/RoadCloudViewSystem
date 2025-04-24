package com.RoadCloudVisualizationSystem.mqtt.dao;

import lombok.Data;

@Data
public class TopicSubscription {
    private String topic;
    private int qos;

    // Getters and Setters

    public TopicSubscription(String topic, int qos) {
        this.topic = topic;
        this.qos = qos;
    }
}