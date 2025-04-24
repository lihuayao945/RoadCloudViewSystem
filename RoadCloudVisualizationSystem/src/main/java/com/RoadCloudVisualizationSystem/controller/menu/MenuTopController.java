package com.RoadCloudVisualizationSystem.controller.menu;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.RoadCloudVisualizationSystem.entity.province;
import com.RoadCloudVisualizationSystem.mapper.provinceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/menu/top")
public class MenuTopController {
    @Autowired
    private provinceMapper provinceMapper;

    
    @GetMapping("/list")
    public Map<String, Object> getProvinceByName(@RequestParam String provincename) {
        province result = provinceMapper.getProvinceByName(provincename);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> dataMap = new HashMap<>();
        if (result != null) {
            try {
                dataMap = mapper.readValue(result.getData(), Map.class);
                dataMap.put("status", "success");
            } catch (Exception e) {
                throw new RuntimeException("数据格式错误");
            }
        }
        else{
            dataMap.put("status", "fail");
        }
        return dataMap;
    }
  
    


}
