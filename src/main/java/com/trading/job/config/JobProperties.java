package com.trading.job.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 【職責】綁定 JOB-A~D 與排程執行緒池設定（{@code trading.job.*}）。
 * 【技巧】{@code @ConfigurationProperties(prefix = "trading.job")} + 巢狀靜態類分組。
 * 【概念】把開關／cron／批次／保留天數集中成型別安全物件，比到處 {@code @Value} 散落好維護。
 * 【邊界】不含 Engine baseUrl（見 {@link EngineClientProperties}）；敏感值請用環境變數覆寫。
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "trading.job")
public class JobProperties {

    private Scheduler scheduler = new Scheduler();
    private StaleOrder staleOrder = new StaleOrder();
    private PnlSnapshot pnlSnapshot = new PnlSnapshot();
    private Retry retry = new Retry();
    private Cleanup cleanup = new Cleanup();

    /**
     * 【職責】Cron {@link org.springframework.scheduling.TaskScheduler} 執行緒池設定。
     * 【技巧】poolSize／threadNamePrefix 對應 {@link SchedulingConfig}。
     * 【概念】預設單執行緒會讓長任務堵住其他 cron；pool-size ≥ 2 可並行。
     */
    @Getter
    @Setter
    public static class Scheduler {
        private int poolSize = 4;
        private String threadNamePrefix = "trading-job-";
    }

    /**
     * 【職責】JOB-A 逾時取消：開關、逾時秒數、批次大小、cron。
     * 【技巧】巢狀 properties 對應 YAML {@code trading.job.stale-order}。
     * 【概念】timeoutSeconds 是「多久沒成交算逾時」的業務門檻，不是 HTTP timeout。
     */
    @Getter
    @Setter
    public static class StaleOrder {
        private boolean enabled = true;
        private long timeoutSeconds = 300;
        private int batchSize = 200;
        private String cron = "0 */5 * * * *";
    }

    /**
     * 【職責】JOB-B PnL 快照：開關與 cron（通常日結）。
     * 【技巧】YAML {@code trading.job.pnl-snapshot}。
     * 【概念】日結頻率用 cron 表達，不必在程式寫死「每天 0 點」。
     */
    @Getter
    @Setter
    public static class PnlSnapshot {
        private boolean enabled = true;
        private String cron = "0 0 0 * * *";
    }

    /**
     * 【職責】JOB-C 失敗重試：最大次數、退避秒數、批次與 cron。
     * 【技巧】YAML {@code trading.job.retry}。
     * 【概念】maxAttempts × backoffSeconds 決定暫態失敗的重試曲線；超過則 DEAD。
     */
    @Getter
    @Setter
    public static class Retry {
        private boolean enabled = true;
        private int maxAttempts = 3;
        private long backoffSeconds = 30;
        private int batchSize = 100;
        private String cron = "0 * * * * *";
    }

    /**
     * 【職責】JOB-D 清理：事件／失敗指令保留天數、批次與 cron。
     * 【技巧】YAML {@code trading.job.cleanup}。
     * 【概念】兩種保留天數可分開調：稽核事件通常比 DLQ 終態列留更久。
     */
    @Getter
    @Setter
    public static class Cleanup {
        private boolean enabled = true;
        private int eventRetentionDays = 30;
        private int failedCommandRetentionDays = 7;
        private int batchSize = 500;
        private String cron = "0 30 0 * * *";
    }
}
