package com.trading.job.dto;

import java.time.OffsetDateTime;

/**
 * 【職責】Job 手動／排程執行結果回應：哪個 Job、影響筆數、說明、執行時間。
 * 【技巧】{@code record} + 靜態工廠 {@link #of} 自動填 {@code executedAt}。
 * 【概念】統一四條 Job 的回傳形狀，前端／測試可用同一 JSON 路徑斷言。
 */
public record JobRunResponse(String job, long affected, String detail, OffsetDateTime executedAt) {

    /**
     * 【職責】以當前時間組裝執行結果。
     * 【技巧】靜態工廠隱藏 {@code OffsetDateTime.now()}。
     * 【概念】呼叫端不必每次手動塞時間戳，避免漏欄位。
     *
     * @param job      Job 代碼（如 JOB-A）
     * @param affected 影響筆數
     * @param detail   人類可讀明細
     * @return 含 executedAt 的回應
     */
    public static JobRunResponse of(String job, long affected, String detail) {
        return new JobRunResponse(job, affected, detail, OffsetDateTime.now());
    }
}
