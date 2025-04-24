package com.RoadCloudVisualizationSystem.controller.system;

import com.RoadCloudVisualizationSystem.entity.User;
import com.RoadCloudVisualizationSystem.log.Log;
import com.RoadCloudVisualizationSystem.log.enuns.BusinessType;
import com.RoadCloudVisualizationSystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/system/user")
public class SystemUserController {
    @Autowired
    private UserService userService;

    // 获取用户列表
    @Log(title = "分页获取用户列表", businessType = BusinessType.QUERY)
    @GetMapping("/list")
    public Map<String, Object> getUserList(@RequestParam(required = false) String username,
                                           @RequestParam Integer pageNum,
                                           @RequestParam Integer pageSize) {
        if (username == null)
            return userService.getUsersPage(pageNum, pageSize);
        else
            return userService.getUsersByUsernamePage(username, pageNum, pageSize);
    }
    // 获取用户详情
    @Log(title = "通过id查询用户信息", businessType = BusinessType.QUERY)
    @GetMapping
    public Map<String, Object> getUserById(@RequestParam String uid) {
        return userService.getUserById(uid);
    }

    // 更新用户信息
    @Log(title = "更新用户信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public Map<String, Object> updateUser(@RequestBody User user) {
        return userService.updateUser(user);
    }

    // 删除用户
    @Log(title = "根据id删除用户", businessType = BusinessType.DELETE)
    @DeleteMapping
    public Map<String, Object> deleteUser(@RequestParam String uid) {
        return userService.deleteUser(uid);
    }

    // 重置密码
    @Log(title = "根据id重置密码", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public Map<String, Object> resetPassword(@RequestParam String uid, @RequestParam String password) {
        return userService.resetPassword(uid, password);
    }

    // 修改密码
    @Log(title = "根据id修改密码", businessType = BusinessType.UPDATE)
    @PutMapping("/updatePwd")
    public Map<String, Object> updatePassword(@RequestParam String uid, @RequestParam String oldPassword, @RequestParam String newPassword) {
        return userService.updatePassword(uid, oldPassword, newPassword);
    }

    // 新建用户
    @Log(title = "新建用户", businessType = BusinessType.INSERT)
    @PostMapping
    public Map<String, Object> insertUser(@RequestBody User user) {
        return userService.insertUser(user);
    }

    // 导出用户列表
    //@Log(title = "导出所有用户信息", businessType = BusinessType.OTHER)
    @GetMapping("/export")
    public ResponseEntity<Object> exportUsers() {
        try {
            // 生成文件流
            ByteArrayInputStream stream = userService.exportUsers();

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=users.xlsx");

            // 成功响应（包含文件）
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(stream));
        } catch (Exception e) {
            // 失败响应（包含状态信息）
            Map<String, Object> response = new HashMap<>();
            response.put("status", "fail");
            response.put("msg", "导出失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        }
    }
}
