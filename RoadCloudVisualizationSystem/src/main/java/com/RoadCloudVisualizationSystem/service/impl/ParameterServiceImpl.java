package com.RoadCloudVisualizationSystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.RoadCloudVisualizationSystem.entity.Parameter;
import com.RoadCloudVisualizationSystem.service.ParameterService;
import com.RoadCloudVisualizationSystem.mapper.ParameterMapper;
import jakarta.persistence.Access;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
* @author Administrator
* @description 针对表【parameter(参数调整表)】的数据库操作Service实现
* @createDate 2025-04-14 14:11:34
*/
@Service
public class ParameterServiceImpl extends ServiceImpl<ParameterMapper, Parameter>
    implements ParameterService{
    @Autowired
    private ParameterMapper parameterMapper;

    @Override
    public Map<String, Object> getParameterListPageByGroup(String group, Integer pageNum, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        try{
            result.put("status", "success");
            result.put("total", parameterMapper.getParameterCountByGroup(group));
            result.put("rows", parameterMapper.getParameterListPageByGroup(group, pageSize, (pageNum - 1) * pageSize));
            return result;
        }
        catch (Exception e){
            result.put("status", "fail");
            return result;
        }
    }


    @Override
    public Map<String, Object>  insertParameter(Parameter parameter) {
        Map<String, Object> result = new HashMap<>();
        try{
            if (parameterMapper.insertParameter(parameter) > 0){
                result.put("status", "success");
                return result;
            }
            else {
                result.put("status", "fail");
                return result;
            }
        }
        catch (Exception e){
            result.put("status", "fail");
            return result;
        }
    }

    @Override
    public Map<String, Object>  updateParameter(Parameter parameter) {
        Map<String, Object> result = new HashMap<>();
        try{
            if (parameterMapper.updateParameter(parameter) > 0){
                result.put("status", "success");
                return result;
            }
            else {
                result.put("status", "fail");
                return result;
            }
        }
        catch (Exception e){
            result.put("status", "fail");
            return result;
        }
    }

    @Override
    public Map<String, Object>  deleteParameter(Integer id) {
        Map<String, Object> result = new HashMap<>();
        try{
            if (parameterMapper.deleteParameter(id) > 0){
                result.put("status", "success");
                return result;
            }
            else {
                result.put("status", "fail");
                return result;
            }
        }
        catch (Exception e){
            result.put("status", "fail");
            return result;
        }
    }
}




