package com.RoadCloudVisualizationSystem.controller.menu;

import com.RoadCloudVisualizationSystem.service.EventService;
import com.RoadCloudVisualizationSystem.service.RcuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/menu/device")
public class MenuDeviceController {
    @Autowired
    private RcuService rcuService;

    @Autowired
    private EventService eventService;


    // 查询设备类型和各自健康状态分布
    @GetMapping("/status")
    public Map<String, Object> getDeviceStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long timestamp = System.currentTimeMillis();
            Map<String, List<Integer>> deviceStatus = rcuService.getDeviceTypeCounts(timestamp);
            response.put("status", "success");
            response.put("deviceStatus", deviceStatus);
        } catch (Exception e) {
            response.put("status", "fail");
        }
        return response;
    }

    // 获取交通设备数量以及类型
    @GetMapping("/numList")
    public Map<String, Object> getDeviceNumList() {
        return rcuService.getDeviceNumList();
    }

    // 获取异常设备列表
    @GetMapping("/abnormal")
    public Map<String, Object> getAbnormalDeviceList() {
        return rcuService.getAbnormalDeviceList(System.currentTimeMillis());
    }

    // 根据rcuid查询设备状态，经纬度
    @GetMapping
    public Map<String, Object> getDeviceStatusByRcuId(String rcuId) {
        return rcuService.getDeviceStatusByRcuId(rcuId);
    }


    // 获取事件列表
    @GetMapping("/event")
    public Map<String, Object> getEventList() {
        return eventService.getEventByIsView();
    }

    // 分页查询rcu设备
    @GetMapping("/list")
    public Map<String, Object> getRcuList(@RequestParam Integer pageNum,
                                           @RequestParam Integer pageSize) {
        return rcuService.getRcuListPage(pageNum, pageSize);
    }

    // 根据rcuId获取设备信息
    @GetMapping("/rcuInfo")
    public Map<String, Object> getRcuInfoByRcuId(@RequestParam String rcuId) {
        return rcuService.getRcuInfoByRcuId(rcuId);
    }
}
