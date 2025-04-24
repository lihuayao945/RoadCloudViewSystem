package com.RoadCloudVisualizationSystem.service;

import com.RoadCloudVisualizationSystem.entity.Data;

public interface DataService {
    /**
     * 解析JSON数据并保存到数据库
     * @param json JSON字符串
     * @return 解析后的数据对象
     * @throws Exception 解析或保存过程中的异常
     */
    Data parseAndSave(String json) throws Exception;
    
    /**
     * 保存数据到数据库
     * @param data 要保存的数据对象
     * @throws Exception 保存过程中的异常
     */
    void save(Data data) throws Exception;
} 