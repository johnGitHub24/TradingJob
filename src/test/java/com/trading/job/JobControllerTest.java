package com.trading.job;

import com.trading.job.api.JobController;
import com.trading.job.application.DataCleanupService;
import com.trading.job.application.FailedCommandService;
import com.trading.job.application.PnlSnapshotService;
import com.trading.job.application.StaleOrderCancellationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 【職責】{@link JobController} WebMvc 單元測試：手動 Job API 與查詢端點的 HTTP 契約。
 * 【技巧】{@code @WebMvcTest} + {@code @MockBean}；套件不放 {@code /api/}，讓配對掃描視為單元層。
 * 【概念】切片測試只起 Web 層；整合層 {@code JobApiIntegrationTest} 再用真實 Service＋H2 對同一 Case ID。
 * 【技巧驗證】JOB-A~D Happy；無效 status 查詢 400。
 */
@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StaleOrderCancellationService staleOrderCancellationService;

    @MockBean
    private PnlSnapshotService pnlSnapshotService;

    @MockBean
    private FailedCommandService failedCommandService;

    @MockBean
    private DataCleanupService dataCleanupService;

    /**
     * CASE-JOB-API-001：JOB-A 手動執行回傳影響筆數。
     * Given: Service 回傳取消 3 筆；When: POST /jobs/stale-order-cancellation；Then: 200 + job=JOB-A + affected=3。
     */
    @Test
    void JOB_API_001_runStaleOrderCancellation_returnsAffectedCount() throws Exception {
        when(staleOrderCancellationService.cancelStaleOrders()).thenReturn(3);

        mockMvc.perform(post("/api/v1/jobs/stale-order-cancellation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job").value("JOB-A"))
                .andExpect(jsonPath("$.affected").value(3));
    }

    /**
     * CASE-JOB-API-002：JOB-B 手動執行回傳寫入筆數。
     * Given: Service 回傳寫入 2 筆；When: POST /jobs/pnl-snapshot；Then: 200 + job=JOB-B + affected=2。
     */
    @Test
    void JOB_API_002_runPnlSnapshot_returnsAffectedCount() throws Exception {
        when(pnlSnapshotService.captureSnapshot()).thenReturn(2);

        mockMvc.perform(post("/api/v1/jobs/pnl-snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job").value("JOB-B"))
                .andExpect(jsonPath("$.affected").value(2));
    }

    /**
     * CASE-JOB-API-003：JOB-C 手動執行回傳成功重放筆數。
     * Given: Service 回傳成功 1 筆；When: POST /jobs/failed-command-retry；Then: 200 + job=JOB-C + affected=1。
     */
    @Test
    void JOB_API_003_runFailedCommandRetry_returnsAffectedCount() throws Exception {
        when(failedCommandService.retryFailedCommands()).thenReturn(1);

        mockMvc.perform(post("/api/v1/jobs/failed-command-retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job").value("JOB-C"))
                .andExpect(jsonPath("$.affected").value(1));
    }

    /**
     * CASE-JOB-API-004：JOB-D 手動執行彙總刪除筆數與 detail。
     * Given: CleanupResult(5,2)；When: POST /jobs/cleanup；Then: affected=7 且 detail 含 events/failedCommands。
     */
    @Test
    void JOB_API_004_runCleanup_returnsTotalAndDetail() throws Exception {
        when(dataCleanupService.cleanup())
                .thenReturn(new DataCleanupService.CleanupResult(5, 2));

        mockMvc.perform(post("/api/v1/jobs/cleanup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job").value("JOB-D"))
                .andExpect(jsonPath("$.affected").value(7))
                .andExpect(jsonPath("$.detail").value("events=5, failedCommands=2"));
    }

    /**
     * CASE-JOB-API-005：PnL 快照列表可回空陣列。
     * Given: Service 回空；When: GET /pnl-snapshots；Then: 200 + JSON 陣列。
     */
    @Test
    void JOB_API_005_listPnlSnapshots_returnsEmptyList() throws Exception {
        when(pnlSnapshotService.findByDate(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pnl-snapshots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    /**
     * CASE-JOB-API-006：失敗指令列表可回空陣列。
     * Given: Service 回空；When: GET /failed-commands；Then: 200 + JSON 陣列。
     */
    @Test
    void JOB_API_006_listFailedCommands_returnsEmptyList() throws Exception {
        when(failedCommandService.findByStatus(null)).thenReturn(List.of());

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
