package com.RoadCloudVisualizationSystem.service;

import java.util.Map;

import com.RoadCloudVisualizationSystem.entity.Role;

public interface RoleService {
    Map<String,Object> getRolePage(Integer pageNum,Integer pageSize);

    Map<String, Object> addRole(Role role);


    Map<String, Object> deleteRoleById(String roleId);

}
