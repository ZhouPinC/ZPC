package com.wash.iot.domain.order.service;

import com.wash.iot.common.exception.BusinessException;
import com.wash.iot.domain.order.model.OrderStatus;
import com.wash.iot.entity.Order;
import org.springframework.stereotype.Service;

/**
 * 订单领域服务
 * 负责管理订单状态流转的业务规则
 * 只关注业务逻辑，不依赖Web层、数据库或MQTT
 */
@Service
public class OrderDomainService {

    /**
     * 执行订单状态转换
     * @param order 订单对象
     * @param targetStatus 目标状态
     * @throws BusinessException 当状态流转不合法时抛出
     */
    public void transitionTo(Order order, OrderStatus targetStatus) {
        // 获取当前状态字符串并转换为枚举
        OrderStatus current = OrderStatus.valueOf(order.getStatus());
        boolean isValid = false;

        // 定义合法的状态流转规则
        switch (current) {
            case CREATED:
                isValid = (targetStatus == OrderStatus.PAID || targetStatus == OrderStatus.CANCELLED);
                break;
            case PAID:
                isValid = (targetStatus == OrderStatus.RUNNING || targetStatus == OrderStatus.REFUNDING);
                break;
            case RUNNING:
                isValid = (targetStatus == OrderStatus.FINISHED || targetStatus == OrderStatus.FAILED);
                break;
            case FINISHED:
                isValid = false;
                break;
            case FAILED:
                isValid = (targetStatus == OrderStatus.REFUNDING);
                break;
            case CANCELLED:
                isValid = false;
                break;
            case REFUNDING:
                isValid = (targetStatus == OrderStatus.REFUNDED);
                break;
            case REFUNDED:
                isValid = false;
                break;
        }

        if (!isValid) {
            throw new BusinessException(
                String.format("非法状态流转: 从 %s 到 %s", current, targetStatus)
            );
        }
        
        // 更新订单状态
        order.setStatus(targetStatus.name());
        // 只负责修改内存对象状态，不保存数据库，保存由AppService负责
    }
}