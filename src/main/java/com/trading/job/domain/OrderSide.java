package com.trading.job.domain;

/**
 * 【職責】買賣方向（BUY／SELL）。
 * 【技巧】字串列舉，與 Engine 契約對齊。
 * 【概念】方向是訂單不可變屬性之一；Job 重放時原樣帶回 Engine。
 */
public enum OrderSide {
    BUY,
    SELL
}
