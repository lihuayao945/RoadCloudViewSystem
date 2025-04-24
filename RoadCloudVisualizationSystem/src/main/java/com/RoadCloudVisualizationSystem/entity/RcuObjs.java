package com.RoadCloudVisualizationSystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

/**
 * RCU感知信息表
 * @TableName rcu_objs
 */
@TableName(value ="rcu_objs")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RcuObjs {
    public static RcuObjs fromJson(String json) throws Exception {
        RcuObjs rcuObjs = new ObjectMapper().readValue(json, RcuObjs.class);
        // 模拟设置最新时间
        rcuObjs.setTimestampOfDetOut(System.currentTimeMillis()+"");
        rcuObjs.setObjsflag(rcuObjs.getRcuId() + "_" + rcuObjs.getTimestampOfDetOut());
        return rcuObjs;
    }

    /**
     * 对象感知信息唯一标识;rcuId+timestampOfDetOut
     */
    @TableId(value = "objsflag")
    private String objsflag;

    /**
     * RCU编号
     */
    @TableField(value = "rcuId")
    private String rcuId;

    /**
     * 输出结果时间戳
     */
    @TableField(value = "timestampOfDetOut")
    private String timestampOfDetOut;

    /**
     * 设备类型
     */
    @TableField(value = "deviceType")
    private String deviceType;

    /**
     * 感知对象数量
     */
    @TableField(value = "objectiveNum")
    private Integer objectiveNum;
}