package com.smartstock.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVO {

    private Long orderId;
    private String stockCode;
    private String stockName;
    private String orderType;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal amount;
    private BigDecimal fee;
    private String status;
    private BigDecimal filledPrice;
    private Integer filledQuantity;
    private LocalDateTime filledTime;
}
