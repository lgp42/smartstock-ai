package com.smartstock.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PositionVO {

    private String stockCode;
    private String stockName;
    private Integer quantity;
    private Integer availableQuantity;
    private BigDecimal costPrice;
    private BigDecimal currentPrice;
    private BigDecimal marketValue;
    private BigDecimal profit;
    private BigDecimal profitRate;
}
