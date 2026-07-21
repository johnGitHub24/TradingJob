package com.trading.job.scheduler;

import com.trading.job.application.FailedCommandService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 【職責】JOB-C 排程入口：依 cron 觸發後委派 {@link FailedCommandService} 重試失敗下單指令。
 * 【技巧】{@code @Scheduled} + {@code @ConditionalOnProperty}；例外僅記錄。
 * 【概念】失敗指令像小型 DLQ：背景輪詢到期列，比在同步 API 裡無限重試更可控（退避、上限、DEAD）。
 * 【邊界】不負責選列、呼叫 Engine、狀態轉移。
 */
@Component
@ConditionalOnProperty(name = "trading.job.retry.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class FailedCommandRetryJob {

    private final FailedCommandService failedCommandService;

    public FailedCommandRetryJob(FailedCommandService failedCommandService) {
        this.failedCommandService = failedCommandService;
    }

    /**
     * 【職責】依 {@code trading.job.retry.cron} 執行一批失敗指令重試。
     * 【技巧】cron 由設定注入；失敗 catch 後 log，不向上拋。
     * 【概念】單次批次失敗不應讓後續 cron 停擺；觀測靠 error log／監控，重試節奏靠 nextRetryAt。
     */
    @Scheduled(cron = "${trading.job.retry.cron}")
    public void run() {
        try {
            failedCommandService.retryFailedCommands();
        } catch (Exception ex) {
            log.error("JOB-C failed command retry failed", ex);
        }
    }
}
