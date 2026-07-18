-- 共享洗衣机系统增强数据库结构
-- 2025-12-23 版本

-- ----------------------------
-- 1. 用户表增强
-- ----------------------------
ALTER TABLE `iot_user`
ADD COLUMN `wx_session_key` varchar(128) DEFAULT NULL COMMENT '微信会话密钥',
ADD COLUMN `wx_unionid` varchar(64) DEFAULT NULL COMMENT '微信UnionID',
ADD COLUMN `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
ADD COLUMN `avatar_url` varchar(512) DEFAULT NULL COMMENT '头像URL',
ADD COLUMN `real_name` varchar(32) DEFAULT NULL COMMENT '真实姓名',
ADD COLUMN `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号',
ADD COLUMN `role` enum('CONSUMER','ADMIN','SUPER_ADMIN') DEFAULT 'CONSUMER' COMMENT '用户角色',
ADD COLUMN `status` enum('ACTIVE','DISABLED','FROZEN') DEFAULT 'ACTIVE' COMMENT '账户状态',
ADD COLUMN `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
ADD COLUMN `token_expire_time` datetime DEFAULT NULL COMMENT 'Token过期时间',
ADD COLUMN `points` int DEFAULT 0 COMMENT '积分',
ADD COLUMN `total_consumption` decimal(10,2) DEFAULT 0.00 COMMENT '累计消费金额',
ADD COLUMN `total_orders` int DEFAULT 0 COMMENT '累计订单数',
ADD INDEX `idx_role` (`role`),
ADD INDEX `idx_status` (`status`),
ADD INDEX `idx_phone` (`phone`);

-- ----------------------------
-- 2. 设备表增强
-- ----------------------------
ALTER TABLE `iot_device`
ADD COLUMN `owner_id` bigint(20) DEFAULT NULL COMMENT '设备所有者ID',
ADD COLUMN `mac_address` varchar(32) DEFAULT NULL COMMENT 'MAC地址',
ADD COLUMN `model` varchar(64) DEFAULT NULL COMMENT '设备型号',
ADD COLUMN `manufacturer` varchar(64) DEFAULT NULL COMMENT '制造商',
ADD COLUMN `install_date` date DEFAULT NULL COMMENT '安装日期',
ADD COLUMN `warranty_expire_date` date DEFAULT NULL COMMENT '保修到期日',
ADD COLUMN `current_order_id` bigint(20) DEFAULT NULL COMMENT '当前订单ID',
ADD COLUMN `current_order_no` varchar(32) DEFAULT NULL COMMENT '当前订单号',
ADD COLUMN `current_user_id` bigint(20) DEFAULT NULL COMMENT '当前使用用户ID',
ADD COLUMN `current_queue_length` int DEFAULT 0 COMMENT '当前排队长度',
ADD COLUMN `total_duration` int DEFAULT 0 COMMENT '累计工作时长(分钟)',
ADD COLUMN `total_orders` int DEFAULT 0 COMMENT '累计服务次数',
ADD COLUMN `price_per_use` decimal(10,2) DEFAULT 0.00 COMMENT '单次价格',
ADD COLUMN `price_per_minute` decimal(10,2) DEFAULT 0.00 COMMENT '每分钟价格',
ADD COLUMN `pricing_mode` enum('PER_USE','PER_MINUTE','FREE') DEFAULT 'PER_USE' COMMENT '计费模式',
ADD COLUMN `remain_seconds` int DEFAULT 0 COMMENT '剩余工作秒数',
ADD COLUMN `estimated_end_time` datetime DEFAULT NULL COMMENT '预计结束时间',
ADD COLUMN `work_start_time` datetime DEFAULT NULL COMMENT '工作开始时间',
ADD COLUMN `wash_mode` varchar(32) DEFAULT NULL COMMENT '当前洗衣模式',
ADD COLUMN `wash_mode_name` varchar(32) DEFAULT NULL COMMENT '洗衣模式名称',
ADD COLUMN `qr_code_url` varchar(512) DEFAULT NULL COMMENT '二维码图片URL',
ADD INDEX `idx_owner` (`owner_id`),
ADD INDEX `idx_status` (`status`),
ADD INDEX `idx_location` (`location`);

-- ----------------------------
-- 3. 订单表增强
-- ----------------------------
ALTER TABLE `iot_order`
ADD COLUMN `wash_mode` varchar(32) DEFAULT NULL COMMENT '洗衣模式',
ADD COLUMN `wash_mode_name` varchar(32) DEFAULT NULL COMMENT '洗衣模式名称',
ADD COLUMN `device_owner_id` bigint(20) DEFAULT NULL COMMENT '设备所有者ID',
ADD COLUMN `platform_fee` decimal(10,2) DEFAULT 0.00 COMMENT '平台服务费',
ADD COLUMN `owner_income` decimal(10,2) DEFAULT 0.00 COMMENT '设备所有者收入',
ADD COLUMN `reservation_id` bigint(20) DEFAULT NULL COMMENT '预约ID',
ADD COLUMN `queue_id` bigint(20) DEFAULT NULL COMMENT '排队ID',
ADD COLUMN `payment_method` varchar(32) DEFAULT NULL COMMENT '支付方式',
ADD COLUMN `payment_channel` varchar(32) DEFAULT NULL COMMENT '支付渠道',
ADD COLUMN `refund_amount` decimal(10,2) DEFAULT 0.00 COMMENT '退款金额',
ADD COLUMN `refund_reason` varchar(255) DEFAULT NULL COMMENT '退款原因',
ADD COLUMN `refund_time` datetime DEFAULT NULL COMMENT '退款时间',
ADD COLUMN `completion_status` enum('SUCCESS','FAILED','INTERRUPTED','TIMEOUT') DEFAULT NULL COMMENT '完成状态',
ADD COLUMN `failure_reason` varchar(255) DEFAULT NULL COMMENT '失败原因',
ADD INDEX `idx_user_device` (`user_id`, `device_id`),
ADD INDEX `idx_device_owner` (`device_owner_id`),
ADD INDEX `idx_create_time` (`create_time`);

-- ----------------------------
-- 4. 设备管理员绑定表
-- ----------------------------
CREATE TABLE `iot_admin_device` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `admin_user_id` bigint(20) NOT NULL COMMENT '管理员用户ID',
  `device_id` bigint(20) NOT NULL COMMENT '设备ID',
  `bind_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `unbind_time` datetime DEFAULT NULL COMMENT '解绑时间',
  `status` enum('ACTIVE','INACTIVE') DEFAULT 'ACTIVE' COMMENT '绑定状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_device` (`admin_user_id`, `device_id`),
  KEY `idx_admin` (`admin_user_id`),
  KEY `idx_device` (`device_id`),
  CONSTRAINT `fk_admin_device_admin` FOREIGN KEY (`admin_user_id`) REFERENCES `iot_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_admin_device_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备管理员绑定表';

-- ----------------------------
-- 5. 收益记录表
-- ----------------------------
CREATE TABLE `iot_income_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `admin_user_id` bigint(20) NOT NULL COMMENT '管理员用户ID',
  `device_id` bigint(20) NOT NULL COMMENT '设备ID',
  `order_id` bigint(20) NOT NULL COMMENT '订单ID',
  `order_amount` decimal(10,2) NOT NULL COMMENT '订单金额',
  `platform_fee` decimal(10,2) NOT NULL COMMENT '平台费用',
  `net_income` decimal(10,2) NOT NULL COMMENT '净收入',
  `settle_status` enum('PENDING','SETTLED','FAILED') DEFAULT 'PENDING' COMMENT '结算状态',
  `settle_time` datetime DEFAULT NULL COMMENT '结算时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_user` (`admin_user_id`),
  KEY `idx_device` (`device_id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_income_admin` FOREIGN KEY (`admin_user_id`) REFERENCES `iot_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_income_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_income_order` FOREIGN KEY (`order_id`) REFERENCES `iot_order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收益记录表';

-- ----------------------------
-- 6. 支付交易表
-- ----------------------------
CREATE TABLE `iot_payment_txn` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL COMMENT '关联订单号',
  `transaction_id` varchar(64) NOT NULL COMMENT '第三方交易ID',
  `amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `currency` varchar(8) DEFAULT 'CNY' COMMENT '货币类型',
  `payment_method` varchar(32) NOT NULL COMMENT '支付方式',
  `payment_channel` varchar(32) NOT NULL COMMENT '支付渠道',
  `status` enum('PENDING','SUCCESS','FAILED','CANCELLED','REFUNDED') DEFAULT 'PENDING' COMMENT '交易状态',
  `third_party_response` text DEFAULT NULL COMMENT '第三方响应数据',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction_id` (`transaction_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付交易表';

-- ----------------------------
-- 7. 预约表
-- ----------------------------
CREATE TABLE `iot_reservation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `device_id` bigint(20) NOT NULL COMMENT '设备ID',
  `reservation_time` datetime NOT NULL COMMENT '预约时间',
  `duration_minutes` int NOT NULL DEFAULT 30 COMMENT '预约时长(分钟)',
  `wash_mode` varchar(32) DEFAULT NULL COMMENT '洗衣模式',
  `status` enum('ACTIVE','CANCELLED','COMPLETED','EXPIRED') DEFAULT 'ACTIVE' COMMENT '预约状态',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` varchar(255) DEFAULT NULL COMMENT '取消原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_device` (`user_id`, `device_id`),
  KEY `idx_device_time` (`device_id`, `reservation_time`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_reservation_user` FOREIGN KEY (`user_id`) REFERENCES `iot_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reservation_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约表';

-- ----------------------------
-- 8. 排队表
-- ----------------------------
CREATE TABLE `iot_queue` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `device_id` bigint(20) NOT NULL COMMENT '设备ID',
  `queue_position` int NOT NULL COMMENT '队列位置',
  `estimated_wait_minutes` int DEFAULT NULL COMMENT '预计等待时间(分钟)',
  `status` enum('WAITING','CANCELLED','SERVED','EXPIRED') DEFAULT 'WAITING' COMMENT '排队状态',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` varchar(255) DEFAULT NULL COMMENT '取消原因',
  `serve_time` datetime DEFAULT NULL COMMENT '开始服务时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_position` (`device_id`, `queue_position`),
  KEY `idx_user_device` (`user_id`, `device_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_queue_user` FOREIGN KEY (`user_id`) REFERENCES `iot_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_queue_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排队表';

-- ----------------------------
-- 9. 通知表
-- ----------------------------
CREATE TABLE `iot_notification` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '接收用户ID',
  `type` varchar(32) NOT NULL COMMENT '通知类型',
  `title` varchar(128) NOT NULL COMMENT '通知标题',
  `content` text NOT NULL COMMENT '通知内容',
  `related_order_no` varchar(32) DEFAULT NULL COMMENT '关联订单号',
  `related_device_id` bigint(20) DEFAULT NULL COMMENT '关联设备ID',
  `read_status` enum('UNREAD','READ') DEFAULT 'UNREAD' COMMENT '阅读状态',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `read_status`),
  KEY `idx_type` (`type`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `iot_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- ----------------------------
-- 10. 用户设备绑定表
-- ----------------------------
CREATE TABLE `iot_user_device_binding` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `device_id` bigint(20) NOT NULL COMMENT '设备ID',
  `binding_type` enum('QR_CODE','MANUAL','ADMIN_ASSIGN') DEFAULT 'QR_CODE' COMMENT '绑定方式',
  `bind_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `unbind_time` datetime DEFAULT NULL COMMENT '解绑时间',
  `status` enum('ACTIVE','INACTIVE') DEFAULT 'ACTIVE' COMMENT '绑定状态',
  `nickname` varchar(64) DEFAULT NULL COMMENT '设备昵称',
  `usage_count` int DEFAULT 0 COMMENT '使用次数',
  `last_use_time` datetime DEFAULT NULL COMMENT '最后使用时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_device` (`user_id`, `device_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_device` (`device_id`),
  CONSTRAINT `fk_user_binding_user` FOREIGN KEY (`user_id`) REFERENCES `iot_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_binding_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设备绑定表';

-- ----------------------------
-- 11. 设备状态历史表
-- ----------------------------
CREATE TABLE `iot_device_status_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `device_id` bigint(20) NOT NULL COMMENT '设备ID',
  `old_status` varchar(32) DEFAULT NULL COMMENT '原状态',
  `new_status` varchar(32) NOT NULL COMMENT '新状态',
  `status_type` varchar(32) NOT NULL COMMENT '状态类型',
  `order_no` varchar(32) DEFAULT NULL COMMENT '关联订单号',
  `temperature` decimal(5,2) DEFAULT NULL COMMENT '温度',
  `remain_seconds` int DEFAULT NULL COMMENT '剩余秒数',
  `error_code` varchar(32) DEFAULT NULL COMMENT '错误代码',
  `message` varchar(255) DEFAULT NULL COMMENT '状态消息',
  `change_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '状态变更时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_time` (`device_id`, `change_time`),
  KEY `idx_status_type` (`status_type`),
  KEY `idx_order_no` (`order_no`),
  CONSTRAINT `fk_device_history_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备状态历史表';

-- ----------------------------
-- 12. 系统配置表
-- ----------------------------
CREATE TABLE `iot_system_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `config_key` varchar(64) NOT NULL COMMENT '配置键',
  `config_value` text NOT NULL COMMENT '配置值',
  `config_type` varchar(32) DEFAULT 'STRING' COMMENT '配置类型',
  `description` varchar(255) DEFAULT NULL COMMENT '配置描述',
  `is_system` tinyint(1) DEFAULT 0 COMMENT '是否系统配置',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ----------------------------
-- 插入初始系统配置
-- ----------------------------
INSERT INTO `iot_system_config` (`config_key`, `config_value`, `config_type`, `description`, `is_system`) VALUES
('platform_fee_rate', '0.10', 'DECIMAL', '平台服务费率', 1),
('max_queue_length', '10', 'INTEGER', '最大排队长度', 1),
('reservation_advance_minutes', '15', 'INTEGER', '预约提前提醒时间(分钟)', 1),
('order_timeout_minutes', '30', 'INTEGER', '订单超时时间(分钟)', 1),
('refund_auto_minutes', '5', 'INTEGER', '自动退款时间(分钟)', 1),
('wash_modes', '[{"code":"standard","name":"标准洗","duration":30,"price":5.00},{"code":"quick","name":"快速洗","duration":15,"price":3.00},{"code":"spin","name":"单脱水","duration":10,"price":2.00}]', 'JSON', '洗衣模式配置', 1);

-- ----------------------------
-- 添加外键约束
-- ----------------------------
ALTER TABLE `iot_device` ADD CONSTRAINT `fk_device_owner` FOREIGN KEY (`owner_id`) REFERENCES `iot_user` (`id`) ON DELETE SET NULL;
ALTER TABLE `iot_order` ADD CONSTRAINT `fk_order_device_owner` FOREIGN KEY (`device_owner_id`) REFERENCES `iot_user` (`id`) ON DELETE SET NULL;