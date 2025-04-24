package com.RoadCloudVisualizationSystem.entity.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class MessageExportVO {
    /**
     * 消息唯一标识号
     */
    @ExcelProperty("消息ID")
    private Integer messageId;

    /**
     * 主题
     */
    @ExcelProperty("主题")
    private String topic;

    /**
     * 内容
     */
    @ExcelProperty("内容")
    private String content;

    /**
     * 创建时间
     */
    @ExcelProperty("创建时间")
    private String receivedTime;


    public MessageExportVO(Integer messageId,String topic, String msg, String receivedTime) {
        this.messageId = messageId;
        this.topic = topic;
        this.content = msg;
        this.receivedTime = receivedTime;
    }
}
