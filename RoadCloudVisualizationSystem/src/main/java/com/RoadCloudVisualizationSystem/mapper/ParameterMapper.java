package com.RoadCloudVisualizationSystem.mapper;

import com.RoadCloudVisualizationSystem.entity.Parameter;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author Administrator
* @description 针对表【parameter(参数调整表)】的数据库操作Mapper
* @createDate 2025-04-14 14:11:34
* @Entity com.RoadCloudVisualizationSystem.entity.Parameter
*/
public interface ParameterMapper extends BaseMapper<Parameter> {

    // 分页查询参数列表
    List<Parameter> getParameterListPageByGroup(String group, Integer limit, Integer offset);

    // 获取参数列表的总记录数
    Integer getParameterCountByGroup(String group);

    // 新增参数
    Integer insertParameter(Parameter parameter);

    // 更新参数
    Integer updateParameter(Parameter parameter);

    // 删除参数
    Integer deleteParameter(Integer id);
}




