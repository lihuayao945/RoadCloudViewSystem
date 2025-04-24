package com.RoadCloudVisualizationSystem.mapper;

import java.util.List;
import java.util.Map;

import com.RoadCloudVisualizationSystem.entity.Role;

import io.lettuce.core.dynamic.annotation.Param;

public interface RoleMapper {
    int deleteByPrimaryKey(String roleid);

    int insert(Role row);

    int insertSelective(Role row);

    Role selectByPrimaryKey(String roleid);

    int updateByPrimaryKeySelective(Role row);

    int updateByPrimaryKey(Role row);


    int getRolePageCount();
    List<Map<String,Object>> getRolePage(@Param("pageSize") Integer pageSize, @Param("offset") Integer offset);   



    
}