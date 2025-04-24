package com.RoadCloudVisualizationSystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 操作日志记录
 * @TableName sys_oper_log
 */
@TableName(value ="sys_oper_log")
@Data
public class SysOperLog {
    /**
     * 日志主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模块标题
     */
    @TableField(value = "title")
    private String title;

    /**
     * 业务类型（0其它 1新增 2修改 3删除）
     */
    @TableField(value = "business_type")
    private String businessType;

    /**
     * 方法名称
     */
    @TableField(value = "method")
    private String method;

    /**
     * 请求方式
     */
    @TableField(value = "request_method")
    private String requestMethod;

    /**
     * 操作人员
     */
    @TableField(value = "oper_name")
    private String operName;

    /**
     * 请求URL
     */
    @TableField(value = "oper_url")
    private String operUrl;

    /**
     * 主机地址
     */
    @TableField(value = "oper_ip")
    private String operIp;

    /**
     * 请求参数
     */
    @TableField(value = "oper_param")
    private String operParam;

    /**
     * 返回参数
     */
    @TableField(value = "json_result")
    private String jsonResult;

    /**
     * 操作状态（1正常 0异常）
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 错误消息
     */
    @TableField(value = "error_msg")
    private String errorMsg;

    /**
     * 操作时间
     */
    @TableField(value = "oper_time")
    private String operTime;

    /**
     * 执行时长(毫秒)
     */
    @TableField(value = "execute_time")
    private Long executeTime;

}