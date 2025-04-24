package com.RoadCloudVisualizationSystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 消息
 * @TableName message
 */
@TableName(value ="message")
@Data
public class Message {
    /**
     * 消息唯一标识号
     */
    @TableId(value = "message_id", type = IdType.AUTO)
    private Integer messageId;

    /**
     * 主题
     */
    @TableField(value = "topic")
    private String topic;

    /**
     * 内容
     */
    @TableField(value = "content")
    private String content;

    /**
     * 创建时间
     */
    @TableField(value = "received_time")
    private String receivedTime;


    public Message(String topic, String msg, String receivedTime) {
        this.topic = topic;
        this.content = msg;
        this.receivedTime = receivedTime;
    }
}