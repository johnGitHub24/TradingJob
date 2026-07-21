package com.trading.job.integration;

import com.trading.job.application.FailedCommandService;
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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【職責】JOB-C 整合測試：真實 DB + 進程內重放，驗證到期重試與未到期略過。
 * 【技巧】test profile 的 {@code InProcessOrderReplayClient}；依 {@code nextRetryAt} 準備資料。
 * 【概念】不需真實 Engine HTTP，仍能驗證「到期→SUCCEEDED＋建單／未到期→略過」。
 * 【技巧驗證】未來 nextRetryAt 不建單、狀態仍 PENDING。
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class FailedCommandRetryJobIntegrationTest {

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

    private FailedCommandEntity saveFailed(String clientOrderId, OffsetDateTime nextRetryAt) {
        FailedCommandEntity e = new FailedCommandEntity();
        e.setCommandId("cmd-" + System.nanoTime());
        e.setClientOrderId(clientOrderId);
        e.setSymbol("BTCUSDT");
        e.setSide(OrderSide.BUY);
        e.setQuantity(new BigDecimal("0.5"));
        e.setPrice(new BigDecimal("65000"));
        e.setFailureReason("simulated infra failure");
        e.setAttempts(0);
        e.setStatus(FailedCommandStatus.PENDING);
        e.setNextRetryAt(nextRetryAt);
        return failedCommandRepository.save(e);
    }

    /**
     * CASE-JOB-RETRY-001：到期指令重放成功並建立訂單。
     * Given: PENDING 且 nextRetryAt 已過；When: retryFailedCommands；Then: SUCCEEDED + orders 有對應 clientOrderId。
     */
    @Test
    void JOB_RETRY_001_SUCCESS_dueCommandIsReplayedAndOrderCreated() {
        FailedCommandEntity failed = saveFailed("retry-success-1", OffsetDateTime.now().minusSeconds(5));

        int succeeded = failedCommandService.retryFailedCommands();

        assertThat(succeeded).isEqualTo(1);
        assertThat(failedCommandRepository.findById(failed.getId()).orElseThrow().getStatus())
                .isEqualTo(FailedCommandStatus.SUCCEEDED);
        assertThat(orderRepository.findByClientOrderId("retry-success-1")).isPresent();
    }

    /**
     * CASE-JOB-RETRY-004：未到期指令略過。
     * Given: PENDING 且 nextRetryAt 在未來；When: retry；Then: 仍 PENDING、不建單。
     */
    @Test
    void JOB_RETRY_004_SKIP_FUTURE_notDueCommandIsSkipped() {
        FailedCommandEntity future = saveFailed("retry-future-1", OffsetDateTime.now().plusMinutes(10));

        int succeeded = failedCommandService.retryFailedCommands();

        assertThat(succeeded).isZero();
        assertThat(failedCommandRepository.findById(future.getId()).orElseThrow().getStatus())
                .isEqualTo(FailedCommandStatus.PENDING);
        assertThat(orderRepository.findByClientOrderId("retry-future-1")).isEmpty();
    }
}
