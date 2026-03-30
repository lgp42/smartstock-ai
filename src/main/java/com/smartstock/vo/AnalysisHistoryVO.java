package com.smartstock.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnalysisHistoryVO {

    private Long id;
    private String stockCode;
    private String stockName;
    private String analysisType;
    private String operationAdvice;
    private String trendPrediction;
    private Integer sentimentScore;
    private String analysisSummary;
    private LocalDateTime createdAt;
}
