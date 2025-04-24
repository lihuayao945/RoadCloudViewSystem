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
 * @TableName phasestate
 */
@TableName(value ="phasestate")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Phasestate {


    public static LinkedList<Phasestate> fromJson(String json) throws Exception {
        LinkedList<Phasestate> phasestateList = new LinkedList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(json);
        JsonNode phasesNode = (rootNode.path("intersections").get(0)).path("phases");
        String intersectionTimestamp = rootNode.path("intersections").get(0).path("intersectionTimestamp").asText();
        for (int i = 0; i < phasesNode.size(); i++) {
            Integer stateid = 2;
            Integer phaseId = phasesNode.get(i).path("phaseId").asInt();
            JsonNode phaseStatesNode = phasesNode.get(i).path("phaseStates");
            for (int j = 0; j < phaseStatesNode.size(); j++) {
                Phasestate phasestate = new Phasestate();
                phasestate = Phasestate.fromJsonString(phaseStatesNode.get(j).toString());
                if (stateid == 2) {
                    phasestate.setStateid(1);
                    stateid = 1;
                }
                else if(stateid == 1){
                    phasestate.setStateid(2);
                    stateid = 2;
                }
                phasestate.setNum(i+1);
                phasestate.setStateflag("state_" + phasestate.getNum() + "_" + phasestate.getStateid() +  "_" + intersectionTimestamp );
                phasestate.setPhaseId(phaseId);
                phasestate.setIntersectionTimestamp(intersectionTimestamp);
                phasestateList.add(phasestate);
            }
        }
        return phasestateList;
        
    }


    public static Phasestate fromJsonString(String json) throws Exception {
        return new ObjectMapper().readValue(json, Phasestate.class);
    }

    /**
     * 
     */
    @TableId(value = "stateflag")
    private String stateflag;

    /**
     * 
     */
    @TableField(value = "intersectionTimestamp")
    private String intersectionTimestamp;

    /**
     * 
     */
    @TableField(value = "phaseId")
    private Integer phaseId;

    /**
     * 
     */
    @TableField(value = "stateid")
    private Integer stateid;

    /**
     * 
     */
    @TableField(value = "timeChangeDetails")
    private Integer timeChangeDetails;

    /**
     * 
     */
    @TableField(value = "light")
    private Integer light;

    /**
     * 
     */
    @TableField(value = "likelyEndTime")
    private String likelyEndTime;

    /**
     * 
     */
    @TableField(value = "nextDuration")
    private String nextDuration;

    /**
     * 
     */
    @TableField(value = "startTime")
    private Integer startTime;

    /**
     * 
     */
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
        Phasestate other = (Phasestate) that;
        return (this.getStateflag() == null ? other.getStateflag() == null : this.getStateflag().equals(other.getStateflag()))
            && (this.getIntersectionTimestamp() == null ? other.getIntersectionTimestamp() == null : this.getIntersectionTimestamp().equals(other.getIntersectionTimestamp()))
            && (this.getPhaseId() == null ? other.getPhaseId() == null : this.getPhaseId().equals(other.getPhaseId()))
            && (this.getStateid() == null ? other.getStateid() == null : this.getStateid().equals(other.getStateid()))
            && (this.getTimeChangeDetails() == null ? other.getTimeChangeDetails() == null : this.getTimeChangeDetails().equals(other.getTimeChangeDetails()))
            && (this.getLight() == null ? other.getLight() == null : this.getLight().equals(other.getLight()))
            && (this.getLikelyEndTime() == null ? other.getLikelyEndTime() == null : this.getLikelyEndTime().equals(other.getLikelyEndTime()))
            && (this.getNextDuration() == null ? other.getNextDuration() == null : this.getNextDuration().equals(other.getNextDuration()))
            && (this.getStartTime() == null ? other.getStartTime() == null : this.getStartTime().equals(other.getStartTime()))
            && (this.getNum() == null ? other.getNum() == null : this.getNum().equals(other.getNum()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getStateflag() == null) ? 0 : getStateflag().hashCode());
        result = prime * result + ((getIntersectionTimestamp() == null) ? 0 : getIntersectionTimestamp().hashCode());
        result = prime * result + ((getPhaseId() == null) ? 0 : getPhaseId().hashCode());
        result = prime * result + ((getStateid() == null) ? 0 : getStateid().hashCode());
        result = prime * result + ((getTimeChangeDetails() == null) ? 0 : getTimeChangeDetails().hashCode());
        result = prime * result + ((getLight() == null) ? 0 : getLight().hashCode());
        result = prime * result + ((getLikelyEndTime() == null) ? 0 : getLikelyEndTime().hashCode());
        result = prime * result + ((getNextDuration() == null) ? 0 : getNextDuration().hashCode());
        result = prime * result + ((getStartTime() == null) ? 0 : getStartTime().hashCode());
        result = prime * result + ((getNum() == null) ? 0 : getNum().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", stateflag=").append(stateflag);
        sb.append(", intersectionTimestamp=").append(intersectionTimestamp);
        sb.append(", phaseId=").append(phaseId);
        sb.append(", stateid=").append(stateid);
        sb.append(", timeChangeDetails=").append(timeChangeDetails);
        sb.append(", light=").append(light);
        sb.append(", likelyEndTime=").append(likelyEndTime);
        sb.append(", nextDuration=").append(nextDuration);
        sb.append(", startTime=").append(startTime);
        sb.append(", num=").append(num);
        sb.append("]");
        return sb.toString();
    }
}