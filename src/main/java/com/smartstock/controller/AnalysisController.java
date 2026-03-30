package com.smartstock.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartstock.common.Result;
import com.smartstock.dto.StockAnalysisRequestDTO;
import com.smartstock.service.AnalysisService;
import com.smartstock.util.UserContext;
import com.smartstock.vo.AnalysisHistoryVO;
import com.smartstock.vo.AnalysisRiskVO;
import com.smartstock.vo.AnalysisSentimentVO;
import com.smartstock.vo.PageVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Tag(name = "Analysis", description = "AI 股票分析接口")
public class AnalysisController {

    private final AnalysisService analysisService;

    @GetMapping("/health")
    public Result<JsonNode> healthCheck() {
        return Result.ok(analysisService.healthCheck());
    }

    @GetMapping("/history")
    public Result<PageVO<AnalysisHistoryVO>> getHistory(HttpServletRequest request,
                                                        @RequestParam(required = false) String stockCode,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = UserContext.getUserId(request);
        return Result.ok(analysisService.getAnalysisHistory(userId, stockCode, page, pageSize));
    }

    @GetMapping("/sentiment")
    public Result<AnalysisSentimentVO> getSentiment(HttpServletRequest request,
                                                    @RequestParam String stockCode) {
        Long userId = UserContext.getUserId(request);
        return Result.ok(analysisService.getSentiment(userId, stockCode));
    }

    @GetMapping("/risk")
    public Result<AnalysisRiskVO> getRisk(HttpServletRequest request,
                                          @RequestParam String stockCode) {
        Long userId = UserContext.getUserId(request);
        return Result.ok(analysisService.getRisk(userId, stockCode));
    }

    @PostMapping("/stock")
    public Result<JsonNode> analyzeStock(HttpServletRequest httpServletRequest,
                                         @Valid @RequestBody StockAnalysisRequestDTO request) {
        Long userId = UserContext.getUserId(httpServletRequest);
        return Result.ok(analysisService.analyzeStock(userId, request));
    }
}
