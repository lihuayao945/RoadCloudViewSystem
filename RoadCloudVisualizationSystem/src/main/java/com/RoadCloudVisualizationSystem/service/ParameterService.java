package com.RoadCloudVisualizationSystem.service;

import com.RoadCloudVisualizationSystem.entity.Parameter;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
* @author Administrator
* @description 针对表【parameter(参数调整表)】的数据库操作Service
* @createDate 2025-04-14 14:11:34
*/
public interface ParameterService extends IService<Parameter> {

    // 根据group分页查询参数列表
    public Map<String, Object> getParameterListPageByGroup(String group, Integer pageNum, Integer pageSize);

    // 新增参数
    public Map<String, Object>  insertParameter(Parameter parameter);

    // 修改参数
    public Map<String, Object>  updateParameter(Parameter parameter);

    // 删除参数
    public Map<String, Object>  deleteParameter(Integer id);
}
