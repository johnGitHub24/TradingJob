package com.trading.job.integration;

import com.trading.job.application.PnlSnapshotService;
import com.trading.job.infrastructure.entity.PositionEntity;
import com.trading.job.infrastructure.repository.PnlSnapshotRepository;
import com.trading.job.infrastructure.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【職責】JOB-B 整合測試：真實 DB 驗證快照寫入、空持倉與同日冪等。
 * 【技巧】{@code @SpringBootTest}；連續兩次 {@code captureSnapshot} 驗證不重複。
 * 【概念】冪等在真實 persistence 下仍成立，不只是 Mock 回傳空列表。
 * 【技巧驗證】兩標的寫兩列；重跑同日 written=0。
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class PnlSnapshotJobIntegrationTest {

    @Autowired
    private PnlSnapshotService pnlSnapshotService;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private PnlSnapshotRepository pnlSnapshotRepository;

    @BeforeEach
    void clean() {
        pnlSnapshotRepository.deleteAll();
        positionRepository.deleteAll();
    }

    private void savePosition(String symbol, String qty, String pnl) {
        PositionEntity p = new PositionEntity();
        p.setSymbol(symbol);
        p.setQuantity(new BigDecimal(qty));
        p.setAvgPrice(new BigDecimal("65000"));
        p.setMarkPrice(new BigDecimal("66000"));
        p.setUnrealizedPnl(new BigDecimal(pnl));
        positionRepository.save(p);
    }

    /**
     * CASE-JOB-PNL-001：每個持倉寫一列快照。
     * Given: 兩筆持倉；When: captureSnapshot；Then: written=2、今日快照 2 筆。
     */
    @Test
    void JOB_PNL_001_SNAPSHOT_writesOneRowPerPosition() {
        savePosition("BTCUSDT", "0.5", "500");
        savePosition("ETHUSDT", "2", "-100");

        int written = pnlSnapshotService.captureSnapshot();

        assertThat(written).isEqualTo(2);
        assertThat(pnlSnapshotRepository.findBySnapshotDate(LocalDate.now())).hasSize(2);
    }

    /**
     * CASE-JOB-PNL-002：無持倉不寫入。
     * Given: 空持倉表；When: captureSnapshot；Then: 0 且表仍空。
     */
    @Test
    void JOB_PNL_002_EMPTY_noPositions_writesNothing() {
        int written = pnlSnapshotService.captureSnapshot();

        assertThat(written).isZero();
        assertThat(pnlSnapshotRepository.count()).isZero();
    }

    /**
     * CASE-JOB-PNL-003：同日重跑不重複寫入。
     * Given: 一筆持倉；When: 連續兩次 captureSnapshot；Then: 第一次 1、第二次 0、表僅 1 列。
     */
    @Test
    void JOB_PNL_003_IDEMPOTENT_rerunSameDayDoesNotDuplicate() {
        savePosition("BTCUSDT", "0.5", "500");

        assertThat(pnlSnapshotService.captureSnapshot()).isEqualTo(1);
        assertThat(pnlSnapshotService.captureSnapshot()).isZero();
        assertThat(pnlSnapshotRepository.findBySnapshotDate(LocalDate.now())).hasSize(1);
    }
}
