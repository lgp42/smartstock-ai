package com.smartstock.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AnalysisRiskVO {

    private String stockCode;
    private String stockName;
    private String riskWarning;
    private String latestNews;
    private List<String> riskAlerts;
    private LocalDateTime createdAt;
}
