package com.wash.iot.security;

import com.wash.iot.common.exception.UnauthorizedException;
import com.wash.iot.enums.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 角色权限检查切面
 */
@Slf4j
@Aspect
@Component
public class RoleCheckAspect {

    @Around("@annotation(com.wash.iot.security.RoleRequired)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RoleRequired roleRequired = method.getAnnotation(RoleRequired.class);

        // 检查是否需要登录
        if (roleRequired.requireLogin()) {
            UserPrincipal currentUser = JwtAuthenticationFilter.getCurrentUser();
            if (currentUser == null) {
                throw new UnauthorizedException("请先登录");
            }

            if (currentUser.isSuperAdmin()) {
                return joinPoint.proceed();
            }

            // 检查角色权限
            UserRole[] allowedRoles = roleRequired.value();
            if (allowedRoles.length > 0) {
                boolean hasRole = Arrays.stream(allowedRoles)
                        .anyMatch(role -> isAllowed(currentUser.getRole(), role));
                
                if (!hasRole) {
                    log.warn("User {} with role {} tried to access {} but requires roles {}", 
                            currentUser.getUserId(), 
                            currentUser.getRole(),
                            method.getName(),
                            Arrays.toString(allowedRoles));
                    throw new UnauthorizedException("权限不足");
                }
            }
        }

        return joinPoint.proceed();
    }

    private boolean isAllowed(UserRole actualRole, UserRole requiredRole) {
        if (actualRole == null || requiredRole == null) {
            return false;
        }
        if (actualRole == UserRole.SUPER_ADMIN) {
            return true;
        }
        if (actualRole == requiredRole) {
            return true;
        }
        if (actualRole == UserRole.ADMIN) {
            return requiredRole == UserRole.CONSUMER;
        }
        return false;
    }
}
