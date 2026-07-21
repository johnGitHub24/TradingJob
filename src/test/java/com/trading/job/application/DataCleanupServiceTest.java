package com.trading.job.application;

import com.trading.job.config.JobProperties;
import com.trading.job.domain.FailedCommandStatus;
import com.trading.job.infrastructure.repository.FailedCommandRepository;
import com.trading.job.infrastructure.repository.OrderEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 【職責】{@link DataCleanupService} 單元測試：覆蓋 JOB-D 刪除筆數回傳與終態過濾邊界。
 * 【技巧】Mock 兩個 Repository 的 delete 回傳值；Captor 驗證狀態集合。
 * 【概念】清理政策「只刪終態」必須在呼叫參數上可觀測，避免誤刪 PENDING。
 * 【技巧驗證】CleanupResult 分項；statuses 僅 SUCCEEDED／DEAD。
 */
@ExtendWith(MockitoExtension.class)
class DataCleanupServiceTest {

    @Mock
    private OrderEventRepository orderEventRepository;

    @Mock
    private FailedCommandRepository failedCommandRepository;

    private final JobProperties jobProperties = new JobProperties();

    private DataCleanupService service;

    @BeforeEach
    void setUp() {
        service = new DataCleanupService(orderEventRepository, failedCommandRepository, jobProperties);
    }

    /**
     * CASE-JOB-CLEAN-001：清理回傳事件與失敗指令刪除筆數。
     * Given: Repository 分別回 7、3；When: cleanup；Then: CleanupResult(7,3)。
     */
    @Test
    void JOB_CLEAN_001_EVENTS_deletesExpiredEventsAndCommands() {
        when(orderEventRepository.deleteByCreatedAtBefore(any())).thenReturn(7);
        when(failedCommandRepository.deleteByStatusInAndUpdatedAtBefore(any(), any())).thenReturn(3);

        DataCleanupService.CleanupResult result = service.cleanup();

        assertThat(result.deletedOrderEvents()).isEqualTo(7);
        assertThat(result.deletedFailedCommands()).isEqualTo(3);
    }

    /**
     * CASE-JOB-CLEAN-002：僅清除終態失敗指令（SUCCEEDED／DEAD）。
     * Given: 任意 cleanup；When: 呼叫刪除；Then: 傳入狀態集合僅含 SUCCEEDED、DEAD。
     */
    @Test
    void JOB_CLEAN_002_onlyPurgesTerminalFailedCommands() {
        when(orderEventRepository.deleteByCreatedAtBefore(any())).thenReturn(0);
        ArgumentCaptor<Collection<FailedCommandStatus>> captor = ArgumentCaptor.forClass(Collection.class);
        when(failedCommandRepository.deleteByStatusInAndUpdatedAtBefore(captor.capture(), any(OffsetDateTime.class)))
                .thenReturn(0);

        service.cleanup();

        assertThat(captor.getValue())
                .containsExactlyInAnyOrder(FailedCommandStatus.SUCCEEDED, FailedCommandStatus.DEAD);
    }
}
