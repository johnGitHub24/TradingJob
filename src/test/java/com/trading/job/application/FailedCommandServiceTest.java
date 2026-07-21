package com.trading.job.application;

import com.trading.job.config.JobProperties;
import com.trading.job.domain.FailedCommandStatus;
import com.trading.job.domain.OrderSide;
import com.trading.job.infrastructure.entity.FailedCommandEntity;
import com.trading.job.infrastructure.repository.FailedCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 【職責】{@link FailedCommandService} 單元測試：覆蓋 JOB-C 成功重放、退避、DEAD、風控終態。
 * 【技巧】Mock {@link OrderReplayPort}；{@code doThrow} 模擬暫態／風控例外。
 * 【概念】用例外類型區分「再試」與「結案」；計數 succeeded 不含風控結案。
 * 【技巧驗證】maxAttempts→DEAD；RiskRejectedException→SUCCEEDED。
 */
@ExtendWith(MockitoExtension.class)
class FailedCommandServiceTest {

    @Mock
    private FailedCommandRepository failedCommandRepository;

    @Mock
    private OrderReplayPort orderReplayPort;

    private final JobProperties jobProperties = new JobProperties();

    private FailedCommandService service;

    @BeforeEach
    void setUp() {
        service = new FailedCommandService(failedCommandRepository, orderReplayPort, jobProperties);
    }

    private FailedCommandEntity pending(int attempts) {
        FailedCommandEntity e = new FailedCommandEntity();
        e.setCommandId("cmd-1");
        e.setClientOrderId("coid-1");
        e.setSymbol("BTCUSDT");
        e.setSide(OrderSide.BUY);
        e.setQuantity(new BigDecimal("0.5"));
        e.setPrice(new BigDecimal("65000"));
        e.setAttempts(attempts);
        e.setStatus(FailedCommandStatus.PENDING);
        e.setNextRetryAt(OffsetDateTime.now().minusSeconds(1));
        return e;
    }

    /**
     * CASE-JOB-RETRY-001：重放成功標記 SUCCEEDED。
     * Given: 一筆到期 PENDING；When: retryFailedCommands；Then: SUCCEEDED、attempts+1、呼叫 placeOrder。
     */
    @Test
    void JOB_RETRY_001_SUCCESS_replaysAndMarksSucceeded() {
        FailedCommandEntity entity = pending(0);
        when(failedCommandRepository.findByStatusAndNextRetryAtLessThanEqual(
                eq(FailedCommandStatus.PENDING), any(), any())).thenReturn(List.of(entity));

        int succeeded = service.retryFailedCommands();

        assertThat(succeeded).isEqualTo(1);
        assertThat(entity.getStatus()).isEqualTo(FailedCommandStatus.SUCCEEDED);
        assertThat(entity.getAttempts()).isEqualTo(1);
        verify(orderReplayPort).placeOrder(any(), eq("coid-1"));
        verify(failedCommandRepository).save(entity);
    }

    /**
     * CASE-JOB-RETRY-002：暫態失敗維持 PENDING 並排程下次重試。
     * Given: placeOrder 拋 RuntimeException；When: retry；Then: PENDING、nextRetryAt 延後、failureReason 記錄。
     */
    @Test
    void JOB_RETRY_002_BACKOFF_transientFailure_staysPendingAndReschedules() {
        FailedCommandEntity entity = pending(0);
        when(failedCommandRepository.findByStatusAndNextRetryAtLessThanEqual(
                eq(FailedCommandStatus.PENDING), any(), any())).thenReturn(List.of(entity));
        doThrow(new RuntimeException("db down")).when(orderReplayPort).placeOrder(any(), any());

        int succeeded = service.retryFailedCommands();

        assertThat(succeeded).isZero();
        assertThat(entity.getStatus()).isEqualTo(FailedCommandStatus.PENDING);
        assertThat(entity.getAttempts()).isEqualTo(1);
        assertThat(entity.getNextRetryAt()).isAfter(OffsetDateTime.now());
        assertThat(entity.getFailureReason()).contains("db down");
    }

    /**
     * CASE-JOB-RETRY-003：達 maxAttempts 轉 DEAD。
     * Given: attempts 已為 2 且重放再失敗；When: retry；Then: status=DEAD、attempts=3。
     */
    @Test
    void JOB_RETRY_003_DEAD_exceedsMaxAttempts_movesToDead() {
        FailedCommandEntity entity = pending(2);
        when(failedCommandRepository.findByStatusAndNextRetryAtLessThanEqual(
                eq(FailedCommandStatus.PENDING), any(), any())).thenReturn(List.of(entity));
        doThrow(new RuntimeException("still failing")).when(orderReplayPort).placeOrder(any(), any());

        int succeeded = service.retryFailedCommands();

        assertThat(succeeded).isZero();
        assertThat(entity.getAttempts()).isEqualTo(3);
        assertThat(entity.getStatus()).isEqualTo(FailedCommandStatus.DEAD);
    }

    /**
     * CASE-JOB-RETRY-004：風控拒絕視為終態，標記 SUCCEEDED 不再重試。
     * Given: placeOrder 拋 RiskRejectedException；When: retry；Then: SUCCEEDED + failureReason 含 errorCode。
     */
    @Test
    void JOB_RETRY_004_RISK_REJECT_terminalNoRetry() {
        FailedCommandEntity entity = pending(0);
        when(failedCommandRepository.findByStatusAndNextRetryAtLessThanEqual(
                eq(FailedCommandStatus.PENDING), any(), any())).thenReturn(List.of(entity));
        doThrow(new RiskRejectedException("RISK_POSITION_LIMIT", "R001", "limit"))
                .when(orderReplayPort).placeOrder(any(), any());

        int succeeded = service.retryFailedCommands();

        assertThat(succeeded).isZero();
        assertThat(entity.getStatus()).isEqualTo(FailedCommandStatus.SUCCEEDED);
        assertThat(entity.getFailureReason()).contains("RISK_POSITION_LIMIT");
    }
}
