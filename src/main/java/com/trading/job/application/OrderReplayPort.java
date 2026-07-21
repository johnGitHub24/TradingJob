package com.trading.job.application;

/**
 * 【職責】JOB-C 重試時將失敗指令重新下單的埠（Port）。
 * 【技巧】介面隔離：業務依賴抽象，實作可換（REST／進程內）。
 * 【概念】這是六角架構的「出站埠」：Service 不直接 new RestClient，測試才能用假實作隔離 HTTP。
 * 【邊界】不規定傳輸協定；風控拒絕語意由實作轉成 {@link RiskRejectedException}。
 */
public interface OrderReplayPort {

    /**
     * 【職責】重放下單；成功則正常返回。
     * 【技巧】契約：風控終態拒絕必須拋 {@link RiskRejectedException}，其餘失敗拋一般例外。
     * 【概念】用例外類型區分「不要再試」與「稍後再試」，比用回傳碼散落 if-else 更集中。
     *
     * @param request       下單內容
     * @param clientOrderId 冪等鍵／客戶端訂單號
     * @throws RiskRejectedException 風控終態拒絕（呼叫端視為不再重試）
     */
    void placeOrder(com.trading.job.dto.CreateOrderRequest request, String clientOrderId);
}
