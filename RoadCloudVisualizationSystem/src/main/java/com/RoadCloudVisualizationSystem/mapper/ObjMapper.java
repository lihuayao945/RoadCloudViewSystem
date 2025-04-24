package com.RoadCloudVisualizationSystem.mapper;

import com.RoadCloudVisualizationSystem.entity.Obj;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
* @author Administrator
* @description 针对表【obj】的数据库操作Mapper
* @createDate 2025-04-08 10:28:02
* @Entity com.RoadCloudVisualizationSystem.entity.Obj
*/
public interface ObjMapper extends BaseMapper<Obj> {
    // 通过rucid删除信息
    Integer deleteObjsByRcuId(String rcuId);

    // 通过objsflag删除obj信息
    Integer deleteObjsByObjsflag(String objsflag);

    // 根据objsflag和uuid删除某条rcu信息中的某个对象信息
    Integer deleteObjByObjsflagAndObjId(String objsflag, String objId);

    // 插入数据
    int insertObj(Obj obj);

    // 根据rcuId和timeRange查询所有对象数据
    List<Map<String, Object>> selectObjsByRcuIdsAndTimeRange(
            @Param("rcuIds") List<String> rcuIds,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    List<Map<String, Object>> selectObjsByObjsflagsAndTimeRange(
            @Param("objsflags") List<String> objsflags,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);


    //根据rcuid和时间范围获取该路口的车辆类型和数量、比例
    List<Map<String, Object>> GetPicTwoList(@Param("rcuId") String rcuId, @Param("startTime") String startTime, @Param("endTime") String endTime);

    //根据rcuid和时间范围获取车流量
    List<Map<String, Object>> GetPicOneList(@Param("rcuId") String rcuId, @Param("startTime") Long startTime, @Param("endTime") Long endTime);

    //根据时间范围获取该路口的总流量数
    List<Map<String, Object>> GetPicTwoListAll(@Param("startTime") String startTime, @Param("endTime") String endTime);

    //根据时间范围获取车流量
    List<Map<String, Object>> GetPicOneListAll(@Param("startTime") Long startTime, @Param("endTime") Long endTime);

    // 查询最新和最久时间
    Map<String, String> getMaxAndMinTime();

    // 根据时间戳查询行人数量和车辆总数量
    Map<String, Object> getPedestrianAndCarCountByTimestamp(@Param("startTime") String startTime, @Param("endTime") String endTime);

    // 根据时间范围查询各个设备的流量
    List<Map<String, Object>> getStreamByTimestamp(@Param("startTime") String startTime, @Param("endTime") String endTime);


    // 根据时间戳查询车辆类型和数量
    List<Map<String, Object>> getCarTypeAndCountByTimestamp(@Param("startTime") String startTime, @Param("endTime") String endTime);
}




