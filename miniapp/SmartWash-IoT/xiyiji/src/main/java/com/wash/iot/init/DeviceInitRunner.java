package com.wash.iot.init;

import com.wash.iot.entity.Device;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.util.DeviceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.logging.Logger;

/**
 * 设备初始化组件，用于在应用启动时为现有设备批量生成SN码和二维码
 */
@Component
public class DeviceInitRunner implements CommandLineRunner {

    private static final Logger logger = Logger.getLogger(DeviceInitRunner.class.getName());

    @Autowired
    private DeviceRepository deviceRepository;

    @Override
    public void run(String... args) throws Exception {
        logger.info("开始初始化设备数据...");
        
        // 查询所有设备
        List<Device> devices = deviceRepository.findAll();
        logger.info("共找到 " + devices.size() + " 台设备");
        
        int updatedCount = 0;
        int newDeviceCount = 0;
        
        // 遍历设备，为没有SN码的设备生成SN码和二维码
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            
            // 如果设备已经有SN码，则跳过
            if (device.getDeviceSn() != null && !device.getDeviceSn().isEmpty()) {
                // 检查是否需要生成二维码
                if (device.getQrCodeContent() == null || device.getQrCodeUrl() == null) {
                    generateQrCodeForDevice(device);
                    updatedCount++;
                }
                continue;
            }
            
            // 为没有SN码的设备生成SN码
            String deviceSn = DeviceUtils.generateSimpleDeviceSn(i + 1);
            device.setDeviceSn(deviceSn);
            
            // 生成二维码
            generateQrCodeForDevice(device);
            
            // 保存设备
            deviceRepository.save(device);
            updatedCount++;
            newDeviceCount++;
            
            logger.info("已为设备ID " + device.getId() + " 生成SN码: " + deviceSn);
        }
        
        logger.info("设备初始化完成！");
        logger.info("更新设备数: " + updatedCount);
        logger.info("新生成SN码设备数: " + newDeviceCount);
    }
    
    /**
     * 为设备生成二维码
     * @param device 设备对象
     */
    private void generateQrCodeForDevice(Device device) {
        String deviceSn = device.getDeviceSn();
        
        // 生成二维码内容
        String qrCodeContent = DeviceUtils.generateQrCodeContent(deviceSn);
        device.setQrCodeContent(qrCodeContent);
        
        // 生成二维码URL
        String qrCodeUrl = DeviceUtils.generateQrCodeUrl(deviceSn);
        device.setQrCodeUrl(qrCodeUrl);
    }
}
