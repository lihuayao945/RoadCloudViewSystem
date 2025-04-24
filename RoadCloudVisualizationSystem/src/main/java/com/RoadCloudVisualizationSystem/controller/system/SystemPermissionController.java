package com.RoadCloudVisualizationSystem.controller.system;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.RoadCloudVisualizationSystem.service.PermissionService;

@RestController
@RequestMapping("/system/permission")
public class SystemPermissionController {
    
    @Autowired
    private PermissionService permissionService;

    @GetMapping("/list")
    public Map<String,Object> getPermissionPage(@RequestParam(defaultValue = "1") Integer pageNum,@RequestParam(defaultValue = "10") Integer pageSize) {
        return permissionService.getPermissionPage(pageNum, pageSize);
    }



    
}
