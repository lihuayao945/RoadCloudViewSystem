package com.RoadCloudVisualizationSystem.service;

import com.RoadCloudVisualizationSystem.entity.Obj;
import com.RoadCloudVisualizationSystem.entity.RcuObjs;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
* @author Administrator
* @description 针对表【rcu_objs(RCU感知信息表)】的数据库操作Service
* @createDate 2025-04-08 10:28:35
*/
public interface RcuObjsService extends IService<RcuObjs> {
    // 根据时间范围、rcuid分页查询数据
    Map<String, Object> getRcuObjsPageByRcuId(String rcuId, Integer pageNum, Integer pageSize);

    Map<String, Object> getRcuObjsPageByRcuIdAndTime(String rcuId, String startTime, String endTime, Integer pageNum, Integer pageSize);

    Map<String, Object> getObjsPageByObjsflag(String objsflag, Integer pageNum, Integer pageSize);

    // 删除所有rcuId下的所有obj
    Map<String, Object> deleteRcuByRcuId(String rcuId);

    // 删除所有objsflag下的所有obj
    Map<String, Object> deleteObjsByObjsflag(String objsflag);

    // 删除rcuId下的objsflag下的单个obj
    Map<String, Object> deleteObjByObjsflagAndUuid(String objsflag, String uuid);
}
