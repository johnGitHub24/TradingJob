package com.trading.job.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 【職責】JobController API 整合測試：真實 Spring 上下文＋H2，驗證手動 Job／查詢 HTTP 契約。
 * 【技巧】{@code @SpringBootTest} + {@code @AutoConfigureMockMvc}；空庫仍應 200。
 * 【概念】單元層 Mock Service 斷言 JSON 形狀；本層證明路由、參數綁定與真實 Service 能跑通。
 * 【技巧驗證】JOB-A~D Happy（affected 為數字）；無效 enum → 400。
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * CASE-JOB-API-001：JOB-A 手動執行回傳影響筆數。
     * Given: 測試庫可為空；When: POST /jobs/stale-order-cancellation；Then: 200 + job=JOB-A + affected 為數字。
     */
    @Test
    void JOB_API_001_runStaleOrderCancellation_returnsJobA() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/stale-order-cancellation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job").value("JOB-A"))
                .andExpect(jsonPath("$.affected").isNumber());
    }

    /**
     * CASE-JOB-API-002：JOB-B 手動執行回傳寫入筆數。
     * Given: 測試庫可為空；When: POST /jobs/pnl-snapshot；Then: 200 + job=JOB-B。
     */
    @Test
    void JOB_API_002_runPnlSnapshot_returnsJobB() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/pnl-snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job").value("JOB-B"))
                .andExpect(jsonPath("$.affected").isNumber());
    }

    /**
     * CASE-JOB-API-003：JOB-C 手動執行回傳成功重放筆數。
     * Given: 測試庫可為空；When: POST /jobs/failed-command-retry；Then: 200 + job=JOB-C。
     */
    @Test
    void JOB_API_003_runFailedCommandRetry_returnsJobC() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/failed-command-retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job").value("JOB-C"))
                .andExpect(jsonPath("$.affected").isNumber());
    }

    /**
     * CASE-JOB-API-004：JOB-D 手動執行彙總刪除筆數與 detail。
     * Given: 測試庫可為空；When: POST /jobs/cleanup；Then: 200 + job=JOB-D + detail 含 events。
     */
    @Test
    void JOB_API_004_runCleanup_returnsJobDAndDetail() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/cleanup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job").value("JOB-D"))
                .andExpect(jsonPath("$.affected").isNumber())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("events=")));
    }

    /**
     * CASE-JOB-API-005：PnL 快照列表可回空陣列。
     * Given: 無快照列；When: GET /pnl-snapshots；Then: 200 + JSON 陣列。
     */
    @Test
    void JOB_API_005_listPnlSnapshots_returnsArray() throws Exception {
        mockMvc.perform(get("/api/v1/pnl-snapshots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    /**
     * CASE-JOB-API-006：失敗指令列表可回空陣列。
     * Given: 無失敗指令；When: GET /failed-commands；Then: 200 + JSON 陣列。
     */
    @Test
    void JOB_API_006_listFailedCommands_returnsArray() throws Exception {
        mockMvc.perform(get("/api/v1/failed-commands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    /**
     * CASE-JOB-API-007：無效失敗指令狀態回 400。
     * Given: status 非整數列舉；When: GET /failed-commands?status=NOT_A_STATUS；Then: 400。
     */
    @Test
    void JOB_API_007_listFailedCommands_invalidStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/failed-commands").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest());
    }
}
