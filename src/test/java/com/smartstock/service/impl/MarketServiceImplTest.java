package com.smartstock.service.impl;

import com.smartstock.client.EastMoneyClient;
import com.smartstock.client.StockRealtimeDTO;
import com.smartstock.client.StockScreenerCandidateDTO;
import com.smartstock.common.BusinessException;
import com.smartstock.entity.StockInfo;
import com.smartstock.mapper.StockInfoMapper;
import com.smartstock.vo.IndicatorVO;
import com.smartstock.vo.KlineVO;
import com.smartstock.vo.MarketSnapshotVO;
import com.smartstock.vo.ScreenerResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketServiceImplTest {

    @Mock
    private StockInfoMapper stockInfoMapper;

    @Mock
    private EastMoneyClient eastMoneyClient;

    private MarketServiceImpl marketService;

    @BeforeEach
    void setUp() {
        marketService = new MarketServiceImpl(stockInfoMapper, eastMoneyClient);
    }

    @Test
    void getIndicatorsShouldReturnBigDecimalIndicatorValues() {
        mockStock("000001", "SZ");
        when(eastMoneyClient.getKlineData("000001", "0", "day", 30)).thenReturn(buildKlines(30));

        List<IndicatorVO> macd = marketService.getIndicators("000001", "macd", "day", 30);
        List<IndicatorVO> kdj = marketService.getIndicators("000001", "kdj", "day", 30);
        List<IndicatorVO> rsi = marketService.getIndicators("000001", "rsi", "day", 30);

        IndicatorVO macdLast = macd.get(macd.size() - 1);
        IndicatorVO kdjLast = kdj.get(kdj.size() - 1);
        IndicatorVO rsiLast = rsi.get(rsi.size() - 1);

        assertInstanceOf(BigDecimal.class, macdLast.getData().get("dif"));
        assertInstanceOf(BigDecimal.class, macdLast.getData().get("dea"));
        assertInstanceOf(BigDecimal.class, macdLast.getData().get("macd"));
        assertEquals(4, ((BigDecimal) macdLast.getData().get("dif")).scale());

        assertInstanceOf(BigDecimal.class, kdjLast.getData().get("k"));
        assertInstanceOf(BigDecimal.class, kdjLast.getData().get("d"));
        assertInstanceOf(BigDecimal.class, kdjLast.getData().get("j"));
        assertEquals(2, ((BigDecimal) kdjLast.getData().get("k")).scale());

        assertInstanceOf(BigDecimal.class, rsiLast.getData().get("rsi6"));
        assertInstanceOf(BigDecimal.class, rsiLast.getData().get("rsi12"));
        assertInstanceOf(BigDecimal.class, rsiLast.getData().get("rsi24"));
        assertEquals(2, ((BigDecimal) rsiLast.getData().get("rsi6")).scale());
    }

    @Test
    void searchStocksShouldRejectBlankKeyword() {
        assertThrows(BusinessException.class, () -> marketService.searchStocks("   "));
    }

    @Test
    void searchStocksShouldMergeLocalAndRemoteResultsAndPrioritizeExactCode() {
        when(stockInfoMapper.selectList(any())).thenReturn(List.of(
                StockInfo.builder().stockCode("000001").stockName("平安银行").market("SZ").industry("银行").build()
        ));
        when(eastMoneyClient.searchStocks("000001")).thenReturn(List.of(
                stockMap("000001", "平安银行", "SZ", null),
                stockMap("000001", "上证指数", "SH", null),
                stockMap("0000012", "示例股票", "SZ", null)
        ));

        List<Map<String, String>> result = marketService.searchStocks("000001");

        assertEquals(2, result.size());
        assertEquals("SZ", result.get(0).get("market"));
        assertEquals("000001", result.get(0).get("stockCode"));
        assertEquals("银行", result.get(0).get("industry"));
        assertEquals("SZ", result.get(1).get("market"));
        assertEquals("0000012", result.get(1).get("stockCode"));
    }

    @Test
    void getMarketSnapshotsShouldReturnConfiguredSnapshots() {
        when(eastMoneyClient.getRealtimeQuote("000001", "1")).thenReturn(realtime("4040.64", "-0.23"));
        when(eastMoneyClient.getRealtimeQuote("399001", "0")).thenReturn(realtime("14025.78", "-0.10"));

        List<MarketSnapshotVO> result = marketService.getMarketSnapshots();

        assertEquals(2, result.size());
        assertEquals("上证指数", result.get(0).getStockName());
        assertEquals(new BigDecimal("4040.64"), result.get(0).getCurrentPrice());
        assertEquals("深证成指", result.get(1).getStockName());
        assertEquals(new BigDecimal("-0.10"), result.get(1).getChangeRate());
    }

    @Test
    void searchStocksShouldSkipIndexPresetWhenKeywordMatchesAlias() {
        when(stockInfoMapper.selectList(any())).thenReturn(List.of());
        when(eastMoneyClient.searchStocks("上证")).thenReturn(List.of());

        List<Map<String, String>> result = marketService.searchStocks("上证");

        assertTrue(result.isEmpty());
    }

    @Test
    void screenStocksShouldApplyBoardAndNumericFilters() {
        when(eastMoneyClient.getScreenerCandidates(List.of("cyb"), 12)).thenReturn(List.of(
                screenerCandidate("600519", "贵州茅台", "SH", "白酒", "1500.00", "2.10", "1.20", "28.40", "22000.00"),
                screenerCandidate("300750", "宁德时代", "SZ", "新能源", "185.30", "5.40", "3.25", "28.40", "8150.80")
        ));

        List<ScreenerResultVO> result = marketService.screenStocks(
                List.of("cyb"), "ev",
                new BigDecimal("1000"), new BigDecimal("9000"),
                new BigDecimal("20"), new BigDecimal("40"),
                null, null, null, null,
                true, true,
                "none"
        );

        assertEquals(1, result.size());
        assertEquals("300750", result.get(0).getStockCode());
        assertEquals(new BigDecimal("28.40"), result.get(0).getPe());
    }

    @Test
    void screenStocksShouldExcludeUnknownIndustryWhenIndustryFilterSpecified() {
        when(eastMoneyClient.getScreenerCandidates(List.of("sh_main", "sz_main"), 12)).thenReturn(List.of(
                screenerCandidate("600036", "招商银行", "SH", "银行", "42.10", "1.26", "0.38", "6.52", "10800.00"),
                screenerCandidate("600026", "中远海能", "SH", null, "14.80", "0.86", "1.10", "12.80", "864.52")
        ));

        List<ScreenerResultVO> result = marketService.screenStocks(
                List.of("sh_main", "sz_main"), "finance",
                null, null, null, null,
                null, null, null, null,
                true, true,
                "none"
        );

        assertEquals(1, result.size());
        assertEquals("600036", result.get(0).getStockCode());
    }

    @Test
    void screenStocksShouldFallbackToSearchPoolWhenPrimarySourceReturnsEmpty() {
        when(eastMoneyClient.getScreenerCandidates(List.of("sh_main", "sz_main"), 12)).thenReturn(List.of());
        when(eastMoneyClient.searchStocks(anyString())).thenReturn(List.of(
                stockMap("600036", "招商银行", "SH", null),
                stockMap("000858", "五粮液", "SZ", null),
                stockMap("300750", "宁德时代", "SZ", null)
        ));
        when(eastMoneyClient.getBatchRealtimeQuotes(anyList())).thenReturn(Map.of(
                "SH:600036", realtimeForScreen("42.10", "1.26", "0.38", "6.52", "10800.00"),
                "SZ:000858", realtimeForScreen("129.88", "2.18", "1.02", "19.40", "6800.00")
        ));

        List<ScreenerResultVO> result = marketService.screenStocks(
                List.of("sh_main", "sz_main"), "all",
                null, null, null, null,
                null, null, null, null,
                true, true,
                "none"
        );

        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
        assertEquals("000858", result.get(0).getStockCode());
        assertEquals("600036", result.get(1).getStockCode());
    }

    @Test
    void getIndicatorsShouldRejectUnsupportedPeriod() {
        assertThrows(BusinessException.class, () -> marketService.getIndicators("000001", "macd", "year", 30));
    }

    @Test
    void getIndicatorsShouldSupportOneMinutePeriod() {
        mockStock("000001", "SZ");
        when(eastMoneyClient.getKlineData("000001", "0", "1min", 30)).thenReturn(buildKlines(30));

        List<IndicatorVO> indicators = marketService.getIndicators("000001", "macd", "1min", 30);

        assertEquals(30, indicators.size());
        assertEquals("macd", indicators.get(indicators.size() - 1).getType());
    }

    @Test
    void getIndicatorsShouldSupportOneSecondPeriod() {
        mockStock("000001", "SZ");
        when(eastMoneyClient.getKlineData("000001", "0", "1s", 30)).thenReturn(buildKlines(30));

        List<IndicatorVO> indicators = marketService.getIndicators("000001", "macd", "1s", 30);

        assertEquals(30, indicators.size());
        assertEquals("macd", indicators.get(indicators.size() - 1).getType());
    }

    @Test
    void getIndicatorsShouldAllowTerminalSizedWindow() {
        mockStock("000001", "SZ");
        when(eastMoneyClient.getKlineData("000001", "0", "60min", 120)).thenReturn(buildKlines(120));

        List<IndicatorVO> indicators = marketService.getIndicators("000001", "macd", "60min", 120);

        assertEquals(120, indicators.size());
        assertEquals("macd", indicators.get(indicators.size() - 1).getType());
    }

    @Test
    void getStockDetailShouldCreateStockInfoWhenExactRemoteSearchMatches() {
        when(stockInfoMapper.selectList(any())).thenReturn(List.of());
        when(eastMoneyClient.searchStocks("688001")).thenReturn(List.of(
                stockMap("688001", "华兴源创", "SH", null)
        ));
        when(eastMoneyClient.getRealtimeQuote("688001", "1")).thenReturn(realtime("32.77", "2.95"));

        var detail = marketService.getStockDetail("688001");

        assertEquals("688001", detail.getStockCode());
        assertEquals("华兴源创", detail.getStockName());
        assertEquals("SH", detail.getMarket());
        assertEquals("STAR", detail.getBoard());
    }

    @Test
    void getStockDetailShouldPreferTradableAStockWhenIndexSharesSameCode() {
        when(stockInfoMapper.selectList(any())).thenReturn(List.of(
                StockInfo.builder().stockCode("000001").stockName("上证指数").market("SH").industry("指数").build(),
                StockInfo.builder().stockCode("000001").stockName("平安银行").market("SZ").industry("银行").build()
        ));
        when(eastMoneyClient.getRealtimeQuote("000001", "0")).thenReturn(realtime("10.96", "-0.63"));

        var detail = marketService.getStockDetail("000001");

        assertEquals("平安银行", detail.getStockName());
        assertEquals("SZ", detail.getMarket());
        assertEquals(new BigDecimal("10.96"), detail.getCurrentPrice());
    }

    @Test
    void getStockDetailShouldRefreshTradableAStockWhenStoredRecordIsIndex() {
        StockInfo storedIndex = StockInfo.builder().id(2L).stockCode("000001").stockName("上证指数").market("SH").industry("指数").build();
        when(stockInfoMapper.selectList(any())).thenReturn(List.of(storedIndex));
        when(stockInfoMapper.selectOne(any())).thenReturn(storedIndex);
        when(eastMoneyClient.searchStocks("000001")).thenReturn(List.of(
                stockMap("000001", "平安银行", "SZ", "银行")
        ));
        when(eastMoneyClient.getRealtimeQuote("000001", "0")).thenReturn(realtime("10.96", "-0.63"));

        var detail = marketService.getStockDetail("000001");

        assertEquals("平安银行", detail.getStockName());
        assertEquals("SZ", detail.getMarket());
        assertEquals("银行", detail.getIndustry());
    }

    @Test
    void screenStocksPageShouldReturnPagedRecords() {
        when(eastMoneyClient.getScreenerCandidates(List.of("cyb"), 12)).thenReturn(List.of(
                screenerCandidate("300750", "宁德时代", "SZ", "新能源", "185.30", "5.40", "3.25", "28.40", "8150.80"),
                screenerCandidate("300760", "迈瑞医疗", "SZ", "医药", "295.00", "3.40", "1.20", "25.00", "1200.00"),
                screenerCandidate("300433", "蓝思科技", "SZ", "电子", "22.00", "2.10", "2.50", "20.00", "500.00")
        ));

        var page = marketService.screenStocksPage(
                List.of("cyb"), "all",
                null, null, null, null,
                null, null, null, null,
                true, true,
                "none",
                2, 1
        );

        assertEquals(3L, page.getTotal());
        assertEquals(1, page.getRecords().size());
        assertTrue(List.of("300760", "300433").contains(page.getRecords().get(0).getStockCode()));
    }

    private void mockStock(String stockCode, String market) {
        when(stockInfoMapper.selectList(any())).thenReturn(List.of(
                StockInfo.builder().stockCode(stockCode).market(market).stockName("平安银行").build()
        ));
    }

    private List<KlineVO> buildKlines(int size) {
        List<KlineVO> klines = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            BigDecimal open = BigDecimal.valueOf(10L + i).setScale(2, RoundingMode.HALF_UP);
            BigDecimal close = BigDecimal.valueOf(10.5 + i).setScale(2, RoundingMode.HALF_UP);
            BigDecimal high = close.add(new BigDecimal("0.30"));
            BigDecimal low = open.subtract(new BigDecimal("0.20"));

            KlineVO vo = new KlineVO();
            vo.setDate(LocalDate.of(2026, 1, 1).plusDays(i).toString());
            vo.setOpen(open);
            vo.setClose(close);
            vo.setHigh(high);
            vo.setLow(low);
            vo.setVolume(100000L + i);
            vo.setAmount(close.multiply(BigDecimal.valueOf(100000L + i)).setScale(2, java.math.RoundingMode.HALF_UP));
            vo.setChangeRate(new BigDecimal("1.00"));
            klines.add(vo);
        }
        return klines;
    }

    private Map<String, String> stockMap(String stockCode, String stockName, String market, String industry) {
        Map<String, String> item = new HashMap<>();
        item.put("stockCode", stockCode);
        item.put("stockName", stockName);
        item.put("market", market);
        item.put("industry", industry);
        return item;
    }

    private StockRealtimeDTO realtime(String currentPrice, String changeRate) {
        StockRealtimeDTO dto = new StockRealtimeDTO();
        dto.setCurrentPrice(new BigDecimal(currentPrice));
        dto.setChangeRate(new BigDecimal(changeRate));
        return dto;
    }

    private StockRealtimeDTO realtimeForScreen(String currentPrice, String changeRate, String turnoverRate, String pe, String totalMarketCap) {
        StockRealtimeDTO dto = realtime(currentPrice, changeRate);
        dto.setTurnoverRate(new BigDecimal(turnoverRate));
        dto.setPe(new BigDecimal(pe));
        dto.setTotalMarketCap(new BigDecimal(totalMarketCap));
        return dto;
    }

    private StockScreenerCandidateDTO screenerCandidate(String stockCode, String stockName, String market, String industry,
                                                        String currentPrice, String changeRate, String turnoverRate,
                                                        String pe, String totalMarketCap) {
        StockScreenerCandidateDTO dto = new StockScreenerCandidateDTO();
        dto.setStockCode(stockCode);
        dto.setStockName(stockName);
        dto.setMarket(market);
        dto.setIndustry(industry);
        dto.setCurrentPrice(new BigDecimal(currentPrice));
        dto.setChangeRate(new BigDecimal(changeRate));
        dto.setTurnoverRate(new BigDecimal(turnoverRate));
        dto.setPe(new BigDecimal(pe));
        dto.setTotalMarketCap(new BigDecimal(totalMarketCap));
        return dto;
    }
}
