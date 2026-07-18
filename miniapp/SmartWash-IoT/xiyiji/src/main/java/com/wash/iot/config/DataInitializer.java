package com.wash.iot.config;

import com.wash.iot.entity.Device;
import com.wash.iot.entity.User;
import com.wash.iot.entity.UserDeviceBinding;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.repository.UserRepository;
import com.wash.iot.repository.UserDeviceBindingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据初始化器 - 应用启动时初始化测试数据
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDeviceBindingRepository userDeviceBindingRepository;

    @Override
    public void run(String... args) {
        initDevices();
        initTestUserBinding();
    }

    /**
     * 初始化测试设备
     */
    private void initDevices() {
        long count = deviceRepository.count();
        log.info("当前数据库中设备数量: {}", count);

        if (count > 0) {
            log.info("数据库中已有 {} 台设备，跳过初始化", count);
            return;
        }

        log.info("数据库中没有设备，开始初始化测试设备...");

        try {
            // 创建测试设备 WASH_001
            Device device1 = new Device();
            device1.setDeviceSn("WASH_001");
            device1.setName("1号洗衣机");
            device1.setQrCodeContent(com.wash.iot.util.DeviceUtils.generateQrCodeContent(device1.getDeviceSn()));
            device1.setQrCodeUrl(com.wash.iot.util.DeviceUtils.generateQrCodeUrl(device1.getDeviceSn()));
            device1.setStatus("IDLE");
            device1.setLocation("1号楼1层");
            device1.setPricingMode("PER_USE");
            device1.setPricePerUse(new BigDecimal("3.00"));
            device1.setLastHeartbeat(LocalDateTime.now());
            deviceRepository.save(device1);
            log.info("创建设备: WASH_001");

            // 创建测试设备 WASH_002
            Device device2 = new Device();
            device2.setDeviceSn("WASH_002");
            device2.setName("2号洗衣机");
            device2.setQrCodeContent(com.wash.iot.util.DeviceUtils.generateQrCodeContent(device2.getDeviceSn()));
            device2.setQrCodeUrl(com.wash.iot.util.DeviceUtils.generateQrCodeUrl(device2.getDeviceSn()));
            device2.setStatus("IDLE");
            device2.setLocation("1号楼2层");
            device2.setPricingMode("PER_USE");
            device2.setPricePerUse(new BigDecimal("3.00"));
            device2.setLastHeartbeat(LocalDateTime.now());
            deviceRepository.save(device2);
            log.info("创建设备: WASH_002");

            // 创建测试设备 WASH_003
            Device device3 = new Device();
            device3.setDeviceSn("WASH_003");
            device3.setName("3号洗衣机");
            device3.setQrCodeContent(com.wash.iot.util.DeviceUtils.generateQrCodeContent(device3.getDeviceSn()));
            device3.setQrCodeUrl(com.wash.iot.util.DeviceUtils.generateQrCodeUrl(device3.getDeviceSn()));
            device3.setStatus("IDLE");
            device3.setLocation("2号楼1层");
            device3.setPricingMode("PER_USE");
            device3.setPricePerUse(new BigDecimal("3.00"));
            device3.setLastHeartbeat(LocalDateTime.now());
            deviceRepository.save(device3);
            log.info("创建设备: WASH_003");

            log.info("测试设备初始化完成，共创建 3 台设备");
        } catch (Exception e) {
            log.error("初始化设备失败", e);
        }
    }

    /**
     * 为所有现有用户绑定 WASH_001 设备（测试用）
     */
    private void initTestUserBinding() {
        try {
            // 查找 WASH_001 设备
            Device device = deviceRepository.findByDeviceSn("WASH_001").orElse(null);
            if (device == null) {
                log.warn("WASH_001 设备不存在，跳过用户绑定初始化");
                return;
            }

            // 获取所有用户
            List<User> users = userRepository.findAll();
            if (users.isEmpty()) {
                log.info("暂无用户，跳过设备绑定初始化");
                return;
            }

            int bindCount = 0;
            for (User user : users) {
                // 检查是否已绑定
                boolean exists = userDeviceBindingRepository.existsByUserIdAndDeviceIdAndStatus(
                        user.getId(), device.getId(), "ACTIVE");

                if (!exists) {
                    UserDeviceBinding binding = new UserDeviceBinding();
                    binding.setUserId(user.getId());
                    binding.setDeviceId(device.getId());
                    binding.setStatus("ACTIVE");
                    userDeviceBindingRepository.save(binding);
                    bindCount++;
                    log.info("为用户 {} (ID:{}) 绑定设备 WASH_001", user.getNickName(), user.getId());
                }
            }

            if (bindCount > 0) {
                log.info("测试用户设备绑定完成，共绑定 {} 个用户", bindCount);
            } else {
                log.info("所有用户已绑定 WASH_001，无需重复绑定");
            }
        } catch (Exception e) {
            log.error("初始化用户设备绑定失败", e);
        }
    }
}
