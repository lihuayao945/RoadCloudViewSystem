package com.RoadCloudVisualizationSystem.service;

import com.RoadCloudVisualizationSystem.entity.Message;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

/**
* @author Administrator
* @description 针对表【message(消息)】的数据库操作Service
* @createDate 2025-04-09 13:11:11
*/
public interface MessageService extends IService<Message> {
    // 分页查询
    Map<String, Object> findByTimeAndTopic(String topic, String startTime, String endTime, Integer pageNum, Integer pageSize);

    // 插入数据
    Integer insertMessage(Message message);

    // 导出用户列表
    Map<String, Object> exportMessages(String topic, String startTime, String endTime);
}
