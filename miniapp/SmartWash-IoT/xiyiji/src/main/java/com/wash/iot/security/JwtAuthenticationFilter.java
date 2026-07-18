package com.wash.iot.security;

import com.wash.iot.enums.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    // 存储当前请求的用户信息
    private static final ThreadLocal<UserPrincipal> currentUser = new ThreadLocal<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = getTokenFromRequest(request);

            if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
                Long userId = tokenProvider.getUserIdFromToken(token);
                UserRole role = tokenProvider.getRoleFromToken(token);
                
                UserPrincipal principal = new UserPrincipal(userId, role);
                currentUser.set(principal);
                
                log.debug("Authenticated user: {}, role: {}", userId, role);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 清理ThreadLocal，防止内存泄漏
            currentUser.remove();
        }
    }

    /**
     * 从请求头中提取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 获取当前登录用户
     */
    public static UserPrincipal getCurrentUser() {
        return currentUser.get();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        UserPrincipal principal = currentUser.get();
        return principal != null ? principal.getUserId() : null;
    }

    /**
     * 检查当前用户是否已登录
     */
    public static boolean isAuthenticated() {
        return currentUser.get() != null;
    }

    /**
     * 检查当前用户是否是管理员
     */
    public static boolean isAdmin() {
        UserPrincipal principal = currentUser.get();
        return principal != null && principal.isAdmin();
    }
}
