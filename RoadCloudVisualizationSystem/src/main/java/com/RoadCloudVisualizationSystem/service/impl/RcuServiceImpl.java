package com.RoadCloudVisualizationSystem.service.impl;

import com.RoadCloudVisualizationSystem.enums.DeviceType;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.RoadCloudVisualizationSystem.entity.Rcu;
import com.RoadCloudVisualizationSystem.service.RcuService;
import com.RoadCloudVisualizationSystem.mapper.RcuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
* @author Administrator
* @description 针对表【rcu】的数据库操作Service实现
* @createDate 2025-04-11 23:14:09
*/
@Service
public class RcuServiceImpl extends ServiceImpl<RcuMapper, Rcu>
    implements RcuService{

    @Autowired
    private RcuMapper rcuMapper;

    @Override
    public Map<String, List<Integer>> getDeviceTypeCounts(Long nowTimestamp) {
        try {
            Long normalTime = nowTimestamp - 60 * 1000;
            Long abnormalTime = nowTimestamp - 60 * 60 * 1000;
            List<Rcu> normalRcuList = rcuMapper.getRcuByReceiveTime(String.valueOf(normalTime), String.valueOf(nowTimestamp));
            List<Rcu> abnormalRcuList = rcuMapper.getRcuByReceiveTime(String.valueOf(abnormalTime), String.valueOf(normalTime - 1));
            List<Rcu> offlineRcuList = rcuMapper.getRcuByReceiveTime("1", String.valueOf(abnormalTime - 1));

            // 初始化统计Map
            Map<String, Integer> normalCounts = new HashMap<>();
            Map<String, Integer> abnormalCounts = new HashMap<>();
            Map<String, Integer> offlineCounts = new HashMap<>();

            // 统计正常状态
            countDevices(normalRcuList, normalCounts);
            // 统计异常状态
            countDevices(abnormalRcuList, abnormalCounts);
            // 统计离线状态
            countDevices(offlineRcuList, offlineCounts);

            // 合并结果
            Set<String> allTypes = new HashSet<>();
            allTypes.addAll(normalCounts.keySet());
            allTypes.addAll(abnormalCounts.keySet());
            allTypes.addAll(offlineCounts.keySet());

            Map<String, List<Integer>> result = new HashMap<>();
            for (String type : allTypes) {
                int normal = normalCounts.getOrDefault(type, 0);
                int abnormal = abnormalCounts.getOrDefault(type, 0);
                int offline = offlineCounts.getOrDefault(type, 0);
                result.put(type, Arrays.asList(normal, abnormal, offline));
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("查询失败", e);
        }
    }

    @Override
    public Map<String, Object> getDeviceNumList() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> list = rcuMapper.getDeviceNumList();
            List<Map<String, Integer>> formattedList = new ArrayList<>();

            // 转换数据结构
            list.forEach(map -> {
                Map<String, Integer> deviceMap = new HashMap<>();
                if (map.containsKey("deviceType") && map.containsKey("count")) {
                    Object typeCode = map.get("deviceType");
                    try {
                        // 特殊处理设备类型映射
                        int code = Integer.parseInt(typeCode.toString());
                        String typeName = switch (code) {
                            case 0 -> "未知";          // 假设0对应rcu
                            case 1 -> "融合设备";
                            case 2 -> "摄像头";       // 假设2对应摄像头
                            case 3 -> "毫秒波雷达";
                            case 4 -> "激光雷达";        // 使用英文名称
                            default -> DeviceType.fromCode(code).getType();
                        };
                        deviceMap.put(typeName, Integer.parseInt(map.get("count").toString()));
                    } catch (Exception e) {
                        deviceMap.put("unknown", 1);
                    }
                }
                formattedList.add(deviceMap);
            });

            // 添加RCU总数（根据需求调整）
            Map<String, Integer> rcuMap = new HashMap<>();
            rcuMap.put("rcu", rcuMapper.getDeviceCount());
            formattedList.add(0, rcuMap);  // 放在列表首位

            if (!formattedList.isEmpty()) {
                result.put("status", "success");
                result.put("deviceNumList", formattedList);
            } else {
                result.put("status", "fail");
            }
        } catch (Exception e) {
            result.put("status", "fail");
        }
        return result;
    }

    // 获取异常设备列表
    @Override
    public Map<String, Object> getAbnormalDeviceList(Long nowTimestamp) {
        Map<String, Object> result = new HashMap<>();
        try{
            Long normalTime = nowTimestamp - 60 * 1000;
            Long abnormalTime = nowTimestamp - 60 * 60 * 1000;
            rcuMapper.updateReceiveTime(String.valueOf(nowTimestamp));
            List<Rcu> abnormalRcuList = rcuMapper.getRcuByReceiveTime(String.valueOf(abnormalTime), String.valueOf(normalTime - 1));
            List<Rcu> offlineRcuList = rcuMapper.getRcuByReceiveTime("1", String.valueOf(abnormalTime - 1));
            // 计算两个列表的长度之和
            int total = abnormalRcuList.size() + offlineRcuList.size();
            List<Map<String, String>> abnormalDevices = new ArrayList<>();
            // 遍历列表，统计异常和离线设备数量
            for (Rcu rcu : abnormalRcuList) {
                Map<String, String> deviceMap = new HashMap<>();
                String type = convertDeviceType(rcu.getDeviceType());
                deviceMap.put("deviceType", type);
                deviceMap.put("rcuId", rcu.getRcuId());
                deviceMap.put("abnormalType", "告警");
                deviceMap.put("latestTime", rcu.getReceiveTime());
                abnormalDevices.add(deviceMap);
            }
            for (Rcu rcu : offlineRcuList) {
                Map<String, String> deviceMap = new HashMap<>();
                String type = convertDeviceType(rcu.getDeviceType());
                deviceMap.put("deviceType", type);
                deviceMap.put("rcuId", rcu.getRcuId());
                deviceMap.put("abnormalType", "离线");
                deviceMap.put("latestTime", rcu.getReceiveTime());
                abnormalDevices.add(deviceMap);
            }
            result.put("status", "success");
            result.put("total", total);
            result.put("abnormalDevices", abnormalDevices);
        }
        catch (Exception e){
            result.put("status", "fail");
            return result;
        }
        return result;
    }

    @Override
    public Map<String, Object> getDeviceStatusByRcuId(String rcuId) {
        Map<String, Object> result = new HashMap<>();
        Long nowTimestamp = System.currentTimeMillis();
        try {
            Rcu rcu = rcuMapper.getRcuByRcuId(rcuId);
            if (rcu != null) {
                String deviceType = rcu.getDeviceType();
                rcu.setDeviceType(convertDeviceType(deviceType));
                result.put("status", "success");
                result.put("device", rcu);
                result.put("deviceStatus", determineAbnormalType(Long.parseLong(rcu.getReceiveTime()), nowTimestamp));
                return result;
            }
            result.put("status", "fail");
            result.put("message", "设备不存在");
            return result;
        }
        catch (Exception e){
            result.put("status", "fail");
            result.put("message", "服务器内部错误");
            return result;
        }
    }

    @Override
    public Map<String, Object> getRcuListPage(Integer pageNum, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        try {
            int total = rcuMapper.getDeviceListCount();
            List<Rcu> rcuList = rcuMapper.getDeviceListPage(pageSize, (pageNum - 1) * pageSize);
            result.put("status", "success");
            result.put("total", total);
            result.put("rows", rcuList);
            return result;
        }
        catch (Exception e){
            System.out.println(e);
            result.put("status", "fail");
            return result;
        }
    }

    @Override
    public Map<String, Object> getRcuInfoByRcuId(String rcuId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> rcuInfo = rcuMapper.getRcuInfoByRcuId(rcuId);
            System.out.println(rcuInfo);
            // 映射设备类型
            if (rcuInfo.containsKey("deviceType")) {
                String typeCode = rcuInfo.get("deviceType").toString();
                rcuInfo.put("deviceType", convertDeviceType(typeCode));
            }
            // 设置设备状态
            if (rcuInfo.containsKey("last_update_time")) {
                Long timestamp = Long.parseLong(rcuInfo.get("last_update_time").toString());
                rcuInfo.put("status", determineAbnormalType(timestamp, System.currentTimeMillis()));
            }
            result.put("status", "success");
            result.put("rcuInfo", rcuInfo);
            return result;
        }
        catch (Exception e){
            result.put("status", "fail");
            return result;
        }
    }


    // 设备类型转换方法
    private String convertDeviceType(String typeCode) {
        try {
            int code = Integer.parseInt(typeCode);
            return DeviceType.fromCode(code).getType();
        } catch (Exception e) {
            return "未知设备";
        }
    }

    // 异常类型判断方法（示例逻辑）
    private String determineAbnormalType(Long timestamp, Long nowTimestamp) {
        Long oneHour = 60 * 60 * 1000L;
        Long oneMinute = 60 * 1000L;

        // 如果超过1小时未更新视为离线
        if (nowTimestamp - timestamp > oneHour) {
            return "离线";
        }
        // 如果超过1分钟未更新视为异常
        if (nowTimestamp - timestamp > oneMinute) {
            return "告警";
        }
        return "正常";
    }


    private void countDevices(List<Rcu> rcuList, Map<String, Integer> countMap) {
        for (Rcu rcu : rcuList) {
            int code = Integer.parseInt(rcu.getDeviceType()); // 假设Rcu有getDeviceType方法返回设备类型代码
            DeviceType type = DeviceType.fromCode(code);
            String typeName = type.getType();
            countMap.put(typeName, countMap.getOrDefault(typeName, 0) + 1);
        }
    }
}




