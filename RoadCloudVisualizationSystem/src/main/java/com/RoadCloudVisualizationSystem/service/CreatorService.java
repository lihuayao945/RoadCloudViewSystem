package com.RoadCloudVisualizationSystem.service;

import java.util.Map;


public interface CreatorService {
    public Map<String, Object> getCreatorListPage(Integer pageNum, Integer pageSize) ;

    public Map<String, Object> getCreatorListPageByTime(String starttime, String endtime);


    public Map<String, Object> exportglb(String starttime, String endtime);
}
