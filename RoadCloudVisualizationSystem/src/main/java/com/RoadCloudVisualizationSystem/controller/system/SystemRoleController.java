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

import com.RoadCloudVisualizationSystem.entity.Role;
import com.RoadCloudVisualizationSystem.service.RoleService;

@RestController
@RequestMapping("/system/role")
public class SystemRoleController {
    
    @Autowired
    private RoleService roleService;

    @GetMapping
    public Map<String,Object> getRolePage(@RequestParam(defaultValue = "1") Integer pageNum,@RequestParam(defaultValue = "10") Integer pageSize) {
        return roleService.getRolePage(pageNum, pageSize);
    }


    @PostMapping
    public Map<String,Object> addRole(@RequestBody Role role) {
        return roleService.addRole(role);
    }

    @DeleteMapping
    public Map<String,Object> deleteRoleById(@RequestParam String roleId) {
        return roleService.deleteRoleById(roleId);
    }
    
    
}
