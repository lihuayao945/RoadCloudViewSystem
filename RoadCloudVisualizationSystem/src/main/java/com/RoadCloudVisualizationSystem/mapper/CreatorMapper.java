package com.RoadCloudVisualizationSystem.mapper;

import java.util.List;
import java.util.Map;

import com.RoadCloudVisualizationSystem.entity.Creator;

public interface CreatorMapper {
    int insert(Creator row);

    int insertSelective(Creator row);

    List<Map<String, Object>> getCreatorListPage(int offset, Integer pageSize);

    List<Map<String, Object>> getCreatorListPageByTime(String starttime, String endtime);


}