package com.trading.job.client;

import com.trading.job.application.OrderReplayPort;
import com.trading.job.domain.OrderStatus;
import com.trading.job.dto.CreateOrderRequest;
import com.trading.job.infrastructure.entity.OrderEntity;
import com.trading.job.infrastructure.repository.OrderRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 【職責】測試環境 Adapter：在本地 DB 建立訂單，模擬 Engine 重放成功。
 * 【技巧】{@code @Profile("test")} 實作同一 {@link OrderReplayPort}，整合測試不必起真實 Engine。
 * 【概念】用「寫入 NEW 訂單」代替 HTTP 成功回應，讓 JOB-C 狀態機仍可端到端驗證。
 * 【邊界】不模擬風控 422；需要風控情境時在單元測試 Mock Port。
 */
@Component
@Profile("test")
public class InProcessOrderReplayClient implements OrderReplayPort {

    private final OrderRepository orderRepository;

    public InProcessOrderReplayClient(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * 【職責】在本地 {@code orders} 表寫入 NEW 訂單，模擬 Engine 重放成功。
     * 【技巧】{@code @Transactional} + {@link OrderRepository#save}。
     * 【概念】與生產路徑共用 Port 方法簽名，Service 測試／整合測試無需改業務碼。
     */
    @Override
    @Transactional
    public void placeOrder(CreateOrderRequest request, String clientOrderId) {
        OrderEntity order = new OrderEntity();
        order.setClientOrderId(clientOrderId != null ? clientOrderId : request.getClientOrderId());
        order.setSymbol(request.getSymbol());
        order.setSide(request.getSide());
        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());
        order.setStatus(OrderStatus.NEW);
        orderRepository.save(order);
    }
}
