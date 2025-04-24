package com.RoadCloudVisualizationSystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

/**
 * 
 * @TableName frsu
 */
@TableName(value ="frsu")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Frsu {
    public static Frsu fromJson(String json) throws Exception {
        return new ObjectMapper().readValue(json, Frsu.class);
    }

    /**
     * 
     */
    @TableId(value = "uuid")
    private String uuid;

    /**
     * 
     */
    @TableField(value = "timestamp")
    private String timestamp;

    /**
     * 
     */
    @TableField(value = "msgType")
    private String msgType;

    /**
     * 
     */
    @TableField(value = "rsuId")
    private String rsuId;

    /**
     * 
     */
    @TableField(value = "msgCnt")
    private Integer msgCnt;

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
        Frsu other = (Frsu) that;
        return (this.getUuid() == null ? other.getUuid() == null : this.getUuid().equals(other.getUuid()))
            && (this.getTimestamp() == null ? other.getTimestamp() == null : this.getTimestamp().equals(other.getTimestamp()))
            && (this.getMsgType() == null ? other.getMsgType() == null : this.getMsgType().equals(other.getMsgType()))
            && (this.getRsuId() == null ? other.getRsuId() == null : this.getRsuId().equals(other.getRsuId()))
            && (this.getMsgCnt() == null ? other.getMsgCnt() == null : this.getMsgCnt().equals(other.getMsgCnt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getUuid() == null) ? 0 : getUuid().hashCode());
        result = prime * result + ((getTimestamp() == null) ? 0 : getTimestamp().hashCode());
        result = prime * result + ((getMsgType() == null) ? 0 : getMsgType().hashCode());
        result = prime * result + ((getRsuId() == null) ? 0 : getRsuId().hashCode());
        result = prime * result + ((getMsgCnt() == null) ? 0 : getMsgCnt().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", uuid=").append(uuid);
        sb.append(", timestamp=").append(timestamp);
        sb.append(", msgType=").append(msgType);
        sb.append(", rsuId=").append(rsuId);
        sb.append(", msgCnt=").append(msgCnt);
        sb.append("]");
        return sb.toString();
    }


}