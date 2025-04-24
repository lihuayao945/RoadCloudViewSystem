package com.RoadCloudVisualizationSystem.service;

import java.util.Map;

public interface RolePermissionService {
    Map<String, Object> getRolePermissionByroleId(String roleId);

    Map<String, Object> addRolePermission(String roleId, String permissiondata);

    Map<String, Object> deleteRolePermission(String roleId, String permissiondata);
}
