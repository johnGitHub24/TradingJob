package com.trading.job.integration;

import com.trading.job.config.JobProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【職責】排程執行緒池整合：啟動完整上下文後斷言 {@link TaskScheduler} 設定落地。
 * 【技巧】{@code @SpringBootTest} + test profile；對照 {@link JobProperties.Scheduler}。
 * 【概念】單元層直接 new {@code SchedulingConfig}；本層證明 Spring 有把 Bean 掛進容器且參數未被覆寫丟棄。
 * 【技巧驗證】型別為 ThreadPoolTaskScheduler；poolSize／threadNamePrefix 等於設定。
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
class SchedulingConfigIntegrationTest {

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private JobProperties jobProperties;

    /**
     * CASE-TASK-001：TaskScheduler 為 ThreadPool 且 poolSize 符合設定。
     * Given: test profile 啟動；When: 注入 TaskScheduler；Then: ThreadPoolTaskScheduler 且 corePoolSize=設定值。
     */
    @Test
    void TASK_001_taskScheduler_isThreadPoolWithConfiguredPoolSize() {
        assertThat(taskScheduler).isInstanceOf(ThreadPoolTaskScheduler.class);

        ThreadPoolTaskScheduler pool = (ThreadPoolTaskScheduler) taskScheduler;
        int configured = jobProperties.getScheduler().getPoolSize();
        assertThat(configured).isGreaterThanOrEqualTo(2);
        assertThat(pool.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(configured);
    }

    /**
     * CASE-TASK-002：執行緒名稱前綴來自 JobProperties。
     * Given: 已注入 ThreadPoolTaskScheduler；When: 讀取 threadNamePrefix；Then: 等於設定值。
     */
    @Test
    void TASK_002_scheduler_threadNamePrefix_isConfigured() {
        ThreadPoolTaskScheduler pool = (ThreadPoolTaskScheduler) taskScheduler;
        assertThat(pool.getThreadNamePrefix()).isEqualTo(jobProperties.getScheduler().getThreadNamePrefix());
    }
}
