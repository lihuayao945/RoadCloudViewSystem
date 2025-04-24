package com.RoadCloudVisualizationSystem.mapper;

import com.RoadCloudVisualizationSystem.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;
import java.util.Map;

/**
* @author Administrator
* @description 针对表【message(消息)】的数据库操作Mapper
* @createDate 2025-04-09 13:11:11
* @Entity com.RoadCloudVisualizationSystem.entity.Message
*/
public interface MessageMapper extends BaseMapper<Message> {
    // 根据时间、topic分页查询数据
    List<Message> findByTimeAndTopic(String topic, String startTime, String endTime, Integer limit, Integer offset);

    // 统计有主题消息总数
    Integer countTotal(String topic, String startTime, String endTime);

    // 根据时间分页查询数据
    List<Message> findByTime(String startTime, String endTime, Integer limit, Integer offset);

    // 统计无主题总数居
    Integer countTotalWithoutTopic(String startTime, String endTime);

    // 插入数据
    Integer insertMessage(Message message);

    // 根据时间、topic查询所有数据
    List<Map<String, Object>> findAllByTimeAndTopic(String topic, String startTime, String endTime);


    // 根据时间查询数据
    List<Map<String, Object>> findAllByTime(String startTime, String endTime);
}




