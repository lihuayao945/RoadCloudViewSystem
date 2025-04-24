package com.RoadCloudVisualizationSystem.controller.system;


import com.RoadCloudVisualizationSystem.log.Log;
import com.RoadCloudVisualizationSystem.log.enuns.BusinessType;
import com.RoadCloudVisualizationSystem.mqtt.MqttService;
import com.RoadCloudVisualizationSystem.mqtt.dao.MqttConfig;
import com.RoadCloudVisualizationSystem.service.MessageService;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/system/mqtt")
public class SystemMqttController {

    private final MqttService mqttService;

    @Autowired
    private MessageService messageService;

    public SystemMqttController(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    @Log(title = "查询mqtt订阅配置信息", businessType = BusinessType.QUERY)
    @GetMapping
    public Map<String, Object> getStatus() {
        return mqttService.getStatus();
    }

    @Log(title = "更新mqtt订阅配置信息", businessType = BusinessType.UPDATE)
    @PostMapping
    public Map<String, Object> setConfig(@RequestBody MqttConfig config) {
        return mqttService.updateConfig(config);
    }

    @Log(title = "开始订阅mqtt消息", businessType = BusinessType.STATUS)
    @GetMapping("/start")
    public Map<String, Object> start() throws MqttException {
        return mqttService.start();
    }

    @Log(title = "停止订阅mqtt消息", businessType = BusinessType.STATUS)
    @GetMapping("/stop")
    public Map<String, Object> stop() {
        try {
            return mqttService.stop();
        } catch (IllegalStateException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "fail");
            result.put("msg", "停止订阅失败， 服务器内部错误");
            return result;
        }
    }

    @Log(title = "分页查询消息列表", businessType = BusinessType.QUERY)
    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(required = false) String topic,
                                    @RequestParam String startTime,
                                    @RequestParam String endTime,
                                    @RequestParam Integer pageNum,
                                    @RequestParam Integer pageSize) {
        return messageService.findByTimeAndTopic(topic, startTime, endTime, pageNum, pageSize);
    }


    // 测试连接
    @Log(title = "测试mqtt订阅连通性", businessType = BusinessType.OTHER)
    @PostMapping("/testConnection")
    public Map<String, Object> testConnection(@RequestBody MqttConfig config) {
        return mqttService.testConnection(config);
    }

    // 导出消息列表
    @Log(title = "根据时间范围导出消息列表", businessType = BusinessType.OTHER)
    @GetMapping("/export")
    public Map<String, Object> exportMessage(@RequestParam(required = false) String topic,
                                             @RequestParam String startTime,
                                             @RequestParam String endTime) {
        return messageService.exportMessages(topic, startTime, endTime);
    }
}