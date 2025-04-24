package com.RoadCloudVisualizationSystem.controller;

import com.RoadCloudVisualizationSystem.entity.User;
import com.RoadCloudVisualizationSystem.service.UserService;
import com.RoadCloudVisualizationSystem.utils.JwtUtil;
import com.RoadCloudVisualizationSystem.utils.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    // 用户登录接口
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody User user, HttpServletResponse response) {
        String username = user.getUsername();
        String password = user.getPassword();
        Map<String, String> result = userService.login(username, password);
        if (result.get("status").equals("fail")){
            return result;
        }
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        // 从数据库中对比查找，如果找到了会返回一个带有认证的封装后的用户，否则会报错，自动处理。（这里我们假设我们配置的security是基于数据库查找的）
        Authentication authenticate = authenticationManager.authenticate(authenticationToken);
        // 获取认证后的用户
        User AuthenUser = (User) authenticate.getPrincipal();
        String token = JwtUtil.createJWT(AuthenUser.getUsername());
        //result.put("Authorization", token);
        // 为token加Bearer 前缀
        //token = "Bearer " + token;
        //System.out.println(token);
        response.addHeader(HttpHeaders.SET_COOKIE,
                "accessToken=" + token + "; Path=/; SameSite=Lax");
        return result;
    }

    // 用户注册接口
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        return userService.register(user.getUsername(), user.getPassword());
    }

}
