package com.trading.job.infrastructure.repository;

import com.trading.job.infrastructure.entity.PositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 【職責】持倉持久化存取：查詢／寫入，不含商業規則。
 * 【技巧】繼承 {@link JpaRepository}；JOB-B 主要用 {@code findAll()}。
 * 【概念】本服務對持倉以讀為主；寫入由 Engine 負責。
 */
public interface PositionRepository extends JpaRepository<PositionEntity, Long> {
}
