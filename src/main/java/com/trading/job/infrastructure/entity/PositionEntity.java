package com.trading.job.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 【職責】持倉實體（與 Engine 共用 {@code positions} 表）。
 * 【技巧】JPA entity；JOB-B 唯讀後複製到快照表。
 * 【概念】持倉由 Engine 維護；本服務只「拍照」，不在 Job 路徑改 quantity／PnL。
 * 【邊界】getter／setter 併入類別說明。
 */
@Getter
@Setter
@Entity
@Table(name = "positions")
public class PositionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "avg_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal avgPrice = BigDecimal.ZERO;

    @Column(name = "unrealized_pnl", nullable = false, precision = 18, scale = 8)
    private BigDecimal unrealizedPnl = BigDecimal.ZERO;

    @Column(name = "mark_price", precision = 18, scale = 8)
    private BigDecimal markPrice = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
