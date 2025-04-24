package com.RoadCloudVisualizationSystem.controller.system;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.RoadCloudVisualizationSystem.service.RolePermissionService;

@RestController
@RequestMapping("/system/role/permission")
public class SystemRolePermissionController {

    @Autowired
    private RolePermissionService rolePermissionService;


    @GetMapping
    public Map<String,Object> getRolePermissionByroleId(@RequestParam String roleId) {
        return rolePermissionService.getRolePermissionByroleId(roleId);
    }


    @PostMapping
    public Map<String,Object> addRolePermission(@RequestBody String roleId,@RequestBody String permissiondata) {
        return rolePermissionService.addRolePermission(roleId, permissiondata);
    }

    @DeleteMapping
    public Map<String,Object> deleteRolePermission(@RequestParam String roleId,@RequestParam String permissiondata) {
        return rolePermissionService.deleteRolePermission(roleId, permissiondata);
    }







    
}
