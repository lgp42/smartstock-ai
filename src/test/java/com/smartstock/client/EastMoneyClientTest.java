package com.smartstock.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstock.vo.KlineVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EastMoneyClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private RemoteHttpClient remoteHttpClient;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private EastMoneyClient eastMoneyClient;

    @BeforeEach
    void setUp() {
        eastMoneyClient = new EastMoneyClient(remoteHttpClient, stringRedisTemplate, objectMapper);
        ReflectionTestUtils.setField(eastMoneyClient, "hisUrl", "https://push2his.eastmoney.com");
        ReflectionTestUtils.setField(eastMoneyClient, "clock", Clock.fixed(
                Instant.parse("2026-03-19T08:30:00Z"), ZoneId.of("Asia/Shanghai")));
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getKlineDataShouldTrimCachedResultToRequestedLimit() throws Exception {
        when(valueOperations.get("stock:realtime:v2:0:000001"))
                .thenReturn(objectMapper.writeValueAsString(new StockRealtimeDTO()));
        List<KlineVO> cachedKlines = List.of(
                kline("2026-03-13", "10.00"),
                kline("2026-03-14", "11.00"),
                kline("2026-03-17", "12.00")
        );
        when(valueOperations.get("stock:kline:000001:day"))
                .thenReturn(objectMapper.writeValueAsString(cachedKlines));

        List<KlineVO> result = eastMoneyClient.getKlineData("000001", "0", "day", 2);

        assertEquals(2, result.size());
        assertEquals("2026-03-14", result.get(0).getDate());
        assertEquals("2026-03-17", result.get(1).getDate());
        verify(remoteHttpClient, never()).get(anyString());
    }

    @Test
    void getKlineDataShouldTrimRemoteResultToRequestedLimit() {
        when(valueOperations.get("stock:realtime:v2:0:000001")).thenReturn("{}");
        String response = """
                callback({"data":{"klines":[
                "2026-03-13,10.00,10.10,10.20,9.90,100,1000.00,1.00",
                "2026-03-14,11.00,11.10,11.20,10.90,200,2000.00,1.10",
                "2026-03-17,12.00,12.10,12.20,11.90,300,3000.00,1.20"
                ]}})
                """;
        when(valueOperations.get("stock:kline:000001:day")).thenReturn(null);
        when(remoteHttpClient.get(anyString())).thenReturn(response);

        List<KlineVO> result = eastMoneyClient.getKlineData("000001", "0", "day", 2);

        assertEquals(2, result.size());
        assertEquals("2026-03-14", result.get(0).getDate());
        assertEquals("2026-03-17", result.get(1).getDate());
    }

    @Test
    void getKlineDataShouldMapOneMinutePeriodToEastMoneyCode() {
        when(valueOperations.get("stock:realtime:v2:0:000001")).thenReturn("{}");
        String response = """
                callback({"data":{"klines":[
                "2026-03-18 09:30,10.00,10.10,10.20,9.90,100,1000.00,1.00"
                ]}})
                """;
        when(valueOperations.get("stock:kline:000001:1min")).thenReturn(null);
        when(remoteHttpClient.get(contains("klt=1"))).thenReturn(response);

        List<KlineVO> result = eastMoneyClient.getKlineData("000001", "0", "1min", 1);

        assertEquals(1, result.size());
        assertEquals("2026-03-18 09:30", result.get(0).getDate());
        verify(remoteHttpClient, times(1)).get(contains("klt=1"));
    }

    @Test
    void getKlineDataShouldBuildOneSecondBarsFromRealtimeSnapshots() throws Exception {
        StockRealtimeDTO first = new StockRealtimeDTO();
        first.setCurrentPrice(new BigDecimal("10.01"));
        first.setChangeRate(new BigDecimal("0.10"));
        first.setVolume(1_000L);
        first.setAmount(new BigDecimal("100000.00"));
        first.setQuoteTime(LocalDateTime.of(2026, 3, 19, 10, 2, 1));

        StockRealtimeDTO second = new StockRealtimeDTO();
        second.setCurrentPrice(new BigDecimal("10.03"));
        second.setChangeRate(new BigDecimal("0.30"));
        second.setVolume(1_080L);
        second.setAmount(new BigDecimal("108600.00"));
        second.setQuoteTime(LocalDateTime.of(2026, 3, 19, 10, 2, 2));

        when(valueOperations.get("stock:realtime:v2:0:000001"))
                .thenReturn(objectMapper.writeValueAsString(first), objectMapper.writeValueAsString(second));

        List<KlineVO> firstResult = eastMoneyClient.getKlineData("000001", "0", "1s", 10);
        List<KlineVO> secondResult = eastMoneyClient.getKlineData("000001", "0", "1s", 10);

        assertEquals(10, firstResult.size());
        assertEquals(10, secondResult.size());
        assertEquals("2026-03-19 10:02:02", secondResult.get(9).getDate());
        assertEquals(80L, secondResult.get(9).getVolume());
        verify(remoteHttpClient, never()).get(anyString());
    }

    @Test
    void getKlineDataShouldBypassRedisCacheDuringTradingSessionForIntradayPeriod() throws Exception {
        String response = """
                callback({"data":{"klines":[
                "2026-03-19 10:01,10.94,10.95,10.96,10.93,130000,1300000.00,0.10"
                ]}})
                """;
        StockRealtimeDTO realtimeDTO = new StockRealtimeDTO();
        realtimeDTO.setQuoteTime(LocalDateTime.of(2026, 3, 19, 10, 2, 51));
        realtimeDTO.setCurrentPrice(new BigDecimal("10.97"));
        when(valueOperations.get("stock:realtime:v2:0:000001"))
                .thenReturn(objectMapper.writeValueAsString(realtimeDTO));
        when(remoteHttpClient.get(contains("klt=1"))).thenReturn(response);

        List<KlineVO> result = eastMoneyClient.getKlineData("000001", "0", "1min", 1);

        assertEquals(1, result.size());
        assertEquals("2026-03-19 10:02", result.get(0).getDate());
        verify(valueOperations, never()).get("stock:kline:000001:1min");
        verify(remoteHttpClient, times(1)).get(contains("klt=1"));
    }

    @Test
    void getRealtimeQuoteShouldUseMarketSpecificCacheKey() throws Exception {
        StockRealtimeDTO cached = new StockRealtimeDTO();
        cached.setStockCode("000001");
        cached.setCurrentPrice(new BigDecimal("4040.64"));
        when(valueOperations.get("stock:realtime:v2:1:000001"))
                .thenReturn(objectMapper.writeValueAsString(cached));

        StockRealtimeDTO result = eastMoneyClient.getRealtimeQuote("000001", "1");

        assertEquals(new BigDecimal("4040.64"), result.getCurrentPrice());
        verify(remoteHttpClient, never()).get(anyString());
    }

    @Test
    void getRealtimeQuoteShouldCacheResultForOneSecond() {
        ReflectionTestUtils.setField(eastMoneyClient, "baseUrl", "https://push2.eastmoney.com");
        String response = """
                callback({"data":{"f43":1012,"f170":25,"f169":3,"f47":1000,"f48":100000,"f60":1009}})
                """;
        when(valueOperations.get("stock:realtime:v2:0:000001")).thenReturn(null);
        when(remoteHttpClient.get(contains("/api/qt/stock/get"))).thenReturn(response);

        StockRealtimeDTO result = eastMoneyClient.getRealtimeQuote("000001", "0");

        assertEquals(new BigDecimal("10.12"), result.getCurrentPrice());
        verify(valueOperations).set(eq("stock:realtime:v2:0:000001"), anyString(), eq(Duration.ofSeconds(1)));
    }

    @Test
    void searchStocksShouldParseLatestQuotationCodeTableAndFilterAssets() {
        String response = """
                ({
                  "QuotationCodeTable":{
                    "Data":[
                      {"Code":"000001","Name":"平安银行","Classify":"AStock","MktNum":"0"},
                      {"Code":"000001","Name":"上证指数","Classify":"Index","MktNum":"1"},
                      {"Code":"000001","Name":"华夏成长混合","Classify":"OTCFUND","MktNum":"150"}
                    ]
                  }
                })
                """;
        when(remoteHttpClient.get(contains("input=%E5%B9%B3%E5%AE%89"))).thenReturn(response);

        List<Map<String, String>> result = eastMoneyClient.searchStocks("平安");

        assertEquals(2, result.size());
        assertEquals("SZ", result.get(0).get("market"));
        assertEquals("平安银行", result.get(0).get("stockName"));
        assertEquals("SH", result.get(1).get("market"));
        assertEquals("上证指数", result.get(1).get("stockName"));
    }

    private KlineVO kline(String date, String close) {
        KlineVO vo = new KlineVO();
        vo.setDate(date);
        vo.setOpen(new BigDecimal(close));
        vo.setClose(new BigDecimal(close));
        vo.setHigh(new BigDecimal(close));
        vo.setLow(new BigDecimal(close));
        vo.setVolume(100L);
        vo.setAmount(new BigDecimal("1000.00"));
        vo.setChangeRate(new BigDecimal("1.00"));
        return vo;
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
