package com.RoadCloudVisualizationSystem.mapper;

import com.RoadCloudVisualizationSystem.entity.Permission;

public interface PermissionMapper {
    int deleteByPrimaryKey(String permissionid);

    int insert(Permission row);

    int insertSelective(Permission row);

    Permission selectByPrimaryKey(String permissionid);

    int updateByPrimaryKeySelective(Permission row);

    int updateByPrimaryKey(Permission row);
}