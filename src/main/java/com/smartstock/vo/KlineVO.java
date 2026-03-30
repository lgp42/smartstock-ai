package com.smartstock.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class KlineVO {

    private String date;
    private BigDecimal open;
    private BigDecimal close;
    private BigDecimal high;
    private BigDecimal low;
    private Long volume;
    private BigDecimal amount;
    private BigDecimal changeRate;
}
