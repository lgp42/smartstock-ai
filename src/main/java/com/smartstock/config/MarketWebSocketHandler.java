package com.smartstock.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstock.service.MarketService;
import com.smartstock.vo.StockDetailVO;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class MarketWebSocketHandler extends TextWebSocketHandler {

    private final MarketService marketService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "market-websocket-push");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, ScheduledFuture<?>> subscriptions = new ConcurrentHashMap<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String stockCode = message.getPayload().trim();
        cancelSubscription(session);
        sendSnapshot(session, stockCode);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> sendSnapshotQuietly(session, stockCode),
                5, 5, TimeUnit.SECONDS);
        subscriptions.put(sessionKey(session), future);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cancelSubscription(session);
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdownNow();
    }

    private void sendSnapshotQuietly(WebSocketSession session, String stockCode) {
        try {
            if (session.isOpen()) {
                sendSnapshot(session, stockCode);
            } else {
                cancelSubscription(session);
            }
        } catch (Exception e) {
            cancelSubscription(session);
        }
    }

    private void sendSnapshot(WebSocketSession session, String stockCode) throws Exception {
        StockDetailVO detail = marketService.getStockDetail(stockCode);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(detail)));
    }

    private void cancelSubscription(WebSocketSession session) {
        ScheduledFuture<?> existing = subscriptions.remove(sessionKey(session));
        if (existing != null) {
            existing.cancel(true);
        }
    }

    private String sessionKey(WebSocketSession session) {
        return session.getId() != null ? session.getId() : String.valueOf(System.identityHashCode(session));
    }
}
