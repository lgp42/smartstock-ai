package com.smartstock.controller;

import com.smartstock.common.GlobalExceptionHandler;
import com.smartstock.service.MarketService;
import com.smartstock.service.TradeService;
import com.smartstock.util.UserContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QueryParamValidationTest {

    private static LocalValidatorFactoryBean validator;

    @BeforeAll
    static void setUpValidator() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
    }

    @AfterAll
    static void closeValidator() {
        validator.close();
    }

    @Test
    void searchStocksShouldRejectBlankKeyword() throws Exception {
        MarketService marketService = Mockito.mock(MarketService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MarketController(marketService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        mockMvc.perform(get("/api/market/stocks/search")
                        .param("keyword", " ")
                        .param("limit", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("搜索关键词不能为空"));
    }

    @Test
    void klineShouldRejectUnsupportedPeriod() throws Exception {
        MarketService marketService = Mockito.mock(MarketService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MarketController(marketService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        mockMvc.perform(get("/api/market/stocks/000001/kline")
                        .param("period", "year")
                        .param("limit", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("period 仅支持 1s/1min/5min/15min/30min/60min/day/week/month"));
    }

    @Test
    void klineShouldAllowOneMinutePeriod() throws Exception {
        MarketService marketService = Mockito.mock(MarketService.class);
        Mockito.when(marketService.getKlineData("000001", "1min", 10)).thenReturn(java.util.List.of());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MarketController(marketService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        mockMvc.perform(get("/api/market/stocks/000001/kline")
                        .param("period", "1min")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void klineShouldAllowOneSecondPeriod() throws Exception {
        MarketService marketService = Mockito.mock(MarketService.class);
        Mockito.when(marketService.getKlineData("000001", "1s", 10)).thenReturn(java.util.List.of());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MarketController(marketService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        mockMvc.perform(get("/api/market/stocks/000001/kline")
                        .param("period", "1s")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void indicatorsShouldRejectNonPositiveLimit() throws Exception {
        MarketService marketService = Mockito.mock(MarketService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MarketController(marketService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        mockMvc.perform(get("/api/market/stocks/000001/indicators")
                        .param("indicators", "macd")
                        .param("period", "day")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("limit 最小为 1"));
    }

    @Test
    void flashNewsPageShouldRejectNonPositivePage() throws Exception {
        com.smartstock.service.NewsService newsService = Mockito.mock(com.smartstock.service.NewsService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new NewsController(newsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        mockMvc.perform(get("/api/news/flash/page")
                        .param("page", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("page 最小为 1"));
    }

    @Test
    void tradeRecordsShouldRejectInvalidTradeType() throws Exception {
        TradeService tradeService = Mockito.mock(TradeService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TradeController(tradeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        mockMvc.perform(get("/api/trade/records")
                        .requestAttr(UserContext.REQUEST_ATTR_USER_ID, 1L)
                        .param("tradeType", "hold")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("tradeType 仅支持 buy 或 sell"));
    }

    @Test
    void tradeRecordsShouldRejectPageSizeOverLimit() throws Exception {
        TradeService tradeService = Mockito.mock(TradeService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TradeController(tradeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        mockMvc.perform(get("/api/trade/records")
                        .requestAttr(UserContext.REQUEST_ATTR_USER_ID, 1L)
                        .param("page", "1")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("pageSize 最大为 100"));
    }

    @Test
    void screenerShouldRejectUnsupportedTechnicalPattern() throws Exception {
        MarketService marketService = Mockito.mock(MarketService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MarketController(marketService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        mockMvc.perform(get("/api/market/screener")
                        .param("technicalPattern", "rocket"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("technicalPattern 仅支持 none/ma_cross/macd_golden/volume_up/breakout/near_high/low_vol_pullback"));
    }
}
