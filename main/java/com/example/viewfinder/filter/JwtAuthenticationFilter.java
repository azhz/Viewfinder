package com.example.viewfinder.filter;

import com.example.viewfinder.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * 拦截所有请求，从请求头中提取JWT token并验证
 * 如果token有效，则将用户信息设置到SecurityContext中
 *
 * @author Viewfinder Team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /**
     * 过滤器核心方法
     * 从请求头中提取token，验证并设置认证信息
     *
     * @param request HTTP请求
     * @param response HTTP响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 跳过公共路径，不进行JWT验证
        String requestURI = request.getRequestURI();
        if (isPublicPath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从请求头中获取token
        String token = getTokenFromRequest(request);

        // 验证token
        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            // 从token中获取用户名和用户ID
            String username = jwtUtil.extractUsername(token);
            Long userId = jwtUtil.extractUserId(token);

            // 如果用户名不为空且当前SecurityContext中没有认证信息
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 创建认证令牌（principal设置为userId，方便后续使用）
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, null);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 设置认证信息到SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }
    
    private boolean isPublicPath(String requestURI) {
        return requestURI.startsWith("/api/v1/posts") ||
               requestURI.startsWith("/api/v1/comments") ||
               requestURI.startsWith("/api/v1/users/") && !requestURI.contains("/avatar") && !requestURI.contains("/follow") && !requestURI.contains("/following") && !requestURI.contains("/followers") ||
               requestURI.startsWith("/uploads/") ||
               requestURI.equals("/default-avatar.png");
    }

    /**
     * 从请求头中提取token
     * 格式：Authorization: Bearer {token}
     *
     * @param request HTTP请求
     * @return JWT token，如果不存在则返回null
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
