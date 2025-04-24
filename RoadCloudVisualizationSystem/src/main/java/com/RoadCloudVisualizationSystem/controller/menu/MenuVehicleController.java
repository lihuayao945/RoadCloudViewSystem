package com.RoadCloudVisualizationSystem.controller.menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.RoadCloudVisualizationSystem.enums.ObjType;
import com.RoadCloudVisualizationSystem.mapper.ObjMapper;
import com.RoadCloudVisualizationSystem.service.ObjService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.RoadCloudVisualizationSystem.service.VehicleMenuService;
import com.RoadCloudVisualizationSystem.service.VehicleService;

@RestController
@RequestMapping("/menu/vehicle")
public class MenuVehicleController {
    @Autowired
    private VehicleService vehicleService;
    @Autowired
    private ObjMapper objMapper;
    @Autowired
    ObjService objService;

    @GetMapping
    public Map<String, Object> getVehicleStatus(@RequestParam String vehicleId) {
        return vehicleService.getVehicleStatus(vehicleId);
    }

    // 分页查询车辆列表
    @GetMapping("/list")
    public Map<String, Object> getVehicleList(@RequestParam Integer pageNum,
                                               @RequestParam Integer pageSize) {
        return  vehicleService.getVehicleListPage(pageNum, pageSize);
    }

    @GetMapping("/kindnum")
    public Map<String, Object> GetPicTwoList(@RequestParam String rcuId,@RequestParam(defaultValue = "1") String startTime,@RequestParam(defaultValue = "9223372036854775806") String endTime) {
        Map<String, Object> result = new HashMap<>();
        try{
            List<Map<String, Object>> data = objMapper.GetPicTwoList(rcuId, startTime, endTime);
            result.put("status", "success");

            // 处理数据，将type数字转换为对应的车辆类型名称
            for (Map<String, Object> item : data) {
                if (item.containsKey("type")) {
                    Object typeObj = item.get("type");
                    if (typeObj != null) {
                        try {
                            int typeCode = Integer.parseInt(typeObj.toString());
                            // 使用CarType枚举将数字编码转换为对应的车辆类型名称
                            String typeName = ObjType.fromCode(typeCode).getType();
                            item.put("typeName", typeName);
                        } catch (NumberFormatException e) {
                            // 如果转换失败，设置为未知类型
                            item.put("typeName", "未知");
                        }
                    }
                }
            }

            result.put("data", data);
        }catch (Exception e){
            result.put("status", "fail");
        }

        return result;
    }

    @GetMapping("/stream")
    public Map<String, Object> GetPicOneList(@RequestParam String rcuId,@RequestParam(defaultValue = "1724034655275") String startTime,@RequestParam(defaultValue = "1724034755275") String endTime){
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> data = objMapper.GetPicOneList(rcuId, Long.parseLong(startTime), Long.parseLong(endTime));
        if (data != null && !data.isEmpty()) {
            result.put("status", "success");
            result.put("data", data);
            return result;
        }
        result.put("status", "fail");
        return result;
    }

    // 分段统计所有路口各类型车辆的的流量数据以及比例
    @GetMapping("/kindnum/all")
    public Map<String, Object> GetPicTwoList(@RequestParam(defaultValue = "1") String startTime,@RequestParam(defaultValue = "9223372036854775806") String endTime) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> data = objMapper.GetPicTwoListAll(startTime, endTime);
        if (data != null && !data.isEmpty()) {
            result.put("status", "success");

            // 处理数据，将type数字转换为对应的车辆类型名称
            for (Map<String, Object> item : data) {
                if (item.containsKey("type")) {
                    Object typeObj = item.get("type");
                    if (typeObj != null) {
                        try {
                            int typeCode = Integer.parseInt(typeObj.toString());
                            // 使用CarType枚举将数字编码转换为对应的车辆类型名称
                            String typeName = ObjType.fromCode(typeCode).getType();
                            item.put("typeName", typeName);
                        } catch (NumberFormatException e) {
                            // 如果转换失败，设置为未知类型
                            item.put("typeName", "未知");
                        }
                    }
                }
            }

            result.put("data", data);
        }
        else {
            result.put("status", "fail");
        }
        return result;
    }

    // 分段获取所有路口流量数据
    @GetMapping("/stream/all")
    public Map<String, Object> GetAllStream(@RequestParam(defaultValue = "1724034655275") String startTime,@RequestParam(defaultValue = "1724034755275") String endTime){
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> data = objMapper.GetPicOneListAll(Long.parseLong(startTime), Long.parseLong(endTime));
        if (data != null && !data.isEmpty()) {
            result.put("status", "success");
            result.put("data", data);
            return result;
        }
        result.put("status", "fail");
        return result;
    }

    // 统计车流量、人流量以及与上次的差值
    @GetMapping("/count")
    public Map<String, Object> getPedestrianAndCarCountByTimestamp(@RequestParam(defaultValue = "1724034655275") String timestamp) {
        return objService.getPedestrianAndCarCountByTimestamp(timestamp);
    }

    // 对各个地标实现实时动态排名
    @GetMapping("/stream/rank")
    public Map<String, Object> getRankByTimestamp(@RequestParam(defaultValue = "1724034655275") String timestamp) {
        String startTime = String.valueOf(Long.parseLong(timestamp) - 1000 * 60);
        return objService.getStreamByTimestamp(startTime, timestamp);
    }

    // 获取车辆类型和数量-饼图
    @GetMapping("/carType/count")
    public Map<String, Object> getCarTypeAndCountByTimestamp(@RequestParam(defaultValue = "1744611830000") String startTime, @RequestParam(defaultValue = "1744615430489") String endTime) {
        return objService.getCarTypeAndCountByTimestamp(startTime, endTime);
    }

    // 统计车辆速度分布区间
    @GetMapping("/speed")
    public Map<String, Object> getSpeedRange() {
        return vehicleService.getVehicleSpeedRange();
    }
}
