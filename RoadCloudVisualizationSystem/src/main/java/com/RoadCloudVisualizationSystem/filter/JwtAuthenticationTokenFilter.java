package com.RoadCloudVisualizationSystem.filter;

import com.RoadCloudVisualizationSystem.entity.User;
import com.RoadCloudVisualizationSystem.mapper.UserMapper;
import com.RoadCloudVisualizationSystem.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    @Autowired
    private UserMapper userMapper;

    private final RequestMatcher excludedMatcher = new OrRequestMatcher(
            new AntPathRequestMatcher("/login"),
            new AntPathRequestMatcher("/register")
//            new AntPathRequestMatcher("/test/**"),
//            new AntPathRequestMatcher("/system/mqtt/**"),
//            new AntPathRequestMatcher("/system/user/**"),
//            new AntPathRequestMatcher("/system/vehicle/**"),
//            new AntPathRequestMatcher("/system/log/**"),
//            new AntPathRequestMatcher("/system/rcu/**"),
//            new AntPathRequestMatcher("/system/creat/**"),
//            new AntPathRequestMatcher("/exports/**.xlsx"),
//            new AntPathRequestMatcher("/insertRcu"),
//            new AntPathRequestMatcher("/system/parameter"),
//            new AntPathRequestMatcher("/menu/vehicle/**"),
//            new AntPathRequestMatcher("/menu/top/**"),
//            new AntPathRequestMatcher("/menu/device/**"),
//            new AntPathRequestMatcher("/system/parameter/**"),
//            new AntPathRequestMatcher("/system/role/**"),
//            new AntPathRequestMatcher("/system/permission/**"),
//            new AntPathRequestMatcher("/system/role/permission/**")
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (excludedMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        

        // 1. 优先从Header获取Token
        String token = request.getHeader("Authorization");
        //System.out.println("tokenfromheader:" + token);
        
        // 2. 如果Header中没有，尝试从Cookie获取
        if (token == null) {
            Cookie[] cookies = request.getCookies();
            // 输出全部cookies用于调试
            if (cookies != null) {
                token = "Bearer " + getAccessTokenFromCookies(cookies);
                System.out.println(token);
            } else {
                System.out.println("没有cookies");
                // 无token拒绝访问
                sendUnauthorizedError(response, "No token");
                return;
            }
        }

        // 1. 无Token或格式错误时的处理
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 提取并验证Token
        token = token.substring(7);
        String username;
        try {
            Claims claims = JwtUtil.parseJWT(token);
            username = claims.getSubject();
        } catch (Exception e) {
            sendUnauthorizedError(response, "Invalid token");
            return;
        }

        // 3. 验证用户是否存在
        User user = userMapper.findByUsername(username);
        if (user == null) {
            sendUnauthorizedError(response, "User not found");
            return;
        }

        // 4. 设置认证信息
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorizedError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
    }

    // 从cookies中获取accessToken
    private String getAccessTokenFromCookies(Cookie[] cookies) {
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }


}