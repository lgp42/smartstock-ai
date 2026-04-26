package com.smartstock.config;

import com.smartstock.service.MarketService;
import com.smartstock.vo.StockDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketWebSocketHandlerTest {

    private MarketService marketService;
    private MarketWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        marketService = mock(MarketService.class);
        handler = new MarketWebSocketHandler(marketService);
    }

    @Test
    void handleMessageShouldSendStockSnapshotJson() throws Exception {
        StockDetailVO detail = new StockDetailVO();
        detail.setStockCode("000001");
        detail.setStockName("平安银行");
        detail.setCurrentPrice(new BigDecimal("10.50"));
        when(marketService.getStockDetail("000001")).thenReturn(detail);

        WebSocketSession session = mock(WebSocketSession.class);
        handler.handleMessage(session, new TextMessage("000001"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        String payload = captor.getValue().getPayload();
        assertTrue(payload.contains("\"stockCode\":\"000001\""));
        assertTrue(payload.contains("\"currentPrice\":10.50"));
    }
}
