# TradingJob

排程後台：JOB-A 逾時取消、JOB-B PnL 快照、JOB-C 失敗重試、JOB-D 資料清理。

## 文件入口

單一入口：本 README。衝突以主規格為準。

| 文件 | 說明 |
|------|------|
| [TradingJob-SPEC.md](TradingJob-SPEC.md) | **主規格（權威）** |
| [docs/architecture.md](docs/architecture.md) | 分層與模組 |
| [docs/codeGraphic.html](docs/codeGraphic.html) | 架構圖（非權威） |
| [docs/testing.md](docs/testing.md) | 測試／Case／check |
| [docs/Task用法.md](docs/Task用法.md) | Job 排程 |
| [docs/資料庫設計.md](docs/資料庫設計.md) | 資料庫 |
| [docs/驗證設計.md](docs/驗證設計.md) | 驗證／權限 |
| [CLAUDE.md](CLAUDE.md) | AI 薄規則 |
| [scripts/README.md](scripts/README.md) | 驗證／啟動腳本 |

## Quick start

```powershell
.\scripts\check.ps1
.\gradlew.bat bootRun
# health: http://localhost:8084/actuator/health
```

- 驗證閘門：`.\scripts\check.ps1`（JDK 21 → `gradlew check`＝unit + integration）
- `bootRun` 預設 `dev`／H2，埠 **8084**
- IntelliJ：只開本目錄 → Gradle Sync → 跑 **Gradle `bootRun`**
- **不要**對 `TradingJobApplication`（或任何 `*Application.java`）按綠色箭頭（Windows 易 0xC0000005）

詳見 [docs/IntelliJ-IDE-啟動設定.md](docs/IntelliJ-IDE-啟動設定.md)（companion：[docs/IntelliJ-IDE-啟動設定.html](docs/IntelliJ-IDE-啟動設定.html)）。

Docs standard: EngineeringOS eos-minimal @ 0.1.10

