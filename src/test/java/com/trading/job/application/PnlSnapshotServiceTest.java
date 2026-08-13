package com.trading.job.application;

import com.trading.job.infrastructure.entity.PnlSnapshotEntity;
import com.trading.job.infrastructure.entity.PositionEntity;
import com.trading.job.infrastructure.repository.PnlSnapshotRepository;
import com.trading.job.infrastructure.repository.PositionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 【職責】{@link PnlSnapshotService} 單元測試：覆蓋 JOB-B 寫入、空持倉與同日冪等。
 * 【技巧】{@code @InjectMocks} + Mock Repository；Captor 驗證快照欄位。
 * 【概念】冪等＝「已有同日同標的就不 save」，與整合測試的真實 DB 行為對齊。
 * 【技巧驗證】空持倉不寫入；已存在則 written=0。
 */
@ExtendWith(MockitoExtension.class)
class PnlSnapshotServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PnlSnapshotRepository pnlSnapshotRepository;

    @InjectMocks
    private PnlSnapshotService service;

    private PositionEntity position(String symbol, String qty, String pnl) {
        PositionEntity p = new PositionEntity();
        p.setSymbol(symbol);
        p.setQuantity(new BigDecimal(qty));
        p.setAvgPrice(new BigDecimal("65000"));
        p.setMarkPrice(new BigDecimal("66000"));
        p.setUnrealizedPnl(new BigDecimal(pnl));
        return p;
    }

    /**
     * CASE-JOB-PNL-001：每個持倉寫一筆今日快照。
     * Given: 一筆持倉且今日尚無快照；When: captureSnapshot；Then: written=1、欄位對齊持倉。
     */
    @Test
    void JOB_PNL_001_SNAPSHOT_writesRowPerPosition() {
        when(positionRepository.findAll()).thenReturn(List.of(position("BTCUSDT", "0.5", "500")));
        when(pnlSnapshotRepository.findBySnapshotDateAndSymbol(any(LocalDate.class), eq("BTCUSDT")))
                .thenReturn(List.of());

        int written = service.captureSnapshot();

        assertThat(written).isEqualTo(1);
        ArgumentCaptor<PnlSnapshotEntity> captor = ArgumentCaptor.forClass(PnlSnapshotEntity.class);
        verify(pnlSnapshotRepository).save(captor.capture());
        PnlSnapshotEntity saved = captor.getValue();
        assertThat(saved.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(saved.getUnrealizedPnl()).isEqualByComparingTo("500");
        assertThat(saved.getSnapshotDate()).isEqualTo(LocalDate.now());
    }

    /**
     * CASE-JOB-PNL-002：無持倉不寫入。
     * Given: 持倉空；When: captureSnapshot；Then: 0 且不 save。
     */
    @Test
    void JOB_PNL_002_EMPTY_noPositions_writesNothing() {
        when(positionRepository.findAll()).thenReturn(List.of());

        int written = service.captureSnapshot();

        assertThat(written).isZero();
        verify(pnlSnapshotRepository, never()).save(any());
    }

    /**
     * CASE-JOB-PNL-003：同日同標的已存在則跳過（冪等）。
     * Given: 今日已有 BTCUSDT 快照；When: captureSnapshot；Then: written=0、不 save。
     */
    @Test
    void JOB_PNL_003_IDEMPOTENT_skipsSymbolAlreadySnapshottedToday() {
        when(positionRepository.findAll()).thenReturn(List.of(position("BTCUSDT", "0.5", "500")));
        when(pnlSnapshotRepository.findBySnapshotDateAndSymbol(any(LocalDate.class), eq("BTCUSDT")))
                .thenReturn(List.of(new PnlSnapshotEntity()));

        int written = service.captureSnapshot();

        assertThat(written).isZero();
        verify(pnlSnapshotRepository, never()).save(any());
    }

    /**
     * CASE-JOB-API-005：PnL 快照列表可回空陣列。
     * Given: Repository 回空；When: findByDate(null)；Then: 空列表（日期預設今日）。
     */
    @Test
    void JOB_API_005_findByDate_null_returnsEmptyList() {
        when(pnlSnapshotRepository.findBySnapshotDate(LocalDate.now())).thenReturn(List.of());

        assertThat(service.findByDate(null)).isEmpty();
        verify(pnlSnapshotRepository).findBySnapshotDate(LocalDate.now());
    }
}
