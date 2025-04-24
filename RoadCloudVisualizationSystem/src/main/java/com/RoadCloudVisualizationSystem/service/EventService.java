package com.RoadCloudVisualizationSystem.service;

import com.RoadCloudVisualizationSystem.entity.Event;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
* @author Administrator
* @description 针对表【event(路测事件)】的数据库操作Service
* @createDate 2025-04-13 23:05:31
*/
public interface EventService extends IService<Event> {

    // 获取所有可展示事件
    public Map<String, Object> getEventByIsView();

}
