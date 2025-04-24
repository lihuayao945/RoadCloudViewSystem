package com.RoadCloudVisualizationSystem.service.impl;

import com.RoadCloudVisualizationSystem.entity.User;
import com.RoadCloudVisualizationSystem.enums.ObjType;
import com.RoadCloudVisualizationSystem.utils.ExcelUtils;
import com.RoadCloudVisualizationSystem.utils.UserInfoUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.RoadCloudVisualizationSystem.entity.Obj;
import com.RoadCloudVisualizationSystem.service.ObjService;
import com.RoadCloudVisualizationSystem.mapper.ObjMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【obj】的数据库操作Service实现
* @createDate 2025-04-08 10:28:02
*/
@Service
public class ObjServiceImpl extends ServiceImpl<ObjMapper, Obj>
    implements ObjService{

    @Autowired
    private ObjMapper objMapper;

    @Autowired
    private UserInfoUtil userInfoUtil;

    @Value("${exportFilePath}")
    private String exportFilePath;

    @Override
    public Map<String, Object> exportRcuAndObjs(String rcuIds, String startTime, String endTime) {
        Map<String, Object> result = new HashMap<>();
        List<String> rcuidList = Arrays.asList(rcuIds.split(","));
        // 根据时间范围和车辆数据获取要保存的车辆信息
        List<Map<String, Object>> saveRcuAndObjs = objMapper.selectObjsByRcuIdsAndTimeRange(rcuidList, startTime, endTime);
        if (saveRcuAndObjs != null && !saveRcuAndObjs.isEmpty()) {
            result.put("status", "success");
            // 获取当前时间戳
            long timestamp = System.currentTimeMillis();
            // 文件后缀
            String username = userInfoUtil.getCurrentUsername();
            String fileSuffix = "rcuObjs_" + username + timestamp + ".xlsx";
            String filename = exportFilePath + fileSuffix;
            try {
                ExcelUtils.exportToExcel(saveRcuAndObjs, filename);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(filename);
            result.put("filepath", "/exports/" + fileSuffix); // 将文件路径添加到结果中
        } else {
            result.put("status", "fail");
        }

        return result;
    }

    @Override
    public Map<String, Object> exportObjs(String objsflags, String startTime, String endTime) {
        Map<String, Object> result = new HashMap<>();
        List<String> objsflagsList = Arrays.asList(objsflags.split(","));
        // 根据时间范围和车辆数据获取要保存的车辆信息
        List<Map<String, Object>> saveObjs = objMapper.selectObjsByObjsflagsAndTimeRange(objsflagsList, startTime, endTime);
        if (saveObjs != null && !saveObjs.isEmpty()) {
            result.put("status", "success");
            // 获取当前时间戳
            long timestamp = System.currentTimeMillis();
            // 文件后缀
            String username = userInfoUtil.getCurrentUsername();
            String fileSuffix = "objs_" + username + timestamp + ".xlsx";
            String filename = exportFilePath + fileSuffix;
            try {
                ExcelUtils.exportToExcel(saveObjs, filename);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(filename);
            result.put("filepath", "/exports/" + fileSuffix); // 将文件路径添加到结果中
        } else {
            result.put("status", "fail");
        }

        return result;
    }

    @Override
    public Map<String, Object> getPedestrianAndCarCountByTimestamp(String timestamp) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 生成现在startTime和endTime，开始时间是5s前，结束时间是现在
            String nowStartTime = String.valueOf(Long.parseLong(timestamp) - 1000 * 60);
            Map<String, Object> pedestrianAndCarCountByTimestamp = objMapper.getPedestrianAndCarCountByTimestamp(nowStartTime, timestamp);
            Integer vehicleCount = ((Number) pedestrianAndCarCountByTimestamp.get("vehicleCount")).intValue();
            Integer pedestrianCount = ((Number) pedestrianAndCarCountByTimestamp.get("pedestrianCount")).intValue();
            result.put("vehicleCount", vehicleCount);
            result.put("pedestrianCount", pedestrianCount);
            // 生成10s前的
            String tenEndTime= String.valueOf(Long.parseLong(timestamp) - 10000);
            String tenStartTime = String.valueOf(Long.parseLong(timestamp) - 10000 - 1000 * 60);
            Map<String, Object> pedestrianAndCarCountByTenTimestamp = objMapper.getPedestrianAndCarCountByTimestamp(tenStartTime, tenEndTime);
            Integer tenVehicleCount = ((Number) pedestrianAndCarCountByTenTimestamp.get("vehicleCount")).intValue();
            Integer tenPedestrianCount = ((Number) pedestrianAndCarCountByTenTimestamp.get("pedestrianCount")).intValue();
            result.put("vehicleDiff", vehicleCount - tenVehicleCount);
            result.put("pedestrianDiff", pedestrianCount - tenPedestrianCount);
            result.put("status", "success");
        }
        catch (Exception e) {
            result.put("status", "fail");
            System.out.println(e);
            throw new RuntimeException(e);
        }
        finally {
            return result;
        }
    }

    // 根据时间范围分段获取车辆类型和数量
    @Override
    public Map<String, Object> getCarTypeAndCountByTimestamp(String startTime, String endTime) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> carTypeAndCountByTimestamp = objMapper.getCarTypeAndCountByTimestamp(startTime, endTime);
            // 将不同的type分为不同的map，每一个map中有一个type类型和对应的count列表
            // 创建按类型分组的Map，每个类型对应一个计数列表
            Map<String, List<Integer>> typeCountMap = new HashMap<>();

            // 首先确定所有可能的类型列名
            List<String> typeColumns = new ArrayList<>();
            for (Map.Entry<String, Object> entry : carTypeAndCountByTimestamp.get(0).entrySet()) {
                if (entry.getKey().startsWith("type1") || entry.getKey().startsWith("type2") || entry.getKey().startsWith("type7")) {
                    typeColumns.add(entry.getKey());
                }
            }

            // 初始化每个类型的计数列表
            for (String typeColumn : typeColumns) {

                typeCountMap.put(typeColumn, new ArrayList<>());
            }

            // 填充每个类型的计数列表
            for (Map<String, Object> row : carTypeAndCountByTimestamp) {
                for (String typeColumn : typeColumns) {
                    Integer count = ((Number) row.get(typeColumn)).intValue();
                    typeCountMap.get(typeColumn).add(count);
                }
            }
            // 创建新的Map来存储转换后的结果
            Map<String, List<Integer>> convertedMap = new HashMap<>();

            // 遍历原Map并转换键名
            for (Map.Entry<String, List<Integer>> entry : typeCountMap.entrySet()) {
                String typeName = entry.getKey().replace("type", "");
                String chineseType = ObjType.fromCode(Integer.parseInt(typeName)).getType();
                convertedMap.put(chineseType, entry.getValue());
            }

            // 清空原Map并放入转换后的结果
            typeCountMap.clear();
            typeCountMap.putAll(convertedMap);
            result.put("status", "success");
            result.put("carTypeAndCount", typeCountMap);
            return result;
        }
        catch (Exception e) {
            result.put("status", "fail");
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> getStreamByTimestamp(String startTime, String endTime) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取原始数据
            List<Map<String, Object>> streamByTimestamp = objMapper.getStreamByTimestamp(startTime, endTime);

            // 创建地标到设备ID的映射关系
            Map<String, List<String>> landmarkDevices = new HashMap<>();
            landmarkDevices.put("解放碑", Arrays.asList("U-WZ000R", "U-WZ000S"));
            landmarkDevices.put("临江", Arrays.asList("U-WZ0012", "U-WZ0013", "U-WZ0015"));
            landmarkDevices.put("三峡广场", Arrays.asList("U-WZ0019", "U-WZ001B"));
            landmarkDevices.put("谢家湾", Collections.singletonList("U-WZ004K"));
            landmarkDevices.put("石坪桥", Arrays.asList("U-ZS000U", "U-ZS0013"));

            // 初始化地标流量统计
            Map<String, Integer> landmarkStreams = new HashMap<>();
            landmarkDevices.keySet().forEach(landmark -> landmarkStreams.put(landmark, 0));

            // 统计各设备流量并归类到地标
            for (Map<String, Object> record : streamByTimestamp) {
                String deviceId = (String) record.get("rcuId");
                Number streamCount = (Number) record.get("stream");

                // 查找设备所属地标
                String landmark = landmarkDevices.entrySet().stream()
                        .filter(entry -> entry.getValue().contains(deviceId))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);

                if (landmark != null) {
                    landmarkStreams.merge(landmark, streamCount.intValue(), Integer::sum);
                }
            }

            // 构建最终结果列表
            List<Map<String, Object>> streams = landmarkStreams.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("landmark", entry.getKey());
                        item.put("stream", entry.getValue());
                        return item;
                    })
                    .collect(Collectors.toList());

            // 构建返回结果
            result.put("status", "success");
            result.put("streams", streams);

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        return result;
    }

}




