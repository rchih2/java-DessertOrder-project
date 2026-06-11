package com.stock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "dingtalk")
public class DingTalkConfig {

    private boolean enabled = true;
    /** 企业内部应用 AppKey */
    private String appKey;
    /** 企业内部应用 AppSecret */
    private String appSecret;
    /** 企业内部应用 AgentId */
    private Long agentId;
    /** 接收消息的用户ID列表（钉钉userId），多个用逗号分隔，留空则发送给全部 */
    private String userIdList;
}
