package com.RoadCloudVisualizationSystem.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.RoadCloudVisualizationSystem.entity.Obj;
import com.RoadCloudVisualizationSystem.entity.Role;
import com.RoadCloudVisualizationSystem.mapper.RoleMapper;
import com.RoadCloudVisualizationSystem.service.RoleService;

@Service
public class RoleServiceImpl implements RoleService {
    
   @Autowired
   private RoleMapper roleMapper;

   @Override
   public Map<String, Object> getRolePage(Integer pageNum, Integer pageSize) {
       int offset = (pageNum - 1) * pageSize;
       int count = roleMapper.getRolePageCount();
       List<Map<String,Object>> list = roleMapper.getRolePage(pageSize, offset);
       Map<String, Object> result = new HashMap<>();
       if(list.size() > 0){
           result.put("status", "success");
           result.put("total", count);
           result.put("rows", list);
       }else{
           result.put("status", "fail");
       }
       return result;
   }

   @Override
   public Map<String, Object> addRole(Role role) {
       Map<String, Object> result = new HashMap<>();
       long timestamp = System.currentTimeMillis();
       role.setRoleid(String.valueOf(timestamp));
       int resultid = roleMapper.insert(role);
       if(resultid > 0){
           result.put("status", "success");
       }else{
           result.put("status", "fail");
       }
       return result;
   }


   
   @Override
   public Map<String, Object> deleteRoleById(String roleId) {
       int ret = roleMapper.deleteByPrimaryKey(roleId);
       Map<String,Object> result = new HashMap();
       if (ret ==0) {
        result.put("status", "fail");
       }
       else{
        result.put("status", "success");
       }
       return result;
   }
   
   
   
   

   
   
    
    
    
    
    
}