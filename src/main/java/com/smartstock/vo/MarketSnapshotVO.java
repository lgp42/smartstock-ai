package com.smartstock.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MarketSnapshotVO {

    private String stockCode;
    private String stockName;
    private BigDecimal currentPrice;
    private BigDecimal changeRate;
}
