package com.trading.job.application;

import com.trading.job.config.JobProperties;
import com.trading.job.domain.FailedCommandStatus;
import com.trading.job.infrastructure.repository.FailedCommandRepository;
import com.trading.job.infrastructure.repository.OrderEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 【職責】JOB-D 商業邏輯：依保留天數刪除過期訂單事件與終態（SUCCEEDED／DEAD）失敗指令。
 * 【技巧】{@code @Transactional} 內呼叫 Repository 的 {@code @Modifying} 批次 DELETE。
 * 【概念】清理是資料生命週期政策：事件保留較久供稽核，終態失敗指令可較短；PENDING 永不刪以免中斷重試。
 * 【邊界】不負責排程觸發；不刪訂單／持倉本體。
 */
@Service
@Slf4j
public class DataCleanupService {

    private final OrderEventRepository orderEventRepository;
    private final FailedCommandRepository failedCommandRepository;
    private final JobProperties jobProperties;

    public DataCleanupService(OrderEventRepository orderEventRepository,
                              FailedCommandRepository failedCommandRepository,
                              JobProperties jobProperties) {
        this.orderEventRepository = orderEventRepository;
        this.failedCommandRepository = failedCommandRepository;
        this.jobProperties = jobProperties;
    }

    /**
     * 【職責】依設定的保留天數執行清理，回傳刪除筆數。
     * 【技巧】分別用 event／failedCommand 的 retentionDays 算 cutoff；失敗指令刪除限定終態集合。
     * 【概念】回傳 {@link CleanupResult} 讓 API 能組 detail，而不是只回一個模糊總數。
     *
     * @return 刪除的事件與失敗指令筆數
     */
    @Transactional
    public CleanupResult cleanup() {
        JobProperties.Cleanup config = jobProperties.getCleanup();
        OffsetDateTime now = OffsetDateTime.now();

        OffsetDateTime eventCutoff = now.minusDays(config.getEventRetentionDays());
        int deletedEvents = orderEventRepository.deleteByCreatedAtBefore(eventCutoff);

        OffsetDateTime commandCutoff = now.minusDays(config.getFailedCommandRetentionDays());
        int deletedCommands = failedCommandRepository.deleteByStatusInAndUpdatedAtBefore(
                List.of(FailedCommandStatus.SUCCEEDED, FailedCommandStatus.DEAD), commandCutoff);

        if (deletedEvents > 0 || deletedCommands > 0) {
            log.info("JOB-D cleanup removed {} order events and {} failed commands",
                    deletedEvents, deletedCommands);
        }
        return new CleanupResult(deletedEvents, deletedCommands);
    }

    /**
     * 【職責】JOB-D 清理結果：分別記錄事件與失敗指令刪除筆數，供 API 組裝 detail。
     * 【技巧】Java {@code record} 不可變值物件。
     * 【概念】比回傳 {@code int[]} 或 Map 更有型別語意，呼叫端不會搞混兩個數字的意義。
     */
    public record CleanupResult(int deletedOrderEvents, int deletedFailedCommands) {
    }
}
