package com.RoadCloudVisualizationSystem.mqtt.dao;


import com.RoadCloudVisualizationSystem.mqtt.MqttService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MqttInitializer {

    private final MqttService mqttService;

    public MqttInitializer(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void autoStart() {
        // 创建初始配置
        MqttConfig config = new MqttConfig();
        config.setBrokerUrl("tcp://192.168.120.82:1887");
        config.setClientId("mqttClientInputId");
        config.setUsername("smqtt");
        config.setPassword("smqtt");
        
        List<TopicSubscription> topics = new ArrayList<>();
        topics.add(new TopicSubscription("#", 2));
        config.setTopics(topics);
        mqttService.updateConfig(config);
    }
}