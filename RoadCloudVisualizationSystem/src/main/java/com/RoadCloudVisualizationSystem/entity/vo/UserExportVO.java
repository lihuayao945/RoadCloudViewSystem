package com.RoadCloudVisualizationSystem.entity.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class UserExportVO {
    @ExcelProperty("用户ID")
    private String uid;

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("用户密码")
    private String password;

    @ExcelProperty("用户角色")
    private String authority;

    public UserExportVO(String uid, String username, String password, String authority) {
        this.uid = uid;
        this.username = username;
        this.password = password;
        this.authority = authority;
    }
}