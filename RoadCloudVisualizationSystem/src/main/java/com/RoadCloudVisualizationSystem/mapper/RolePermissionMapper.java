package com.RoadCloudVisualizationSystem.mapper;

import java.util.List;
import java.util.Map;

import com.RoadCloudVisualizationSystem.entity.RolePermission;

public interface RolePermissionMapper {
    int deleteByPrimaryKey(String rpid);

    int insert(RolePermission row);

    int insertSelective(RolePermission row);

    RolePermission selectByPrimaryKey(String rpid);

    int updateByPrimaryKeySelective(RolePermission row);

    int updateByPrimaryKey(RolePermission row);

    List<RolePermission> findRolePermissionsByPermissionid(String permissionid);

    int deleteByPermissionidAndRoleid(String permissionid, String roleid);

    List<Map<String, Object>> getRolePermissionByroleId(String roleId);
}