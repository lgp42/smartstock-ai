package com.smartstock.client;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockRealtimeDTO {

    private String stockCode;
    private String stockName;
    private BigDecimal currentPrice;
    private BigDecimal changeRate;
    private BigDecimal changeAmount;
    private Long volume;
    private BigDecimal amount;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal preClose;
    private BigDecimal open;
    private BigDecimal pe;
    private BigDecimal turnoverRate;
    private BigDecimal totalMarketCap;
    private BigDecimal circulatingMarketCap;
    private String industry;
    private LocalDateTime quoteTime;
}
