package com.wash.iot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 安全过滤器
 * 用于验证请求签名和增强安全性
 */
@Slf4j
@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private RequestSignatureUtil requestSignatureUtil;
    
    @Autowired
    private DataEncryptionUtil dataEncryptionUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // 添加安全响应头
        addSecurityHeaders(response);
        
        // 检查是否需要验证签名
        if (requestSignatureUtil.isSensitiveApi(request.getRequestURI())) {
            if (!requestSignatureUtil.verifySignature(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"请求签名验证失败\"}");
                return;
            }
        }
        
        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }
    
    /**
     * 添加安全响应头
     */
    private void addSecurityHeaders(HttpServletResponse response) {
        // 防止XSS攻击
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // 防止MIME类型嗅探
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // 防止点击劫持
        response.setHeader("X-Frame-Options", "DENY");
        
        // 强制HTTPS（如果可用）
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        
        // 内容安全策略
        response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'");
        
        // 引用策略
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // 权限策略
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
    }
}