package com.stock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockApplication.class, args);
        System.out.println("========================================");
        System.out.println("  A股智能选股系统启动成功！");
        System.out.println("  访问 http://localhost:8088");
        System.out.println("========================================");
    }
}
