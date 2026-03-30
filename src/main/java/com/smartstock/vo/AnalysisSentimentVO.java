package com.smartstock.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnalysisSentimentVO {

    private String stockCode;
    private String stockName;
    private Integer sentimentScore;
    private String sentimentLabel;
    private String trendPrediction;
    private String operationAdvice;
    private String analysisSummary;
    private LocalDateTime createdAt;
}
