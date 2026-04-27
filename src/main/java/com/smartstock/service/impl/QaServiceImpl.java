package com.smartstock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.smartstock.common.BusinessException;
import com.smartstock.common.ErrorCode;
import com.smartstock.config.AnalysisServiceProperties;
import com.smartstock.dto.QaAskRequestDTO;
import com.smartstock.entity.QaHistory;
import com.smartstock.mapper.QaHistoryMapper;
import com.smartstock.service.QaService;
import com.smartstock.vo.QaAnswerVO;
import com.smartstock.vo.QaHistoryVO;
import com.smartstock.vo.QaSessionDetailVO;
import com.smartstock.vo.QaSessionMessageVO;
import com.smartstock.vo.QaSessionVO;
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
import reactor.netty.http.client.HttpClient;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class QaServiceImpl implements QaService {

    private final WebClient.Builder webClientBuilder;
    private final AnalysisServiceProperties analysisServiceProperties;
    private final QaHistoryMapper qaHistoryMapper;

    private static final String SESSION_DELIMITER = "__";

    @Override
    public QaAnswerVO ask(Long userId, QaAskRequestDTO request) {
        String stockCode = normalizeStockCode(request.getStockCode());
        String question = request.getQuestion().trim();
        String sessionId = resolveSessionId(userId, stockCode, request.getSessionId());
        JsonNode response = callAgentChat(stockCode, question, sessionId);
        String answer = response.path("content").asText(null);
        String resolvedSessionId = response.path("session_id").asText(sessionId);
        if (!StringUtils.hasText(answer)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Python Agent 返回空内容");
        }
        LocalDateTime createdAt = LocalDateTime.now();
        qaHistoryMapper.insert(QaHistory.builder()
                .userId(userId)
                .question(question)
                .answer(answer)
                .createdAt(createdAt)
                .build());

        QaAnswerVO vo = new QaAnswerVO();
        vo.setSessionId(resolvedSessionId);
        vo.setStockCode(stockCode);
        vo.setQuestion(question);
        vo.setAnswer(answer);
        vo.setCreatedAt(createdAt.toString());
        return vo;
    }

    @Override
    public List<QaHistoryVO> getHistory(Long userId, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 20));
        List<QaHistory> rows = qaHistoryMapper.selectList(new LambdaQueryWrapper<QaHistory>()
                .eq(QaHistory::getUserId, userId)
                .orderByDesc(QaHistory::getCreatedAt)
                .orderByDesc(QaHistory::getId)
                .last("limit " + normalizedLimit));
        List<QaHistoryVO> result = new ArrayList<>();
        for (QaHistory row : rows) {
            QaHistoryVO vo = new QaHistoryVO();
            vo.setId(row.getId());
            vo.setQuestion(row.getQuestion());
            vo.setAnswer(row.getAnswer());
            vo.setCreatedAt(row.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<QaSessionVO> getSessions(Long userId, String stockCode, int limit) {
        String normalizedStockCode = StringUtils.hasText(stockCode) ? stockCode.trim() : null;
        int normalizedLimit = Math.max(1, Math.min(limit, 50));
        String userPrefix = buildUserPrefix(userId);
        JsonNode response = buildWebClient()
                .get()
                .uri((uriBuilder) -> uriBuilder
                        .path("/api/v1/agent/chat/sessions")
                        .queryParam("limit", Math.max(normalizedLimit * 5, 50))
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(analysisServiceProperties.getTimeout());

        List<QaSessionVO> result = new ArrayList<>();
        JsonNode sessions = response.path("sessions");
        if (!sessions.isArray()) {
            return result;
        }
        for (JsonNode session : sessions) {
            String sessionId = session.path("session_id").asText(null);
            if (!StringUtils.hasText(sessionId) || !sessionId.startsWith(userPrefix)) {
                continue;
            }
            String sessionStockCode = extractStockCode(sessionId);
            if (normalizedStockCode != null && !normalizedStockCode.equalsIgnoreCase(sessionStockCode)) {
                continue;
            }
            QaSessionVO vo = new QaSessionVO();
            vo.setSessionId(sessionId);
            vo.setStockCode(sessionStockCode);
            vo.setTitle(session.path("title").asText("未命名会话"));
            vo.setMessageCount(session.path("message_count").asInt(0));
            vo.setCreatedAt(session.path("created_at").asText(null));
            vo.setLastActive(session.path("last_active").asText(null));
            result.add(vo);
            if (result.size() >= normalizedLimit) {
                break;
            }
        }
        return result;
    }

    @Override
    public QaSessionDetailVO getSessionDetail(Long userId, String sessionId, int limit) {
        String normalizedSessionId = normalizeSessionId(userId, sessionId);
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        JsonNode response = buildWebClient()
                .get()
                .uri((uriBuilder) -> uriBuilder
                        .path("/api/v1/agent/chat/sessions/{sessionId}")
                        .queryParam("limit", normalizedLimit)
                        .build(normalizedSessionId))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(analysisServiceProperties.getTimeout());

        List<QaSessionMessageVO> messages = new ArrayList<>();
        JsonNode rows = response.path("messages");
        if (rows.isArray()) {
            for (JsonNode row : rows) {
                QaSessionMessageVO message = new QaSessionMessageVO();
                message.setId(row.path("id").asText());
                message.setRole(row.path("role").asText());
                message.setContent(row.path("content").asText());
                message.setCreatedAt(row.path("created_at").asText(null));
                messages.add(message);
            }
        }
        QaSessionDetailVO detail = new QaSessionDetailVO();
        detail.setSessionId(response.path("session_id").asText(normalizedSessionId));
        detail.setMessages(messages);
        return detail;
    }

    private JsonNode callAgentChat(String stockCode, String question, String sessionId) {
        JsonNode response = buildWebClient()
                .post()
                .uri("/api/v1/agent/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "message", question,
                        "session_id", sessionId,
                        "context", Map.of("stock_code", stockCode)
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(analysisServiceProperties.getTimeout());
        if (!response.path("success").asBoolean(false)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, response.path("error").asText("Python Agent 调用失败"));
        }
        return response;
    }

    private WebClient buildWebClient() {
        Duration timeout = analysisServiceProperties.getTimeout();
        HttpClient httpClient = HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(timeout)
                .doOnConnected((connection) -> connection
                        .addHandlerLast(new ReadTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout.toSeconds(), TimeUnit.SECONDS)));

        return webClientBuilder.clone()
                .baseUrl(analysisServiceProperties.getBaseUrl())
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private String normalizeStockCode(String stockCode) {
        if (!StringUtils.hasText(stockCode)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "stockCode 不能为空");
        }
        return stockCode.trim();
    }

    private String resolveSessionId(Long userId, String stockCode, String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return buildSessionId(userId, stockCode);
        }
        String normalizedSessionId = normalizeSessionId(userId, sessionId);
        String sessionStockCode = extractStockCode(normalizedSessionId);
        if (StringUtils.hasText(sessionStockCode) && !stockCode.equalsIgnoreCase(sessionStockCode)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "sessionId 与当前股票不匹配");
        }
        return normalizedSessionId;
    }

    private String normalizeSessionId(Long userId, String sessionId) {
        String normalizedSessionId = sessionId.trim();
        if (!normalizedSessionId.startsWith(buildUserPrefix(userId))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "sessionId 不属于当前用户");
        }
        return normalizedSessionId;
    }

    private String buildSessionId(Long userId, String stockCode) {
        return buildUserPrefix(userId) + stockCode + SESSION_DELIMITER + UUID.randomUUID();
    }

    private String buildUserPrefix(Long userId) {
        return "web" + SESSION_DELIMITER + userId + SESSION_DELIMITER;
    }

    private String extractStockCode(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        String[] segments = sessionId.split(SESSION_DELIMITER, 4);
        if (segments.length < 4) {
            return null;
        }
        return segments[2];
    }
}
