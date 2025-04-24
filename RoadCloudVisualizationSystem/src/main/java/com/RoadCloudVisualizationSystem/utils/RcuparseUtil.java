package com.RoadCloudVisualizationSystem.utils;

import com.RoadCloudVisualizationSystem.entity.Rcu;
import com.RoadCloudVisualizationSystem.mapper.RcuMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RcuparseUtil {
    @Autowired
    private RcuMapper rcuMapper;

    // rcu解析
    public Rcu extractRcuFromJson(String jsonString) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        // 使用JsonNode解析JSON
        JsonNode rootNode = objectMapper.readTree(jsonString);
        JsonNode bodyNode = rootNode.path("body");

        // 转换为你的Rcu实体
        Rcu rcu = new Rcu();
        rcu.setRcuId(bodyNode.path("rcuId").asText());

        // 处理deviceType可能为null的情况
        if (bodyNode.has("deviceType")) {
            rcu.setDeviceType(String.valueOf(bodyNode.path("deviceType").asInt()));
        } else {
            rcu.setDeviceType("0"); // 默认值
        }

        return rcu;
    }

    // 插入rcu数据
    public void insertRcu(Rcu rcu) throws Exception {
        try {
            // 查询rcuId是否存在
            Rcu existingRcu = rcuMapper.getRcuByRcuId(rcu.getRcuId());
            if (existingRcu != null) {
                // 如果存在，则更新时间
                rcuMapper.updateReceiveTimeByRcuId(rcu.getRcuId(), rcu.getReceiveTime());
            } else {
                // 如果不存在，则插入
                rcuMapper.insertRcu(rcu);
            }
        } catch (Exception e) {
            throw new Exception("保存rcu数据失败: " + e.getMessage(), e);
        }
    }
}
