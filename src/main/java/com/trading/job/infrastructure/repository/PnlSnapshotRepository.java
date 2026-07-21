package com.trading.job.infrastructure.repository;

import com.trading.job.infrastructure.entity.PnlSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * 【職責】PnL 快照持久化存取：查詢／寫入，不含商業規則。
 * 【技巧】Spring Data 依日期／標的衍生查詢。
 * 【概念】冪等檢查與 API 列表都走這裡；是否寫入由 Service 決定。
 */
public interface PnlSnapshotRepository extends JpaRepository<PnlSnapshotEntity, Long> {

    /**
     * 【職責】依快照日期查詢（API／驗證）。
     * 【技巧】{@code findBySnapshotDate}。
     * 【概念】一日多標的 → 多列；用日期當查詢鍵而非 id。
     */
    List<PnlSnapshotEntity> findBySnapshotDate(LocalDate snapshotDate);

    /**
     * 【職責】JOB-B 冪等檢查：同日同標的是否已存在。
     * 【技巧】複合條件衍生查詢。
     * 【概念】回傳 List 非 Optional：空＝尚未快照；非空＝跳過寫入。
     */
    List<PnlSnapshotEntity> findBySnapshotDateAndSymbol(LocalDate snapshotDate, String symbol);
}
