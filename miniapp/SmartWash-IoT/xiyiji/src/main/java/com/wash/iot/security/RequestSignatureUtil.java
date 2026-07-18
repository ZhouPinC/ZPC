package com.wash.iot.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求签名验证工具
 * 用于防止API被恶意调用
 */
@Slf4j
@Component
public class RequestSignatureUtil {

    @Value("${app.api.signature.enabled:true}")
    private boolean signatureEnabled;
    
    @Value("${app.api.signature.secret:XiYiJi2024SignatureSecret}")
    private String signatureSecret;
    
    @Value("${app.api.signature.expiration-time:300}")
    private long expirationTime; // 签名过期时间，单位：秒

    /**
     * 验证请求签名
     */
    public boolean verifySignature(HttpServletRequest request) {
        // 如果未启用签名验证，直接返回true
        if (!signatureEnabled) {
            return true;
        }
        
        try {
            // 获取请求参数
            Map<String, String> params = getRequestParams(request);
            
            // 获取签名
            String signature = params.remove("signature");
            if (!StringUtils.hasText(signature)) {
                log.warn("请求缺少签名参数");
                return false;
            }
            
            // 获取时间戳
            String timestamp = params.remove("timestamp");
            if (!StringUtils.hasText(timestamp)) {
                log.warn("请求缺少时间戳参数");
                return false;
            }
            
            // 验证时间戳是否在有效期内
            long currentTime = System.currentTimeMillis() / 1000;
            long requestTime = Long.parseLong(timestamp);
            if (Math.abs(currentTime - requestTime) > expirationTime) {
                log.warn("请求签名已过期，当前时间：{}，请求时间：{}", currentTime, requestTime);
                return false;
            }
            
            // 生成签名并比较
            String expectedSignature = generateSignature(params, timestamp);
            boolean isValid = signature.equals(expectedSignature);
            
            if (!isValid) {
                log.warn("请求签名验证失败，期望签名：{}，实际签名：{}", expectedSignature, signature);
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("签名验证过程中发生错误", e);
            return false;
        }
    }

    /**
     * 生成请求签名
     */
    public String generateSignature(Map<String, String> params, String timestamp) {
        try {
            // 将参数按字典序排序
            String[] sortedKeys = params.keySet().toArray(new String[0]);
            Arrays.sort(sortedKeys);
            
            // 构建待签名字符串
            StringBuilder sb = new StringBuilder();
            for (String key : sortedKeys) {
                if (StringUtils.hasText(params.get(key))) {
                    sb.append(key).append("=").append(params.get(key)).append("&");
                }
            }
            sb.append("timestamp=").append(timestamp);
            sb.append("&secret=").append(signatureSecret);
            
            // 生成MD5签名
            return DigestUtils.md5DigestAsHex(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("生成签名失败", e);
            throw new RuntimeException("生成签名失败", e);
        }
    }
    
    /**
     * 生成API调用签名（供客户端使用）
     */
    public String generateClientSignature(Map<String, String> params) {
        try {
            // 添加时间戳
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            params.put("timestamp", timestamp);
            
            // 生成签名
            String signature = generateSignature(params, timestamp);
            
            return signature;
        } catch (Exception e) {
            log.error("生成客户端签名失败", e);
            throw new RuntimeException("生成客户端签名失败", e);
        }
    }

    /**
     * 获取请求参数
     */
    private Map<String, String> getRequestParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        
        // 获取URL参数
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String paramValue = request.getParameter(paramName);
            params.put(paramName, paramValue);
        }
        
        return params;
    }
    
    /**
     * 检查是否为敏感API，需要签名验证
     */
    public boolean isSensitiveApi(String requestUri) {
        // 定义需要签名验证的API路径
        String[] sensitiveApis = {
            "/api/v1/auth/wx-phone-login",
            "/api/v1/auth/bind-phone",
            "/api/v1/admin/users/cleanup",
            "/api/v1/admin/users/{userId}/activate"
        };
        
        for (String api : sensitiveApis) {
            if (requestUri.contains(api)) {
                return true;
            }
        }
        
        return false;
    }
}