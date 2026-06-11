package com.stock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tencent-ai")
public class TencentAIConfig {

    private boolean enabled = true;
    private String apiKey;
    private String model = "hunyuan-lite";
    private String baseUrl = "https://hunyuan.tencentcloudapi.com";
}
