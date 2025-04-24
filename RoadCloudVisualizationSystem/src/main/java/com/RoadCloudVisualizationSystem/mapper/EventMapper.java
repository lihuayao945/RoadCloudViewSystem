package com.RoadCloudVisualizationSystem.mapper;

import com.RoadCloudVisualizationSystem.entity.Event;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author Administrator
* @description 针对表【event(路测事件)】的数据库操作Mapper
* @createDate 2025-04-13 23:05:31
* @Entity com.RoadCloudVisualizationSystem.entity.Event
*/
public interface EventMapper extends BaseMapper<Event> {

    // 查询所有isView为0的事件
    List<Event> getEventByIsView();

    //更新所有event的timestamp为当前时间
    int updateEventTimestamp(String nowTimestamp);

    // 随机更新所有数据的isView字段
    int updateIsViewRand();
}




