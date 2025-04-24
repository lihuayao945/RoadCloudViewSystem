package com.RoadCloudVisualizationSystem.entity;

import java.util.LinkedList;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

/**
 * 
 * @TableName phase
 */
@TableName(value ="phase")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Phase {
    public static LinkedList<Phase> fromJson(String json) throws Exception {
        // 创建 ObjectMapper 实例
        ObjectMapper objectMapper = new ObjectMapper();
        // 解析 JSON 字符串为 JsonNode
        JsonNode rootNode = objectMapper.readTree(json);
        JsonNode phasesNode = (rootNode.path("intersections").get(0)).path("phases");
        
        // 创建一个LinkedList来存储Phase对象
        LinkedList<Phase> phaseList = new LinkedList<>();
        String intersectionTimestamp = rootNode.path("intersections").get(0).path("intersectionTimestamp").asText();
        for (int i = 0; i < phasesNode.size(); i++) {
            JsonNode phaseNode = phasesNode.get(i);
            Phase phase = new Phase();
            phase.setNum(i+1);
            phase.setPhaseflag("phase" + "_" + phase.getNum() + "_" +  intersectionTimestamp );
            phase.setIntersectionTimestamp(intersectionTimestamp);
            phase.setPhaseId(phaseNode.path("phaseId").asInt());
            
            phaseList.add(phase);
        }
        return phaseList;
    }


    @TableId(value = "phaseflag")
    private String phaseflag;


    @TableField(value = "intersectionTimestamp")
    private String intersectionTimestamp;


    @TableField(value = "phaseId")
    private Integer phaseId;


    @TableField(value = "num")
    private Integer num;



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
        Phase other = (Phase) that;
        return (this.getPhaseflag() == null ? other.getPhaseflag() == null : this.getPhaseflag().equals(other.getPhaseflag()))
            && (this.getIntersectionTimestamp() == null ? other.getIntersectionTimestamp() == null : this.getIntersectionTimestamp().equals(other.getIntersectionTimestamp()))
            && (this.getPhaseId() == null ? other.getPhaseId() == null : this.getPhaseId().equals(other.getPhaseId()))
            && (this.getNum() == null ? other.getNum() == null : this.getNum().equals(other.getNum()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getPhaseflag() == null) ? 0 : getPhaseflag().hashCode());
        result = prime * result + ((getIntersectionTimestamp() == null) ? 0 : getIntersectionTimestamp().hashCode());
        result = prime * result + ((getPhaseId() == null) ? 0 : getPhaseId().hashCode());
        result = prime * result + ((getNum() == null) ? 0 : getNum().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", phaseflag=").append(phaseflag);
        sb.append(", intersectionTimestamp=").append(intersectionTimestamp);
        sb.append(", phaseId=").append(phaseId);
        sb.append(", num=").append(num);
        sb.append("]");
        return sb.toString();
    }
}