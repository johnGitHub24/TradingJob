# Spring Boot Cron / Task 用法（TradingJob）

## 核心概念

| 元件 | 角色 |
|------|------|
| `@EnableScheduling` | 開啟排程掃描 |
| `@Scheduled(cron = "...")` | 宣告 cron 任務（JOB-A~D） |
| `TaskScheduler` | 實際執行任務的排程器 |
| `ThreadPoolTaskScheduler` | 執行緒池版 TaskScheduler（本專案採用） |
| `SchedulingConfigurer` | 把自訂 TaskScheduler 掛到 Spring |

預設若不自訂，Spring 使用**單執行緒** scheduler，長任務會堵住其他 cron。

## 本專案設定

```yaml
trading.job.scheduler:
  pool-size: 4
  thread-name-prefix: trading-job-
```

實作：`SchedulingConfig` 建立 `ThreadPoolTaskScheduler` Bean，並在 `configureTasks` 註冊。

Job 類別（例：`DataCleanupJob`）只呼叫 Service，不碰 Repository —— 符合 EOS 分層。

## 手動觸發（驗證用）

排程以外，可用 HTTP 立即跑一次：

```http
POST /api/v1/jobs/stale-order-cancellation
POST /api/v1/jobs/pnl-snapshot
POST /api/v1/jobs/failed-command-retry
POST /api/v1/jobs/cleanup
```

## 本機跑通

```powershell
.\gradlew.bat check
.\gradlew.bat bootRun --args='--spring.profiles.active=dev'
# health: http://localhost:8084/actuator/health
```
