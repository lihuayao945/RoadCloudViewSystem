package com.RoadCloudVisualizationSystem.utils;

import com.RoadCloudVisualizationSystem.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserInfoUtil {
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return "匿名"; // 匿名用户返回 null
        }
        return ((User) authentication.getPrincipal()).getUsername();
    }
}
