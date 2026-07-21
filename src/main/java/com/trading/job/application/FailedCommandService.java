package com.trading.job.application;

import com.trading.job.config.JobProperties;
import com.trading.job.domain.FailedCommandStatus;
import com.trading.job.dto.CreateOrderRequest;
import com.trading.job.infrastructure.entity.FailedCommandEntity;
import com.trading.job.infrastructure.repository.FailedCommandRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 【職責】JOB-C 商業邏輯：撈取到期 PENDING 指令，經 {@link OrderReplayPort} 重放；處理成功／風控終態／退避／DEAD。
 * 【技巧】Port／Adapter：業務只依賴 {@link OrderReplayPort}；生產走 REST、測試走進程內實作。
 * 【概念】風控拒絕是「業務終態」不是「再試一次」；暫態失敗才用 attempts × backoff 延後 nextRetryAt。
 * 【邊界】不負責 cron；不負責 Engine HTTP 細節（由 client 實作）。
 */
@Service
@Slf4j
public class FailedCommandService {

    private final FailedCommandRepository failedCommandRepository;
    private final OrderReplayPort orderReplayPort;
    private final JobProperties jobProperties;

    public FailedCommandService(FailedCommandRepository failedCommandRepository,
                                OrderReplayPort orderReplayPort,
                                JobProperties jobProperties) {
        this.failedCommandRepository = failedCommandRepository;
        this.orderReplayPort = orderReplayPort;
        this.jobProperties = jobProperties;
    }

    /**
     * 【職責】批次重試到期的 PENDING 失敗指令。
     * 【技巧】依狀態與 {@code nextRetryAt ≤ now} 分頁撈取；區分 {@link RiskRejectedException} 與一般 Exception。
     * 【概念】風控拒絕標 SUCCEEDED（結案、不再重試）但回傳的 succeeded 計數不含它——「真正重放成功」與「終態結案」分開。
     *         超過 maxAttempts 標 DEAD，避免無限重試打爆下游。
     *
     * @return 成功重放（不含風控終態結案）筆數
     */
    @Transactional
    public int retryFailedCommands() {
        JobProperties.Retry config = jobProperties.getRetry();
        OffsetDateTime now = OffsetDateTime.now();

        List<FailedCommandEntity> due = failedCommandRepository.findByStatusAndNextRetryAtLessThanEqual(
                FailedCommandStatus.PENDING, now, PageRequest.of(0, config.getBatchSize()));

        int succeeded = 0;
        for (FailedCommandEntity entity : due) {
            entity.setAttempts(entity.getAttempts() + 1);
            try {
                orderReplayPort.placeOrder(toRequest(entity), entity.getClientOrderId());
                entity.setStatus(FailedCommandStatus.SUCCEEDED);
                entity.setFailureReason(null);
                succeeded++;
            } catch (RiskRejectedException ex) {
                entity.setStatus(FailedCommandStatus.SUCCEEDED);
                entity.setFailureReason("Risk rejected: " + ex.getErrorCode());
            } catch (Exception ex) {
                entity.setFailureReason(truncate(ex.getMessage()));
                if (entity.getAttempts() >= config.getMaxAttempts()) {
                    entity.setStatus(FailedCommandStatus.DEAD);
                    log.error("JOB-C command commandId={} moved to DEAD after {} attempts",
                            entity.getCommandId(), entity.getAttempts());
                } else {
                    entity.setNextRetryAt(now.plusSeconds(config.getBackoffSeconds() * entity.getAttempts()));
                }
            }
            failedCommandRepository.save(entity);
        }

        if (!due.isEmpty()) {
            log.info("JOB-C processed {} failed commands, {} succeeded", due.size(), succeeded);
        }
        return succeeded;
    }

    /**
     * 【職責】查詢失敗指令（供 API 列表）；筆數上限取 batchSize 與 100 的較大值。
     * 【技巧】{@code readOnly} 交易；{@code status == null} 時用 {@code findAll(PageRequest)}。
     * 【概念】列表一定要有上限，避免把整張 DLQ 一次吐給前端。
     *
     * @param status 狀態過濾；{@code null} 表示不限狀態
     * @return 失敗指令實體列表
     */
    @Transactional(readOnly = true)
    public List<FailedCommandEntity> findByStatus(FailedCommandStatus status) {
        int limit = Math.max(jobProperties.getRetry().getBatchSize(), 100);
        if (status == null) {
            return failedCommandRepository.findAll(PageRequest.of(0, limit)).getContent();
        }
        return failedCommandRepository.findByStatus(status, PageRequest.of(0, limit));
    }

    private CreateOrderRequest toRequest(FailedCommandEntity entity) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setClientOrderId(entity.getClientOrderId());
        request.setSymbol(entity.getSymbol());
        request.setSide(entity.getSide());
        request.setQuantity(entity.getQuantity());
        request.setPrice(entity.getPrice());
        return request;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 512 ? value.substring(0, 512) : value;
    }
}
