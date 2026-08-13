# Testing and Verification — TradingJob

> 衝突以 [TradingJob-SPEC.md](../TradingJob-SPEC.md) 為準。  
> 規範：EngineeringOS `knowledge/testing.md`（單元 ↔ 整合成對）

## Check command

```powershell
.\scripts\check.ps1
```

（載入 JDK 21 後 `gradlew check`＝unit + `@Tag("integration")`；與 CI 同一入口。）

成對掃描（WarnOnly）：

```powershell
& "..\EngineeringOS\eos-minimal\hooks\scan-paired-tests.ps1" -ProjectRoot . -WarnOnly
```

## Test layers

| Layer | Location | Tag | 說明 |
|-------|----------|-----|------|
| 單元 | `src/test/.../application`、`config`、根套件 `JobControllerTest` | — | Service Mock／WebMvc／SchedulingConfig 組裝 |
| 整合 | `src/test/.../integration` | `@Tag("integration")` | JOB-A~D + H2；Job API Happy＋錯誤 |

`JobControllerTest` **不要**放在 `.../api/`：掃描腳本會把 `/api/` 當成整合層。

## Minimum case types

| Type | Coverage |
|------|----------|
| Happy Path | JOB-A~D Service／整合；手動 API `CASE-JOB-API-001`～`006` |
| Error / edge | `CASE-JOB-API-007` 無效 status→400；重試退避／DEAD；空批次 |
| Paired IDs | 同一 Case ID 必須同時出現在單元與整合層 |

## Key classes

| Test | 對應 |
|------|------|
| `StaleOrderCancellationServiceTest` | JOB-A |
| `PnlSnapshotServiceTest` | JOB-B |
| `FailedCommandServiceTest` | JOB-C |
| `DataCleanupServiceTest` | JOB-D |
| `OrderEventServiceTest` | 審計事件寫入 |
| `JobControllerTest` | 手動觸發 API（WebMvc 單元） |
| `JobApiIntegrationTest` | 手動 API HTTP＋H2 |
| `SchedulingConfigTest` / `SchedulingConfigIntegrationTest` | ThreadPoolTaskScheduler |
| `*JobIntegrationTest` | 端到端 Job |

## Case pairing（摘要）

| Case ID | 單元 | 整合 |
|---------|------|------|
| CASE-JOB-STALE-001～004 | Service | `StaleOrderTimeoutJobIntegrationTest` |
| CASE-JOB-PNL-001～003 | Service | `PnlSnapshotJobIntegrationTest` |
| CASE-JOB-RETRY-001～004 | Service | Retry 成功／失敗整合 |
| CASE-JOB-CLEAN-001～003 | Service | `DataCleanupJobIntegrationTest` |
| CASE-JOB-API-001～007 | `JobControllerTest`（+ Service 列表） | `JobApiIntegrationTest` |
| CASE-TASK-001～002 | `SchedulingConfigTest` | `SchedulingConfigIntegrationTest` |
| CASE-JOB-EVENT-001 | `OrderEventServiceTest` | Stale 整合寫事件 |

## DoD

- [x] Unit tests green
- [x] Integration tests green
- [x] Check command matches CI（`scripts/check.ps1`）
- [x] 公開 Job API：Happy Path + ≥1 錯誤路徑（整合）
- [x] 公開 Service ≥1 單元；Case ID 單元↔整合成對

詳見 [測試與CI.md](測試與CI.md)。
