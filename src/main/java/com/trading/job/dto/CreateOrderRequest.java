package com.trading.job.dto;

import com.trading.job.domain.OrderSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 【職責】重放下單請求體：JOB-C 經 {@link com.trading.job.application.OrderReplayPort} 送往 Engine。
 * 【技巧】Jakarta Validation（{@code @NotBlank}/{@code @NotNull}）對齊 Engine 契約欄位。
 * 【概念】這是出站 DTO，不是對外公開的 Job API body；欄位語意與 Engine 下單一致以便冪等重放。
 */
@Data
public class CreateOrderRequest {

    private String clientOrderId;

    @NotBlank(message = "symbol must not be blank")
    private String symbol;

    @NotNull(message = "side must not be null")
    private OrderSide side;

    @NotNull(message = "quantity must not be null")
    private BigDecimal quantity;

    @NotNull(message = "price must not be null")
    private BigDecimal price;
}
