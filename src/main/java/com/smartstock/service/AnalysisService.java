package com.smartstock.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartstock.dto.StockAnalysisRequestDTO;
import com.smartstock.vo.AnalysisHistoryVO;
import com.smartstock.vo.AnalysisRiskVO;
import com.smartstock.vo.AnalysisSentimentVO;
import com.smartstock.vo.PageVO;

public interface AnalysisService {

    JsonNode healthCheck();

    JsonNode analyzeStock(Long userId, StockAnalysisRequestDTO request);

    PageVO<AnalysisHistoryVO> getAnalysisHistory(Long userId, String stockCode, int page, int pageSize);

    AnalysisSentimentVO getSentiment(Long userId, String stockCode);

    AnalysisRiskVO getRisk(Long userId, String stockCode);

    JsonNode getLatestAnalysis(Long userId, String stockCode);
}
