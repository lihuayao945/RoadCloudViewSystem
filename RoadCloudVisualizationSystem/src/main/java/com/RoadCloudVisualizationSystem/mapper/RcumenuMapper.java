package com.RoadCloudVisualizationSystem.mapper;

import java.util.List;
import java.util.Map;

import com.RoadCloudVisualizationSystem.entity.Rcumenu;

import io.lettuce.core.dynamic.annotation.Param;

public interface RcumenuMapper {
    int deleteByPrimaryKey(String rcuId);

    int insert(Rcumenu row);

    int insertSelective(Rcumenu row);

    Rcumenu selectByPrimaryKey(String rcuId);

    int updateByPrimaryKeySelective(Rcumenu row);

    int updateByPrimaryKey(Rcumenu row);

    int getVehicleMenuPageCount();

    List<Map<String, Object>> getVehicleMenuPage(@Param("vehicleId") String vehicleId, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset);


    List<Map<String, Object>> getDeviceNumList();
}