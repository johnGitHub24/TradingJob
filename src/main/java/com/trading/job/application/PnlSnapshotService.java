package com.trading.job.application;

import com.trading.job.infrastructure.entity.PnlSnapshotEntity;
import com.trading.job.infrastructure.entity.PositionEntity;
import com.trading.job.infrastructure.repository.PnlSnapshotRepository;
import com.trading.job.infrastructure.repository.PositionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 【職責】JOB-B 商業邏輯：把當日持倉複製成不可變 PnL 快照；同日同標的已存在則跳過。
 * 【技巧】{@code @Transactional}；寫入前 {@code findBySnapshotDateAndSymbol} 做應用層冪等檢查。
 * 【概念】快照是「某一天的結算點」，與即時 positions 分離；重跑同一天不應產生重複列。
 * 【邊界】不負責市價計算、排程觸發；不更新 positions。
 */
@Service
@Slf4j
public class PnlSnapshotService {

    private final PositionRepository positionRepository;
    private final PnlSnapshotRepository pnlSnapshotRepository;

    public PnlSnapshotService(PositionRepository positionRepository,
                              PnlSnapshotRepository pnlSnapshotRepository) {
        this.positionRepository = positionRepository;
        this.pnlSnapshotRepository = pnlSnapshotRepository;
    }

    /**
     * 【職責】為每個持倉寫入今日快照；已存在同日同標的則略過。
     * 【技巧】遍歷 {@code findAll()} 持倉；null 數值以 {@code BigDecimal.ZERO} 落地，避免快照列出現 null。
     * 【概念】冪等靠「查了再寫」而非依賴 DB 唯一約束失敗重試；教學上較直覺，正式環境可再加唯一索引雙保險。
     *
     * @return 新寫入的快照筆數
     */
    @Transactional
    public int captureSnapshot() {
        LocalDate today = LocalDate.now();
        List<PositionEntity> positions = positionRepository.findAll();
        int written = 0;

        for (PositionEntity position : positions) {
            boolean alreadyCaptured = !pnlSnapshotRepository
                    .findBySnapshotDateAndSymbol(today, position.getSymbol()).isEmpty();
            if (alreadyCaptured) {
                continue;
            }

            PnlSnapshotEntity snapshot = new PnlSnapshotEntity();
            snapshot.setSnapshotDate(today);
            snapshot.setSymbol(position.getSymbol());
            snapshot.setQuantity(nullSafe(position.getQuantity()));
            snapshot.setAvgPrice(nullSafe(position.getAvgPrice()));
            snapshot.setMarkPrice(nullSafe(position.getMarkPrice()));
            snapshot.setUnrealizedPnl(nullSafe(position.getUnrealizedPnl()));
            pnlSnapshotRepository.save(snapshot);
            written++;
        }

        if (written > 0) {
            log.info("JOB-B captured {} PnL snapshots for {}", written, today);
        }
        return written;
    }

    /**
     * 【職責】依日期查詢快照；{@code date} 為 null 時預設今日。
     * 【技巧】{@code @Transactional(readOnly = true)} 標明唯讀，利於連線／快取優化。
     * 【概念】查詢 API 與寫入 Job 共用同一 Service，避免 Controller 直接碰 Repository。
     *
     * @param date 快照日期，可為 null
     * @return 該日快照列表
     */
    @Transactional(readOnly = true)
    public List<PnlSnapshotEntity> findByDate(LocalDate date) {
        return pnlSnapshotRepository.findBySnapshotDate(date != null ? date : LocalDate.now());
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
