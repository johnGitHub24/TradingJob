package com.trading.job.application;

import com.trading.job.config.JobProperties;
import com.trading.job.domain.OrderEventType;
import com.trading.job.domain.OrderStatus;
import com.trading.job.infrastructure.entity.OrderEntity;
import com.trading.job.infrastructure.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 【職責】JOB-A 商業邏輯：取消逾時仍為 NEW／PARTIALLY_FILLED 的訂單，並寫入 CANCELLED 審計事件。
 * 【技巧】{@code @Transactional} + Repository 分頁查詢（{@link PageRequest}）+ {@link OrderEventService} 寫事件。
 * 【概念】「逾時」用 createdAt 與 timeoutSeconds 算 cutoff，而不是靠記憶體計時器；批次上限避免一次鎖太多列。
 * 【邊界】不負責 cron 觸發；不取消 FILLED／REJECTED／CANCELLED 等終態。
 */
@Service
@Slf4j
public class StaleOrderCancellationService {

    private static final List<OrderStatus> CANCELLABLE =
            List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED);

    private final OrderRepository orderRepository;
    private final OrderEventService orderEventService;
    private final JobProperties jobProperties;

    public StaleOrderCancellationService(OrderRepository orderRepository,
                                         OrderEventService orderEventService,
                                         JobProperties jobProperties) {
        this.orderRepository = orderRepository;
        this.orderEventService = orderEventService;
        this.jobProperties = jobProperties;
    }

    /**
     * 【職責】取消建立時間早於 timeout 門檻的可取消訂單，並記錄事件。
     * 【技巧】{@code OffsetDateTime.now().minusSeconds} 算 cutoff；{@code findByStatusInAndCreatedAtBefore} 批次撈取。
     * 【概念】狀態過濾放在查詢條件，比撈全表再 if 過濾更省；事件與狀態變更同交易，避免「已取消但無審計」。
     *
     * @return 本次取消筆數
     */
    @Transactional
    public int cancelStaleOrders() {
        JobProperties.StaleOrder config = jobProperties.getStaleOrder();
        OffsetDateTime cutoff = OffsetDateTime.now().minusSeconds(config.getTimeoutSeconds());

        List<OrderEntity> staleOrders = orderRepository.findByStatusInAndCreatedAtBefore(
                CANCELLABLE, cutoff, PageRequest.of(0, config.getBatchSize()));

        for (OrderEntity order : staleOrders) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setRejectReason("Auto-cancelled: order timed out after " + config.getTimeoutSeconds() + "s");
            orderRepository.save(order);
            orderEventService.log(order.getId(), OrderEventType.CANCELLED, null,
                    "Stale order timeout", null);
        }

        if (!staleOrders.isEmpty()) {
            log.info("JOB-A cancelled {} stale orders (cutoff={})", staleOrders.size(), cutoff);
        }
        return staleOrders.size();
    }
}
