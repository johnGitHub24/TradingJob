# IntelliJ IDEA 啟動設定（TradingJob）

> 本專案預設 DB 是 **PostgreSQL**；本機 Demo 必須用 **`dev` profile（H2）**。  
> Terminal `gradlew bootRun` 已預設帶 `dev`；IntelliJ 請用下方 Run Configuration。

---

## 症狀對照

| 症狀 | 原因 |
|------|------|
| `Connection refused` / 連不上 `localhost:5432` | 未啟用 `dev`，仍連 PostgreSQL |
| 綠色 ▶ 灰色、找不到 `TradingJobApplication` | 未以 Gradle 匯入、Module 不是 `TradingJob.main` |
| Run 選單出現別專案的 Application | 開錯父目錄（例如 `D:\ClaudeCode`） |
| Terminal 能跑、IDE 不行 | Project SDK / Gradle JVM 不是 JDK 21 |

---

## 正確開啟（第一次或修好後）

1. **只開專案根目錄**  
   `File → Open → D:\ClaudeCode\TradingJob`（含 `build.gradle` 的那層）

2. **刪除壞掉的舊 Module（若曾手動開過）**  
   若 Project 視窗裡沒有 `TradingJob.main` / `TradingJob.test`，請：  
   - `File → Invalidate Caches` 可選，或先關閉專案  
   - 刪除本機 `.idea/TradingJob.iml`、`.idea/modules.xml`（若存在）  
   - 重新 Open，對 `build.gradle` 選 **Open as Gradle Project** / **Link Gradle Project**

3. **JDK 21**  
   - `Project Structure → Project SDK` = **21**  
   - `Settings → Build Tools → Gradle → Gradle JVM` = **Project SDK**

4. **Gradle Sync**  
   右側 Gradle 面板 → Reload。成功後應有：  
   `TradingJob → Tasks → application → bootRun`

5. **Run**  
   右上角選：

   | 名稱 | 說明 |
   |------|------|
   | **TradingJobApplication** | Spring Boot 直接跑（已設 Active profiles=`dev`） |
   | **bootRun (TradingJob)** | Gradle `bootRun`（預設同樣 `dev`） |

6. **驗證**  
   - Console 出現：`The following 1 profile is active: "dev"`  
   - http://localhost:8084/actuator/health → UP  
   - H2：http://localhost:8084/h2-console ，JDBC `jdbc:h2:mem:tradingjob`

---

## 若仍要連正式 PostgreSQL

不要用本機 Run Config 的 `dev`：另建一個 Configuration，Active profiles 留空，並確保本機有 `jdbc:postgresql://localhost:5432/trading`。
