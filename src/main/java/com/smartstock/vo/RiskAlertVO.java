package com.smartstock.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RiskAlertVO {

    private String alertType;
    private String alertLevel;
    private String stockCode;
    private String message;
    private BigDecimal value;
}
