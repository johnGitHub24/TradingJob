package com.trading.job.integration;

import com.trading.job.application.FailedCommandService;
import com.trading.job.application.OrderReplayPort;
import com.trading.job.domain.FailedCommandStatus;
import com.trading.job.domain.OrderSide;
import com.trading.job.infrastructure.entity.FailedCommandEntity;
import com.trading.job.infrastructure.repository.FailedCommandRepository;
import com.trading.job.infrastructure.repository.OrderEventRepository;
import com.trading.job.infrastructure.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * 【職責】JOB-C 失敗路徑整合：Mock {@link OrderReplayPort} 驗證退避與 DEAD。
 * 【技巧】獨立類別 {@code @MockBean} Port，避免破壞成功重放整合（進程內 Adapter）。
 * 【概念】暫態例外要留下 PENDING＋延後 nextRetryAt；打滿 maxAttempts 才結案 DEAD。
 * 【技巧驗證】attempts=0 失敗仍 PENDING；attempts 已達上限前一次再失敗 → DEAD。
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class FailedCommandRetryFailureIntegrationTest {

    @MockBean
    private OrderReplayPort orderReplayPort;

    @Autowired
    private FailedCommandService failedCommandService;

    @Autowired
    private FailedCommandRepository failedCommandRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderEventRepository orderEventRepository;

    @BeforeEach
    void clean() {
        failedCommandRepository.deleteAll();
        orderEventRepository.deleteAll();
        orderRepository.deleteAll();
    }

    private FailedCommandEntity savePending(String clientOrderId, int attempts) {
        FailedCommandEntity e = new FailedCommandEntity();
        e.setCommandId("cmd-" + System.nanoTime());
        e.setClientOrderId(clientOrderId);
        e.setSymbol("BTCUSDT");
        e.setSide(OrderSide.BUY);
        e.setQuantity(new BigDecimal("0.5"));
        e.setPrice(new BigDecimal("65000"));
        e.setFailureReason("simulated infra failure");
        e.setAttempts(attempts);
        e.setStatus(FailedCommandStatus.PENDING);
        e.setNextRetryAt(OffsetDateTime.now().minusSeconds(5));
        return failedCommandRepository.save(e);
    }

    /**
     * CASE-JOB-RETRY-002：暫態失敗維持 PENDING 並排程下次重試。
     * Given: 到期 PENDING 且 Port 拋 RuntimeException；When: retry；Then: 仍 PENDING、attempts+1、failureReason 記錄、不建單。
     */
    @Test
    void JOB_RETRY_002_BACKOFF_transientFailure_staysPendingAndReschedules() {
        FailedCommandEntity entity = savePending("retry-backoff-1", 0);
        doThrow(new RuntimeException("db down")).when(orderReplayPort).placeOrder(any(), any());

        int succeeded = failedCommandService.retryFailedCommands();

        FailedCommandEntity updated = failedCommandRepository.findById(entity.getId()).orElseThrow();
        assertThat(succeeded).isZero();
        assertThat(updated.getStatus()).isEqualTo(FailedCommandStatus.PENDING);
        assertThat(updated.getAttempts()).isEqualTo(1);
        assertThat(updated.getNextRetryAt()).isNotNull();
        assertThat(updated.getFailureReason()).contains("db down");
        assertThat(orderRepository.findByClientOrderId("retry-backoff-1")).isEmpty();
    }

    /**
     * CASE-JOB-RETRY-003：達 maxAttempts 轉 DEAD。
     * Given: attempts 已為 2 且重放再失敗；When: retry；Then: status=DEAD、attempts=3。
     */
    @Test
    void JOB_RETRY_003_DEAD_exceedsMaxAttempts_movesToDead() {
        FailedCommandEntity entity = savePending("retry-dead-1", 2);
        doThrow(new RuntimeException("still failing")).when(orderReplayPort).placeOrder(any(), any());

        int succeeded = failedCommandService.retryFailedCommands();

        FailedCommandEntity updated = failedCommandRepository.findById(entity.getId()).orElseThrow();
        assertThat(succeeded).isZero();
        assertThat(updated.getAttempts()).isEqualTo(3);
        assertThat(updated.getStatus()).isEqualTo(FailedCommandStatus.DEAD);
        assertThat(orderRepository.findByClientOrderId("retry-dead-1")).isEmpty();
    }
}
