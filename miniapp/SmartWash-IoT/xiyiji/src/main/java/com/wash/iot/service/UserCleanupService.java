package com.wash.iot.service;

import com.wash.iot.entity.User;
import com.wash.iot.enums.UserRole;
import com.wash.iot.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户清理服务
 * 定期清理无效用户数据，保持系统数据质量
 */
@Slf4j
@Service
public class UserCleanupService {

    @Autowired
    private UserRepository userRepository;
    
    @Value("${app.user-cleanup.enabled:true}")
    private boolean cleanupEnabled;
    
    @Value("${app.user-cleanup.inactive-months:6}")
    private int inactiveMonths;
    
    @Value("${app.user-cleanup.no-order-months:12}")
    private int noOrderMonths;
    
    @Value("${app.user-cleanup.dry-run:false}")
    private boolean dryRun;

    /**
     * 定期清理无效用户
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupInactiveUsers() {
        if (!cleanupEnabled) {
            log.debug("用户清理功能已禁用");
            return;
        }
        
        log.info("开始执行用户清理任务");
        
        try {
            // 1. 清理长时间未活跃的用户
            cleanupInactiveUsersByLastActiveTime();
            
            // 2. 清理没有订单记录的长期未登录用户
            cleanupUsersWithoutOrders();
            
            // 3. 清理测试用户（如果存在）
            cleanupTestUsers();
            
            log.info("用户清理任务执行完成");
        } catch (Exception e) {
            log.error("用户清理任务执行失败", e);
        }
    }
    
    /**
     * 根据最后活跃时间清理用户
     */
    private void cleanupInactiveUsersByLastActiveTime() {
        LocalDateTime threshold = LocalDateTime.now().minusMonths(inactiveMonths);
        List<User> inactiveUsers = userRepository.findInactiveUsersBefore(threshold);
        
        log.info("找到{}个长时间未活跃的用户（阈值：{}个月）", inactiveUsers.size(), inactiveMonths);
        
        for (User user : inactiveUsers) {
            // 跳过管理员用户
            if (UserRole.ADMIN.name().equals(user.getRole()) || UserRole.SUPER_ADMIN.name().equals(user.getRole())) {
                log.debug("跳过管理员用户：{}", user.getId());
                continue;
            }
            
            // 跳过有消费记录的用户
            if (user.getTotalConsumption() != null && user.getTotalConsumption().doubleValue() > 0) {
                log.debug("跳过有消费记录的用户：{}", user.getId());
                continue;
            }
            
            if (dryRun) {
                log.info("[DRY RUN] 将用户标记为非活跃状态：{}", user.getId());
            } else {
                // 软删除：标记为非活跃状态，而不是物理删除
                user.setStatus("INACTIVE");
                userRepository.save(user);
                log.info("用户已标记为非活跃状态：{}", user.getId());
            }
        }
    }
    
    /**
     * 清理没有订单记录的长期未登录用户
     */
    private void cleanupUsersWithoutOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMonths(noOrderMonths);
        List<User> usersWithoutOrders = userRepository.findUsersWithoutOrders();
        
        log.info("找到{}个没有订单记录的用户", usersWithoutOrders.size());
        
        for (User user : usersWithoutOrders) {
            // 跳过管理员用户
            if (UserRole.ADMIN.name().equals(user.getRole()) || UserRole.SUPER_ADMIN.name().equals(user.getRole())) {
                log.debug("跳过管理员用户：{}", user.getId());
                continue;
            }
            
            // 检查是否长期未登录
            if (user.getLastLoginTime() != null && user.getLastLoginTime().isAfter(threshold)) {
                log.debug("跳过最近登录过的用户：{}", user.getId());
                continue;
            }
            
            if (dryRun) {
                log.info("[DRY RUN] 将用户标记为非活跃状态：{}", user.getId());
            } else {
                // 软删除：标记为非活跃状态，而不是物理删除
                user.setStatus("INACTIVE");
                userRepository.save(user);
                log.info("无订单记录用户已标记为非活跃状态：{}", user.getId());
            }
        }
    }
    
    /**
     * 清理测试用户
     */
    private void cleanupTestUsers() {
        // 查找可能的测试用户（根据特定规则）
        List<User> testUsers = userRepository.findByNickNameContaining("test");
        testUsers.addAll(userRepository.findByNickNameContaining("测试"));
        
        log.info("找到{}个可能的测试用户", testUsers.size());
        
        for (User user : testUsers) {
            // 跳过有消费记录的用户
            if (user.getTotalConsumption() != null && user.getTotalConsumption().doubleValue() > 0) {
                log.debug("跳过有消费记录的测试用户：{}", user.getId());
                continue;
            }
            
            if (dryRun) {
                log.info("[DRY RUN] 将测试用户标记为非活跃状态：{}", user.getId());
            } else {
                // 软删除：标记为非活跃状态，而不是物理删除
                user.setStatus("INACTIVE");
                userRepository.save(user);
                log.info("测试用户已标记为非活跃状态：{}", user.getId());
            }
        }
    }
    
    /**
     * 手动执行用户清理（用于管理界面）
     */
    @Transactional
    public void executeCleanupManually() {
        log.info("手动执行用户清理任务");
        cleanupInactiveUsers();
    }
    
    /**
     * 恢复被标记为非活跃的用户
     */
    @Transactional
    public boolean reactivateUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("用户不存在：{}", userId);
            return false;
        }
        
        if (!"INACTIVE".equals(user.getStatus())) {
            log.warn("用户状态不是非活跃状态：{}", userId);
            return false;
        }
        
        user.setStatus("ACTIVE");
        user.setLastActiveTime(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("用户已重新激活：{}", userId);
        return true;
    }
}