package com.RoadCloudVisualizationSystem.service;

import com.RoadCloudVisualizationSystem.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.RoadCloudVisualizationSystem.utils.Result;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.util.Map;


public interface UserService extends IService<User> {

    //登录
    Map<String, String> login(String username, String password);

    //注册
    Result register(String username, String password);

    // 分页查询用户信息
    Map<String, Object> getUsersPage(Integer pageNum, Integer pageSize);

    // 根据用户名分页查询用户信息
    Map<String, Object> getUsersByUsernamePage(String username, Integer pageNum, Integer pageSize);

    // 根据用户id查询用户信息
    Map<String, Object> getUserById(String uid);

    // 修改用户信息
    Map<String, Object> updateUser(User user);

    // 删除用户信息
    Map<String, Object> deleteUser(String uid);

    // 重置密码
    Map<String, Object> resetPassword(String uid, String password);

    // 修改密码
    Map<String, Object> updatePassword(String uid, String oldPassword, String newPassword);

    // 新建用户
    Map<String, Object> insertUser(User user);

    // 导出用户列表
    ByteArrayInputStream exportUsers();

    // 导出所有用户列表
    Map<String, Object> exportAllUsers();
}
