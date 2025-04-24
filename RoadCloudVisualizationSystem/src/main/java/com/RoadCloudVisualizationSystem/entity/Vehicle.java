package com.RoadCloudVisualizationSystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

/**
 * 车辆基础信息--状态
 * @TableName vehicle
 */
@TableName(value ="vehicle")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Vehicle {
    public static Vehicle fromJson(String json) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(json);
        
        Vehicle vehicle = new ObjectMapper().readValue(json, Vehicle.class);

        JsonNode position = rootNode.get("position");
        vehicle.setLongitude(position.get("longitude").asText());
        vehicle.setLatitude(position.get("latitude").asText());
        vehicle.setElevation(position.get("elevation").asText());
        // 模拟设置最新时间
        //vehicle.setTimestampGNSS(System.currentTimeMillis()+"");
        vehicle.setStateId(vehicle.getVehicleId() + "_"  + vehicle.getTimestampGNSS());
        return vehicle;
    }
    /**
     * 状态id;id+车牌+timestamp
     */
    @TableId(value = "stateId")
    private String stateId;

    /**
     * 车辆编号
     */
    @TableField(value = "vehicleId")
    private String vehicleId;

    /**
     * GNSS时间戳
     */
    @TableField(value = "timestampGNSS")
    private String timestampGNSS;

    /**
     * 速度
     */
    @TableField(value = "velocityGNSS")
    private String velocityGNSS;

    /**
     * 航向角
     */
    @TableField(value = "heading")
    private String heading;

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
     * 高度
     */
    @TableField(value = "elevation")
    private String elevation;


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
        Vehicle other = (Vehicle) that;
        return (this.getStateId() == null ? other.getStateId() == null : this.getStateId().equals(other.getStateId()))
            && (this.getVehicleId() == null ? other.getVehicleId() == null : this.getVehicleId().equals(other.getVehicleId()))
            && (this.getTimestampGNSS() == null ? other.getTimestampGNSS() == null : this.getTimestampGNSS().equals(other.getTimestampGNSS()))
            && (this.getVelocityGNSS() == null ? other.getVelocityGNSS() == null : this.getVelocityGNSS().equals(other.getVelocityGNSS()))
            && (this.getHeading() == null ? other.getHeading() == null : this.getHeading().equals(other.getHeading()))
            && (this.getLongitude() == null ? other.getLongitude() == null : this.getLongitude().equals(other.getLongitude()))
            && (this.getLatitude() == null ? other.getLatitude() == null : this.getLatitude().equals(other.getLatitude()))
            && (this.getElevation() == null ? other.getElevation() == null : this.getElevation().equals(other.getElevation()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getStateId() == null) ? 0 : getStateId().hashCode());
        result = prime * result + ((getVehicleId() == null) ? 0 : getVehicleId().hashCode());
        result = prime * result + ((getTimestampGNSS() == null) ? 0 : getTimestampGNSS().hashCode());
        result = prime * result + ((getVelocityGNSS() == null) ? 0 : getVelocityGNSS().hashCode());
        result = prime * result + ((getHeading() == null) ? 0 : getHeading().hashCode());
        result = prime * result + ((getLongitude() == null) ? 0 : getLongitude().hashCode());
        result = prime * result + ((getLatitude() == null) ? 0 : getLatitude().hashCode());
        result = prime * result + ((getElevation() == null) ? 0 : getElevation().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", stateId=").append(stateId);
        sb.append(", vehicleId=").append(vehicleId);
        sb.append(", timestampGNSS=").append(timestampGNSS);
        sb.append(", velocityGNSS=").append(velocityGNSS);
        sb.append(", heading=").append(heading);
        sb.append(", longitude=").append(longitude);
        sb.append(", latitude=").append(latitude);
        sb.append(", elevation=").append(elevation);
        sb.append("]");
        return sb.toString();
    }
}