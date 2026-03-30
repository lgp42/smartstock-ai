package com.smartstock.service;

import com.smartstock.vo.IndicatorVO;
import com.smartstock.vo.KlineVO;
import com.smartstock.vo.MarketSnapshotVO;
import com.smartstock.vo.PageVO;
import com.smartstock.vo.ScreenerResultVO;
import com.smartstock.vo.StockDetailVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MarketService {

    StockDetailVO getStockDetail(String stockCode);

    List<KlineVO> getKlineData(String stockCode, String period, int limit);

    List<Map<String, String>> searchStocks(String keyword);

    List<IndicatorVO> getIndicators(String stockCode, String indicatorType, String period, int limit);

    List<MarketSnapshotVO> getMarketSnapshots();

    List<ScreenerResultVO> screenStocks(List<String> boards, String industryGroup,
                                        BigDecimal minMarketCap, BigDecimal maxMarketCap,
                                        BigDecimal minPe, BigDecimal maxPe,
                                        BigDecimal minTurnoverRate, BigDecimal maxTurnoverRate,
                                        BigDecimal minChangeRate, BigDecimal maxChangeRate,
                                        boolean excludeSt, boolean excludeDelisted,
                                        String technicalPattern);

    PageVO<ScreenerResultVO> screenStocksPage(List<String> boards, String industryGroup,
                                              BigDecimal minMarketCap, BigDecimal maxMarketCap,
                                              BigDecimal minPe, BigDecimal maxPe,
                                              BigDecimal minTurnoverRate, BigDecimal maxTurnoverRate,
                                              BigDecimal minChangeRate, BigDecimal maxChangeRate,
                                              boolean excludeSt, boolean excludeDelisted,
                                              String technicalPattern,
                                              int page, int pageSize);
}
