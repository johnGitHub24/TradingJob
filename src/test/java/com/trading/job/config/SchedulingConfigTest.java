package com.trading.job.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【職責】{@link SchedulingConfig} 單元測試：不啟動 Spring，直接組裝執行緒池並斷言設定。
 * 【技巧】new {@link JobProperties} + {@link SchedulingConfig#taskScheduler()}；{@code @AfterEach} shutdown 避免執行緒洩漏。
 * 【概念】整合層再驗證 Bean 有進容器；本層鎖定「poolSize／前綴來自 properties」這段純組裝邏輯。
 * 【技巧驗證】corePoolSize 與 threadNamePrefix 對齊手動設定值。
 */
class SchedulingConfigTest {

    private ThreadPoolTaskScheduler scheduler;

    @AfterEach
    void tearDown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    /**
     * CASE-TASK-001：TaskScheduler 為 ThreadPool 且 poolSize 符合設定。
     * Given: poolSize=4；When: taskScheduler()；Then: ThreadPoolTaskScheduler 且 corePoolSize=4。
     */
    @Test
    void TASK_001_taskScheduler_isThreadPoolWithConfiguredPoolSize() {
        JobProperties props = new JobProperties();
        props.getScheduler().setPoolSize(4);
        SchedulingConfig config = new SchedulingConfig(props);

        scheduler = (ThreadPoolTaskScheduler) config.taskScheduler();

        assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(4);
    }

    /**
     * CASE-TASK-002：執行緒名稱前綴來自 JobProperties。
     * Given: threadNamePrefix=job-unit-；When: taskScheduler()；Then: 前綴等於設定值。
     */
    @Test
    void TASK_002_scheduler_threadNamePrefix_isConfigured() {
        JobProperties props = new JobProperties();
        props.getScheduler().setThreadNamePrefix("job-unit-");
        SchedulingConfig config = new SchedulingConfig(props);

        scheduler = (ThreadPoolTaskScheduler) config.taskScheduler();

        assertThat(scheduler.getThreadNamePrefix()).isEqualTo("job-unit-");
    }
}
