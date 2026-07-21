# TradingJob

## 文件入口

| 文件 | 說明 |
|------|------|
| [TradingJob-SPEC.md](TradingJob-SPEC.md) | **主規格書（權威）** |
| [docs/architecture.md](docs/architecture.md) | 分層與模組 |
| [docs/codeGraphic.html](docs/codeGraphic.html) | Tab 式架構圖（JOB-A~D／Runtime） |
| [docs/testing.md](docs/testing.md) | 測試／DoD |
| [docs/測試與CI.md](docs/測試與CI.md) | 指令與 Gradle |
| [docs/資料庫設計.md](docs/資料庫設計.md) | 表／Entity |
| [docs/驗證設計.md](docs/驗證設計.md) | 業務規則／無 JWT 說明 |
| [docs/功能流程說明.md](docs/功能流程說明.md) | JOB-A~D 流程 |
| [docs/Task用法.md](docs/Task用法.md) | 排程用法 |
| [CLAUDE.md](CLAUDE.md) | AI／工程薄規則（繼承 EOS） |

## Quick start

```powershell
.\gradlew.bat check
.\gradlew.bat bootRun
# health: http://localhost:8084/actuator/health
```

`bootRun` 預設已帶 `dev`（H2）。IntelliJ：只開本目錄 → Gradle Sync → Run **TradingJobApplication**（詳見 [docs/IntelliJ-IDE-啟動設定.md](docs/IntelliJ-IDE-啟動設定.md)）。

Docs standard: EngineeringOS eos-minimal @ 0.1.4
