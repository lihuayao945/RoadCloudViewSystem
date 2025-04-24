package com.RoadCloudVisualizationSystem.service.impl;

import com.RoadCloudVisualizationSystem.entity.User;
import com.RoadCloudVisualizationSystem.utils.UserInfoUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.RoadCloudVisualizationSystem.entity.Vehicle;
import com.RoadCloudVisualizationSystem.service.VehicleService;
import com.RoadCloudVisualizationSystem.utils.ExcelUtils;
import com.RoadCloudVisualizationSystem.mapper.VehicleMapper;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;

/**
 * @author Administrator
 * @description 针对表【vehicle(车辆基础信息--状态)】的数据库操作Service实现
 * @createDate 2025-04-08 10:28:40
 */
@Service
public class VehicleServiceImpl extends ServiceImpl<VehicleMapper, Vehicle> implements VehicleService {
    @Autowired
    private VehicleMapper vehicleMapper;

    @Autowired
    private UserInfoUtil userInfoUtil;

    @Value("${file.xlsxssavepath}")
    private String filepath;

    @Value("${exportFilePath}")
    private String exportFilePath;

    @Cacheable(value = "vehiclesCache", key = "'vehicles_' + #pageNum + '_' + #pageSize")
    @Override // 查询车辆(所有车辆)数据
    public Map<String, Object> getVehiclesPage(Integer pageNum, Integer pageSize) {
        System.out.println("getVehiclesPage called with pageNum: " + pageNum + " and pageSize: " + pageSize);
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> list = vehicleMapper.getVehiclesPage(pageSize, offset);
        Integer total = vehicleMapper.getDataCount();
        Map<String, Object> result = new HashMap<>();
        if (list.size() > 0) {
            result.put("status", "success");
            result.put("rows", list);
            result.put("total", total);
        } else {
            result.put("status", "fail");
        }
        return result;
    }

    @Override // 根据vehicleId模糊查询车辆数据
    public Map<String, Object> getVehiclePageByvehicleId(String vehicleId, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> vehicle = vehicleMapper.getVehiclesPageByvehicleId(vehicleId, pageSize, offset);
        Integer total = vehicleMapper.getDataCountByvehicleId(vehicleId);
        Map<String, Object> result = new HashMap<>();
        if (vehicle.size() > 0) {
            result.put("status", "success");
            result.put("total", total);
            result.put("rows", vehicle);
        } else {
            result.put("status", "fail");
        }
        return result;
    }

    @Override // 获取车辆管理界面所需数据(vehicleId、position数量、数据条数)
    public Map<String, Object> getManagevehiclePage(Integer pageNum, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> vehicleKinds = vehicleMapper.getManagevehiclePage(pageSize, offset);
        List<Map<String, Object>> vehicleList = new ArrayList<>();
        for (Map<String, Object> vehicleInfo : vehicleKinds) {
            vehicleList.add(vehicleInfo);
        }
        if (vehicleKinds.size() > 0) {
            result.put("status", "success");
            result.put("total", vehicleMapper.getVehicleCount());
            result.put("rows", vehicleList);
        } else {
            result.put("status", "fail");
        }
        return result;
    }

    @Override // 根据stateId删除车辆
    public Map<String, Object> deleteVehicleByStateId(String stateId) {
        Map<String, Object> result = new HashMap<>();
        int count = vehicleMapper.deleteVehicleByStateId(stateId);
        if (count > 0) {
            result.put("status", "success");
        } else {
            result.put("status", "fail");
        }
        return result;
    }

    @Override // 根据plateNo删除车辆(多条数据请慎重)
    public Map<String, Object> deleteVehicleByvehicleId(String vehicleId) {
        Map<String, Object> result = new HashMap<>();
        int count = vehicleMapper.deleteVehicleByvehicleId(vehicleId);
        if (count > 0) {
            result.put("status", "success");
        } else {
            result.put("status", "fail");
        }
        return result;
    }

    @Override // 导出数据
    public Map<String, Object> exportVehicle(String starttime, String endtime, String savedata) {
        Map<String, Object> result = new HashMap<>();
        List<String> plateNos = Arrays.asList(savedata.split(","));
        // 根据时间范围和车辆数据获取要保存的车辆信息
        List<Map<String, Object>> saveVehicles = vehicleMapper.getExportFileVehicles(starttime, endtime, plateNos);
        if (saveVehicles != null && !saveVehicles.isEmpty()) {
            // 获取当前时间戳
            long timestamp = System.currentTimeMillis();
            // 文件后缀
            String username = userInfoUtil.getCurrentUsername();
            String fileSuffix = "vehicle_" + username + timestamp + ".xlsx";
            String filename = exportFilePath + fileSuffix;
            try {
                ExcelUtils.exportToExcel(saveVehicles, filename);
            } catch (Exception e) {
                e.printStackTrace();
            }
            result.put("status", "success");
            result.put("filepath", "/exports/" + fileSuffix); // 将文件路径添加到结果中
        } else {
            result.put("status", "fail");
        }

        return result;
    }

    @Override // 根据vehicleId和时间查询车辆数据
    public Map<String, Object> getVehiclePageByvehicleIdAndTime(String vehicleId, String starttime, String endtime, Integer pageNum, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> vehicle = vehicleMapper.getVehiclePageByvehicleIdAndTime(vehicleId, starttime, endtime, pageSize, offset);
        Integer total = vehicleMapper.getDataCountByvehicleIdAndTime(vehicleId, starttime, endtime);
        if (vehicle.size() > 0) {
            result.put("status", "success");
            result.put("total", total);
            result.put("rows", vehicle);
        } else {
            result.put("status", "fail");
        }
        return result;
    }

    @Override 
    public Map<String, Object> getVehicleStatus(String vehicleId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> vehicle = vehicleMapper.getVehicleStatus(vehicleId);
        // 获取轨迹点和时间戳
        List<Map<String, Object>> trackPoints = vehicleMapper.getVehicleHistoryTrack(vehicleId);
        if (vehicle.size() > 0) {
            result.put("status", "success");
            result.put("vehicle", vehicle);
            result.put("trackPoints", trackPoints);
        } else {
            result.put("status", "fail");
        }

        return result;
    }

    @Override
    public Map<String, Object> getVehicleSpeedRange() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> vehicleSpeedRange = vehicleMapper.getVehicleSpeedRange();
            result.put("status", "success");
            result.put("vehicleSpeedRange", vehicleSpeedRange);
        } catch (Exception e) {
            result.put("status", "fail");
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public Map<String, Object> getVehicleListPage(Integer pageNum, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> vehicleList = vehicleMapper.getVehicleNewestInfoList(pageSize, (pageNum - 1) * pageSize);
            result.put("status", "success");
            result.put("total", vehicleMapper.getVehicleCount());
            result.put("rows", vehicleList);
            return result;
        }
        catch (Exception e){
            result.put("status", "fail");
            return result;
        }
    }


}
