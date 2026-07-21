package com.trading.job.domain;

/**
 * 【職責】失敗指令生命週期：PENDING 可重試；SUCCEEDED／DEAD 為終態。
 * 【技巧】字串列舉，供 JOB-C 狀態機與 JOB-D 清理過濾。
 * 【概念】SUCCEEDED 含「真正成功」與「風控拒絕結案」；DEAD 表示超過重試上限，需人工介入。
 */
public enum FailedCommandStatus {
    PENDING,
    SUCCEEDED,
    DEAD
}
