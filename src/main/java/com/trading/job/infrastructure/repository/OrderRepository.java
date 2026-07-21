package com.trading.job.infrastructure.repository;

import com.trading.job.domain.OrderStatus;
import com.trading.job.infrastructure.entity.OrderEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 【職責】訂單持久化存取：查詢／寫入，不含商業規則。
 * 【技巧】Spring Data JPA 方法命名查詢 + {@link Pageable} 批次。
 * 【概念】Repository 只回答「資料怎麼存取」；逾時門檻與可取消狀態由 Service 決定後傳入。
 */
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * 【職責】依客戶端訂單號查詢（冪等／重放驗證）。
     * 【技巧】衍生查詢 {@code findByClientOrderId}。
     * 【概念】clientOrderId 是跨系統對帳鍵，不是資料庫 surrogate id。
     */
    Optional<OrderEntity> findByClientOrderId(String clientOrderId);

    /**
     * 【職責】JOB-A：找出指定狀態且建立時間早於 cutoff 的訂單（分頁批次）。
     * 【技巧】{@code StatusIn + CreatedAtBefore + Pageable}。
     * 【概念】條件下推到 SQL，避免全表載入再過濾；Pageable 限制單次處理量。
     */
    List<OrderEntity> findByStatusInAndCreatedAtBefore(
            Collection<OrderStatus> statuses, OffsetDateTime cutoff, Pageable pageable);
}
