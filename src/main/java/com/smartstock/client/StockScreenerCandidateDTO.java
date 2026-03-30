package com.smartstock.client;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockScreenerCandidateDTO {

    private String stockCode;
    private String stockName;
    private String market;
    private String board;
    private String industry;
    private Integer isSt;
    private Integer isDelisted;
    private BigDecimal currentPrice;
    private BigDecimal changeRate;
    private BigDecimal turnoverRate;
    private BigDecimal pe;
    private BigDecimal totalMarketCap;
}
