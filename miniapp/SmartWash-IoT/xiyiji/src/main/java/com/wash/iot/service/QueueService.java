package com.wash.iot.service;

import com.wash.iot.common.exception.BusinessException;
import com.wash.iot.dto.response.QueueResponse;
import com.wash.iot.entity.Device;
import com.wash.iot.entity.Queue;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.repository.QueueRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 排队服务
 */
@Slf4j
@Service
public class QueueService {

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    // 排队规则配置
    private static final int MAX_QUEUE_LENGTH = 10;              // 单设备最大排队数
    private static final int NOTIFICATION_VALID_MINUTES = 10;    // 通知后10分钟内有效
    private static final int ESTIMATED_WAIT_PER_ORDER = 35;      // 预估每单等待时间(分钟)

    /**
     * 加入排队
     */
    @Transactional
    public QueueResponse joinQueue(Long userId, Long deviceId) {
        // 1. 验证设备
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException("设备不存在"));

        // 2. 检查用户是否已在排队
        if (queueRepository.findByUserIdAndDeviceIdAndStatus(userId, deviceId, "WAITING").isPresent()) {
            throw new BusinessException("您已在该设备排队中");
        }

        // 3. 检查排队人数
        int currentQueueLength = queueRepository.countByDeviceIdAndStatus(deviceId, "WAITING");
        if (currentQueueLength >= MAX_QUEUE_LENGTH) {
            throw new BusinessException("排队人数已满，请稍后再试");
        }

        // 4. 创建排队记录
        Queue queue = new Queue();
        queue.setUserId(userId);
        queue.setDeviceId(deviceId);
        queue.setQueuePosition(currentQueueLength + 1);
        queue.setEstimatedWaitMinutes(currentQueueLength * ESTIMATED_WAIT_PER_ORDER);
        queue.setStatus("WAITING");

        queue = queueRepository.save(queue);

        // 5. 更新设备排队人数
        device.setCurrentQueueLength(currentQueueLength + 1);
        deviceRepository.save(device);

        log.info("用户加入排队: userId={}, deviceId={}, position={}", userId, deviceId, queue.getQueuePosition());
        return buildQueueResponse(queue, device);
    }

    /**
     * 获取用户排队状态
     */
    public List<QueueResponse> getUserQueues(Long userId) {
        List<Queue> queues = queueRepository.findByUserIdAndStatusIn(userId, 
                Arrays.asList("WAITING", "NOTIFIED"));

        return queues.stream()
                .map(q -> {
                    Device device = deviceRepository.findById(q.getDeviceId()).orElse(null);
                    return buildQueueResponse(q, device);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取设备排队列表
     */
    public List<QueueResponse> getDeviceQueue(Long deviceId) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        List<Queue> queues = queueRepository.findByDeviceIdAndStatusOrderByQueuePosition(deviceId, "WAITING");

        return queues.stream()
                .map(q -> buildQueueResponse(q, device))
                .collect(Collectors.toList());
    }

    /**
     * 退出排队
     */
    @Transactional
    public void leaveQueue(Long userId, Long queueId) {
        Queue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new BusinessException("排队记录不存在"));

        if (!queue.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此排队");
        }

        if (!"WAITING".equals(queue.getStatus()) && !"NOTIFIED".equals(queue.getStatus())) {
            throw new BusinessException("该排队无法取消");
        }

        Long deviceId = queue.getDeviceId();
        int position = queue.getQueuePosition();

        // 更新状态
        queue.setStatus("CANCELLED");
        queueRepository.save(queue);

        // 更新后面排队者的位置
        List<Queue> laterQueues = queueRepository.findByDeviceIdAndStatusOrderByQueuePosition(deviceId, "WAITING");
        for (Queue q : laterQueues) {
            if (q.getQueuePosition() > position) {
                q.setQueuePosition(q.getQueuePosition() - 1);
                q.setEstimatedWaitMinutes((q.getQueuePosition() - 1) * ESTIMATED_WAIT_PER_ORDER);
                queueRepository.save(q);
            }
        }

        // 更新设备排队人数
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device != null) {
            int newLength = queueRepository.countByDeviceIdAndStatus(deviceId, "WAITING");
            device.setCurrentQueueLength(newLength);
            deviceRepository.save(device);
        }

        log.info("用户退出排队: userId={}, queueId={}", userId, queueId);
    }

    /**
     * 通知下一位排队者（设备空闲时调用）
     */
    @Transactional
    public void notifyNextInQueue(Long deviceId) {
        List<Queue> waitingQueues = queueRepository.findByDeviceIdAndStatusOrderByQueuePosition(deviceId, "WAITING");
        
        if (!waitingQueues.isEmpty()) {
            Queue nextQueue = waitingQueues.get(0);
            nextQueue.setStatus("NOTIFIED");
            nextQueue.setNotifyTime(LocalDateTime.now());
            nextQueue.setExpireTime(LocalDateTime.now().plusMinutes(NOTIFICATION_VALID_MINUTES));
            queueRepository.save(nextQueue);

            log.info("通知排队用户: userId={}, deviceId={}", nextQueue.getUserId(), deviceId);
            // TODO: 发送微信模板消息通知用户
        }
    }

    /**
     * 构建排队响应
     */
    private QueueResponse buildQueueResponse(Queue queue, Device device) {
        return QueueResponse.builder()
                .id(queue.getId())
                .deviceId(queue.getDeviceId())
                .deviceSn(device != null ? device.getDeviceSn() : null)
                .deviceLocation(device != null ? device.getLocation() : null)
                .position(queue.getQueuePosition())
                .estimatedWaitMinutes(queue.getEstimatedWaitMinutes())
                .status(queue.getStatus())
                .statusText(getStatusText(queue.getStatus()))
                .joinTime(queue.getJoinTime())
                .notifyTime(queue.getNotifyTime())
                .expireTime(queue.getExpireTime())
                .build();
    }

    private String getStatusText(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "WAITING": return "排队中";
            case "NOTIFIED": return "轮到您了";
            case "EXPIRED": return "已过期";
            case "CANCELLED": return "已取消";
            case "USED": return "已使用";
            default: return "未知";
        }
    }
}
