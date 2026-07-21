package com.trading.job.dto;

import com.trading.job.domain.FailedCommandStatus;
import com.trading.job.domain.OrderSide;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 【職責】失敗指令 DLQ 查詢回應（API 對外投影）。
 * 【技巧】Java {@code record} 不可變；由 Controller 自 entity 組裝。
 * 【概念】投影只暴露運維需要的欄位，隱藏 JPA／內部實作細節。
 */
public record FailedCommandResponse(
        Long id,
        String commandId,
        String clientOrderId,
        String symbol,
        OrderSide side,
        BigDecimal quantity,
        BigDecimal price,
        int attempts,
        FailedCommandStatus status,
        String failureReason,
        OffsetDateTime nextRetryAt) {
}
