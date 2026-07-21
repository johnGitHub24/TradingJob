package com.trading.job.infrastructure.entity;

import com.trading.job.domain.FailedCommandStatus;
import com.trading.job.domain.OrderSide;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 【職責】失敗下單指令 DLQ 實體（{@code failed_commands}）。
 * 【技巧】JPA entity；{@code attempts}/{@code nextRetryAt}/{@code status} 支撐重試狀態機。
 * 【概念】像資料庫裡的小型死信佇列：PENDING 待重試，SUCCEEDED／DEAD 終態後由 JOB-D 清理。
 * 【邊界】getter／setter 併入類別說明。
 */
@Getter
@Setter
@Entity
@Table(name = "failed_commands")
public class FailedCommandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "command_id", length = 64)
    private String commandId;

    @Column(name = "client_order_id", length = 64)
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

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(nullable = false)
    private int attempts = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FailedCommandStatus status = FailedCommandStatus.PENDING;

    @Column(name = "next_retry_at", nullable = false)
    private OffsetDateTime nextRetryAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
