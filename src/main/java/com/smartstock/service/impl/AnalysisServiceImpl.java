package com.smartstock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstock.common.BusinessException;
import com.smartstock.common.ErrorCode;
import com.smartstock.config.AnalysisServiceProperties;
import com.smartstock.dto.StockAnalysisRequestDTO;
import com.smartstock.entity.AiAnalysis;
import com.smartstock.mapper.AiAnalysisMapper;
import com.smartstock.service.AnalysisService;
import com.smartstock.vo.AnalysisHistoryVO;
import com.smartstock.vo.AnalysisRiskVO;
import com.smartstock.vo.AnalysisSentimentVO;
import com.smartstock.vo.PageVO;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final WebClient.Builder webClientBuilder;
    private final AnalysisServiceProperties properties;
    private final ObjectMapper objectMapper;
    private final AiAnalysisMapper aiAnalysisMapper;

    private static final String ANALYSIS_TYPE_MARKET = "market";

    @Override
    public JsonNode healthCheck() {
        return buildWebClient()
                .get()
                .uri("/api/health")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(properties.getTimeout());
    }

    @Override
    public JsonNode analyzeStock(Long userId, StockAnalysisRequestDTO request) {
        try {
            JsonNode response = buildWebClient()
                    .post()
                    .uri("/api/v1/analysis/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "stock_code", request.getStockCode().trim(),
                            "report_type", request.getReportType(),
                            "force_refresh", request.isForceRefresh(),
                            "async_mode", false,
                            "notify", false
                    ))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(properties.getTimeout());
            persistAnalysis(userId, request, response);
            return response;
        } catch (WebClientResponseException e) {
            throw new BusinessException(resolveErrorCode(e), resolveErrorMessage(e));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "分析服务调用失败: " + e.getMessage());
        }
    }

    @Override
    public PageVO<AnalysisHistoryVO> getAnalysisHistory(Long userId, String stockCode, int page, int pageSize) {
        Page<AiAnalysis> pageResult = aiAnalysisMapper.selectPage(
                new Page<>(page, pageSize),
                buildHistoryQuery(userId, stockCode)
        );
        List<AnalysisHistoryVO> records = new ArrayList<>();
        for (AiAnalysis analysis : pageResult.getRecords()) {
            records.add(toHistoryVO(analysis));
        }
        return new PageVO<>(pageResult.getTotal(), page, pageSize, records);
    }

    @Override
    public AnalysisSentimentVO getSentiment(Long userId, String stockCode) {
        JsonNode analysis = getLatestOrCreateAnalysis(userId, stockCode);
        AnalysisSentimentVO vo = new AnalysisSentimentVO();
        vo.setStockCode(textValue(analysis, "/stock_code"));
        vo.setStockName(textValue(analysis, "/stock_name"));
        vo.setSentimentScore(intValue(analysis, "/report/summary/sentiment_score"));
        vo.setSentimentLabel(textValue(analysis, "/report/summary/sentiment_label"));
        vo.setTrendPrediction(textValue(analysis, "/report/summary/trend_prediction"));
        vo.setOperationAdvice(textValue(analysis, "/report/summary/operation_advice"));
        vo.setAnalysisSummary(textValue(analysis, "/report/summary/analysis_summary"));
        vo.setCreatedAt(dateTimeValue(analysis, "/report/meta/created_at"));
        return vo;
    }

    @Override
    public AnalysisRiskVO getRisk(Long userId, String stockCode) {
        JsonNode analysis = getLatestAnalysis(userId, stockCode);
        AnalysisRiskVO vo = new AnalysisRiskVO();
        vo.setStockCode(textValue(analysis, "/stock_code"));
        vo.setStockName(textValue(analysis, "/stock_name"));
        vo.setRiskWarning(firstNonBlank(
                textValue(analysis, "/report/details/raw_result/risk_warning"),
                textValue(analysis, "/report/details/raw_result/fundamental_analysis")
        ));
        vo.setLatestNews(firstNonBlank(
                textValue(analysis, "/report/details/news_content"),
                textValue(analysis, "/report/details/raw_result/news_summary")
        ));
        vo.setRiskAlerts(listValue(analysis, "/report/details/context_snapshot/enhanced_context/trend_analysis/risk_factors"));
        vo.setCreatedAt(dateTimeValue(analysis, "/report/meta/created_at"));
        return vo;
    }

    @Override
    public JsonNode getLatestAnalysis(Long userId, String stockCode) {
        return getLatestOrCreateAnalysis(userId, stockCode);
    }

    private WebClient buildWebClient() {
        Duration timeout = properties.getTimeout();
        HttpClient httpClient = HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(timeout)
                .doOnConnected((connection) -> connection
                        .addHandlerLast(new ReadTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS)));

        return webClientBuilder.clone()
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private int resolveErrorCode(WebClientResponseException e) {
        if (e.getStatusCode().is4xxClientError()) {
            return ErrorCode.BAD_REQUEST;
        }
        return ErrorCode.INTERNAL_ERROR;
    }

    private String resolveErrorMessage(WebClientResponseException e) {
        String responseBody = e.getResponseBodyAsString();
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JsonNode body = objectMapper.readTree(responseBody);
                if (body.hasNonNull("message")) {
                    return body.get("message").asText();
                }
                JsonNode detail = body.get("detail");
                if (detail != null) {
                    if (detail.isTextual()) {
                        return detail.asText();
                    }
                    if (detail.hasNonNull("message")) {
                        return detail.get("message").asText();
                    }
                }
            } catch (Exception ignored) {
                // Ignore parse errors and fall back to status text.
            }
        }
        return "分析服务调用失败: " + e.getStatusCode().value();
    }

    private void persistAnalysis(Long userId, StockAnalysisRequestDTO request, JsonNode response) throws Exception {
        aiAnalysisMapper.insert(AiAnalysis.builder()
                .userId(userId)
                .stockCode(request.getStockCode().trim())
                .analysisType(ANALYSIS_TYPE_MARKET)
                .inputData(objectMapper.writeValueAsString(request))
                .outputText(objectMapper.writeValueAsString(response))
                .build());
    }

    private LambdaQueryWrapper<AiAnalysis> buildHistoryQuery(Long userId, String stockCode) {
        LambdaQueryWrapper<AiAnalysis> wrapper = new LambdaQueryWrapper<AiAnalysis>()
                .eq(AiAnalysis::getUserId, userId)
                .eq(AiAnalysis::getAnalysisType, ANALYSIS_TYPE_MARKET);
        if (StringUtils.hasText(stockCode)) {
            wrapper.eq(AiAnalysis::getStockCode, stockCode.trim());
        }
        return wrapper.orderByDesc(AiAnalysis::getCreatedAt).orderByDesc(AiAnalysis::getId);
    }

    private JsonNode getLatestOrCreateAnalysis(Long userId, String stockCode) {
        String normalizedStockCode = normalizeStockCode(stockCode);
        AiAnalysis latest = aiAnalysisMapper.selectOne(buildHistoryQuery(userId, normalizedStockCode).last("limit 1"));
        if (latest != null && StringUtils.hasText(latest.getOutputText())) {
            return parseStoredOutput(latest.getOutputText());
        }
        StockAnalysisRequestDTO request = new StockAnalysisRequestDTO();
        request.setStockCode(normalizedStockCode);
        return analyzeStock(userId, request);
    }

    private JsonNode parseStoredOutput(String outputText) {
        try {
            return objectMapper.readTree(outputText);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "分析记录解析失败");
        }
    }

    private AnalysisHistoryVO toHistoryVO(AiAnalysis analysis) {
        JsonNode output = parseStoredOutput(analysis.getOutputText());
        AnalysisHistoryVO vo = new AnalysisHistoryVO();
        vo.setId(analysis.getId());
        vo.setStockCode(analysis.getStockCode());
        vo.setStockName(textValue(output, "/stock_name"));
        vo.setAnalysisType(analysis.getAnalysisType());
        vo.setOperationAdvice(textValue(output, "/report/summary/operation_advice"));
        vo.setTrendPrediction(textValue(output, "/report/summary/trend_prediction"));
        vo.setSentimentScore(intValue(output, "/report/summary/sentiment_score"));
        vo.setAnalysisSummary(textValue(output, "/report/summary/analysis_summary"));
        LocalDateTime createdAt = dateTimeValue(output, "/report/meta/created_at");
        if (createdAt == null) {
            createdAt = dateTimeValue(output, "/created_at");
        }
        vo.setCreatedAt(createdAt != null ? createdAt : analysis.getCreatedAt());
        return vo;
    }

    private String normalizeStockCode(String stockCode) {
        if (!StringUtils.hasText(stockCode)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "stockCode 不能为空");
        }
        return stockCode.trim();
    }

    private String textValue(JsonNode node, String pointer) {
        JsonNode value = node.at(pointer);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private Integer intValue(JsonNode node, String pointer) {
        JsonNode value = node.at(pointer);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.isNumber() ? value.asInt() : null;
    }

    private LocalDateTime dateTimeValue(JsonNode node, String pointer) {
        String value = textValue(node, pointer);
        return value == null ? null : LocalDateTime.parse(value);
    }

    private List<String> listValue(JsonNode node, String pointer) {
        JsonNode value = node.at(pointer);
        if (value == null || !value.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : value) {
            if (item != null && !item.isNull() && item.isValueNode()) {
                String text = item.asText();
                if (text != null && !text.isBlank()) {
                    result.add(text);
                }
            }
        }
        return result;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
