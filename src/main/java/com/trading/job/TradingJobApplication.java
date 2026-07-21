package com.trading.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 【職責】Trading Job 服務啟動入口：獨立部署的排程服務（JOB-A~D）。
 * 【技巧】{@code @SpringBootApplication} 啟動元件掃描與自動設定。
 * 【概念】與 APIGatewayMQ Engine 共用同一資料庫，但進程分離：下單熱路徑在 Engine，背景維運在本服務。
 * 【邊界】不內嵌撮合／風控引擎本體。
 */
@SpringBootApplication
public class TradingJobApplication {

    /**
     * 【職責】啟動 Spring Boot 應用。
     * 【技巧】{@link SpringApplication#run}。
     * 【概念】標準 Boot 入口；本機可用 {@code --spring.profiles.active=dev} 切 H2。
     *
     * @param args 命令列參數
     */
    public static void main(String[] args) {
        SpringApplication.run(TradingJobApplication.class, args);
    }
}
