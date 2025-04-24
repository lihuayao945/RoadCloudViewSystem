package com.RoadCloudVisualizationSystem.mapper;

import java.util.Map;

import com.RoadCloudVisualizationSystem.entity.province;

import io.lettuce.core.dynamic.annotation.Param;

public interface provinceMapper {
    int deleteByPrimaryKey(String provincename);

    int insert(province row);

    int insertSelective(province row);

    province selectByPrimaryKey(String provincename);

    int updateByPrimaryKeySelective(province row);

    int updateByPrimaryKey(province row);


    province getProvinceByName(@Param("provincename") String provincename);
}