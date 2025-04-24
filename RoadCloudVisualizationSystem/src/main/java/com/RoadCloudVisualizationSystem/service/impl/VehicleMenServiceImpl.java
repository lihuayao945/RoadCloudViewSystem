package com.RoadCloudVisualizationSystem.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.RoadCloudVisualizationSystem.mapper.RcumenuMapper;
import com.RoadCloudVisualizationSystem.service.VehicleMenuService;

@Service
public class VehicleMenServiceImpl implements VehicleMenuService {


    @Autowired
    private RcumenuMapper rcumenuMapper;

    @Override
    public Map<String, Object> getVehicleMenuPage(String vehicleId, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> list = rcumenuMapper.getVehicleMenuPage(vehicleId, pageSize, offset);
        System.out.println(list);

        if (list.size() > 0) {
            result.put("status", "sucess");
            result.put("total", rcumenuMapper.getVehicleMenuPageCount());
            result.put("rows", list);
        } else {
            result.put("status", "fail");
        }

        return result;
    }



    @Override
    public Map<String, Object> getDeviceNumList() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> list = rcumenuMapper.getDeviceNumList();
        System.out.println(list);
        if (list.size() > 0) {
            result.put("status", "sucess");
            result.put("deviceNumList", list);
        } else {
            result.put("status", "fail");
        }
        return result;
    }
}
