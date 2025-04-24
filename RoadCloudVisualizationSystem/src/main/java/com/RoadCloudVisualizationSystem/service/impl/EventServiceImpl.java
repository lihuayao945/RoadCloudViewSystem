package com.RoadCloudVisualizationSystem.service.impl;

import com.RoadCloudVisualizationSystem.enums.EventType;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.RoadCloudVisualizationSystem.entity.Event;
import com.RoadCloudVisualizationSystem.service.EventService;
import com.RoadCloudVisualizationSystem.mapper.EventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author Administrator
* @description 针对表【event(路测事件)】的数据库操作Service实现
* @createDate 2025-04-13 23:05:31
*/
@Service
public class EventServiceImpl extends ServiceImpl<EventMapper, Event>
    implements EventService{
    @Autowired
    private EventMapper eventMapper;

    @Override
    public Map<String, Object> getEventByIsView() {
        Map<String, Object> result = new HashMap<>();
        try{
            String nowTimestamp = System.currentTimeMillis() + "";
            eventMapper.updateEventTimestamp(nowTimestamp);
            List<Event> events = eventMapper.getEventByIsView();
            eventMapper.updateIsViewRand();
            // 对events中每个event的eventType进行转换
            for (Event event : events) {
                int eventType = event.getEventType();
                String description = EventType.getDescriptionByCode(eventType);
                event.setEventType(eventType);
                event.setDescription(description);
            }
            result.put("status", "success");
            result.put("total", events.size());
            result.put("events", events);
            return result;
        }
        catch (Exception e){
            result.put("status", "fail");
            return result;
        }
    }
}




