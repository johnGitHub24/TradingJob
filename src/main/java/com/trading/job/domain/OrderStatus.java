package com.trading.job.domain;

/**
 * 【職責】訂單狀態列舉；JOB-A 僅取消 NEW／PARTIALLY_FILLED。
 * 【技巧】JPA {@code @Enumerated(STRING)} 存名稱，避免 ordinal 重排風險。
 * 【概念】FILLED／REJECTED／CANCELLED 為終態：不應再被逾時 Job 改寫。
 */
public enum OrderStatus {
    NEW,
    PARTIALLY_FILLED,
    FILLED,
    REJECTED,
    CANCELLED
}
