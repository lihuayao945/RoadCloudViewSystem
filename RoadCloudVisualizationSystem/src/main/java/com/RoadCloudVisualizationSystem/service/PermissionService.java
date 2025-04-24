package com.RoadCloudVisualizationSystem.service;

import java.util.Map;

public interface PermissionService {
    Map<String,Object> getPermissionPage(Integer pageNum,Integer pageSize);
}
