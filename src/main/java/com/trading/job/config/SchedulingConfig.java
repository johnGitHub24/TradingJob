package com.trading.job.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 【職責】啟用 Spring Scheduling，並以可配置執行緒池承載 {@code @Scheduled} cron。
 * 【技巧】{@code @EnableScheduling} + {@link SchedulingConfigurer} 掛自訂 {@link TaskScheduler} Bean。
 * 【概念】Spring 預設單執行緒 scheduler：一個長 Job 會堵住其他 cron。pool-size ≥ 2 讓 JOB-A~D 可並行。
 * 【邊界】不定義各 Job 的 cron 字串（在 {@link JobProperties}）；不實作 Job 本體。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {

    private final JobProperties jobProperties;

    public SchedulingConfig(JobProperties jobProperties) {
        this.jobProperties = jobProperties;
    }

    /**
     * 【職責】建立可配置執行緒池的 {@link TaskScheduler}。
     * 【技巧】{@link ThreadPoolTaskScheduler}：poolSize、threadNamePrefix、shutdown 等待。
     * 【概念】具名執行緒前綴方便在 thread dump／日誌辨識是哪個排程池。
     *
     * @return 已 initialize 的 ThreadPoolTaskScheduler
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        JobProperties.Scheduler props = jobProperties.getScheduler();
        scheduler.setPoolSize(props.getPoolSize());
        scheduler.setThreadNamePrefix(props.getThreadNamePrefix());
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }

    /**
     * 【職責】把自訂 {@link TaskScheduler} 掛到 Spring 排程註冊器。
     * 【技巧】實作 {@link SchedulingConfigurer#configureTasks}。
     * 【概念】只宣告 {@code @Bean TaskScheduler} 不夠時，用此回呼明確綁定 registrar。
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler());
    }
}
