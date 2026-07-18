-- 共享洗衣机物联网系统数据库初始化脚本
-- 请在MySQL中执行此脚本以创建数据库

-- 创建数据库
CREATE DATABASE IF NOT EXISTS iot_wash_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE iot_wash_db;

-- 创建用户表（如果不存在）
CREATE TABLE IF NOT EXISTS iot_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    open_id VARCHAR(100) UNIQUE NOT NULL COMMENT '微信openId',
    union_id VARCHAR(100) COMMENT '微信unionId',
    phone VARCHAR(20) COMMENT '手机号',
    nick_name VARCHAR(100) COMMENT '昵称',
    real_name VARCHAR(100) COMMENT '真实姓名',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    role VARCHAR(20) DEFAULT 'CONSUMER' COMMENT '角色：CONSUMER/ADMIN/SUPER_ADMIN',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED',
    balance DECIMAL(10,2) DEFAULT 0.00 COMMENT '余额',
    total_consumption DECIMAL(10,2) DEFAULT 0.00 COMMENT '总消费金额',
    points INT DEFAULT 0 COMMENT '积分',
    total_orders INT DEFAULT 0 COMMENT '总订单数',
    session_key VARCHAR(200) COMMENT '微信session_key',
    last_login_time DATETIME COMMENT '最后登录时间',
    token_expire_time DATETIME COMMENT 'token过期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '用户表';

-- 创建设备表（如果不存在）
CREATE TABLE IF NOT EXISTS iot_device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_sn VARCHAR(50) UNIQUE NOT NULL COMMENT '设备序列号',
    name VARCHAR(100) COMMENT '设备名称',
    location VARCHAR(200) COMMENT '设备位置',
    status VARCHAR(20) DEFAULT 'IDLE' COMMENT '设备状态：IDLE/RUNNING/PAUSED/FINISHED/OFFLINE/FAULT',
    pricing_mode VARCHAR(20) DEFAULT 'PER_USE' COMMENT '计费模式',
    price_per_use DECIMAL(10,2) DEFAULT 3.00 COMMENT '单次使用价格',
    price_per_minute DECIMAL(10,2) COMMENT '每分钟价格',
    current_order_no VARCHAR(50) COMMENT '当前订单号',
    current_user_id BIGINT COMMENT '当前使用用户ID',
    wash_mode VARCHAR(50) COMMENT '当前洗衣模式',
    wash_mode_name VARCHAR(50) COMMENT '洗衣模式名称',
    wash_start_time DATETIME COMMENT '洗衣开始时间',
    wash_duration INT COMMENT '洗衣时长(分钟)',
    estimated_end_time DATETIME COMMENT '预计结束时间',
    pause_time DATETIME COMMENT '暂停时间',
    paused_duration INT DEFAULT 0 COMMENT '已暂停时长(秒)',
    last_heartbeat DATETIME COMMENT '最后心跳时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '设备表';

-- 创建用户设备绑定表（如果不存在）
CREATE TABLE IF NOT EXISTS iot_user_device_binding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    bind_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/REMOVED',
    INDEX idx_user_id (user_id),
    INDEX idx_device_id (device_id)
) COMMENT '用户设备绑定表';

-- 创建订单表（如果不存在）
CREATE TABLE IF NOT EXISTS iot_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) UNIQUE NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    device_id BIGINT COMMENT '设备ID',
    device_sn VARCHAR(50) COMMENT '设备序列号',
    amount DECIMAL(10,2) COMMENT '订单金额',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '订单状态',
    wash_mode VARCHAR(50) COMMENT '洗衣模式',
    wash_mode_name VARCHAR(50) COMMENT '洗衣模式名称',
    duration_minutes INT COMMENT '洗衣时长(分钟)',
    payment_method VARCHAR(20) COMMENT '支付方式',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    pay_time DATETIME COMMENT '支付时间',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_device_id (device_id),
    INDEX idx_order_no (order_no)
) COMMENT '订单表';

-- 插入测试设备（如果不存在）
INSERT IGNORE INTO iot_device (device_sn, name, location, status, pricing_mode, price_per_use)
VALUES
    ('WASH_001', '1号洗衣机', '1号楼1层', 'IDLE', 'PER_USE', 3.00),
    ('WASH_002', '2号洗衣机', '1号楼2层', 'IDLE', 'PER_USE', 3.00),
    ('WASH_003', '3号洗衣机', '2号楼1层', 'IDLE', 'PER_USE', 3.00);

-- 显示创建的表
SHOW TABLES;

-- 提示信息
SELECT '数据库初始化完成！' as message;