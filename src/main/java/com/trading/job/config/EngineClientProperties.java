package com.trading.job.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 【職責】Engine REST 客戶端設定（{@code trading.engine.*}）。
 * 【技巧】{@code @ConfigurationProperties(prefix = "trading.engine")}。
 * 【概念】把下游位址外置，本機／Docker／正式只需改設定，不必改程式。
 * 【邊界】不含認證密鑰細節；正式環境請用環境變數覆寫 baseUrl。
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "trading.engine")
public class EngineClientProperties {

    private String baseUrl = "http://localhost:8081";
}
