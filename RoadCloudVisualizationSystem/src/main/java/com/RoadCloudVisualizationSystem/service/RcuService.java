package com.RoadCloudVisualizationSystem.service;

import com.RoadCloudVisualizationSystem.entity.Rcu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
* @author Administrator
* @description 针对表【rcu】的数据库操作Service
* @createDate 2025-04-11 23:14:09
*/
public interface RcuService extends IService<Rcu> {
    // 根据当前时间戳获取不同设备类型的数量
    Map<String, List<Integer>> getDeviceTypeCounts(Long nowTimestamp);

    // 获取设备列表
    Map<String, Object> getDeviceNumList();

    // 获取异常设备列表
    Map<String, Object> getAbnormalDeviceList(Long nowTimestamp);

    // 根据rcuid查询设备状态
    Map<String, Object> getDeviceStatusByRcuId(String rcuId);

    // 分页查询设备列表
    Map<String, Object> getRcuListPage(Integer pageNum, Integer pageSize);

    // 根据rcuId获取设备信息
    Map<String, Object> getRcuInfoByRcuId(String rcuId);
}
