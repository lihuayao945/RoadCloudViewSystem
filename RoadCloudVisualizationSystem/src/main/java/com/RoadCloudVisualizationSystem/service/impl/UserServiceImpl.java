package com.RoadCloudVisualizationSystem.service.impl;

import com.RoadCloudVisualizationSystem.entity.User;
import com.RoadCloudVisualizationSystem.entity.vo.UserExportVO;
import com.RoadCloudVisualizationSystem.mapper.UserMapper;
import com.RoadCloudVisualizationSystem.service.UserService;
import com.RoadCloudVisualizationSystem.utils.ExcelUtils;
import com.RoadCloudVisualizationSystem.utils.Result;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserDetailsService, UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${exportFilePath}")
    private String exportFilePath;

    @Override
    public Map<String, Object> exportAllUsers() {
//        Map<String, Object> result = new HashMap<>();
//        List<String> rcuidList = Arrays.asList(rcuIds.split(","));
//        // 根据时间范围和车辆数据获取要保存的车辆信息
//        List<Map<String, Object>> saveRcuAndObjs = objMapper.selectObjsByRcuIdsAndTimeRange(rcuidList, startTime, endTime);
//        if (saveRcuAndObjs != null && !saveRcuAndObjs.isEmpty()) {
//            result.put("status", "success");
//            // 获取当前时间戳
//            long timestamp = System.currentTimeMillis();
//            // 文件后缀
//            String username = userInfoUtil.getCurrentUsername();
//            String fileSuffix = "rcuObjs_" + username + timestamp + ".xlsx";
//            String filename = exportFilePath + fileSuffix;
//            try {
//                ExcelUtils.exportToExcel(saveRcuAndObjs, filename);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            System.out.println(filename);
//            result.put("filepath", "/exports/" + fileSuffix); // 将文件路径添加到结果中
//        } else {
//            result.put("status", "fail");
//        }
//
        return null;
    }


    // 通过用户名加载用户信息
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        QueryWrapper<User> queryWrapper = new QueryWrapper<User>();
        queryWrapper.eq("username", username);	// 这里不止可以用username，你可以自定义，主要根据你自己写的查询逻辑
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return user;
    }

    // 登录实现
    @Override
    public Map<String, String> login(String username, String password) {
        Map<String, String> result = new HashMap<>();
        if (username == null || password == null){
            result.put("status", "fail");
            result.put("msg", "网络连接失败");
            return result;
        }
        User user = userMapper.findByUsername(username);
        if (user == null) {
            result.put("status", "fail");
            result.put("msg", "用户名不存在");
            return result;
        }
        // 3. 使用PasswordEncoder验证
        boolean matches = passwordEncoder.matches(password, user.getPassword());

        if (!matches) {
            result.put("status", "fail");
            result.put("msg", "密码错误");
            return result;
        }
        result.put("status", "success");
        return result;
    }


    // 注册实现
    @Override
    public Result register(String username, String password) {
        if (username == null || password == null){
            return Result.fail("网络连接失败");
        }
        User user = userMapper.findByUsername(username);
        if (user != null){
            return Result.fail("用户名重复");
        }
        // uid = username_timestamp
        String uid = username + "_" + System.currentTimeMillis();
        // 加密密码
        String encodedPassword = passwordEncoder.encode(password);
        user = new User(uid, username, encodedPassword, "1");
        if (userMapper.insert(user) <= 0)
            return Result.fail("注册失败");
        return Result.success();
    }

    // 分页查询用户
    @Override
    public Map<String, Object> getUsersPage(Integer pageNum, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        try{
            int total = userMapper.countUsers();
            List<User> users = userMapper.findUsersPage(pageSize, pageSize * (pageNum - 1));
            result.put("status", "success");
            result.put("total", total);
            result.put("rows", users);
            return result;
        }
        catch (Exception e){
            result.put("status", "fail");
            return result;
        }
    }

    // 过滤用户名，分页查询用户
    @Override
    public Map<String, Object> getUsersByUsernamePage(String username, Integer pageNum, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        try{
            int total = userMapper.countUsersByUsername(username);
            List<User> users = userMapper.findUsersByUsernamePage(username, pageSize, pageSize * (pageNum - 1));
            result.put("status", "success");
            result.put("total", total);
            result.put("rows", users);
            return result;
        }
        catch (Exception e){
            result.put("status", "fail");
            return result;
        }
    }


    // 根据用户id查询用户信息
    @Override
    public Map<String, Object> getUserById(String uid) {
        Map<String, Object> result = new HashMap<>();
        User user = userMapper.findByUid(uid);
        if (user == null){
            result.put("status", "fail");
            result.put("msg", "用户不存在");
            return result;
        }
        result.put("status", "success");
        result.put("data", user);
        return result;
    }

    // 修改用户信息
    @Override
    public Map<String, Object> updateUser(User user) {
        Map<String, Object> result = new HashMap<>();
        try{
            User oldUser = userMapper.findByUid(user.getUid());
            if (oldUser == null){
                result.put("status", "fail");
                result.put("msg", "用户不存在");
                return result;
            }
            if (!user.getUsername().equals(oldUser.getUsername())){
                if (userMapper.findByUsername(user.getUsername()) != null){
                    result.put("status", "fail");
                    result.put("msg", "修改失败，用户名重复");
                    return result;
                }
            }
            if (user.getPassword() == null){
                // 除了密码更新用户信息
                userMapper.updateByUidExceptPassword(user.getUid(), user.getUsername(), user.getAuthority());
                result.put("status", "success");
                result.put("msg", "修改成功");
                return result;
            }
            else{
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                if (userMapper.updateByUid(user.getUid(), user.getUsername(), user.getPassword(), user.getAuthority()) <= 0){
                    result.put("status", "fail");
                    result.put("msg", "修改失败");
                    return result;
                }
                result.put("status", "success");
                result.put("msg", "修改成功");
                return result;
            }
        }
        catch (Exception e){
            result.put("status", "fail");
            result.put("msg", "修改失败，服务器内部错误");
            return result;
        }
    }

    // 删除用户
    @Override
    public Map<String, Object> deleteUser(String uid) {
        Map<String, Object> result = new HashMap<>();
        if (userMapper.delByUid(uid) <= 0){
            result.put("status", "fail");
            result.put("msg", "删除失败");
            return result;
        }
        result.put("status", "success");
        result.put("msg", "删除成功");
        return result;
    }

    // 重置密码
    @Override
    public Map<String, Object> resetPassword(String uid, String password) {
        Map<String, Object> result = new HashMap<>();
        if (password == null){
            result.put("status", "fail");
            result.put("msg", "密码不能为空");
            return result;
        }
        User user = userMapper.findByUid(uid);
        String encodedPassword = passwordEncoder.encode(password);
        user.setPassword(encodedPassword);
        if (userMapper.updateByUid(user.getUid(), user.getUsername(), user.getPassword(), user.getAuthority()) <= 0){
            result.put("status", "fail");
            result.put("msg", "重置密码失败");
            return result;
        }
        result.put("status", "success");
        result.put("msg", "重置密码成功");
        return result;
    }

    // 修改密码
    @Override
    public Map<String, Object> updatePassword(String uid, String oldPassword, String newPassword) {
        Map<String, Object> result = new HashMap<>();
        if (oldPassword == null || newPassword == null){
            result.put("status", "fail");
            result.put("msg", "修改密码失败，密码不能为空");
            return result;
        }
        User user = userMapper.findByUid(uid);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())){
            result.put("status", "fail");
            result.put("msg", "修改密码失败，旧密码错误");
            return result;
        }
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        if (userMapper.updateByUid(user.getUid(), user.getUsername(), user.getPassword(), user.getAuthority()) <= 0){
            result.put("status", "fail");
            result.put("msg", "修改密码失败，服务器内部错误");
            return result;
        }
        result.put("status", "success");
        result.put("msg", "修改密码成功");
        return result;
    }

    // 新建用户
    @Override
    public Map<String, Object> insertUser(User user) {
        Map<String, Object> result = new HashMap<>();
        // 检查信息是否完整
        if (user.getUsername() == null || user.getPassword() == null || user.getAuthority() == null){
            result.put("status", "fail");
            result.put("msg", "新建用户失败，信息填写不完整");
            return result;
        }
        // 查询用户名是否冲突
        if (userMapper.findByUsername(user.getUsername()) != null){
            result.put("status", "fail");
            result.put("msg", "新建用户失败，用户名已存在");
            return result;
        }
        String uid = user.getUsername() + "_" + System.currentTimeMillis();
        String password = passwordEncoder.encode(user.getPassword());
        try{
            if (userMapper.insertUser(uid, user.getUsername(), password, user.getAuthority()) <= 0){
                result.put("status", "fail");
                result.put("msg", "新建用户失败， 服务器内部问题");
                return result;
            }
            result.put("status", "success");
            result.put("msg", "新建用户成功");
            return result;
        }
        catch (Exception e){
            result.put("status", "fail");
            result.put("msg", "新建用户失败，服务器内部错误");
            return result;
        }
    }

    // 导出用户信息
    public ByteArrayInputStream exportUsers() {
        // 1. 查询数据
        List<User> users = userMapper.findAll();

        // 2. 数据转换
        List<UserExportVO> data = users.stream().map(user ->
                new UserExportVO(
                        user.getUid(),
                        user.getUsername(),
                        user.getPassword(),
                        user.getAuthority()
                )).collect(Collectors.toList());

        // 3. 生成Excel流
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out)
                .head(UserExportVO.class)
                .sheet("用户列表")
                .doWrite(data);

        return new ByteArrayInputStream(out.toByteArray());
    }
}
