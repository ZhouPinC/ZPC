package com.wash.iot.config;

import com.wash.iot.entity.Device;
import com.wash.iot.entity.User;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.repository.UserRepository;
import com.wash.iot.repository.UserDeviceBindingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * 测试环境配置，用于测试账号自动绑定设备
 */
@Component
@Profile({"test", "dev"}) // 在测试和开发环境生效
public class TestEnvironmentConfig {

    private static final Logger logger = Logger.getLogger(TestEnvironmentConfig.class.getName());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private UserDeviceBindingRepository userDeviceBindingRepository;

    // 测试设备SN
    private static final String TEST_DEVICE_SN = "WASH_001";
    // 测试账号ID（假设固定为1）
    private static final Long TEST_USER_ID = 1L;

    @PostConstruct
    public void initTestEnvironment() {
        logger.info("初始化测试环境...");
        
        // 确保测试设备存在
        Optional<Device> deviceOpt = deviceRepository.findByDeviceSn(TEST_DEVICE_SN);
        if (!deviceOpt.isPresent()) {
            // 创建测试设备
            logger.info("创建测试设备: " + TEST_DEVICE_SN);
            Device device = new Device();
            device.setDeviceSn(TEST_DEVICE_SN);
            device.setName("测试洗衣机");
            device.setLocation("测试位置");
            device.setStatus("IDLE");
            deviceRepository.save(device);
        }
        
        // 确保测试用户存在
        Optional<User> userOpt = userRepository.findById(TEST_USER_ID);
        if (!userOpt.isPresent()) {
            // 创建测试用户
            logger.info("创建测试用户，ID: " + TEST_USER_ID);
            User user = new User();
            user.setId(TEST_USER_ID);
            user.setNickName("测试用户");
            user.setAvatarUrl("/images/me.png");
            user.setPhone("******");
            user.setOpenId("test_openid_001");
            user.setRole("SUPER_ADMIN");
            userRepository.save(user);
        } else {
            User user = userOpt.get();
            if (user.getRole() == null || !"SUPER_ADMIN".equals(user.getRole())) {
                user.setRole("SUPER_ADMIN");
                userRepository.save(user);
            }
        }
        
        // 查找设备
        Device device = deviceRepository.findByDeviceSn(TEST_DEVICE_SN).orElse(null);
        if (device == null) {
            logger.warning("测试设备不存在，无法绑定");
            return;
        }
        
        // 自动绑定设备到测试用户
        logger.info("将测试设备 " + TEST_DEVICE_SN + " 绑定到测试用户 ID: " + TEST_USER_ID);
        
        // 检查是否已绑定
        boolean isBound = userDeviceBindingRepository.existsByUserIdAndDeviceIdAndStatus(
                TEST_USER_ID, device.getId(), "ACTIVE");
        
        if (!isBound) {
            // 创建绑定关系
            com.wash.iot.entity.UserDeviceBinding binding = new com.wash.iot.entity.UserDeviceBinding();
            binding.setUserId(TEST_USER_ID);
            binding.setDeviceId(device.getId());
            binding.setStatus("ACTIVE");
            userDeviceBindingRepository.save(binding);
            logger.info("测试设备绑定成功");
        } else {
            logger.info("测试设备已绑定");
        }
    }
}
