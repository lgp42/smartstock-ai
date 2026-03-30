package com.smartstock.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TradeRecordVO {

    private Long recordId;
    private String stockCode;
    private String stockName;
    private String tradeType;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal amount;
    private BigDecimal fee;
    private LocalDateTime tradeTime;
}
