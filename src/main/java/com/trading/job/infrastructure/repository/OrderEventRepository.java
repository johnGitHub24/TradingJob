package com.trading.job.infrastructure.repository;

import com.trading.job.infrastructure.entity.OrderEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 【職責】訂單事件持久化存取：查詢／寫入／批次刪除，不含商業規則。
 * 【技巧】衍生查詢 + {@code @Modifying} JPQL DELETE。
 * 【概念】append-only 寫入與依時間清理分開；刪除筆數回傳給 JOB-D 組結果。
 */
public interface OrderEventRepository extends JpaRepository<OrderEventEntity, Long> {

    /**
     * 【職責】依訂單 ID 依時間升序查詢事件時間線。
     * 【技巧】{@code OrderByCreatedAtAsc}。
     * 【概念】時間線查詢給整合測試／稽核，不是熱路徑下單。
     */
    List<OrderEventEntity> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    /**
     * 【職責】JOB-D：刪除建立時間早於 cutoff 的事件。
     * 【技巧】{@code @Modifying @Query} 批次 DELETE，回傳影響列數。
     * 【概念】用 JPQL 實體名刪除，避免手寫表名字串散落。
     *
     * @return 刪除筆數
     */
    @Modifying
    @Query("DELETE FROM OrderEventEntity e WHERE e.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") OffsetDateTime cutoff);
}
