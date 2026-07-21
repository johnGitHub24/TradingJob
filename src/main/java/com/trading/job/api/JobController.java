package com.trading.job.api;

import com.trading.job.application.DataCleanupService;
import com.trading.job.application.FailedCommandService;
import com.trading.job.application.PnlSnapshotService;
import com.trading.job.application.StaleOrderCancellationService;
import com.trading.job.domain.FailedCommandStatus;
import com.trading.job.dto.FailedCommandResponse;
import com.trading.job.dto.JobRunResponse;
import com.trading.job.dto.PnlSnapshotResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 【職責】對外 API 入口：提供 JOB-A~D 手動觸發與 PnL／失敗指令查詢。
 * 【技巧】薄 {@code @RestController}：收參數、轉交 Service、組 DTO／{@link JobRunResponse}。
 * 【概念】手動觸發與 cron 共用同一 Service，方便本機驗證與運維補跑；Controller 不寫商業規則。
 * 【邊界】不負責排程、不直接存取 Repository、不做風控。
 */
@Tag(name = "Jobs", description = "排程 Job（JOB-A 超時取消 / JOB-B PnL 快照 / JOB-C 失敗重試 / JOB-D 資料清理）")
@RestController
@RequestMapping("/api/v1")
public class JobController {

    private final StaleOrderCancellationService staleOrderCancellationService;
    private final PnlSnapshotService pnlSnapshotService;
    private final FailedCommandService failedCommandService;
    private final DataCleanupService dataCleanupService;

    public JobController(StaleOrderCancellationService staleOrderCancellationService,
                         PnlSnapshotService pnlSnapshotService,
                         FailedCommandService failedCommandService,
                         DataCleanupService dataCleanupService) {
        this.staleOrderCancellationService = staleOrderCancellationService;
        this.pnlSnapshotService = pnlSnapshotService;
        this.failedCommandService = failedCommandService;
        this.dataCleanupService = dataCleanupService;
    }

    /**
     * 【職責】手動觸發 JOB-A：取消逾時未成交訂單。
     * 【技巧】POST 無 body；回傳 {@link JobRunResponse#of}。
     * 【概念】與 cron 路徑相同邏輯，只是觸發來源改為 HTTP。
     *
     * @return 執行結果（job 代碼、影響筆數、說明）
     */
    @Operation(summary = "JOB-A 手動執行：取消逾時未成交訂單")
    @PostMapping("/jobs/stale-order-cancellation")
    public JobRunResponse runStaleOrderCancellation() {
        int cancelled = staleOrderCancellationService.cancelStaleOrders();
        return JobRunResponse.of("JOB-A", cancelled, "cancelled stale orders");
    }

    /**
     * 【職責】手動觸發 JOB-B：建立當日 PnL／持倉結算快照。
     * 【技巧】委派 {@link PnlSnapshotService#captureSnapshot()}。
     * 【概念】日結可補跑；冪等由 Service 保證。
     *
     * @return 執行結果（寫入快照筆數）
     */
    @Operation(summary = "JOB-B 手動執行：建立當日 PnL/持倉結算快照")
    @PostMapping("/jobs/pnl-snapshot")
    public JobRunResponse runPnlSnapshot() {
        int written = pnlSnapshotService.captureSnapshot();
        return JobRunResponse.of("JOB-B", written, "pnl snapshots written");
    }

    /**
     * 【職責】手動觸發 JOB-C：重試到期的失敗下單指令。
     * 【技巧】委派 {@link FailedCommandService#retryFailedCommands()}。
     * 【概念】運維可在觀察到暫態故障恢復後立刻補跑，不必等下一個 cron。
     *
     * @return 執行結果（成功重放筆數）
     */
    @Operation(summary = "JOB-C 手動執行：重試失敗的下單指令")
    @PostMapping("/jobs/failed-command-retry")
    public JobRunResponse runFailedCommandRetry() {
        int succeeded = failedCommandService.retryFailedCommands();
        return JobRunResponse.of("JOB-C", succeeded, "failed commands replayed");
    }

    /**
     * 【職責】手動觸發 JOB-D：清理過期審計事件與終態失敗指令。
     * 【技巧】把 {@link DataCleanupService.CleanupResult} 彙總成 affected + detail 字串。
     * 【概念】detail 保留分項數字，方便對帳「刪了哪些類資料」。
     *
     * @return 執行結果（刪除總筆數與明細）
     */
    @Operation(summary = "JOB-D 手動執行：清理過期審計事件與失敗指令")
    @PostMapping("/jobs/cleanup")
    public JobRunResponse runCleanup() {
        DataCleanupService.CleanupResult result = dataCleanupService.cleanup();
        long total = (long) result.deletedOrderEvents() + result.deletedFailedCommands();
        String detail = "events=" + result.deletedOrderEvents() + ", failedCommands=" + result.deletedFailedCommands();
        return JobRunResponse.of("JOB-D", total, detail);
    }

    /**
     * 【職責】查詢 PnL 結算快照（JOB-B 輸出）。
     * 【技巧】可選 {@code date} query；實體 stream map 成 {@link PnlSnapshotResponse}。
     * 【概念】API 回傳投影 DTO，不直接暴露 JPA entity（避免懶載入／內部欄位外洩）。
     *
     * @param date 快照日期；未指定則為今日
     * @return 該日各標的快照列表
     */
    @Operation(summary = "查詢 PnL 結算快照（JOB-B 輸出），未指定日期則為今日")
    @GetMapping("/pnl-snapshots")
    public List<PnlSnapshotResponse> listPnlSnapshots(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return pnlSnapshotService.findByDate(date).stream()
                .map(s -> new PnlSnapshotResponse(s.getId(), s.getSnapshotDate(), s.getSymbol(),
                        s.getQuantity(), s.getAvgPrice(), s.getMarkPrice(), s.getUnrealizedPnl(), s.getCreatedAt()))
                .toList();
    }

    /**
     * 【職責】查詢失敗指令 DLQ（JOB-C 佇列）。
     * 【技巧】可選 {@code status} 過濾；map 成 {@link FailedCommandResponse}。
     * 【概念】給運維／教學觀察重試狀態，不是下單入口。
     *
     * @param status 可選狀態過濾；未指定則回傳近期筆數上限內全部
     * @return 失敗指令列表
     */
    @Operation(summary = "查詢失敗指令 DLQ（JOB-C 佇列），可依狀態過濾")
    @GetMapping("/failed-commands")
    public List<FailedCommandResponse> listFailedCommands(
            @RequestParam(required = false) FailedCommandStatus status) {
        return failedCommandService.findByStatus(status).stream()
                .map(c -> new FailedCommandResponse(c.getId(), c.getCommandId(), c.getClientOrderId(),
                        c.getSymbol(), c.getSide(), c.getQuantity(), c.getPrice(), c.getAttempts(),
                        c.getStatus(), c.getFailureReason(), c.getNextRetryAt()))
                .toList();
    }
}
