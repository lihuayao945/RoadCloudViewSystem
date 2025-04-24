package com.RoadCloudVisualizationSystem.mqtt;

import com.RoadCloudVisualizationSystem.entity.Message;
import com.RoadCloudVisualizationSystem.entity.Rcu;
import com.RoadCloudVisualizationSystem.mqtt.dao.MqttConfig;
import com.RoadCloudVisualizationSystem.mqtt.dao.MqttMessageEvent;
import com.RoadCloudVisualizationSystem.mqtt.dao.TopicSubscription;
import com.RoadCloudVisualizationSystem.service.DataService;
import com.RoadCloudVisualizationSystem.service.MessageService;
import com.RoadCloudVisualizationSystem.utils.RcuparseUtil;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MqttService implements MqttCallback {

    @Autowired
    private DataService dataService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private RcuparseUtil rcuparseUtil;

    private MqttClient mqttClient;
    private MqttConfig currentConfig;
    private volatile boolean isRunning = false;
    private final Object lock = new Object();
    private final ApplicationEventPublisher eventPublisher;
    // 增加日志
    private static final Logger logger = LoggerFactory.getLogger(MqttService.class);

    // 新增一个专门用于资源操作的锁
    private final Object resourceLock = new Object();

    // 增加重连尝试计数器
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;

    public MqttService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public Map<String, Object> updateConfig(MqttConfig config) {
        Map<String, Object> result = new HashMap<>();
        synchronized (lock) {
            if (isRunning) {
                result.put("status", "fail");
                result.put("msg", "配置设置失败，请先停止订阅");
                return result;
            }
            try{
                this.currentConfig = config;
                result.put("status", "success");
                result.put("msg", "配置设置成功");
                return result;
            }catch (Exception e) {
                result.put("status", "fail");
                result.put("msg", "配置设置失败，服务器内部错误");
                return result;
            }
        }
    }

    public Map<String, Object> start() throws MqttException {
        Map<String, Object> result = new HashMap<>();
        if (isRunning) {
            result.put("status", "fail");
            result.put("msg", "开启订阅失败，请先停止订阅");
            logger.warn("MQTT client already running");
            return result;
        }
        synchronized (lock) {
            if (currentConfig == null) {
                result.put("status", "fail");
                result.put("msg", "开启订阅失败，配置信息不存在");
                logger.error("Start failed: No configuration");
                return result;
            }
            try {
                // 打印连接信息
                logger.info("Connecting to {} with clientID: {}",
                        currentConfig.getBrokerUrl(),
                        currentConfig.getClientId());

                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                if (currentConfig.getUsername() != null) options.setUserName(currentConfig.getUsername());
                if (currentConfig.getPassword() != null) options.setPassword(currentConfig.getPassword().toCharArray());
                options.setAutomaticReconnect(true);
                // 增加连接超时设置
                options.setConnectionTimeout(10);
                mqttClient = new MqttClient(currentConfig.getBrokerUrl(), currentConfig.getClientId());
                mqttClient.setCallback(this);
                mqttClient.connect(options);
                if (!mqttClient.isConnected()) {
                    logger.error("Connection failed: no error thrown but not connected");
                    result.put("status", "fail");
                    result.put("msg", "开启订阅失败，网络连接不到mqtt服务器");
                    return result;
                }

                // 打印订阅信息
                currentConfig.getTopics().forEach(topic ->
                        logger.info("Subscribing to {} [QoS {}]",
                                topic.getTopic(),
                                topic.getQos()));

                List<TopicSubscription> subs = currentConfig.getTopics();
                String[] topics = subs.stream().map(TopicSubscription::getTopic).toArray(String[]::new);
                int[] qos = subs.stream().mapToInt(TopicSubscription::getQos).toArray();
                mqttClient.subscribe(topics, qos);

                isRunning = true;
                logger.info("MQTT client started successfully");
                result.put("status", "success" );
                result.put("msg", "开启订阅成功");
                return result;
            } catch (MqttSecurityException e) {
                logger.error("Authentication failed: " + e.getMessage());
                result.put("status", "fail");
                result.put("msg", "开启订阅失败，用户密码信息认证无效");
                return result;
            } catch (MqttException e) {
                logger.error("Connection failed: " + e.getMessage());
                result.put("status", "fail");
                result.put("msg", "开启订阅失败，网络连接不到mqtt服务器");
                return result;
            }
        }
    }

    // 修改stop方法
    // 修改 stop() 方法
    public Map<String, Object> stop() {
        Map<String, Object> result = new HashMap<>();

        // 第一步：快速设置状态标志（不阻塞）
        synchronized (lock) {
            if (!isRunning) {
                result.put("status", "success");
                result.put("msg", "当前未开启订阅");
                return result;
            }
            isRunning = false; // 立即终止重连循环
        }

        // 第二步：强制释放资源（独立加锁，避免阻塞）
        synchronized (resourceLock) {
            try {
                if (mqttClient != null) {
                    mqttClient.disconnectForcibly(1000); // 强制断开连接
                    mqttClient.close(true);              // 立即释放资源
                    mqttClient = null;
                }
                logger.info("资源强制释放完成");
            } catch (MqttException e) {
                logger.error("强制释放资源失败: {}", e.getMessage());
            }
        }

        result.put("status", "success");
        result.put("msg", "停止订阅成功");
        return result;
    }

    // 对订阅到的消息进行处理
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        try {
            // 如果topic以_hex结尾，则跳过
            if (topic.endsWith("_hex")) {
                return;
            }
            //System.out.println("topic:" + topic);
            // 正确方式：使用 new String(byte[], charset) 解码
            String msg = new String(message.getPayload(), StandardCharsets.UTF_8); // 推荐 UTF-8
            System.out.println(msg);
            String nowTime = System.currentTimeMillis() + "";
            if (topic.contains("/rcu/objs")){
                try {
                    Rcu rcu = rcuparseUtil.extractRcuFromJson(msg);
                    rcu.setReceiveTime(nowTime);
                    rcuparseUtil.insertRcu(rcu);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            dataService.parseAndSave(msg);
            eventPublisher.publishEvent(new MqttMessageEvent(topic, new String(message.getPayload())));

            Message message1 = new Message(topic, msg, nowTime);
            messageService.insertMessage(message1);

        } catch (Exception e) {
            logger.error("Failed to handle MQTT message", e);
        }
    }


    // 新增重新订阅方法
    private void resubscribeTopics() {
        try {
            List<TopicSubscription> subs = currentConfig.getTopics();
            String[] topics = subs.stream()
                    .map(TopicSubscription::getTopic)
                    .toArray(String[]::new);
            int[] qos = subs.stream()
                    .mapToInt(TopicSubscription::getQos)
                    .toArray();
            mqttClient.subscribe(topics, qos);
            logger.info("Topics resubscribed successfully");
        } catch (MqttException e) {
            logger.error("Resubscribe failed: {}", e.getMessage());
        }
    }

    // 修改 connectionLost 方法，缩小同步范围
    @Override
    public void connectionLost(Throwable cause) {
        logger.warn("Connection lost: {}", cause.getMessage());

        int retryCount = 0;
        while (retryCount < MAX_RECONNECT_ATTEMPTS) {
            // 每次重试前检查状态（不加锁，避免阻塞 stop()）
            if (!isRunning) {
                logger.info("检测到停止信号，终止重连");
                break;
            }

            try {
                Thread.sleep(5000); // 休眠期间不持有锁
                logger.info("尝试第 {} 次重连", retryCount + 1);

                // 仅在实际操作时加锁（短暂占用）
                synchronized (resourceLock) {
                    if (!isRunning) break;
                    if (mqttClient == null || !mqttClient.isConnected()) {
                        if (mqttClient != null) {
                            mqttClient.close(true);
                        }
                        mqttClient = new MqttClient(currentConfig.getBrokerUrl(), currentConfig.getClientId());
                        mqttClient.setCallback(this);
                        mqttClient.connect(createConnectOptions());
                        resubscribeTopics();
                        logger.info("重连成功");
                        reconnectAttempts = 0;
                        return;
                    }
                }
            } catch (Exception e) {
                retryCount++;
                logger.error("重连失败: {}", e.getMessage());
            }
        }

        // 重连失败后清理资源
        synchronized (resourceLock) {
            if (mqttClient != null) {
                try {
                    mqttClient.disconnectForcibly();
                    mqttClient.close(true);
                    mqttClient = null;
                } catch (MqttException e) {
                    logger.error("资源清理失败: {}", e.getMessage());
                }
            }
        }
    }

    @Override public void deliveryComplete(IMqttDeliveryToken token) {}

    // 修改连接选项配置
    private MqttConnectOptions createConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);  // 重要修改！保持会话状态
        options.setAutomaticReconnect(false); // 禁用Paho内部重连
        options.setConnectionTimeout(30);
        options.setMaxReconnectDelay(60000);  // 最大重连间隔60秒
        if (currentConfig.getUsername() != null) {
            options.setUserName(currentConfig.getUsername());
        }
        if (currentConfig.getPassword() != null) {
            options.setPassword(currentConfig.getPassword().toCharArray());
        }
        return options;
    }

    // 测试传来的配置是否能够连接成功
    public Map<String, Object> testConnection(MqttConfig config) {
        Map<String, Object> result = new HashMap<>();
        MqttClient testClient = null;

        try {
            // 验证必要参数
            if (config == null || config.getBrokerUrl() == null || config.getClientId() == null) {
                result.put("status", "fail");
                result.put("msg", "配置信息不完整");
                return result;
            }

            logger.info("开始测试连接: broker={}, clientId={}",
                    config.getBrokerUrl(), config.getClientId());

            // 创建测试用客户端（使用临时clientId避免冲突）
            String testClientId = config.getClientId() + "_test_" + System.currentTimeMillis();
            testClient = new MqttClient(config.getBrokerUrl(), testClientId);

            // 配置连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(7); // 10秒连接超时
            if (config.getUsername() != null) options.setUserName(config.getUsername());
            if (config.getPassword() != null) options.setPassword(config.getPassword().toCharArray());

            // 尝试连接
            testClient.connect(options);

            if (testClient.isConnected()) {
                logger.info("连接测试成功");
                result.put("status", "success");
                result.put("msg", "连接测试成功");
            } else {
                logger.error("连接测试失败：未知原因");
                result.put("status", "fail");
                result.put("msg", "连接测试失败");
            }
        } catch (MqttSecurityException e) {
            logger.error("认证失败: " + e.getMessage());
            result.put("status", "fail");
            result.put("msg", "认证失败: " + e.getMessage());
        } catch (MqttException e) {
            logger.error("连接失败: " + e.getMessage());
            result.put("status", "fail");
            result.put("msg", "连接失败: " + e.getMessage());
        } finally {
            // 确保测试连接被关闭
            if (testClient != null) {
                try {
                    if (testClient.isConnected()) {
                        testClient.disconnect();
                    }
                    testClient.close();
                } catch (MqttException e) {
                    logger.warn("测试连接关闭时出错: " + e.getMessage());
                }
            }
        }

        return result;
    }


    public MqttConfig getCurrentConfig() {
        return currentConfig;
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> result = new HashMap<>();
        try{
            result.put("currentConfig", currentConfig);
            result.put("isRunning", isRunning);
            result.put("status", "success");
            result.put("msg", "获取状态成功");
            return result;
        }catch (Exception e) {
            result.put("status", "fail");
            result.put("msg", "获取状态失败，服务器内部错误");
            return result;
        }
    }


}

