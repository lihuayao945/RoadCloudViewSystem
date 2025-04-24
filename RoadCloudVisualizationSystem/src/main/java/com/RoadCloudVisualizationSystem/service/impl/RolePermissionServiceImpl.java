package com.RoadCloudVisualizationSystem.service.impl;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.RoadCloudVisualizationSystem.entity.RolePermission;
import com.RoadCloudVisualizationSystem.mapper.RolePermissionMapper;
import com.RoadCloudVisualizationSystem.service.RolePermissionService;

@Service
public class RolePermissionServiceImpl implements RolePermissionService {

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Override
    public Map<String, Object> getRolePermissionByroleId(String roleId) {
        List<Map<String ,Object>> list = rolePermissionMapper.getRolePermissionByroleId(roleId);
        Map<String,Object> result = new HashMap<>();
        if (list.size() > 0) {
            result.put("status", "success");
            result.put("rows", list);
        }
        else{
            result.put("status", "fail");
        }
        return result;
    }

    @Override
    public Map<String, Object> addRolePermission(String roleId, String permissiondata) {
        // 先用 & 拆分参数
        String[] parts = permissiondata.split("&");
        String roleIdString = parts[0].split("=")[1];
        //System.out.println(roleIdString);
        String permissiondataString = parts[1].split("=")[1];
        //System.out.println(permissiondataString);

        Map<String, Object> result = new HashMap<>();
        String[] all = permissiondataString.split("%2C");
        for(String demo : all){
            RolePermission rolePermission = new RolePermission();
            //System.out.println(demo);
            String Random = UUID.randomUUID().toString().replace("-", "").substring(6, 12);
            //System.out.println(Random);
            String time = String.valueOf(System.currentTimeMillis()) + Random;
            rolePermission.setRpid(time);
            rolePermission.setRoleid(roleIdString);
            rolePermission.setPermissionid(demo);
            rolePermission.setDetail("系统自动生成");
            rolePermissionMapper.insert(rolePermission);
        }
        result.put("status", "success");
        return result;
    }

    @Override
    public Map<String, Object> deleteRolePermission(String roleId, String permissiondata) {

         Map<String, Object> result = new HashMap<>();
         String[] all = permissiondata.split(",");
         for(String demo : all){
            int a = rolePermissionMapper.deleteByPermissionidAndRoleid(demo, roleId);
            if(a == 0){
                result.put("status", "fail");
                return result;
            }
            //System.out.println(demo);
         }
         result.put("status", "success");
         return result;

    }
    
}
