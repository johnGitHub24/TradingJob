# Testing and Verification — TradingJob

> 衝突以 [TradingJob-SPEC.md](../TradingJob-SPEC.md) 為準。  
> 規範：EngineeringOS `knowledge/testing.md`

## Check command

```powershell
.\gradlew.bat check
```

（需 JDK 21；與 CI 同一入口。）

## Test layers

| Layer | Location | Tag | 說明 |
|-------|----------|-----|------|
| 單元 | `src/test/.../application`、`api`、`config` | — | Service Mock／Controller／SchedulingConfig |
| 整合 | `src/test/.../integration` | `@Tag("integration")` | JOB-A~D + H2 |

## Minimum case types

| Type | Coverage |
|------|----------|
| Happy Path | JOB-A~D Service／整合；手動 API |
| Error / edge | 重試上限、空批次、Scheduler pool 設定 |

## Key classes

| Test | 對應 |
|------|------|
| `StaleOrderCancellationServiceTest` | JOB-A |
| `PnlSnapshotServiceTest` | JOB-B |
| `FailedCommandServiceTest` | JOB-C |
| `DataCleanupServiceTest` | JOB-D |
| `JobControllerTest` | 手動觸發 API |
| `SchedulingConfigTest` | ThreadPoolTaskScheduler |
| `*JobIntegrationTest` | 端到端 Job |

## DoD

- [x] Unit tests green
- [x] Integration tests green
- [x] Check command matches CI
- [x] 公開 Job API 有 Happy Path 覆蓋

詳見 [測試與CI.md](測試與CI.md)。
