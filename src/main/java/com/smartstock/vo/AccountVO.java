package com.smartstock.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountVO {

    private Long userId;
    private BigDecimal totalAssets;
    private BigDecimal availableCash;
    private BigDecimal frozenCash;
    private BigDecimal positionValue;
    private BigDecimal totalProfit;
    private BigDecimal profitRate;
}
