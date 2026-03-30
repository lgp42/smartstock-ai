package com.smartstock.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockDetailVO {

    private String stockCode;
    private String stockName;
    private String market;
    private String board;
    private String industry;
    private Boolean st;
    private Boolean delisted;
    private BigDecimal currentPrice;
    private BigDecimal changeRate;
    private BigDecimal changeAmount;
    private Long volume;
    private BigDecimal amount;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal open;
    private BigDecimal close;
    private BigDecimal preClose;
}
