package com.trading.job.infrastructure.entity;

import com.trading.job.domain.OrderEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * 【職責】訂單審計事件實體（{@code order_events}，append-only）。
 * 【技巧】JPA entity；僅有 {@code createdAt}，無 update 語意。
 * 【概念】事件只追加：JOB-A 寫取消；JOB-D 依保留天數批次刪舊列，不改歷史內容。
 * 【邊界】getter／setter 併入類別說明。
 */
@Getter
@Setter
@Entity
@Table(name = "order_events")
public class OrderEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderEventType event;

    @Column(name = "risk_rule_code", length = 16)
    private String riskRuleCode;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
