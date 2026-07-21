package com.trading.job.integration;

import com.trading.job.application.DataCleanupService;
import com.trading.job.domain.FailedCommandStatus;
import com.trading.job.domain.OrderEventType;
import com.trading.job.domain.OrderSide;
import com.trading.job.infrastructure.entity.FailedCommandEntity;
import com.trading.job.infrastructure.entity.OrderEventEntity;
import com.trading.job.infrastructure.repository.FailedCommandRepository;
import com.trading.job.infrastructure.repository.OrderEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【職責】JOB-D 整合測試：真實 DB 驗證過期事件與終態失敗指令清理邊界。
 * 【技巧】JdbcTemplate 把 {@code created_at}/{@code updated_at} 調舊以超過保留天數。
 * 【概念】證明 PENDING 即使「看起來很舊」也不會被刪；終態才清。
 * 【技巧驗證】舊事件刪、新事件留；DEAD／SUCCEEDED 刪、PENDING 留。
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class DataCleanupJobIntegrationTest {

    @Autowired
    private DataCleanupService dataCleanupService;

    @Autowired
    private OrderEventRepository orderEventRepository;

    @Autowired
    private FailedCommandRepository failedCommandRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        orderEventRepository.deleteAll();
        failedCommandRepository.deleteAll();
    }

    private OrderEventEntity saveEvent(Long orderId) {
        OrderEventEntity e = new OrderEventEntity();
        e.setOrderId(orderId);
        e.setEvent(OrderEventType.RECEIVED);
        return orderEventRepository.save(e);
    }

    private FailedCommandEntity saveCommand(FailedCommandStatus status) {
        FailedCommandEntity e = new FailedCommandEntity();
        e.setSymbol("BTCUSDT");
        e.setSide(OrderSide.BUY);
        e.setQuantity(new BigDecimal("0.5"));
        e.setPrice(new BigDecimal("65000"));
        e.setStatus(status);
        e.setNextRetryAt(OffsetDateTime.now());
        return failedCommandRepository.save(e);
    }

    /**
     * CASE-JOB-CLEAN-001：刪除過期事件、保留近期事件。
     * Given: 一筆 31 天前事件 + 一筆近期；When: cleanup；Then: 僅刪舊事件。
     */
    @Test
    void JOB_CLEAN_001_EVENTS_deletesExpiredEventsKeepsRecent() {
        OrderEventEntity old = saveEvent(1L);
        OrderEventEntity recent = saveEvent(2L);
        jdbcTemplate.update("UPDATE order_events SET created_at = ? WHERE id = ?",
                OffsetDateTime.now().minusDays(31), old.getId());

        DataCleanupService.CleanupResult result = dataCleanupService.cleanup();

        assertThat(result.deletedOrderEvents()).isEqualTo(1);
        assertThat(orderEventRepository.findById(old.getId())).isEmpty();
        assertThat(orderEventRepository.findById(recent.getId())).isPresent();
    }

    /**
     * CASE-JOB-CLEAN-003：清除過期終態失敗指令、保留 PENDING。
     * Given: DEAD／SUCCEEDED／PENDING 皆過期；When: cleanup；Then: 刪 2 筆終態、PENDING 仍在。
     */
    @Test
    void JOB_CLEAN_003_FAILED_COMMANDS_purgesTerminalOldKeepsPending() {
        FailedCommandEntity dead = saveCommand(FailedCommandStatus.DEAD);
        FailedCommandEntity succeeded = saveCommand(FailedCommandStatus.SUCCEEDED);
        FailedCommandEntity pending = saveCommand(FailedCommandStatus.PENDING);
        jdbcTemplate.update("UPDATE failed_commands SET updated_at = ? WHERE id IN (?, ?, ?)",
                OffsetDateTime.now().minusDays(8), dead.getId(), succeeded.getId(), pending.getId());

        DataCleanupService.CleanupResult result = dataCleanupService.cleanup();

        assertThat(result.deletedFailedCommands()).isEqualTo(2);
        assertThat(failedCommandRepository.findById(dead.getId())).isEmpty();
        assertThat(failedCommandRepository.findById(succeeded.getId())).isEmpty();
        assertThat(failedCommandRepository.findById(pending.getId())).isPresent();
    }
}
