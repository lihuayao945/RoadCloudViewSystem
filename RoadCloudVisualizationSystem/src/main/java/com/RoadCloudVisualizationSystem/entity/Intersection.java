package com.RoadCloudVisualizationSystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

/**
 * 
 * @TableName intersection
 */
@TableName(value ="intersection")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Intersection {
    public static Intersection fromJson(String json) throws Exception {
        // 创建 ObjectMapper 实例
        ObjectMapper objectMapper = new ObjectMapper();
        // 解析 JSON 字符串为 JsonNode
        JsonNode rootNode = objectMapper.readTree(json);
        // 提取 intersections 键值对应的内容
        JsonNode intersectionsNode = rootNode.path("intersections");
        String Intersectionjson = intersectionsNode.get(0).toString();
        Intersection intersectiondemo = new ObjectMapper().readValue(Intersectionjson, Intersection.class);
        String uuid = rootNode.path("uuid").asText();
        intersectiondemo.setUuid(uuid);
        return intersectiondemo;
    }
    /**
     * 
     */
    @TableId(value = "intersectionTimestamp")
    private String intersectionTimestamp;

    /**
     * 
     */
    @TableField(value = "uuid")
    private String uuid;

    /**
     * 
     */
    @TableField(value = "regionId")
    private Integer regionId;

    /**
     * 
     */
    @TableField(value = "nodeId")
    private Integer nodeId;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        Intersection other = (Intersection) that;
        return (this.getIntersectionTimestamp() == null ? other.getIntersectionTimestamp() == null : this.getIntersectionTimestamp().equals(other.getIntersectionTimestamp()))
            && (this.getUuid() == null ? other.getUuid() == null : this.getUuid().equals(other.getUuid()))
            && (this.getRegionId() == null ? other.getRegionId() == null : this.getRegionId().equals(other.getRegionId()))
            && (this.getNodeId() == null ? other.getNodeId() == null : this.getNodeId().equals(other.getNodeId()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getIntersectionTimestamp() == null) ? 0 : getIntersectionTimestamp().hashCode());
        result = prime * result + ((getUuid() == null) ? 0 : getUuid().hashCode());
        result = prime * result + ((getRegionId() == null) ? 0 : getRegionId().hashCode());
        result = prime * result + ((getNodeId() == null) ? 0 : getNodeId().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", intersectionTimestamp=").append(intersectionTimestamp);
        sb.append(", uuid=").append(uuid);
        sb.append(", regionId=").append(regionId);
        sb.append(", nodeId=").append(nodeId);
        sb.append("]");
        return sb.toString();
    }
}