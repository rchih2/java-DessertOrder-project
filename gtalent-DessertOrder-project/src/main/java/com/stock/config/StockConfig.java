package com.stock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "stock")
public class StockConfig {

    private String poolFile = "stock-pool.txt";
    private List<String> klinePeriods = List.of("daily", "weekly", "monthly");
}
