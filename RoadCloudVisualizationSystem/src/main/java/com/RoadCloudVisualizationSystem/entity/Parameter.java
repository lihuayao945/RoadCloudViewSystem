package com.RoadCloudVisualizationSystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 参数调整表
 * @TableName parameter
 */
@TableName(value ="parameter")
@Data
public class Parameter {
    /**
     * 参数id
     */
    @TableId(value = "id")
    private Integer id;

    /**
     * 参数名
     */
    @TableField(value = "name")
    private String name;

    /**
     * 键
     */
    @TableField(value = "key")
    private String key;

    /**
     * 值
     */
    @TableField(value = "value")
    private String value;

    /**
     * 描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 分组
     */
    @TableField(value = "group")
    private String group;

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
        Parameter other = (Parameter) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getKey() == null ? other.getKey() == null : this.getKey().equals(other.getKey()))
            && (this.getValue() == null ? other.getValue() == null : this.getValue().equals(other.getValue()))
            && (this.getDescription() == null ? other.getDescription() == null : this.getDescription().equals(other.getDescription()))
            && (this.getGroup() == null ? other.getGroup() == null : this.getGroup().equals(other.getGroup()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getKey() == null) ? 0 : getKey().hashCode());
        result = prime * result + ((getValue() == null) ? 0 : getValue().hashCode());
        result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
        result = prime * result + ((getGroup() == null) ? 0 : getGroup().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", key=").append(key);
        sb.append(", value=").append(value);
        sb.append(", description=").append(description);
        sb.append(", group=").append(group);
        sb.append("]");
        return sb.toString();
    }
}