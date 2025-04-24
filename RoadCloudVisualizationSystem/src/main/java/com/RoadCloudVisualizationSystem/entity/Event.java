package com.RoadCloudVisualizationSystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 路测事件
 * @TableName event
 */
@TableName(value ="event")
@Data
public class Event {
    /**
     * 事件唯一编号
     */
    @TableId(value = "eventId")
    private String eventId;

    /**
     * 事件类别
0 倒车\逆行
1 慢行
2 快行
3 紧急制动
4 异常停车

     */
    @TableField(value = "eventType")
    private Integer eventType;

    /**
     * rcuId
     */
    @TableField(value = "rcuId")
    private String rcuId;

    /**
     * 经度
     */
    @TableField(value = "longitude")
    private String longitude;

    /**
     * 纬度
     */
    @TableField(value = "latitude")
    private String latitude;

    /**
     * 时间戳
     */
    @TableField(value = "timestamp")
    private String timestamp;

    /**
     * 事件关联的目标 对象uuid个数
     */
    @TableField(value = "targetIdsLen")
    private Integer targetIdsLen;

    /**
     * 事件关联目标对 象uuid列表
     */
    @TableField(value = "targetIds")
    private String targetIds;

    /**
     * 路口名
     */
    @TableField(value = "intersectionName")
    private String intersectionName;

    /**
     * 是否给展示过
     */
    @TableField(value = "isView")
    private Integer isView;

    private String description;

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
        Event other = (Event) that;
        return (this.getEventId() == null ? other.getEventId() == null : this.getEventId().equals(other.getEventId()))
            && (this.getEventType() == null ? other.getEventType() == null : this.getEventType().equals(other.getEventType()))
            && (this.getRcuId() == null ? other.getRcuId() == null : this.getRcuId().equals(other.getRcuId()))
            && (this.getLongitude() == null ? other.getLongitude() == null : this.getLongitude().equals(other.getLongitude()))
            && (this.getLatitude() == null ? other.getLatitude() == null : this.getLatitude().equals(other.getLatitude()))
            && (this.getTimestamp() == null ? other.getTimestamp() == null : this.getTimestamp().equals(other.getTimestamp()))
            && (this.getTargetIdsLen() == null ? other.getTargetIdsLen() == null : this.getTargetIdsLen().equals(other.getTargetIdsLen()))
            && (this.getTargetIds() == null ? other.getTargetIds() == null : this.getTargetIds().equals(other.getTargetIds()))
            && (this.getIntersectionName() == null ? other.getIntersectionName() == null : this.getIntersectionName().equals(other.getIntersectionName()))
            && (this.getIsView() == null ? other.getIsView() == null : this.getIsView().equals(other.getIsView()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getEventId() == null) ? 0 : getEventId().hashCode());
        result = prime * result + ((getEventType() == null) ? 0 : getEventType().hashCode());
        result = prime * result + ((getRcuId() == null) ? 0 : getRcuId().hashCode());
        result = prime * result + ((getLongitude() == null) ? 0 : getLongitude().hashCode());
        result = prime * result + ((getLatitude() == null) ? 0 : getLatitude().hashCode());
        result = prime * result + ((getTimestamp() == null) ? 0 : getTimestamp().hashCode());
        result = prime * result + ((getTargetIdsLen() == null) ? 0 : getTargetIdsLen().hashCode());
        result = prime * result + ((getTargetIds() == null) ? 0 : getTargetIds().hashCode());
        result = prime * result + ((getIntersectionName() == null) ? 0 : getIntersectionName().hashCode());
        result = prime * result + ((getIsView() == null) ? 0 : getIsView().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", eventId=").append(eventId);
        sb.append(", eventType=").append(eventType);
        sb.append(", rcuId=").append(rcuId);
        sb.append(", longitude=").append(longitude);
        sb.append(", latitude=").append(latitude);
        sb.append(", timestamp=").append(timestamp);
        sb.append(", targetIdsLen=").append(targetIdsLen);
        sb.append(", targetIds=").append(targetIds);
        sb.append(", intersectionName=").append(intersectionName);
        sb.append(", isView=").append(isView);
        sb.append("]");
        return sb.toString();
    }
}