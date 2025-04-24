package com.RoadCloudVisualizationSystem.recycled;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.RoadCloudVisualizationSystem.enums.ObjType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.RoadCloudVisualizationSystem.mapper.ObjMapper;


@RestController
public class MenuPicTwoController {
    
    @Autowired
    private  ObjMapper objMapper;

    
    @GetMapping("/menu/vehicle/kindnum/1")
    public Map<String, Object> GetPicTwoList(@RequestParam String rcuId,@RequestParam(defaultValue = "1") String startTime,@RequestParam(defaultValue = "9223372036854775806") String endTime) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> data = objMapper.GetPicTwoList(rcuId, startTime, endTime);
        if (data != null && !data.isEmpty()) {
            result.put("status", "success");
           
            // 处理数据，将type数字转换为对应的车辆类型名称
            for (Map<String, Object> item : data) {
                if (item.containsKey("type")) {
                    Object typeObj = item.get("type");
                    if (typeObj != null) {
                        try {
                            int typeCode = Integer.parseInt(typeObj.toString());
                            // 使用CarType枚举将数字编码转换为对应的车辆类型名称
                            String typeName = ObjType.fromCode(typeCode).getType();
                            item.put("typeName", typeName);
                        } catch (NumberFormatException e) {
                            // 如果转换失败，设置为未知类型
                            item.put("typeName", "未知");
                        }
                    }
                }
            }

            result.put("data", data);
        }
        else {
            result.put("status", "fail");
        }
        return result;
    }

    @GetMapping("/menu/vehicle/stream/2")
    public Map<String, Object> GetPicOneList(@RequestParam String rcuId,@RequestParam(defaultValue = "1724034655275") String startTime,@RequestParam(defaultValue = "1724034755275") String endTime){
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> data = objMapper.GetPicOneList(rcuId, Long.parseLong(startTime), Long.parseLong(endTime));
        if (data != null && !data.isEmpty()) {
            result.put("status", "success");
            result.put("data", data);
            return result;
        }
        result.put("status", "fail");
        return result;
    }


}
