package com.trading.job.application;

import com.trading.job.domain.OrderEventType;
import com.trading.job.infrastructure.entity.OrderEventEntity;
import com.trading.job.infrastructure.repository.OrderEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 【職責】訂單審計事件寫入（append-only），供 JOB-A 等流程記錄狀態變更。
 * 【技巧】薄 Service 包一層 {@link OrderEventRepository#save}，統一欄位組裝。
 * 【概念】事件表只追加、不改歷史；查詢與清理分屬其他元件（清理見 {@link DataCleanupService}）。
 * 【邊界】不負責事件查詢 API、不負責依保留天數刪除。
 */
@Service
public class OrderEventService {

    private final OrderEventRepository orderEventRepository;

    public OrderEventService(OrderEventRepository orderEventRepository) {
        this.orderEventRepository = orderEventRepository;
    }

    /**
     * 【職責】寫入一筆訂單事件。
     * 【技巧】組裝 {@link OrderEventEntity} 後 save；可選欄位允許 null。
     * 【概念】把「記一筆審計」收斂成單一方法，呼叫端不必重複設 entity 欄位。
     *
     * @param orderId      訂單 ID
     * @param event        事件類型
     * @param ruleCode     風控規則代碼，可為 null
     * @param rejectReason 拒絕／取消原因，可為 null
     * @param payloadJson  附加 JSON，可為 null
     */
    @Transactional
    public void log(Long orderId, OrderEventType event, String ruleCode, String rejectReason, String payloadJson) {
        OrderEventEntity entity = new OrderEventEntity();
        entity.setOrderId(orderId);
        entity.setEvent(event);
        entity.setRiskRuleCode(ruleCode);
        entity.setRejectReason(rejectReason);
        entity.setPayloadJson(payloadJson);
        orderEventRepository.save(entity);
    }
}
