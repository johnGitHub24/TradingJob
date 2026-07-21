package com.trading.job.application;

import com.trading.job.config.JobProperties;
import com.trading.job.domain.OrderEventType;
import com.trading.job.domain.OrderSide;
import com.trading.job.domain.OrderStatus;
import com.trading.job.infrastructure.entity.OrderEntity;
import com.trading.job.infrastructure.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 【職責】{@link StaleOrderCancellationService} 單元測試：覆蓋 JOB-A 取消、空結果、批次與狀態過濾。
 * 【技巧】Mockito {@code @ExtendWith}；ArgumentCaptor 驗證查詢狀態集合。
 * 【概念】不碰真實 DB，專注「取消規則與副作用（save／事件）」是否正確。
 * 【技巧驗證】僅掃描 NEW／PARTIALLY_FILLED；空結果不寫庫。
 */
@ExtendWith(MockitoExtension.class)
class StaleOrderCancellationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventService orderEventService;

    private final JobProperties jobProperties = new JobProperties();

    private StaleOrderCancellationService service;

    @BeforeEach
    void setUp() {
        service = new StaleOrderCancellationService(orderRepository, orderEventService, jobProperties);
    }

    private OrderEntity order(long id, OrderStatus status) {
        OrderEntity o = new OrderEntity();
        o.setId(id);
        o.setSymbol("BTCUSDT");
        o.setSide(OrderSide.BUY);
        o.setQuantity(new BigDecimal("1"));
        o.setPrice(new BigDecimal("65000"));
        o.setStatus(status);
        return o;
    }

    /**
     * CASE-JOB-STALE-001：逾時訂單標記 CANCELLED 並寫事件。
     * Given: 一筆 NEW 逾時單；When: cancelStaleOrders；Then: CANCELLED + rejectReason + CANCELLED 事件。
     */
    @Test
    void JOB_STALE_001_CANCEL_marksStaleOrdersCancelledAndLogsEvent() {
        OrderEntity stale = order(1L, OrderStatus.NEW);
        when(orderRepository.findByStatusInAndCreatedAtBefore(any(Collection.class), any(), any()))
                .thenReturn(List.of(stale));

        int cancelled = service.cancelStaleOrders();

        assertThat(cancelled).isEqualTo(1);
        assertThat(stale.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stale.getRejectReason()).contains("timed out");
        verify(orderRepository).save(stale);
        verify(orderEventService).log(eq(1L), eq(OrderEventType.CANCELLED), isNull(), any(), isNull());
    }

    /**
     * CASE-JOB-STALE-002：無逾時單回傳 0 且不寫庫。
     * Given: 查詢空列表；When: cancelStaleOrders；Then: 0、不 save、不 log。
     */
    @Test
    void JOB_STALE_002_EMPTY_noStaleOrders_returnsZero() {
        when(orderRepository.findByStatusInAndCreatedAtBefore(any(Collection.class), any(), any()))
                .thenReturn(List.of());

        int cancelled = service.cancelStaleOrders();

        assertThat(cancelled).isZero();
        verify(orderRepository, never()).save(any());
        verify(orderEventService, never()).log(anyLong(), any(), any(), any(), any());
    }

    /**
     * CASE-JOB-STALE-003：批次取消多筆。
     * Given: NEW + PARTIALLY_FILLED 各一；When: cancel；Then: 回傳 2、save 兩次。
     */
    @Test
    void JOB_STALE_003_MULTI_cancelsAllReturnedOrders() {
        when(orderRepository.findByStatusInAndCreatedAtBefore(any(Collection.class), any(), any()))
                .thenReturn(List.of(order(1L, OrderStatus.NEW), order(2L, OrderStatus.PARTIALLY_FILLED)));

        int cancelled = service.cancelStaleOrders();

        assertThat(cancelled).isEqualTo(2);
        verify(orderRepository, times(2)).save(any(OrderEntity.class));
    }

    /**
     * CASE-JOB-STALE-004：查詢僅掃描可取消狀態。
     * Given: 任意呼叫；When: cancelStaleOrders；Then: 狀態集合為 NEW、PARTIALLY_FILLED。
     */
    @Test
    void JOB_STALE_004_onlyScansCancellableStatuses() {
        ArgumentCaptor<Collection<OrderStatus>> captor = ArgumentCaptor.forClass(Collection.class);
        when(orderRepository.findByStatusInAndCreatedAtBefore(captor.capture(), any(), any()))
                .thenReturn(List.of());

        service.cancelStaleOrders();

        assertThat(captor.getValue())
                .containsExactlyInAnyOrder(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED);
    }
}
