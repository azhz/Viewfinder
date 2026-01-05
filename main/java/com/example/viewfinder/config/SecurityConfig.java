package com.example.viewfinder.config;

import com.example.viewfinder.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 配置安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/users/register").permitAll()
                        .requestMatchers("/api/v1/users/login").permitAll()
                        .requestMatchers("/api/v1/users/verification-code").permitAll()
                        .requestMatchers("/api/v1/users/*/avatar").permitAll() // 允许访问头像
                        .requestMatchers("/uploads/**").permitAll() // 允许访问上传的文件
                        .requestMatchers("/default-avatar.png").permitAll() // 允许访问默认头像
                        .requestMatchers("/static/**").permitAll() // 允许访问静态资源
                        .requestMatchers("/api/v1/posts").permitAll() // 允许访问帖子列表
                        .requestMatchers("/api/v1/posts/**").permitAll() // 允许访问单个帖子
                        .requestMatchers("/api/v1/posts/user/{userId}").permitAll() // 允许访问用户帖子
                        .requestMatchers("/api/v1/comments/**").permitAll() // 允许访问评论相关接口
                        .requestMatchers("/api/v1/users/{userId}").permitAll() // 允许访问用户信息
                        .requestMatchers("/api/v1/users/*/follow/*").authenticated() // 关注/取消关注需要认证
                        .requestMatchers("/api/v1/users/profile").authenticated() // 获取当前用户信息需要认证
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 配置密码编码器
     * 使用BCryptPasswordEncoder，它会自动生成盐值
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}