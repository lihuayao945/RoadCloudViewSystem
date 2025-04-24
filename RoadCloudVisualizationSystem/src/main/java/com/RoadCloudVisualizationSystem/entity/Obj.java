package com.RoadCloudVisualizationSystem.entity;

import java.util.LinkedList;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Data;

/**
 * 
 * @TableName obj
 */
@TableName(value ="obj")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Obj {
    public static LinkedList<Obj>  fromJson(String json) throws Exception {
        JsonNode rootNode = new ObjectMapper().readTree(json);
        JsonNode objectiveNode = rootNode.get("objective");
        RcuObjs rcuObjs = RcuObjs.fromJson(json);
        rcuObjs.setObjsflag(rcuObjs.getRcuId() + "_" + rcuObjs.getTimestampOfDetOut());

        LinkedList<Obj> objs = new LinkedList<>();
        for (JsonNode objNode : objectiveNode) {
            Obj obj = new ObjectMapper().readValue(objNode.toString(), Obj.class);
            obj.setObjsflag(rcuObjs.getObjsflag());
            objs.add(obj);
        }

        
        return objs;
    }


    /**
     * 该数据的唯一编号
     */
    @TableField(value = "uuid")
    private String uuid;

    /**
     * 对象感知信息唯一标识
     */
    @TableField(value = "objsflag")
    private String objsflag;

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
     * 航向角
     */
    @TableField(value = "heading")
    private String heading;

    /**
     * 速度
     */
    @TableField(value = "speed")
    private String speed;

    /**
     * 对象类型
     */
    @TableField(value = "type")
    private String type;

    /**
     * 对象编号
     */
    @TableField(value = "objId")
    private Integer objId;

    /**
     * 高程
     */
    @TableField(value = "elevation")
    private String elevation;

    /**
     * 车牌号
     */
    @TableField(value = "plateNo")
    private String plateNo;
}