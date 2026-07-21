package com.trading.job.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【職責】{@link SchedulingConfig} 整合驗證：Cron Job 使用可配置的 {@link TaskScheduler}（非預設單執行緒）。
 * 【技巧】{@code @SpringBootTest} + test profile；斷言 {@link ThreadPoolTaskScheduler} 與 poolSize。
 * 【概念】若誤用單執行緒預設，長 Job 會互堵——此測試鎖定「有執行緒池且參數來自設定」。
 * 【技巧驗證】corePoolSize／threadNamePrefix 對齊 {@link JobProperties}。
 */
@SpringBootTest
@ActiveProfiles("test")
class SchedulingConfigTest {

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private JobProperties jobProperties;

    /**
     * CASE-TASK-001：TaskScheduler 為 ThreadPool 且 poolSize 符合設定。
     * Given: test profile 啟動；When: 注入 TaskScheduler；Then: 型別為 ThreadPoolTaskScheduler 且 corePoolSize=設定值。
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
