package com.wash.iot.security;

import com.wash.iot.enums.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限注解
 * 用于标注Controller方法需要的角色权限
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RoleRequired {
    /**
     * 允许访问的角色列表
     */
    UserRole[] value() default {};
    
    /**
     * 是否需要登录（默认需要）
     */
    boolean requireLogin() default true;
}
