# TradingJob — 專案規則（薄）

繼承：EngineeringOS eos-minimal @ **0.1.13**  
公版：`EngineeringOS/eos-minimal/`  
權威規格：[TradingJob-SPEC.md](TradingJob-SPEC.md)

## 與公版差異

- Backend port: 8083（prod/default）· **8084**（`dev` profile / H2）
- Framework: Spring Boot 3.2 · Java 21 · `@Scheduled` + `ThreadPoolTaskScheduler`
- DB: PostgreSQL（預設）· H2（`test` / `dev`）
- 驗證入口：`.\scripts\check.ps1`（載入 JDK 21 後 `gradlew check`＝unit + integration）
- 本機 Demo：IntelliJ／終端 **Gradle `bootRun`**（**勿**對 `*Application` 綠箭頭；Windows 易 0xC0000005）→ http://localhost:8084

## 本專案專屬

- Domain: JOB-A 逾時取消、JOB-B PnL 快照、JOB-C 失敗重試、JOB-D 資料清理
- Task 用法：見 `docs/Task用法.md`；設定 `trading.job.scheduler.*`
- 與 Engine 共用 DB（正式）；本機驗證用 H2，不假設 Engine 已啟動

## 註解深度
- comment_verbosity: **detailed**
- 權威：`EngineeringOS/eos-minimal/knowledge/comments.md` §0／§3b（eos-minimal @ 0.1.13）
- 結構：【職責】【技巧】【概念】；簡單 getter 可併入類別說明

## Git Remote
- 帳號：`johnGitHub24`；一專案一 repo
- 規範：`EngineeringOS/eos-minimal/knowledge/專案上船-GitHub.md`

## 回寫

問題與公版改善建議 → `EngineeringOS/eos-minimal/feedback/SYNC_LOG.md`
