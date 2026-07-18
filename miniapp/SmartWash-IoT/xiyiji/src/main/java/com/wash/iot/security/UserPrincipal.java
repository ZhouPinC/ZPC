package com.wash.iot.security;

import com.wash.iot.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录用户信息
 */
@Data
@AllArgsConstructor
public class UserPrincipal {
    private Long userId;
    private UserRole role;
    
    public boolean isAdmin() {
        return role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN;
    }
    
    public boolean isSuperAdmin() {
        return role == UserRole.SUPER_ADMIN;
    }
}
