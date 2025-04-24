package com.RoadCloudVisualizationSystem.filter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.RoadCloudVisualizationSystem.entity.Permission;
import com.RoadCloudVisualizationSystem.entity.RolePermission;
import com.RoadCloudVisualizationSystem.entity.User;
import com.RoadCloudVisualizationSystem.mapper.RolePermissionMapper;
import com.RoadCloudVisualizationSystem.mapper.UserMapper;

import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class PermissionFilter extends OncePerRequestFilter {

    @Autowired
    private UserMapper userMapper; // 用于获取数据库中的权限信息
    @Autowired
    private RolePermissionMapper rolePermissionMapper; // 用于获取数据库中的权限信息
    



    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException, java.io.IOException {
//        if (true){
//            filterChain.doFilter(request, response);
//            return;
//        }
        String url = request.getRequestURI();
        String method = request.getMethod(); // 获取请求的 HTTP 方法
//        System.out.println("url:" +url);
//        System.out.println("method:" + method);

        // 查询数据库中是否有对应的权限（这里假设你已将权限和URL绑定）
        List<Permission> permissions = userMapper.findPermissionsByUrlAndMethod(url, method);

        if (permissions.isEmpty()) {
            filterChain.doFilter(request, response); // 如果没有配置的权限，放行
            return;
        }

        // 获取当前登录用户的角色或权限
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        System.out.println("authentication:" + authentication);


        if (authentication == null || !hasPermission(authentication, permissions, request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 没有权限，返回403
            response.getWriter().write("Access Denied"); // 可自定义提示
            return;
        }

        filterChain.doFilter(request, response); // 放行
    }

    // 判断用户是否有权限访问
    private boolean hasPermission(Authentication authentication, List<Permission> permissions,HttpServletRequest request) {
        if(permissions.isEmpty()){
            return true;//如果查不到说明不限制
        }
        //System.out.println(authentication);
        String permissionid = permissions.get(0).getPermissionid();
        List<RolePermission> result = rolePermissionMapper.findRolePermissionsByPermissionid(permissionid);
        List<String> allRole = new ArrayList();
        for (RolePermission rolePermission : result) {
            allRole.add(rolePermission.getRoleid());
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            User user = (User) principal;
            String authority = user.getAuthority();
            for(String demo : allRole){
                if (authority .equals(demo)) {
                    return true;
                }
            }
        }
        return false;
    }
}
