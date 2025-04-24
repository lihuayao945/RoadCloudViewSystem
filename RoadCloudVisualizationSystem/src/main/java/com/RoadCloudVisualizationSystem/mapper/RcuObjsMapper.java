package com.RoadCloudVisualizationSystem.mapper;

import com.RoadCloudVisualizationSystem.entity.Obj;
import com.RoadCloudVisualizationSystem.entity.RcuObjs;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;
import java.util.Map;

/**
* @author Administrator
* @description 针对表【rcu_objs(RCU感知信息表)】的数据库操作Mapper
* @createDate 2025-04-08 10:28:35
* @Entity com.RoadCloudVisualizationSystem.entity.RcuObjs
*/
public interface RcuObjsMapper extends BaseMapper<RcuObjs> {
    // 根据时间范围、rcuid分页查询数据
    List<RcuObjs> getRcuObjsPageByRcuId(String rcuId, Integer limit, Integer offset);

    // 统计有多少各rcu设备
    Integer countRcu(String rcuId);

    // 根据时间范围、rcuid分页查询数据
    List<RcuObjs> getRcuObjsPageByRcuIdAndTime(String rcuId, String startTime, String endTime, Integer limit, Integer offset);

    // 统计有多少个数据
    Integer countRcuObjsByRcuIdAndTime(String rcuId, String startTime, String endTime);

    // 根据objsflag分页查询Obj
    List<Obj> getObjsPageByObjsflag(String objsflag, Integer limit, Integer offset);

    // 根据objsflag查询的数量
    Integer countObjsByObjsflag(String objsflag);

    // 根据rcuid删除该设备以及其相关所有信息
    Integer deleteRcuByRcuId(String rcuId);

    // 插入数据
    int insertRcuObjs(RcuObjs rcuObjs);

    // 根据objsflag查询rcuobjs
    RcuObjs getRcuObjsByObjsflag(String objsflag);

}




