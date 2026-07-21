package com.trading.job.integration;

import com.trading.job.application.StaleOrderCancellationService;
import com.trading.job.domain.OrderSide;
import com.trading.job.domain.OrderStatus;
import com.trading.job.infrastructure.entity.OrderEntity;
import com.trading.job.infrastructure.repository.OrderEventRepository;
import com.trading.job.infrastructure.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【職責】JOB-A 整合測試：真實 DB 驗證逾時取消、近期略過與終態不取消。
 * 【技巧】{@code @SpringBootTest} + JdbcTemplate 回寫舊 {@code created_at} 模擬逾時。
 * 【概念】單元測試 Mock 查詢結果；整合測試證明「cutoff 與狀態過濾」在 SQL／JPA 真的生效。
 * 【技巧驗證】FILLED 過期仍不取消；近期 NEW 不取消。
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class StaleOrderTimeoutJobIntegrationTest {

    @Autowired
    private StaleOrderCancellationService staleOrderCancellationService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderEventRepository orderEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        orderEventRepository.deleteAll();
        orderRepository.deleteAll();
    }

    private OrderEntity save(OrderStatus status) {
        OrderEntity o = new OrderEntity();
        o.setClientOrderId("coid-" + System.nanoTime());
        o.setSymbol("BTCUSDT");
        o.setSide(OrderSide.BUY);
        o.setQuantity(new BigDecimal("1"));
        o.setPrice(new BigDecimal("65000"));
        o.setStatus(status);
        return orderRepository.save(o);
    }

    private void ageOrder(Long id) {
        jdbcTemplate.update("UPDATE orders SET created_at = ? WHERE id = ?",
                OffsetDateTime.now().minusDays(2), id);
    }

    /**
     * CASE-JOB-STALE-001：逾時 NEW 訂單取消並寫 CANCELLED 事件。
     * Given: NEW 且 created_at 過舊；When: cancelStaleOrders；Then: CANCELLED + 事件存在。
     */
    @Test
    void JOB_STALE_001_CANCEL_agedNewOrderIsCancelledWithEvent() {
        OrderEntity stale = save(OrderStatus.NEW);
        ageOrder(stale.getId());

        int cancelled = staleOrderCancellationService.cancelStaleOrders();

        assertThat(cancelled).isEqualTo(1);
        assertThat(orderRepository.findById(stale.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
        assertThat(orderEventRepository.findByOrderIdOrderByCreatedAtAsc(stale.getId()))
                .anyMatch(e -> e.getEvent().name().equals("CANCELLED"));
    }

    /**
     * CASE-JOB-STALE-002：近期訂單不取消。
     * Given: 剛建立的 NEW；When: cancel；Then: 仍 NEW、cancelled=0。
     */
    @Test
    void JOB_STALE_002_SKIP_RECENT_recentOrderNotCancelled() {
        OrderEntity recent = save(OrderStatus.NEW);

        int cancelled = staleOrderCancellationService.cancelStaleOrders();

        assertThat(cancelled).isZero();
        assertThat(orderRepository.findById(recent.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.NEW);
    }

    /**
     * CASE-JOB-STALE-003：終態 FILLED 即使過期也不取消。
     * Given: FILLED 且 created_at 過舊；When: cancel；Then: 仍 FILLED。
     */
    @Test
    void JOB_STALE_003_SKIP_TERMINAL_filledOrderNeverCancelled() {
        OrderEntity filled = save(OrderStatus.FILLED);
        ageOrder(filled.getId());

        int cancelled = staleOrderCancellationService.cancelStaleOrders();

        assertThat(cancelled).isZero();
        assertThat(orderRepository.findById(filled.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.FILLED);
    }
}
