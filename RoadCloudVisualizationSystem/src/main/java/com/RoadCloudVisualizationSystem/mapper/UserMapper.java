package com.RoadCloudVisualizationSystem.mapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

import com.RoadCloudVisualizationSystem.entity.Permission;
import com.RoadCloudVisualizationSystem.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Administrator
* @description 针对表【user】的数据库操作Mapper
* @createDate 2025-04-03 09:35:05
* @Entity com.RoadCloudVisualizationSystem.entity.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {

    // 查询所有用户
    List<User> findAll();

    User findByUid(@Param("uid") String uid);

    User findByUsername(@Param("username") String username);

    // 分页查询用户
    List<User> findUsersPage(@Param("limit") Integer limit, @Param("offset") Integer offset);

    // 计算用户总数
    Integer countUsers();

    // 根据用户名为过滤条件分页查询用户信息
    List<User> findUsersByUsernamePage(@Param("username") String username, @Param("limit") Integer limit, @Param("offset") Integer offset);

    // 根据用户名为过滤条件计算用户总数
    Integer countUsersByUsername(@Param("username") String username);

    // 通过id删除用户
    Integer delByUid(@Param("uid") String uid);

    // 通过用户id更新用户信息
    Integer updateByUid(@Param("uid") String uid, @Param("username") String username, @Param("password") String password, @Param("authority") String authority);

    // 通过用户id更新用户信息除了密码
    Integer updateByUidExceptPassword(@Param("uid") String uid, @Param("username") String username, @Param("authority") String authority);

    // 新建用户
    Integer insertUser(@Param("uid") String uid, @Param("username") String username, @Param("password") String password, @Param("authority") String authority);

    List<Permission> findPermissionsByUrlAndMethod(String url, String method);
    
}




