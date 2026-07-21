package com.trading.job.infrastructure.repository;

import com.trading.job.domain.FailedCommandStatus;
import com.trading.job.infrastructure.entity.FailedCommandEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 【職責】失敗指令 DLQ 持久化存取：查詢／寫入／批次刪除，不含商業規則。
 * 【技巧】衍生查詢（到期重試）+ {@code @Modifying} 終態清理。
 * 【概念】「誰該重試」用 status + nextRetryAt 表達；清理條件由 Service 傳入終態集合。
 */
public interface FailedCommandRepository extends JpaRepository<FailedCommandEntity, Long> {

    /**
     * 【職責】JOB-C：撈取到期可重試的指令（狀態 + nextRetryAt ≤ now）。
     * 【技巧】{@code LessThanEqual} + {@link Pageable}。
     * 【概念】未到期列不會被撈到，等同排程退避。
     */
    List<FailedCommandEntity> findByStatusAndNextRetryAtLessThanEqual(
            FailedCommandStatus status, OffsetDateTime now, Pageable pageable);

    /**
     * 【職責】依狀態分頁查詢（API 列表）。
     * 【技巧】衍生查詢 + Pageable。
     * 【概念】列表必帶上限，避免一次載入整張 DLQ。
     */
    List<FailedCommandEntity> findByStatus(FailedCommandStatus status, Pageable pageable);

    /**
     * 【職責】JOB-D：刪除指定終態且更新時間早於 cutoff 的指令。
     * 【技巧】JPQL {@code IN :statuses AND updatedAt < :cutoff}。
     * 【概念】PENDING 不在 statuses 內就不會被刪，保護重試佇列。
     *
     * @return 刪除筆數
     */
    @Modifying
    @Query("DELETE FROM FailedCommandEntity f WHERE f.status IN :statuses AND f.updatedAt < :cutoff")
    int deleteByStatusInAndUpdatedAtBefore(
            @Param("statuses") Collection<FailedCommandStatus> statuses,
            @Param("cutoff") OffsetDateTime cutoff);
}
