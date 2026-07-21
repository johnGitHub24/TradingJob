package com.trading.job.application;

/**
 * 【職責】表示 Engine 風控終態拒絕：JOB-C 應結案、不再重試。
 * 【技巧】自訂 {@link RuntimeException}，攜帶 errorCode／ruleCode 供寫入 failureReason。
 * 【概念】與「網路／5xx 暫態失敗」分開：後者要退避重試，前者重試也只會再被拒。
 * 【邊界】不負責 HTTP 狀態碼對應（由 client 在收到 422 時拋出本例外）。
 */
public class RiskRejectedException extends RuntimeException {

    private final String errorCode;
    private final String ruleCode;

    /**
     * 【職責】建立風控拒絕例外。
     * 【技巧】detail 進 {@code super(detail)} 作為訊息；代碼另存欄位。
     * 【概念】訊息給人看，errorCode 給程式／報表對帳。
     *
     * @param errorCode 錯誤代碼（如 RISK_POSITION_LIMIT）
     * @param ruleCode  觸發的風控規則代碼，可為 null
     * @param detail    人類可讀說明
     */
    public RiskRejectedException(String errorCode, String ruleCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
        this.ruleCode = ruleCode;
    }

    /** Engine 回傳的錯誤代碼。 */
    public String getErrorCode() {
        return errorCode;
    }

    /** 觸發的風控規則代碼，可能為 null。 */
    public String getRuleCode() {
        return ruleCode;
    }
}
