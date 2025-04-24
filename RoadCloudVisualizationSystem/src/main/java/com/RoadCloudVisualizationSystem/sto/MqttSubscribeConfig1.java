package com.RoadCloudVisualizationSystem.sto;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MqttSubscribeConfig1 {
    @Value("${spring.mqtt.subscribe.username}")
    private String username;
    @Value("${spring.mqtt.subscribe.password}")
    private String password;
    @Value("${spring.mqtt.subscribe.url}")
    private String hostUrl;

    // 订阅主题的客户端ID
    @Value("${spring.mqtt.subscribe.client.inid}")
    private String clientId;

    @Value("${spring.mqtt.subscribe.default.topic}")
    private String defaultTopic;
    @Value("${spring.mqtt.subscribe.completionTimeout}")
    private int completionTimeout;   //连接超时


    // 在类中定义全局集合
    private static final Set<String> uniqueTopics = ConcurrentHashMap.newKeySet();


    public static Set<String> getUniqueTopics() {
        return uniqueTopics;
    }

    public MqttPahoMessageDrivenChannelAdapter createAdapter(String[] topics) {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId, receiverMqttClientFactoryForSub(), topics);
        adapter.setCompletionTimeout(completionTimeout);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(2);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    public MqttConnectOptions getReceiverMqttConnectOptionsForSub() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        try {
            mqttConnectOptions.setUserName(username);
            mqttConnectOptions.setPassword(password.toCharArray());
            List<String> hostList = Arrays.asList(hostUrl.trim().split(","));
            String[] serverURIs = new String[hostList.size()];
            hostList.toArray(serverURIs);
            mqttConnectOptions.setServerURIs(serverURIs);
            mqttConnectOptions.setKeepAliveInterval(2);
            mqttConnectOptions.setAutomaticReconnect(true); // 启用自动重连
            mqttConnectOptions.setConnectionTimeout(30); // 设置连接超时时间
        } catch (Exception e) {
            log.error("Failed to set MQTT connection options", e);
        }
        return mqttConnectOptions;
    }

    @Bean
    public MqttPahoClientFactory receiverMqttClientFactoryForSub() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(getReceiverMqttConnectOptionsForSub());
        return factory;
    }

    //接收通道
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    //配置client,监听的topic
    @Bean
    public MessageProducer inbound() {
        List<String> topicList = Arrays.asList(defaultTopic.trim().split(","));
        String[] topics = new String[topicList.size()];
        topicList.toArray(topics);

        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId, receiverMqttClientFactoryForSub(), topics);
        adapter.setCompletionTimeout(completionTimeout);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(2);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    //通过通道获取数据
    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        return new MessageHandler() {
            @Override
            public void handleMessage(Message<?> message) throws MessagingException {
                try {
                    String topic = message.getHeaders().get("mqtt_receivedTopic").toString();
                    String msg = message.getPayload().toString();
                    uniqueTopics.add(topic);
                    Map<String, Object> msgMap = JSONObject.parseObject(msg);
                    // 这里可以处理接收的数据
//                    log.info("\n----------------------------START---------------------------\n" +
//                            "接收到订阅消息:\ntopic:" + topic + "\nmessage:" + msg +
//                            "\n-----------------------------END----------------------------");
                    //log.info("当前主题列表:topics:" + uniqueTopics);

                } catch (Exception e) {
                    log.error("Failed to handle MQTT message", e);
                }
            }
        };
    }
}

