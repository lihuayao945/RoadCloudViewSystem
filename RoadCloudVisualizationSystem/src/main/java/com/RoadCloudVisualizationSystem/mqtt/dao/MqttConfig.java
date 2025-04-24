package com.RoadCloudVisualizationSystem.mqtt.dao;

import lombok.Data;

import java.util.List;

@Data
public class MqttConfig {
    private String brokerUrl;
    private String clientId;
    private List<TopicSubscription> topics;
    private String username;
    private String password;

    // Getters and Setters

}


