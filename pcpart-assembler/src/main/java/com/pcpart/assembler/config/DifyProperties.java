package com.pcpart.assembler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Dify 应用配置。
 * base-url 指向 Dify 站点根地址，client 会自动拼上 /v1/chat-messages。
 */
@Data
@Component
@ConfigurationProperties(prefix = "dify")
public class DifyProperties {

    /** 例如 http://139.196.7.130 或 http://139.196.7.130/v1 */
    private String baseUrl = "http://139.196.7.130";

    /** 应用 API Key，形如 app-xxxx */
    private String apiKey = "";

    /** Dify 侧的用户标识，用于区分会话归属 */
    private String user = "pcpart-java";

    /** 连接超时（毫秒） */
    private int connectTimeout = 10000;

    /** 读取超时（毫秒），LLM 生成较慢，默认 3 分钟 */
    private int readTimeout = 180000;
}
