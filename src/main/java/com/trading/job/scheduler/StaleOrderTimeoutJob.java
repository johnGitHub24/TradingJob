package com.trading.job.scheduler;

import com.trading.job.application.StaleOrderCancellationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 【職責】JOB-A 排程入口：依 cron 觸發後委派 {@link StaleOrderCancellationService} 取消逾時未成交訂單。
 * 【技巧】{@code @Scheduled(cron=…)} + {@code @ConditionalOnProperty}；例外 catch 後只打 log。
 * 【概念】排程類只負責「何時跑」與「失敗不拖垮執行緒」；取消規則、批次、事件寫入都在 Service。
 *         若把商業邏輯寫在 Job 裡，手動 API 觸發就無法重用同一套規則。
 * 【邊界】不負責查庫、改狀態、寫審計事件。
 */
@Component
@ConditionalOnProperty(name = "trading.job.stale-order.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class StaleOrderTimeoutJob {

    private final StaleOrderCancellationService staleOrderCancellationService;

    public StaleOrderTimeoutJob(StaleOrderCancellationService staleOrderCancellationService) {
        this.staleOrderCancellationService = staleOrderCancellationService;
    }

    /**
     * 【職責】依 {@code trading.job.stale-order.cron} 執行一次逾時取消。
     * 【技巧】cron 字串由設定注入；{@code try/catch(Exception)} 吞掉後記錄，避免中斷排程執行緒。
     * 【概念】Spring 預設不會因為單次 Job 例外就停掉整個 scheduler，但未捕捉的例外仍會污染日誌與監控；
     *         這裡明確「失敗可觀測、下次 cron 仍會跑」。
     */
    @Scheduled(cron = "${trading.job.stale-order.cron}")
    public void run() {
        try {
            staleOrderCancellationService.cancelStaleOrders();
        } catch (Exception ex) {
            log.error("JOB-A stale order cancellation failed", ex);
        }
    }
}
