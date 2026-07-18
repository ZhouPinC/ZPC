package com.wash.iot.service;

import com.wash.iot.entity.Device;
import com.wash.iot.entity.Order;
import com.wash.iot.entity.Queue;
import com.wash.iot.entity.Reservation;
import com.wash.iot.entity.User;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.repository.QueueRepository;
import com.wash.iot.repository.ReservationRepository;
import com.wash.iot.repository.UserRepository;
import com.wash.iot.enums.DeviceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 排队预约服务
 * 管理设备排队和预约业务逻辑，确保公平性和效率
 */
@Slf4j
@Service
@Transactional
public class QueueReservationService {

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OrderLifecycleService orderLifecycleService;

    // 配置参数
    private static final int MAX_QUEUE_LENGTH = 10;
    private static final int RESERVATION_ADVANCE_MINUTES = 15;
    private static final int QUEUE_TIMEOUT_MINUTES = 30;
    private static final int RESERVATION_TIMEOUT_MINUTES = 10;

    /**
     * 加入排队
     */
    public Queue joinQueue(Long userId, Long deviceId, String washMode, Integer durationMinutes) {
        try {
            // 1. 验证设备和用户
            Device device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new RuntimeException("设备不存在"));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 2. 检查设备状态
            if (!DeviceStatus.RUNNING.name().equals(device.getStatus())) {
                throw new RuntimeException("设备当前无需排队");
            }

            // 3. 检查排队长度
            long currentQueueLength = queueRepository.countByDeviceIdAndStatus(deviceId, "WAITING");
            if (currentQueueLength >= MAX_QUEUE_LENGTH) {
                throw new RuntimeException("排队人数已满，请稍后再试");
            }

            // 4. 检查用户是否已在排队
            Optional<Queue> existingQueue = queueRepository.findByUserIdAndDeviceIdAndStatus(userId, deviceId, "WAITING");
            if (existingQueue.isPresent()) {
                throw new RuntimeException("您已在排队中，请勿重复排队");
            }

            // 5. 计算排队位置和预计等待时间
            int queuePosition = (int) currentQueueLength + 1;
            Integer estimatedWaitMinutes = calculateEstimatedWaitTime(device);

            // 6. 创建排队记录
            Queue queue = new Queue();
            queue.setUserId(userId);
            queue.setDeviceId(deviceId);
            queue.setQueuePosition(queuePosition);
            queue.setEstimatedWaitMinutes(estimatedWaitMinutes);
            queue.setWashMode(washMode);
            queue.setDurationMinutes(durationMinutes);
            queue.setStatus("WAITING");
            queue.setCreateTime(LocalDateTime.now());

            queue = queueRepository.save(queue);

            // 7. 发送排队成功通知
            notificationService.sendQueueJoinNotification(queue, device);

            log.info("用户加入排队成功: userId={}, deviceId={}, queuePosition={}",
                    userId, deviceId, queuePosition);

            return queue;

        } catch (Exception e) {
            log.error("加入排队失败: userId={}, deviceId={}", userId, deviceId, e);
            throw new RuntimeException("加入排队失败: " + e.getMessage());
        }
    }

    /**
     * 取消排队
     */
    public void cancelQueue(Long userId, Long queueId) {
        try {
            Queue queue = queueRepository.findById(queueId)
                    .orElseThrow(() -> new RuntimeException("排队记录不存在"));

            if (!queue.getUserId().equals(userId)) {
                throw new RuntimeException("无权操作此排队记录");
            }

            if (!"WAITING".equals(queue.getStatus())) {
                throw new RuntimeException("排队状态不允许取消");
            }

            // 更新排队状态
            queue.setStatus("CANCELLED");
            queue.setCancelTime(LocalDateTime.now());
            queue.setCancelReason("用户主动取消");
            queueRepository.save(queue);

            // 更新其他排队用户的位置
            updateQueuePositions(queue.getDeviceId());

            // 发送取消通知
            notificationService.sendQueueCancelNotification(queue);

            log.info("用户取消排队成功: userId={}, queueId={}", userId, queueId);

        } catch (Exception e) {
            log.error("取消排队失败: userId={}, queueId={}", userId, queueId, e);
            throw new RuntimeException("取消排队失败: " + e.getMessage());
        }
    }

    /**
     * 创建预约
     */
    public Reservation createReservation(Long userId, Long deviceId, LocalDateTime reservationTime,
                                       String washMode, Integer durationMinutes) {
        try {
            // 1. 验证时间和设备
            if (reservationTime.isBefore(LocalDateTime.now())) {
                throw new RuntimeException("预约时间不能早于当前时间");
            }

            if (reservationTime.isAfter(LocalDateTime.now().plusDays(7))) {
                throw new RuntimeException("预约时间不能超过7天");
            }

            Device device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new RuntimeException("设备不存在"));

            // 2. 检查时间冲突
            if (hasReservationConflict(deviceId, reservationTime, durationMinutes)) {
                throw new RuntimeException("该时间段已被预约");
            }

            // 3. 检查用户预约数量限制
            long userReservationCount = reservationRepository.countByUserIdAndStatus(userId, "ACTIVE");
            if (userReservationCount >= 3) {
                throw new RuntimeException("最多只能同时有3个有效预约");
            }

            // 4. 创建预约记录
            Reservation reservation = new Reservation();
            reservation.setUserId(userId);
            reservation.setDeviceId(deviceId);
            reservation.setReservationTime(reservationTime);
            reservation.setDurationMinutes(durationMinutes);
            reservation.setWashMode(washMode);
            reservation.setStatus("ACTIVE");
            reservation.setCreateTime(LocalDateTime.now());

            reservation = reservationRepository.save(reservation);

            // 5. 发送预约成功通知
            notificationService.sendReservationCreateNotification(reservation, device);

            log.info("创建预约成功: userId={}, deviceId={}, reservationTime={}",
                    userId, deviceId, reservationTime);

            return reservation;

        } catch (Exception e) {
            log.error("创建预约失败: userId={}, deviceId={}", userId, deviceId, e);
            throw new RuntimeException("创建预约失败: " + e.getMessage());
        }
    }

    /**
     * 取消预约
     */
    public void cancelReservation(Long userId, Long reservationId) {
        try {
            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new RuntimeException("预约记录不存在"));

            if (!reservation.getUserId().equals(userId)) {
                throw new RuntimeException("无权操作此预约记录");
            }

            if (!"ACTIVE".equals(reservation.getStatus())) {
                throw new RuntimeException("预约状态不允许取消");
            }

            // 检查取消时间限制
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cutoffTime = reservation.getReservationTime().minusMinutes(RESERVATION_ADVANCE_MINUTES);
            if (now.isAfter(cutoffTime)) {
                throw new RuntimeException("预约开始前" + RESERVATION_ADVANCE_MINUTES + "分钟内不能取消");
            }

            // 更新预约状态
            reservation.setStatus("CANCELLED");
            reservation.setCancelTime(now);
            reservation.setCancelReason("用户主动取消");
            reservationRepository.save(reservation);

            // 发送取消通知
            notificationService.sendReservationCancelNotification(reservation);

            log.info("用户取消预约成功: userId={}, reservationId={}", userId, reservationId);

        } catch (Exception e) {
            log.error("取消预约失败: userId={}, reservationId={}", userId, reservationId, e);
            throw new RuntimeException("取消预约失败: " + e.getMessage());
        }
    }

    /**
     * 预约转订单
     */
    public void convertReservationToOrder(Reservation reservation) {
        try {
            if (!"ACTIVE".equals(reservation.getStatus())) {
                throw new RuntimeException("预约状态无效");
            }

            // 检查预约时间
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime reservationTime = reservation.getReservationTime();

            // 允许提前5分钟或延后10分钟
            if (now.isBefore(reservationTime.minusMinutes(5)) || now.isAfter(reservationTime.plusMinutes(10))) {
                throw new RuntimeException("不在预约时间范围内");
            }

            // 检查设备状态
            Device device = deviceRepository.findById(reservation.getDeviceId())
                    .orElseThrow(() -> new RuntimeException("设备不存在"));

            if (!DeviceStatus.IDLE.name().equals(device.getStatus())) {
                throw new RuntimeException("设备当前不可用");
            }

            // 创建订单
            Order order = orderLifecycleService.createOrder(
                reservation.getUserId(),
                reservation.getDeviceId(),
                reservation.getWashMode(),
                reservation.getDurationMinutes(),
                reservation.getId()
            );

            // 更新预约状态
            reservation.setStatus("COMPLETED");
            reservationRepository.save(reservation);

            // 发送转换通知
            notificationService.sendReservationConvertNotification(reservation, order);

            log.info("预约转订单成功: reservationId={}, orderNo={}",
                    reservation.getId(), order.getOrderNo());

        } catch (Exception e) {
            log.error("预约转订单失败: reservationId={}", reservation.getId(), e);
            throw new RuntimeException("预约转订单失败: " + e.getMessage());
        }
    }

    /**
     * 获取排队信息
     */
    public QueueInfo getQueueInfo(Long deviceId) {
        try {
            List<Queue> waitingQueues = queueRepository.findByDeviceIdAndStatusOrderByQueuePosition(deviceId, "WAITING");

            QueueInfo info = new QueueInfo();
            info.setDeviceId(deviceId);
            info.setQueueLength(waitingQueues.size());
            info.setQueues(waitingQueues);

            if (!waitingQueues.isEmpty()) {
                Queue firstQueue = waitingQueues.get(0);
                info.setEstimatedWaitMinutes(firstQueue.getEstimatedWaitMinutes());
                info.setNextUser(firstQueue.getUserId());
            }

            return info;

        } catch (Exception e) {
            log.error("获取排队信息失败: deviceId={}", deviceId, e);
            throw new RuntimeException("获取排队信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户排队状态
     */
    public Queue getUserQueueStatus(Long userId) {
        List<Queue> queues = queueRepository.findByUserIdAndStatus(userId, "WAITING");
        return queues.isEmpty() ? null : queues.get(0);
    }

    /**
     * 获取用户预约列表
     */
    public List<Reservation> getUserReservations(Long userId) {
        return reservationRepository.findByUserIdAndStatusOrderByReservationTime(userId, "ACTIVE");
    }

    /**
     * 计算预计等待时间
     */
    private Integer calculateEstimatedWaitTime(Device device) {
        try {
            // 获取当前正在工作的订单
            if (device.getCurrentOrderId() != null && device.getEstimatedEndTime() != null) {
                long remainingMinutes = java.time.Duration.between(
                    LocalDateTime.now(),
                    device.getEstimatedEndTime()
                ).toMinutes();

                if (remainingMinutes > 0) {
                    // 基础等待时间 + 排队人数 * 平均工作时长
                    long queueCount = queueRepository.countByDeviceIdAndStatus(device.getId(), "WAITING");
                    return (int) (remainingMinutes + queueCount * 30); // 假设平均工作30分钟
                }
            }

            // 如果没有正在工作的订单，检查排队人数
            long queueCount = queueRepository.countByDeviceIdAndStatus(device.getId(), "WAITING");
            return (int) (queueCount * 30); // 每人30分钟

        } catch (Exception e) {
            log.error("计算预计等待时间失败: deviceId={}", device.getId(), e);
            return 30; // 默认30分钟
        }
    }

    /**
     * 检查预约时间冲突
     */
    private boolean hasReservationConflict(Long deviceId, LocalDateTime startTime, Integer durationMinutes) {
        LocalDateTime endTime = startTime.plusMinutes(durationMinutes);

        List<Reservation> existingReservations = reservationRepository
                .findByDeviceIdAndStatusAndReservationTimeBetween(
                    deviceId, "ACTIVE",
                    startTime.minusMinutes(durationMinutes),
                    endTime.plusMinutes(durationMinutes)
                );

        for (Reservation reservation : existingReservations) {
            LocalDateTime existingStart = reservation.getReservationTime();
            LocalDateTime existingEnd = existingStart.plusMinutes(reservation.getDurationMinutes());

            // 检查时间重叠
            if (startTime.isBefore(existingEnd) && endTime.isAfter(existingStart)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 更新排队位置
     */
    private void updateQueuePositions(Long deviceId) {
        List<Queue> waitingQueues = queueRepository.findByDeviceIdAndStatusOrderByQueuePosition(deviceId, "WAITING");

        for (int i = 0; i < waitingQueues.size(); i++) {
            Queue queue = waitingQueues.get(i);
            int newPosition = i + 1;

            if (queue.getQueuePosition() != newPosition) {
                queue.setQueuePosition(newPosition);
                queueRepository.save(queue);

                // 通知用户位置变化
                notificationService.sendQueuePositionUpdateNotification(queue);
            }
        }
    }

    /**
     * 定时检查超时排队
     */
    @Scheduled(fixedDelay = 60000) // 每分钟执行一次
    public void checkTimeoutQueues() {
        try {
            LocalDateTime timeout = LocalDateTime.now().minusMinutes(QUEUE_TIMEOUT_MINUTES);
            List<Queue> timeoutQueues = queueRepository.findTimeoutQueues(timeout);

            for (Queue queue : timeoutQueues) {
                log.warn("排队超时，自动取消: queueId={}, userId={}", queue.getId(), queue.getUserId());

                queue.setStatus("EXPIRED");
                queue.setCancelTime(LocalDateTime.now());
                queue.setCancelReason("排队超时自动取消");
                queueRepository.save(queue);

                notificationService.sendQueueTimeoutNotification(queue);
            }

            // 更新排队位置
            for (Queue queue : timeoutQueues) {
                updateQueuePositions(queue.getDeviceId());
            }

        } catch (Exception e) {
            log.error("检查超时排队失败", e);
        }
    }

    /**
     * 定时检查预约提醒
     */
    @Scheduled(fixedDelay = 60000) // 每分钟执行一次
    public void checkReservationReminders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime reminderTime1 = now.plusMinutes(15); // 15分钟提醒
            LocalDateTime reminderTime2 = now.plusMinutes(5);  // 5分钟提醒

            // 检查15分钟提醒
            List<Reservation> reservations1 = reservationRepository
                    .findReservationsForReminder("ACTIVE", reminderTime1, now);

            for (Reservation reservation : reservations1) {
                notificationService.sendReservationReminderNotification(reservation, 15);
                reservationRepository.save(reservation);
            }

            // 检查5分钟提醒
            List<Reservation> reservations2 = reservationRepository
                    .findReservationsForReminder("ACTIVE", reminderTime2, now);

            for (Reservation reservation : reservations2) {
                notificationService.sendReservationReminderNotification(reservation, 5);
                reservationRepository.save(reservation);
            }

        } catch (Exception e) {
            log.error("检查预约提醒失败", e);
        }
    }

    /**
     * 定时检查过期预约
     */
    @Scheduled(fixedDelay = 60000) // 每分钟执行一次
    public void checkExpiredReservations() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expireTime = now.minusMinutes(RESERVATION_TIMEOUT_MINUTES);

            List<Reservation> expiredReservations = reservationRepository
                    .findExpiredReservations("CONFIRMED", expireTime);

            for (Reservation reservation : expiredReservations) {
                log.warn("预约过期，自动取消: reservationId={}, userId={}",
                        reservation.getId(), reservation.getUserId());

                reservation.setStatus("EXPIRED");
                reservation.setCancelTime(now);
                reservation.setCancelReason("预约过期自动取消");
                reservationRepository.save(reservation);

                notificationService.sendReservationExpiredNotification(reservation);
            }

        } catch (Exception e) {
            log.error("检查过期预约失败", e);
        }
    }

    // 数据传输对象
    public static class QueueInfo {
        private Long deviceId;
        private Integer queueLength;
        private Integer estimatedWaitMinutes;
        private Long nextUser;
        private List<Queue> queues;

        // getters and setters
        public Long getDeviceId() { return deviceId; }
        public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
        public Integer getQueueLength() { return queueLength; }
        public void setQueueLength(Integer queueLength) { this.queueLength = queueLength; }
        public Integer getEstimatedWaitMinutes() { return estimatedWaitMinutes; }
        public void setEstimatedWaitMinutes(Integer estimatedWaitMinutes) { this.estimatedWaitMinutes = estimatedWaitMinutes; }
        public Long getNextUser() { return nextUser; }
        public void setNextUser(Long nextUser) { this.nextUser = nextUser; }
        public List<Queue> getQueues() { return queues; }
        public void setQueues(List<Queue> queues) { this.queues = queues; }
    }
}