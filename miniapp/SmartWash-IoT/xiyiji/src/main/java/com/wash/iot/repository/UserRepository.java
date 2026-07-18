package com.wash.iot.repository;

import com.wash.iot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 用户Repository接口
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据微信openId查询用户
     */
    Optional<User> findByOpenId(String openId);

    /**
     * 根据用户标识符查询用户
     */
    Optional<User> findByUserIdentifier(String userIdentifier);

    /**
     * 根据手机号查询用户
     */
    Optional<User> findByPhone(String phone);

    /**
     * 检查手机号是否存在
     */
    boolean existsByPhone(String phone);
    
    /**
     * 检查用户标识符是否存在
     */
    boolean existsByUserIdentifier(String userIdentifier);

    /**
     * 根据角色查找用户
     */
    List<User> findByRole(String role);

    /**
     * 根据状态查找用户
     */
    List<User> findByStatus(String status);

    /**
     * 根据角色和状态查找用户
     */
    List<User> findByRoleAndStatus(String role, String status);

    /**
     * 根据ID列表查找用户
     */
    List<User> findByIdIn(List<Long> userIds);

    /**
     * 查找活跃用户（有订单记录的用户）
     */
    @Query("SELECT DISTINCT u FROM User u JOIN Order o ON u.id = o.userId")
    List<User> findActiveUsers();

    /**
     * 统计指定角色的用户数量
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") String role);

    /**
     * 统计指定状态的用户数量
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    long countByStatus(@Param("status") String status);

    /**
     * 根据用户名模糊查找用户
     */
    @Query("SELECT u FROM User u WHERE u.nickName LIKE %:nickName%")
    List<User> findByNickNameContaining(@Param("nickName") String nickName);

    /**
     * 查找最近登录的用户
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginTime IS NOT NULL ORDER BY u.lastLoginTime DESC")
    List<User> findRecentlyLoggedInUsers();

    /**
     * 查找总消费金额大于指定金额的用户
     */
    @Query("SELECT u FROM User u WHERE u.totalConsumption > :amount ORDER BY u.totalConsumption DESC")
    List<User> findUsersByTotalConsumptionGreaterThan(@Param("amount") java.math.BigDecimal amount);
    
    /**
     * 查找长时间未活跃的用户
     */
    @Query("SELECT u FROM User u WHERE u.lastActiveTime < :threshold OR u.lastActiveTime IS NULL")
    List<User> findInactiveUsersBefore(@Param("threshold") java.time.LocalDateTime threshold);
    
    /**
     * 查找没有订单记录的用户
     */
    @Query("SELECT u FROM User u WHERE u.id NOT IN (SELECT DISTINCT o.userId FROM Order o)")
    List<User> findUsersWithoutOrders();
    
    /**
     * 查找长时间未登录的用户
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginTime < :threshold OR u.lastLoginTime IS NULL")
    List<User> findUsersNotLoggedInSince(@Param("threshold") java.time.LocalDateTime threshold);
}
