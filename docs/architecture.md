# Architecture — TradingJob

> 衝突以 [TradingJob-SPEC.md](../TradingJob-SPEC.md) 為準。  
> 規範：EngineeringOS `knowledge/architecture.md`、`templates/spring-boot/docs/scheduling-task.md`

## Layers

| Layer | Package | Responsibility |
|-------|---------|----------------|
| API | `api` | HTTP、參數、組 `JobRunResponse`／DTO |
| Application | `application` | JOB 商業邏輯、`@Transactional`、重試／清理規則 |
| Scheduler | `scheduler` | `@Scheduled` 觸發，**只呼叫 Service** |
| Config | `config` | `SchedulingConfig`、`JobProperties`、Engine client |
| Client | `client` | `OrderReplayPort` 實作（本機／Engine） |
| Domain | `domain` | Enum（狀態、方向、事件類型） |
| Infrastructure | `infrastructure.entity` / `repository` | JPA Entity／Repository |
| DTO | `dto` | Request／Response |

## Module map

| Module | Notes |
|--------|-------|
| JOB-A | `StaleOrderTimeoutJob` → `StaleOrderCancellationService` — 逾時未成交取消 |
| JOB-B | `PnlSnapshotJob` → `PnlSnapshotService` — 日結 PnL／持倉快照 |
| JOB-C | `FailedCommandRetryJob` → `FailedCommandService` — DLQ 重試 |
| JOB-D | `DataCleanupJob` → `DataCleanupService` — 事件／失敗指令清理 |
| Manual API | `JobController` — 與 cron 同一 Service 路徑 |
| Scheduling | `ThreadPoolTaskScheduler`（`pool-size` 預設 4） |

## Runtime

```text
Cron / HTTP POST
    → Scheduler Job 或 JobController
        → Application Service
            → Repository (orders / positions / pnl_snapshots / failed_commands / order_events)
            → OrderReplayPort（JOB-C 重放，可選 Engine HTTP）
```

本機 `dev`：H2 + port 8084，不依賴 Engine。  
正式：PostgreSQL + `trading.engine.base-url`。

## Visual maps

| 文件 | 用途 |
|------|------|
| [codeGraphic.html](codeGraphic.html) | Tab：JOB-A／B／C-D／Runtime（圖為主） |
