-- ============================================
-- IoT洗衣机系统重构 - 数据库迁移脚本
-- 版本: V1
-- 日期: 2024
-- ============================================

-- ============================================
-- 1. 用户表重构 (iot_user)
-- ============================================
ALTER TABLE iot_user ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'CONSUMER' COMMENT '用户角色: CONSUMER, ADMIN, SUPER_ADMIN';
ALTER TABLE iot_user ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '账户状态: ACTIVE, DISABLED';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_user_role ON iot_user(role);
CREATE INDEX IF NOT EXISTS idx_user_status ON iot_user(status);

-- ============================================
-- 2. 新增: 管理者-设备关联表 (iot_admin_device)
-- ============================================
CREATE TABLE IF NOT EXISTS iot_admin_device (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_user_id BIGINT NOT NULL COMMENT '管理者用户ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    bind_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_admin_device (admin_user_id, device_id),
    INDEX idx_admin_user (admin_user_id),
    INDEX idx_device (device_id)
);

-- ============================================
-- 3. 新增: 用户-设备绑定表 (iot_user_device_binding)
-- ============================================
CREATE TABLE IF NOT EXISTS iot_user_device_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '终端用户ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    bind_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, REMOVED',
    UNIQUE KEY uk_user_device (user_id, device_id),
    INDEX idx_user (user_id),
    INDEX idx_device (device_id)
);

-- ============================================
-- 4. 新增: 用户权限表 (iot_user_permission)
-- ============================================
CREATE TABLE IF NOT EXISTS iot_user_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '被授权用户ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    granted_by BIGINT NOT NULL COMMENT '授权管理者ID',
    permission_type VARCHAR(20) DEFAULT 'UNLIMITED' COMMENT 'UNLIMITED, TIME_RANGE, COUNT_LIMIT',
    start_time TIME COMMENT '允许使用开始时间',
    end_time TIME COMMENT '允许使用结束时间',
    remaining_count INT COMMENT '剩余使用次数',
    expire_date DATE COMMENT '权限过期日期',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, EXPIRED, REVOKED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_device (user_id, device_id),
    INDEX idx_granted_by (granted_by)
);

-- ============================================
-- 5. 新增: 预约表 (iot_reservation)
-- ============================================
CREATE TABLE IF NOT EXISTS iot_reservation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_no VARCHAR(32) UNIQUE COMMENT '预约编号',
    user_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    reserved_date DATE NOT NULL COMMENT '预约日期',
    start_time TIME NOT NULL COMMENT '预约开始时间',
    end_time TIME NOT NULL COMMENT '预约结束时间',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, CONFIRMED, CANCELLED, EXPIRED, USED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device_date (device_id, reserved_date),
    INDEX idx_user (user_id)
);

-- ============================================
-- 6. 新增: 排队表 (iot_queue)
-- ============================================
CREATE TABLE IF NOT EXISTS iot_queue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    queue_position INT NOT NULL COMMENT '队列位置',
    estimated_wait_minutes INT COMMENT '预计等待分钟',
    status VARCHAR(20) DEFAULT 'WAITING' COMMENT 'WAITING, NOTIFIED, EXPIRED, CANCELLED',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    notify_time DATETIME COMMENT '通知时间',
    expire_time DATETIME COMMENT '过期时间',
    INDEX idx_device_status (device_id, status),
    INDEX idx_user (user_id)
);

-- ============================================
-- 7. 设备表扩展 (iot_device)
-- ============================================
ALTER TABLE iot_device ADD COLUMN IF NOT EXISTS owner_id BIGINT COMMENT '设备所有者(管理者)ID';
ALTER TABLE iot_device ADD COLUMN IF NOT EXISTS pricing_mode VARCHAR(20) DEFAULT 'PER_USE' COMMENT 'FREE, PER_USE, PER_MINUTE';
ALTER TABLE iot_device ADD COLUMN IF NOT EXISTS price_per_use DECIMAL(10,2) DEFAULT 0 COMMENT '每次使用价格';
ALTER TABLE iot_device ADD COLUMN IF NOT EXISTS price_per_minute DECIMAL(10,4) DEFAULT 0 COMMENT '每分钟价格';
ALTER TABLE iot_device ADD COLUMN IF NOT EXISTS current_order_id BIGINT COMMENT '当前进行中的订单ID';
ALTER TABLE iot_device ADD COLUMN IF NOT EXISTS current_queue_length INT DEFAULT 0 COMMENT '当前排队人数';

-- ============================================
-- 8. 订单表扩展 (iot_order)
-- ============================================
ALTER TABLE iot_order ADD COLUMN IF NOT EXISTS reservation_id BIGINT COMMENT '关联预约ID';
ALTER TABLE iot_order ADD COLUMN IF NOT EXISTS queue_id BIGINT COMMENT '关联排队ID';
ALTER TABLE iot_order ADD COLUMN IF NOT EXISTS wash_mode VARCHAR(20) COMMENT '洗衣模式';
ALTER TABLE iot_order ADD COLUMN IF NOT EXISTS device_owner_id BIGINT COMMENT '设备所有者ID';
ALTER TABLE iot_order ADD COLUMN IF NOT EXISTS platform_fee DECIMAL(10,2) DEFAULT 0 COMMENT '平台服务费';
ALTER TABLE iot_order ADD COLUMN IF NOT EXISTS owner_income DECIMAL(10,2) DEFAULT 0 COMMENT '设备所有者收入';

-- ============================================
-- 9. 新增: 收益明细表 (iot_income_record)
-- ============================================
CREATE TABLE IF NOT EXISTS iot_income_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_user_id BIGINT NOT NULL COMMENT '管理者ID',
    device_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    order_amount DECIMAL(10,2) COMMENT '订单金额',
    platform_fee DECIMAL(10,2) COMMENT '平台抽成',
    net_income DECIMAL(10,2) COMMENT '净收入',
    settle_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, SETTLED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    settle_time DATETIME,
    INDEX idx_admin (admin_user_id),
    INDEX idx_device (device_id),
    INDEX idx_order (order_id)
);

-- ============================================
-- 10. 新增: 消息通知记录表 (iot_notification)
-- ============================================
CREATE TABLE IF NOT EXISTS iot_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL COMMENT 'RESERVATION_REMIND, WASH_COMPLETE, QUEUE_TURN, DEVICE_FAULT, SYSTEM',
    title VARCHAR(100),
    content TEXT,
    related_id BIGINT COMMENT '关联业务ID',
    is_read TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_unread (user_id, is_read)
);
