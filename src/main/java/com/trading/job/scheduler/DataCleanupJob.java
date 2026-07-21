package com.trading.job.scheduler;

import com.trading.job.application.DataCleanupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 【職責】JOB-D 排程入口：依 cron 觸發後委派 {@link DataCleanupService} 清理過期資料。
 * 【技巧】{@code @Scheduled} + {@code @ConditionalOnProperty}；例外僅記錄。
 * 【概念】清理是「保留天數」政策的執行者，應與業務寫入路徑分離，避免在熱路徑做大量 DELETE。
 * 【邊界】不負責計算 cutoff、執行刪除、決定哪些狀態可清。
 */
@Component
@ConditionalOnProperty(name = "trading.job.cleanup.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class DataCleanupJob {

    private final DataCleanupService dataCleanupService;

    public DataCleanupJob(DataCleanupService dataCleanupService) {
        this.dataCleanupService = dataCleanupService;
    }

    /**
     * 【職責】依 {@code trading.job.cleanup.cron} 執行一次過期資料清理。
     * 【技巧】cron 由設定注入；失敗 catch 後 log，不向上拋。
     * 【概念】清理失敗通常可下次再跑；重要的是 PENDING 失敗指令永不被誤刪（規則在 Service）。
     */
    @Scheduled(cron = "${trading.job.cleanup.cron}")
    public void run() {
        try {
            dataCleanupService.cleanup();
        } catch (Exception ex) {
            log.error("JOB-D data cleanup failed", ex);
        }
    }
}
