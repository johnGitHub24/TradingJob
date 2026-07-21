package com.trading.job.infrastructure.entity;

import com.trading.job.domain.OrderSide;
import com.trading.job.domain.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 【職責】訂單實體（與 Engine 共用 {@code orders} 表）。
 * 【技巧】JPA {@code @Entity}；Lombok getter／setter；{@code @CreationTimestamp}/{@code @UpdateTimestamp}。
 * 【概念】Job 服務不擁有下單主流程，但透過共用表執行逾時取消與測試重放寫入。
 * 【邊界】getter／setter 語意見欄位名，不另灌水。
 */
@Getter
@Setter
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_order_id", unique = true, length = 64)
    private String clientOrderId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private OrderSide side;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal price;

    @Column(name = "filled_quantity", precision = 18, scale = 8)
    private BigDecimal filledQuantity = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "risk_rule_code", length = 16)
    private String riskRuleCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
