package com.RoadCloudVisualizationSystem.service.impl;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.RoadCloudVisualizationSystem.service.PermissionService;

@Service
public class PermissionServiceImpl implements PermissionService{

    @Override
    public Map<String, Object> getPermissionPage(Integer pageNum, Integer pageSize) {
        throw new UnsupportedOperationException("Unimplemented method 'getPermissionPage'");
    }
}