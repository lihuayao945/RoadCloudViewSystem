package com.RoadCloudVisualizationSystem.mapper;

import com.RoadCloudVisualizationSystem.entity.Rcu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;
import java.util.Map;

/**
* @author Administrator
* @description 针对表【rcu】的数据库操作Mapper
* @createDate 2025-04-11 23:14:09
* @Entity com.RoadCloudVisualizationSystem.entity.Rcu
*/
public interface RcuMapper extends BaseMapper<Rcu> {
    // 根据rcuId查询设备
    Rcu getRcuByRcuId(String rcuId);

    // 根据rcuId更新设备的receiveTime
    int updateReceiveTimeByRcuId(String rcuId, String receiveTime);

    // 插入rcu设备
    int insertRcu(Rcu rcu);

    // 更新某些设备的时间戳
    int updateReceiveTime(String receiveTime);

    // 查询receiveTime在某时间范围内的列表，用于统计异常设备列表
    List<Rcu> getRcuByReceiveTime(String startTime, String endTime);

    // 获取设备列表
    List<Map<String, Object>> getDeviceNumList();

    // 获取设备总数量
    int getDeviceCount();

    // 分页查询设备列表
    List<Rcu> getDeviceListPage(int limit, int offset);

    // 计数
    int getDeviceListCount();

    // 根据rcuid查询设备信息
    Map<String, Object> getRcuInfoByRcuId(String rcuId);
}




