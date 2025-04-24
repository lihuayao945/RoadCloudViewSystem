package com.RoadCloudVisualizationSystem.service;

import java.util.Map;
import com.RoadCloudVisualizationSystem.entity.Vehicle;
import com.baomidou.mybatisplus.extension.service.IService;
/**
* @author Administrator
* @description 针对表【vehicle(车辆基础信息--状态)】的数据库操作Service
* @createDate 2025-04-08 10:28:40
*/
public interface VehicleService extends IService<Vehicle> {
    Map<String, Object> getVehiclesPage(Integer pageNum, Integer pageSize);//查询车辆(所有车辆)数据
    Map<String, Object> getVehiclePageByvehicleId(String vehicleId, Integer pageNum, Integer pageSize);//根据vehicleId模糊查询车辆数据
    Map<String, Object> getManagevehiclePage( Integer pageNum, Integer pageSize);//获取车辆管理界面所需数据(vehicleId、position数量、数据条数)
    Map<String, Object> deleteVehicleByStateId(String stateId);//根据stateId删除车辆
    Map<String, Object> deleteVehicleByvehicleId(String vehicleId);//根据plateNo删除车辆(多条数据请慎重)
    Map<String, Object> exportVehicle(String starttime, String endtime, String savedata);//导出数据
    Map<String, Object> getVehiclePageByvehicleIdAndTime(String vehicleId,String starttime,String endtime,Integer pageNum,Integer pageSize);//根据vehicleId和时间查询车辆数据
    Map<String, Object> getVehicleStatus(String vehicleId);

    // 统计车辆速度分布区间
    Map<String, Object> getVehicleSpeedRange();

    // 获取车辆最新信息列表
    Map<String, Object> getVehicleListPage(Integer pageNum, Integer pageSize);
}
