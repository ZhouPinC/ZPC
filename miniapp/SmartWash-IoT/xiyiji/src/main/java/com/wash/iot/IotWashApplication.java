package com.wash.iot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
public class IotWashApplication {
    public static void main(String[] args) {
        SpringApplication.run(IotWashApplication.class, args);
        System.out.println(">>> 共享洗衣机物联网后端启动成功！ <<<");
    }
}
