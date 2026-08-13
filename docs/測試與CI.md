# 測試與 CI — TradingJob

> 衝突以主規格為準。規範：EngineeringOS `knowledge/testing.md`

## 1. 驗證指令

| 指令 | 用途 | 需服務運行 |
|------|------|------------|
| `.\gradlew.bat check` | 單元 + 整合 | 否 |
| `.\gradlew.bat test` | 單元為主 | 否 |
| `.\gradlew.bat bootRun --args='--spring.profiles.active=dev'` | 本機手動驗 API | 是 (:8084) |

## 2. Gradle 任務

```powershell
.\gradlew.bat test
.\gradlew.bat check
.\gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

## 3. 三層測試

| 層 | 說明 |
|----|------|
| 單元 | Service（Mock Repo）、Controller、SchedulingConfig |
| 整合 | `@SpringBootTest` + H2，跑 JOB-A~D |
| Smoke | 可選：對 `:8084` POST 四個 `/jobs/*` 與 GET health |

## 4. DoD

- [x] check 全綠
- [x] 公開 Job API 有 Happy Path
- [ ] 正式 PostgreSQL／Engine 聯調（部署環境）
