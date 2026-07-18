-- ----------------------------
-- 1. 用户表 (User)
-- 简单模型，主要存储微信标识
-- ----------------------------
CREATE TABLE `iot_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `openid` varchar(64) NOT NULL COMMENT '微信OpenID',
  `nickname` varchar(64) DEFAULT NULL,
  `balance` decimal(10,2) DEFAULT '0.00' COMMENT '余额(可选)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 2. 设备表 (Device)
-- 设备只存储物理属性和当前快照状态
-- ----------------------------
CREATE TABLE `iot_device` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `device_sn` varchar(64) NOT NULL COMMENT '设备序列号(MQTT Topic用)',
  `mac_address` varchar(32) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'OFFLINE' COMMENT '枚举: OFFLINE, IDLE, RUNNING, FAULT',
  `location` varchar(100) DEFAULT NULL COMMENT '投放位置',
  `last_heartbeat` datetime DEFAULT NULL COMMENT '最后心跳时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_sn` (`device_sn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

-- ----------------------------
-- 3. 订单表 (Order)
-- 核心业务表，关联用户与设备
-- ----------------------------
CREATE TABLE `iot_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL COMMENT '业务订单号',
  `user_id` bigint(20) NOT NULL,
  `device_id` bigint(20) NOT NULL,
  `amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `duration_minutes` int(11) NOT NULL DEFAULT '30' COMMENT '洗衣时长',
  `status` varchar(20) NOT NULL DEFAULT 'CREATED' COMMENT '枚举: CREATED, PAID, RUNNING, FINISHED, FAILED',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `start_time` datetime DEFAULT NULL COMMENT '设备实际启动时间',
  `end_time` datetime DEFAULT NULL COMMENT '设备实际结束时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user` (`user_id`),
  KEY `idx_device` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ----------------------------
-- 4. 用户状态表 (User Status)
-- 用于持久化存储用户状态
-- ----------------------------
CREATE TABLE `iot_user_status` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '关联用户表',
  `device_id` bigint(20) DEFAULT NULL COMMENT '关联设备表，用户当前使用的设备',
  `status` varchar(20) NOT NULL DEFAULT 'OFFLINE' COMMENT '用户状态: OFFLINE, ONLINE, USING_DEVICE',
  `last_activity` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最后活动时间',
  `last_update` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '状态最后更新时间',
  `update_reason` varchar(100) DEFAULT NULL COMMENT '状态更新原因',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_device_id` (`device_id`),
  CONSTRAINT `fk_user_status_user` FOREIGN KEY (`user_id`) REFERENCES `iot_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_status_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户状态表';

-- ----------------------------
-- 5. 用户状态历史表 (User Status History)
-- 用于记录用户状态变化历史
-- ----------------------------
CREATE TABLE `iot_user_status_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '关联用户表',
  `device_id` bigint(20) DEFAULT NULL COMMENT '关联设备表',
  `old_status` varchar(20) NOT NULL COMMENT '变更前状态',
  `new_status` varchar(20) NOT NULL COMMENT '变更后状态',
  `change_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '状态变更时间',
  `change_reason` varchar(100) DEFAULT NULL COMMENT '状态变更原因',
  `related_order_no` varchar(32) DEFAULT NULL COMMENT '关联订单号',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_change_time` (`change_time`),
  CONSTRAINT `fk_user_status_history_user` FOREIGN KEY (`user_id`) REFERENCES `iot_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_status_history_device` FOREIGN KEY (`device_id`) REFERENCES `iot_device` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户状态历史表';
