package com.smartstock.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScreenerResultVO {

    private String stockCode;
    private String stockName;
    private String market;
    private String board;
    private String industry;
    private Boolean st;
    private Boolean delisted;
    private BigDecimal currentPrice;
    private BigDecimal changeRate;
    private BigDecimal turnoverRate;
    private BigDecimal pe;
    private BigDecimal totalMarketCap;
}
