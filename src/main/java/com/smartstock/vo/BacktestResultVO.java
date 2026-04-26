package com.smartstock.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BacktestResultVO {

    private String stockCode;
    private String strategyType;
    private BigDecimal initialCapital;
    private BigDecimal finalCapital;
    private BigDecimal totalReturn;
    private BigDecimal returnRate;
    private BigDecimal maxDrawdown;
    private Integer tradeCount;
}
