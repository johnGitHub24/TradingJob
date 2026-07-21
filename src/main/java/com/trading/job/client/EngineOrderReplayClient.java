package com.trading.job.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.job.application.OrderReplayPort;
import com.trading.job.application.RiskRejectedException;
import com.trading.job.config.EngineClientProperties;
import com.trading.job.dto.CreateOrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

/**
 * 【職責】JOB-C 生產環境 Adapter：透過 Engine REST 重放下單。
 * 【技巧】Spring {@link RestClient}；{@code @Profile("!test")} 與測試實作互斥。
 * 【概念】HTTP 細節（狀態碼、Idempotency-Key）留在邊界適配器，Service 只看到 Port 契約。
 * 【邊界】不實作重試／退避（由 {@link com.trading.job.application.FailedCommandService} 負責）。
 */
@Component
@Profile("!test")
@Slf4j
public class EngineOrderReplayClient implements OrderReplayPort {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EngineOrderReplayClient(EngineClientProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    /**
     * 【職責】呼叫 Engine {@code POST /api/v1/orders}；HTTP 422 轉為 {@link RiskRejectedException}。
     * 【技巧】{@code onStatus} 依狀態碼分支；可選 {@code Idempotency-Key} header。
     * 【概念】422＝風控終態（不要再試）；其他 4xx／5xx＝暫態或基礎設施錯誤，讓上層退避。
     */
    @Override
    public void placeOrder(CreateOrderRequest request, String clientOrderId) {
        var spec = restClient.post()
                .uri("/api/v1/orders")
                .header("Content-Type", "application/json");

        if (clientOrderId != null && !clientOrderId.isBlank()) {
            spec = spec.header("Idempotency-Key", clientOrderId);
        }

        spec.body(request)
                .retrieve()
                .onStatus(status -> status.value() == 422, (req, res) -> {
                    String raw = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    JsonNode body = objectMapper.readTree(raw);
                    String code = body.has("errorCode") ? body.get("errorCode").asText() : "RISK_REJECTED";
                    String rule = body.has("ruleCode") ? body.get("ruleCode").asText() : null;
                    String detail = body.has("message") ? body.get("message").asText() : "Risk rejected";
                    throw new RiskRejectedException(code, rule, detail);
                })
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("Engine replay failed: HTTP " + res.getStatusCode().value());
                })
                .toBodilessEntity();
    }
}
