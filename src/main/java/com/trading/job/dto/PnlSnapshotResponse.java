package com.trading.job.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 【職責】PnL 結算快照查詢回應（JOB-B 輸出的 API 投影）。
 * 【技巧】{@code record}；欄位對齊快照表語意。
 * 【概念】查詢 API 回傳不可變投影，與寫入用的 entity 分離。
 */
public record PnlSnapshotResponse(
        Long id,
        LocalDate snapshotDate,
        String symbol,
        BigDecimal quantity,
        BigDecimal avgPrice,
        BigDecimal markPrice,
        BigDecimal unrealizedPnl,
        OffsetDateTime createdAt) {
}
