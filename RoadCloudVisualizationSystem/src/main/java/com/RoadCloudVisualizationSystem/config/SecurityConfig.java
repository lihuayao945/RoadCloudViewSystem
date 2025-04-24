package com.RoadCloudVisualizationSystem.config;


import com.RoadCloudVisualizationSystem.filter.CustomFilter;
import com.RoadCloudVisualizationSystem.filter.JwtAuthenticationTokenFilter;
import com.RoadCloudVisualizationSystem.filter.PermissionFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    @Autowired
    private CustomFilter customFilter;

    @Autowired
    private PermissionFilter permissionFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(CsrfConfigurer::disable) // 基于token，不需要csrf
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 基于token，不需要session
                .authorizeHttpRequests((authz) -> authz
                        //王浩
                        .requestMatchers("/login",  "/register", "/insertRcu","/system/mqtt/**","/system/user/**","/system/rcu/**").permitAll()
                        //欲儿
                        .requestMatchers("/system/vehicle/**","/system/log/**").permitAll()
                        .requestMatchers("/menu/vehicle/**","/menu/device/**","/menu/top/**").permitAll()
                        .requestMatchers("/system/creat/**", "/system/parameter/**").permitAll()
                        .requestMatchers("/system/role/**","/system/permission/**", "/system/role/permission/**").permitAll()
                        // 静态资源授权
                        .requestMatchers("**.jpg").permitAll()
                        .requestMatchers("/xlsxs/**.xlsx", "/exports/**.xlsx", "/exports/**.csv").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(permissionFilter, AuthorizationFilter.class);

        return http.build();
    }
}
