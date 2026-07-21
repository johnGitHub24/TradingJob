package com.trading.job.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 【職責】PnL／持倉日結快照實體（{@code pnl_snapshots}）。
 * 【技巧】JPA entity；以 {@code snapshotDate + symbol} 表達「某日某標的」結算點。
 * 【概念】快照是不可變歷史點；與即時 {@code positions} 分離，重跑同日靠應用層冪等跳過。
 * 【邊界】getter／setter 併入類別說明。
 */
@Getter
@Setter
@Entity
@Table(name = "pnl_snapshots")
public class PnlSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "avg_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal avgPrice = BigDecimal.ZERO;

    @Column(name = "mark_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal markPrice = BigDecimal.ZERO;

    @Column(name = "unrealized_pnl", nullable = false, precision = 18, scale = 8)
    private BigDecimal unrealizedPnl = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
