package com.wash.iot.config;

import com.wash.iot.security.SecurityFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 应用配置类
 * 配置定时任务和安全过滤器
 */
@Configuration
@EnableScheduling
public class ApplicationConfig {

    /**
     * 注册安全过滤器
     */
    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> securityFilterRegistration(SecurityFilter securityFilter) {
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(securityFilter);
        registration.addUrlPatterns("/*");
        registration.setName("securityFilter");
        registration.setOrder(1); // 设置过滤器顺序，确保在其他过滤器之前执行
        return registration;
    }
}