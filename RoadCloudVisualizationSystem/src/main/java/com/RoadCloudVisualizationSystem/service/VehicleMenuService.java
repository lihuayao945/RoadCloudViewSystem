package com.RoadCloudVisualizationSystem.service;

import java.util.Map;

public interface VehicleMenuService {


    Map<String, Object> getVehicleMenuPage(String vehicleId, Integer pageNum, Integer pageSize);

   Map<String, Object> getDeviceNumList();
    
} 