package com.RoadCloudVisualizationSystem.service;

import com.RoadCloudVisualizationSystem.entity.Obj;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
* @author Administrator
* @description 针对表【obj】的数据库操作Service
* @createDate 2025-04-08 10:28:02
*/
public interface ObjService extends IService<Obj> {
    // 根据Rcuids导出数据
    Map<String, Object> exportRcuAndObjs(String rcuIds, String startTime, String endTime);

    // 根据objsflag导出数据
    Map<String, Object> exportObjs(String objsflags, String startTime, String endTime);

    // 根据时间戳范围统计 pedestrian 和 car 的数量
    Map<String, Object> getPedestrianAndCarCountByTimestamp(String timestamp);

    // 根据时间戳范围统计 car 的类型和数量
    Map<String, Object> getCarTypeAndCountByTimestamp(String startTime, String endTime);

    // 根据时间戳范围获取各地标流量数据
    Map<String, Object> getStreamByTimestamp(String startTime, String endTime);
}
