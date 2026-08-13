package com.trading.job.application;

import com.trading.job.domain.OrderEventType;
import com.trading.job.infrastructure.entity.OrderEventEntity;
import com.trading.job.infrastructure.repository.OrderEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * 【職責】{@link OrderEventService} 單元測試：公開 {@code log} 組裝並寫入事件。
 * 【技巧】Mock Repository；Captor 驗證欄位。
 * 【概念】審計只追加：Service 負責欄位組裝，不在這裡做查詢或清理。
 * 【技巧驗證】orderId／event／ruleCode／rejectReason／payload 原樣落地。
 */
@ExtendWith(MockitoExtension.class)
class OrderEventServiceTest {

    @Mock
    private OrderEventRepository orderEventRepository;

    @InjectMocks
    private OrderEventService service;

    /**
     * CASE-JOB-EVENT-001：寫入一筆訂單事件並帶齊可選欄位。
     * Given: orderId=9、CANCELLED、規則與 payload；When: log；Then: save 的 entity 欄位對齊。
     */
    @Test
    void JOB_EVENT_001_log_persistsEntityWithFields() {
        service.log(9L, OrderEventType.CANCELLED, "R001", "timed out", "{\"src\":\"JOB-A\"}");

        ArgumentCaptor<OrderEventEntity> captor = ArgumentCaptor.forClass(OrderEventEntity.class);
        verify(orderEventRepository).save(captor.capture());
        OrderEventEntity saved = captor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(9L);
        assertThat(saved.getEvent()).isEqualTo(OrderEventType.CANCELLED);
        assertThat(saved.getRiskRuleCode()).isEqualTo("R001");
        assertThat(saved.getRejectReason()).isEqualTo("timed out");
        assertThat(saved.getPayloadJson()).isEqualTo("{\"src\":\"JOB-A\"}");
    }
}
