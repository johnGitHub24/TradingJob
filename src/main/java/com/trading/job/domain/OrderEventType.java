package com.trading.job.domain;

/**
 * 【職責】訂單審計事件類型（與 Engine 共用語意，append-only）。
 * 【技巧】字串列舉寫入 {@code order_events.event}。
 * 【概念】事件是「發生過什麼」的時間線，不是訂單目前狀態的替代品；JOB-A 主要寫 CANCELLED。
 */
public enum OrderEventType {
    RECEIVED,
    RISK_CHECK,
    APPROVED,
    REJECTED,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    OUTCOME_RECORDED,
    DISCIPLINE_FLAG,
    POSITION_UPDATED
}
