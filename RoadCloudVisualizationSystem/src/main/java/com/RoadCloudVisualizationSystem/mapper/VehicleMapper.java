package com.RoadCloudVisualizationSystem.mapper;

import java.util.List;
import java.util.Map;
import com.RoadCloudVisualizationSystem.entity.Vehicle;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author Administrator
 * @description 针对表【vehicle(车辆基础信息--状态)】的数据库操作Mapper
 * @createDate 2025-04-08 10:28:40
 * @Entity com.RoadCloudVisualizationSystem.entity.Vehicle
 */
@Mapper
public interface VehicleMapper extends BaseMapper<Vehicle> {

    // 获取车辆数据
    int getDataCount();

    List<Map<String, Object>> getVehiclesPage(@Param("pageSize") Integer pageSize, @Param("offset") int offset);

    // 获取车辆数据根据vehicleId
    int getDataCountByvehicleId(String vehicleId);

    List<Map<String, Object>> getVehiclesPageByvehicleId(@Param("vehicleId") String vehicleId,@Param("pageSize") Integer pageSize, @Param("offset") int offset);

    // DashBoard
    List<Map<String, Object>> getManagevehiclePage(@Param("pageSize") Integer pageSize, @Param("offset") int offset);

    // 删除车辆根据StateId
    int deleteVehicleByStateId(String stateId);

    // 删除车辆根据VehicleId
    int deleteVehicleByvehicleId(String vehicleId);

    // 获取要导出的数据
    List<Map<String, Object>> getExportFileVehicles(@Param("starttime") String starttime,@Param("endtime") String endtime, @Param("savedata") List<String> savedata);// 导出数据


    // 获取根据vehicleId和时间查询车辆数据的数据总数
    int getDataCountByvehicleIdAndTime(String vehicleId, String starttime, String endtime);
    // 根据vehicleId和时间查询车辆数据
    List<Map<String, Object>> getVehiclePageByvehicleIdAndTime(String vehicleId, String starttime, String endtime,Integer pageSize, int offset);

    List<Map<String, Object>> getVehicleStatus(@Param("vehicleId") String vehicleId);

    // 根据车辆id获取历史轨迹点以及时间戳
    List<Map<String, Object>> getVehicleHistoryTrack(@Param("vehicleId") String vehicleId);

    // 获取车辆速度区间以及数量
    List<Map<String, Object>> getVehicleSpeedRange();

    // 获取车辆数量
    int getVehicleCount();

    // 获取车辆最新信息列表
    List<Map<String, Object>> getVehicleNewestInfoList(Integer limit, Integer offset);

}
