package com.RoadCloudVisualizationSystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 
 * @TableName rcu
 */
@TableName(value ="rcu")
@Data
public class Rcu {
    /**
     * rcu设备唯一id
     */
    @TableId(value = "rcuId")
    private String rcuId;

    /**
     * 设备类型
     */
    @TableField(value = "deviceType")
    private String deviceType;

    /**
     * 接收日期
     */
    @TableField(value = "receiveTime")
    private String receiveTime;

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
}