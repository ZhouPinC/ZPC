package com.wash.iot.service;

import com.wash.iot.dto.response.NotificationResponse;
import com.wash.iot.entity.*;
import com.wash.iot.repository.NotificationRepository;
import com.wash.iot.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知服务
 */
@Slf4j
@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取用户通知列表
     */
    public List<NotificationResponse> getUserNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreateTimeDesc(userId);
        return notifications.stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取未读通知数量
     */
    public int getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    /**
     * 标记通知为已读
     */
    public void markAsRead(Long userId, List<Long> notificationIds) {
        for (Long id : notificationIds) {
            Notification notification = notificationRepository.findById(id).orElse(null);
            if (notification != null && notification.getUserId().equals(userId)) {
                notification.setIsRead(true);
                notificationRepository.save(notification);
            }
        }
    }

    /**
     * 发送通知
     */
    public void sendNotification(Long userId, String type, String title, String content, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notificationRepository.save(notification);

        log.info("发送通知: userId={}, type={}, title={}", userId, type, title);
        // TODO: 发送微信模板消息
    }

    /**
     * 发送预约提醒
     */
    public void sendReservationReminder(Long userId, Long reservationId, String deviceSn, String time) {
        sendNotification(userId, "RESERVATION_REMIND",
                "预约提醒",
                String.format("您预约的设备 %s 将于 %s 开始，请及时前往", deviceSn, time),
                reservationId);
    }

    /**
     * 发送洗衣完成通知
     */
    public void sendWashCompleteNotification(Long userId, Long orderId, String deviceSn) {
        sendNotification(userId, "WASH_COMPLETE",
                "洗衣完成",
                String.format("您在设备 %s 的洗衣已完成，请及时取衣", deviceSn),
                orderId);
    }

    /**
     * 发送排队轮到通知
     */
    public void sendQueueTurnNotification(Long userId, Long queueId, String deviceSn) {
        sendNotification(userId, "QUEUE_TURN",
                "轮到您了",
                String.format("设备 %s 已空闲，轮到您使用了，请在10分钟内前往", deviceSn),
                queueId);
    }

    /**
     * 发送订单完成通知
     */
    public void sendOrderCompletionNotification(Order order) {
        try {
            if (order.getUserId() != null) {
                userRepository.findById(order.getUserId()).ifPresent(user -> {
                    String message = String.format(
                        "您的洗衣已完成！\n设备：%s\n订单号：%s\n消费金额：%.2f元",
                        getDeviceInfo(order.getDeviceId()),
                        order.getOrderNo(),
                        order.getAmount()
                    );
                    sendNotification(user.getId(), "ORDER_COMPLETE", "洗衣完成", message, order.getId());
                });
            }
        } catch (Exception e) {
            log.error("发送订单完成通知失败", e);
        }
    }

    /**
     * 发送预约提醒通知
     */
    public void sendReservationReminderNotification(Reservation reservation, int minutes) {
        try {
            userRepository.findById(reservation.getUserId()).ifPresent(user -> {
                String message = String.format(
                    "预约提醒！\n您的预约将在%d分钟后开始\n设备：%s\n预约时间：%s",
                    minutes,
                    getDeviceInfo(reservation.getDeviceId()),
                    reservation.getReservationTime().toString()
                );
                sendNotification(user.getId(), "RESERVATION_REMIND", "预约提醒", message, reservation.getId());
            });
        } catch (Exception e) {
            log.error("发送预约提醒失败", e);
        }
    }

    /**
     * 发送设备故障通知
     */
    public void sendDeviceFaultNotification(Device device, Order order) {
        try {
            if (order.getUserId() != null) {
                userRepository.findById(order.getUserId()).ifPresent(user -> {
                    String message = String.format(
                        "设备故障通知！\n设备：%s\n订单：%s\n请及时联系管理员",
                        getDeviceInfo(device.getId()),
                        order.getOrderNo()
                    );
                    sendNotification(user.getId(), "DEVICE_FAULT", "设备故障", message, order.getId());
                });
            }
        } catch (Exception e) {
            log.error("发送设备故障通知失败", e);
        }
    }

    /**
     * 发送设备故障通知给管理员
     */
    public void sendDeviceFaultToAdmin(Device device, Object event) {
        try {
            List<User> admins = userRepository.findByRole("ADMIN");

            for (User admin : admins) {
                String message = String.format(
                    "设备故障报告！\n设备：%s\n时间：%s\n请及时处理",
                    getDeviceInfo(device.getId()),
                    LocalDateTime.now().toString()
                );
                sendNotification(admin.getId(), "DEVICE_FAULT_ADMIN", "设备故障报告", message, device.getId());
            }
        } catch (Exception e) {
            log.error("发送设备故障通知给管理员失败", e);
        }
    }

    /**
     * 发送设备离线通知
     */
    public void sendDeviceOfflineNotification(Device device) {
        try {
            List<User> admins = userRepository.findByRole("ADMIN");

            for (User admin : admins) {
                String message = String.format(
                    "设备离线通知！\n设备：%s\n离线时间：%s\n请检查设备状态",
                    getDeviceInfo(device.getId()),
                    LocalDateTime.now().toString()
                );
                sendNotification(admin.getId(), "DEVICE_OFFLINE", "设备离线", message, device.getId());
            }
        } catch (Exception e) {
            log.error("发送设备离线通知失败", e);
        }
    }

    /**
     * 发送排队加入通知
     */
    public void sendQueueJoinNotification(Queue queue, Device device) {
        try {
            userRepository.findById(queue.getUserId()).ifPresent(user -> {
                String message = String.format(
                    "排队成功！\n设备：%s\n当前位置：%d\n预计等待：%d分钟",
                    getDeviceInfo(device.getId()),
                    queue.getQueuePosition(),
                    queue.getEstimatedWaitMinutes()
                );
                sendNotification(user.getId(), "QUEUE_JOIN", "排队成功", message, queue.getId());
            });
        } catch (Exception e) {
            log.error("发送排队加入通知失败", e);
        }
    }

    /**
     * 发送排队取消通知
     */
    public void sendQueueCancelNotification(Queue queue) {
        try {
            userRepository.findById(queue.getUserId()).ifPresent(user -> {
                String message = "排队已取消";
                sendNotification(user.getId(), "QUEUE_CANCEL", "排队取消", message, queue.getId());
            });
        } catch (Exception e) {
            log.error("发送排队取消通知失败", e);
        }
    }

    /**
     * 发送排队位置更新通知
     */
    public void sendQueuePositionUpdateNotification(Queue queue) {
        try {
            userRepository.findById(queue.getUserId()).ifPresent(user -> {
                String message = String.format(
                    "排队位置更新！\n当前位置：%d\n预计等待：%d分钟",
                    queue.getQueuePosition(),
                    queue.getEstimatedWaitMinutes()
                );
                sendNotification(user.getId(), "QUEUE_UPDATE", "排队更新", message, queue.getId());
            });
        } catch (Exception e) {
            log.error("发送排队位置更新通知失败", e);
        }
    }

    /**
     * 发送预约创建通知
     */
    public void sendReservationCreateNotification(Reservation reservation, Device device) {
        try {
            userRepository.findById(reservation.getUserId()).ifPresent(user -> {
                String message = String.format(
                    "预约成功！\n设备：%s\n预约时间：%s\n请准时使用",
                    getDeviceInfo(device.getId()),
                    reservation.getReservationTime().toString()
                );
                sendNotification(user.getId(), "RESERVATION_CREATE", "预约成功", message, reservation.getId());
            });
        } catch (Exception e) {
            log.error("发送预约创建通知失败", e);
        }
    }

    /**
     * 发送预约取消通知
     */
    public void sendReservationCancelNotification(Reservation reservation) {
        try {
            userRepository.findById(reservation.getUserId()).ifPresent(user -> {
                String message = "预约已取消";
                sendNotification(user.getId(), "RESERVATION_CANCEL", "预约取消", message, reservation.getId());
            });
        } catch (Exception e) {
            log.error("发送预约取消通知失败", e);
        }
    }

    /**
     * 发送预约转换通知
     */
    public void sendReservationConvertNotification(Reservation reservation, Order order) {
        try {
            userRepository.findById(reservation.getUserId()).ifPresent(user -> {
                String message = String.format(
                    "预约已转为订单！\n订单号：%s\n请开始使用",
                    order.getOrderNo()
                );
                sendNotification(user.getId(), "RESERVATION_CONVERT", "预约转订单", message, order.getId());
            });
        } catch (Exception e) {
            log.error("发送预约转换通知失败", e);
        }
    }

    /**
     * 发送预约过期通知
     */
    public void sendReservationExpiredNotification(Reservation reservation) {
        try {
            userRepository.findById(reservation.getUserId()).ifPresent(user -> {
                String message = "预约已过期，请重新预约";
                sendNotification(user.getId(), "RESERVATION_EXPIRED", "预约过期", message, reservation.getId());
            });
        } catch (Exception e) {
            log.error("发送预约过期通知失败", e);
        }
    }

    /**
     * 发送订单启动通知
     */
    public void sendOrderStartNotification(Order order) {
        try {
            userRepository.findById(order.getUserId()).ifPresent(user -> {
                String message = String.format(
                    "洗衣机已启动！\n设备：%s\n订单号：%s\n预计时长：%d分钟",
                    getDeviceInfo(order.getDeviceId()),
                    order.getOrderNo(),
                    order.getDurationMinutes()
                );
                sendNotification(user.getId(), "ORDER_START", "洗衣机启动", message, order.getId());
            });
        } catch (Exception e) {
            log.error("发送订单启动通知失败", e);
        }
    }

    /**
     * 发送订单中断通知
     */
    public void sendOrderInterruptNotification(Order order, String reason) {
        try {
            userRepository.findById(order.getUserId()).ifPresent(user -> {
                String message = String.format(
                    "洗衣机已中断！\n设备：%s\n订单号：%s\n原因：%s",
                    getDeviceInfo(order.getDeviceId()),
                    order.getOrderNo(),
                    reason
                );
                sendNotification(user.getId(), "ORDER_INTERRUPT", "洗衣机中断", message, order.getId());
            });
        } catch (Exception e) {
            log.error("发送订单中断通知失败", e);
        }
    }

    /**
     * 发送支付异常通知
     */
    public void sendPaymentExceptionNotification(Order order, String errorMessage) {
        try {
            userRepository.findById(order.getUserId()).ifPresent(user -> {
                String message = String.format(
                    "支付异常！\n订单号：%s\n请重新支付或联系客服",
                    order.getOrderNo()
                );
                sendNotification(user.getId(), "PAYMENT_EXCEPTION", "支付异常", message, order.getId());
            });
        } catch (Exception e) {
            log.error("发送支付异常通知失败", e);
        }
    }

    /**
     * 发送结算通知
     */
    public void sendSettlementNotification(User user, List<IncomeRecord> records,
                                         BigDecimal totalAmount, LocalDateTime settleTime) {
        try {
            String message = String.format(
                "收益结算通知！\n结算金额：%.2f元\n结算时间：%s\n订单数：%d笔",
                totalAmount,
                settleTime.toString(),
                records.size()
            );
            sendNotification(user.getId(), "SETTLEMENT", "收益结算", message, null);
        } catch (Exception e) {
            log.error("发送结算通知失败", e);
        }
    }

    /**
     * 发送提现申请通知
     */
    public void sendWithdrawalRequestNotification(User user, WithdrawalRecord withdrawal) {
        try {
            String message = String.format(
                "提现申请！\n用户：%s\n金额：%.2f元\n时间：%s",
                user.getNickName(),
                withdrawal.getAmount(),
                withdrawal.getCreateTime().toString()
            );
            // 发送给管理员
            List<User> admins = userRepository.findByRole("ADMIN");
            for (User admin : admins) {
                sendNotification(admin.getId(), "WITHDRAWAL_REQUEST", "提现申请", message, withdrawal.getId());
            }
        } catch (Exception e) {
            log.error("发送提现申请通知失败", e);
        }
    }

    /**
     * 发送提现批准通知
     */
    public void sendWithdrawalApprovedNotification(User user, WithdrawalRecord withdrawal) {
        try {
            String message = String.format(
                "提现已批准！\n金额：%.2f元\n预计到账时间：1-3个工作日",
                withdrawal.getAmount()
            );
            sendNotification(user.getId(), "WITHDRAWAL_APPROVED", "提现批准", message, withdrawal.getId());
        } catch (Exception e) {
            log.error("发送提现批准通知失败", e);
        }
    }

    /**
     * 发送提现拒绝通知
     */
    public void sendWithdrawalRejectedNotification(User user, WithdrawalRecord withdrawal, String remark) {
        try {
            String message = String.format(
                "提现申请已拒绝！\n金额：%.2f元\n原因：%s",
                withdrawal.getAmount(),
                remark
            );
            sendNotification(user.getId(), "WITHDRAWAL_REJECTED", "提现拒绝", message, withdrawal.getId());
        } catch (Exception e) {
            log.error("发送提现拒绝通知失败", e);
        }
    }

    /**
     * 发送管理员通知
     */
    public void sendAdminNotification(String title, String message) {
        try {
            List<User> admins = userRepository.findByRole("ADMIN");
            for (User admin : admins) {
                sendNotification(admin.getId(), "ADMIN_NOTIFICATION", title, message, null);
            }
        } catch (Exception e) {
            log.error("发送管理员通知失败", e);
        }
    }

    /**
     * 发送排队超时通知
     */
    public void sendQueueTimeoutNotification(Queue queue) {
        try {
            userRepository.findById(queue.getUserId()).ifPresent(user -> {
                String message = "排队已超时，请重新排队";
                sendNotification(user.getId(), "QUEUE_TIMEOUT", "排队超时", message, queue.getId());
            });
        } catch (Exception e) {
            log.error("发送排队超时通知失败", e);
        }
    }

    private NotificationResponse buildResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .relatedId(notification.getRelatedId())
                .isRead(notification.getIsRead())
                .createTime(notification.getCreateTime())
                .build();
    }

    private String getDeviceInfo(Long deviceId) {
        // TODO: 根据设备ID获取设备信息
        return "设备ID: " + deviceId;
    }

    /**
     * 发送订单异常通知
     */
    public void sendOrderAbnormalNotification(Order order, String reason) {
        try {
            String title = "订单异常通知";
            String content = String.format("您的订单 %s 出现异常：%s", order.getOrderNo(), reason);

            Notification notification = new Notification();
            notification.setUserId(order.getUserId());
            notification.setType("ORDER_ABNORMAL");
            notification.setTitle(title);
            notification.setContent(content);
            notification.setRelatedId(order.getId());
            notification.setIsRead(false);

            notificationRepository.save(notification);
            log.info("订单异常通知已发送: userId={}, orderNo={}", order.getUserId(), order.getOrderNo());
        } catch (Exception e) {
            log.error("发送订单异常通知失败", e);
        }
    }

    /**
     * 发送队列服务通知
     */
    public void sendQueueServeNotification(Queue queue, Order order) {
        try {
            String title = "排队服务通知";
            String content = String.format("您排队的设备已空闲，请及时使用。订单号：%s", order.getOrderNo());

            Notification notification = new Notification();
            notification.setUserId(queue.getUserId());
            notification.setType("QUEUE_SERVE");
            notification.setTitle(title);
            notification.setContent(content);
            notification.setRelatedId(order.getId());
            notification.setIsRead(false);

            notificationRepository.save(notification);
            log.info("队列服务通知已发送: userId={}, orderNo={}", queue.getUserId(), order.getOrderNo());
        } catch (Exception e) {
            log.error("发送队列服务通知失败", e);
        }
    }
}
