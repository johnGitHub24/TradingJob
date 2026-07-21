package com.trading.job.scheduler;

import com.trading.job.application.PnlSnapshotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 【職責】JOB-B 排程入口：依 cron 觸發後委派 {@link PnlSnapshotService} 寫入當日 PnL／持倉快照。
 * 【技巧】{@code @Scheduled} + {@code @ConditionalOnProperty}；例外僅記錄。
 * 【概念】日結快照適合「定時、可重跑、冪等」的背景工作；與即時查持倉 API 分離，避免把結算邏輯綁在請求路徑。
 * 【邊界】不負責讀持倉、算市價、寫快照列。
 */
@Component
@ConditionalOnProperty(name = "trading.job.pnl-snapshot.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class PnlSnapshotJob {

    private final PnlSnapshotService pnlSnapshotService;

    public PnlSnapshotJob(PnlSnapshotService pnlSnapshotService) {
        this.pnlSnapshotService = pnlSnapshotService;
    }

    /**
     * 【職責】依 {@code trading.job.pnl-snapshot.cron} 執行一次當日快照。
     * 【技巧】cron 由設定注入；失敗 catch 後 log，不向上拋。
     * 【概念】同一天重跑應由 Service 冪等處理；Job 層只保證「觸發與容錯」。
     */
    @Scheduled(cron = "${trading.job.pnl-snapshot.cron}")
    public void run() {
        try {
            pnlSnapshotService.captureSnapshot();
        } catch (Exception ex) {
            log.error("JOB-B PnL snapshot failed", ex);
        }
    }
}
