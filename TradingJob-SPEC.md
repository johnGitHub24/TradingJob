# TradingJob Specification

> **Authority contract.** Conflicts resolve to this file.  
> EOS docs standard: EngineeringOS `eos-minimal/knowledge/documentation.md` @ 0.1.10

## 0. Document map

| File | Role |
|------|------|
| This file | Master spec (authority) |
| [README.md](README.md) | Entry |
| [CLAUDE.md](CLAUDE.md) | Thin AI rules |
| [docs/architecture.md](docs/architecture.md) | Layers / modules / runtime |
| [docs/testing.md](docs/testing.md) | Test layers / DoD |
| [docs/testing.md](docs/testing.md) | Commands / Gradle tasks |
| [docs/資料庫設計.md](docs/資料庫設計.md) | Tables / Entity map |
| [docs/驗證設計.md](docs/驗證設計.md) | Business rules（no JWT） |
| [docs/architecture.md](docs/architecture.md) | JOB-A~D flows |
| [docs/Task用法.md](docs/Task用法.md) | `@Scheduled` + ThreadPoolTaskScheduler |

## 1. Scope

- **Purpose:** 交易後台排程服務 — 逾時取消、PnL 快照、失敗指令重試、資料清理；並提供手動觸發／查詢 API。
- **Stack:** Java 21 · Spring Boot 3.2 · Spring Data JPA · `@EnableScheduling` + `ThreadPoolTaskScheduler` · OpenAPI
- **DB:** PostgreSQL（預設，可與 Engine 共用）· H2（`dev` / `test`）
- **Ports:** `8083`（預設）· `8084`（`dev` profile）
- **Non-goals:** 不實作撮合引擎；不取代 TradingCRUD 的訂單 CRUD UI；本機 H2 不假設 Engine 已啟動

## 2. Architecture

See [docs/architecture.md](docs/architecture.md).  
排程規則遵循 EOS：Job 只呼叫 Service；`pool-size ≥ 2`。

## 3. API / Contract

| Method | Path | Job | 說明 |
|--------|------|-----|------|
| POST | `/api/v1/jobs/stale-order-cancellation` | JOB-A | 取消逾時未成交訂單 |
| POST | `/api/v1/jobs/pnl-snapshot` | JOB-B | 寫入當日 PnL／持倉快照 |
| POST | `/api/v1/jobs/failed-command-retry` | JOB-C | 重試到期失敗指令 |
| POST | `/api/v1/jobs/cleanup` | JOB-D | 清理過期事件／終態失敗指令 |
| GET | `/api/v1/pnl-snapshots?date=` | — | 查詢快照（預設今日） |
| GET | `/api/v1/failed-commands?status=` | — | 查詢失敗指令 DLQ |
| GET | `/actuator/health` | — | 健康檢查 |

回應：`JobRunResponse`（job 代碼、影響筆數、說明）。OpenAPI：啟動後 `/swagger-ui.html`（若已啟用）。

### Cron（預設，可用環境變數覆寫）

| Job | Cron | 設定鍵 |
|-----|------|--------|
| JOB-A | `0 */5 * * * *` | `trading.job.stale-order.cron` |
| JOB-B | `0 0 0 * * *` | `trading.job.pnl-snapshot.cron` |
| JOB-C | `0 * * * * *` | `trading.job.retry.cron` |
| JOB-D | `0 30 0 * * *` | `trading.job.cleanup.cron` |

Scheduler pool：`trading.job.scheduler.pool-size`（預設 4）。

## 4. Test DoD

- [x] `.\scripts\check.ps1` 全綠（JDK 21 → unit + `@Tag("integration")`）
- [x] 各公開 Job Service ≥1 單元；Controller WebMvc 單元 + API 整合（Happy + ≥1 錯誤）
- [x] 整合測試覆蓋 JOB-A~D 主路徑；Case ID 與單元層成對
- [x] 本機 Demo 僅 Gradle `bootRun`（:8084／`dev`／H2）；**勿**用 `*Application` 綠箭
- [ ] 正式環境連 PostgreSQL／Engine 的 smoke（部署時另驗）

## 5. Changelog

| Date | Note |
|------|------|
| 2026-07-10 | EOS skeleton |
| 2026-07-10 | 依實作補齊 SPEC／架構／測試／DB／流程（EOS 0.1.10） |
| 2026-08-13 | 成對 Case ID；check.ps1 閘門；bootRun-only Demo |
