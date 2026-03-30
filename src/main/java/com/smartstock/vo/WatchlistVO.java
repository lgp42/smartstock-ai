package com.smartstock.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WatchlistVO {

    private String stockCode;
    private String stockName;
    private String market;
    private BigDecimal currentPrice;
    private BigDecimal changeRate;
    private Integer sortOrder;
}
