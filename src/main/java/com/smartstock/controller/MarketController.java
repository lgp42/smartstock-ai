package com.smartstock.controller;

import com.smartstock.common.BusinessException;
import com.smartstock.common.ErrorCode;
import com.smartstock.common.Result;
import com.smartstock.service.MarketService;
import com.smartstock.vo.IndicatorVO;
import com.smartstock.vo.KlineVO;
import com.smartstock.vo.MarketSnapshotVO;
import com.smartstock.vo.PageVO;
import com.smartstock.vo.ScreenerResultVO;
import com.smartstock.vo.StockDetailVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@Tag(name = "Market", description = "行情、K线、指标与选股接口")
public class MarketController {

    private final MarketService marketService;

    @GetMapping("/stocks/search")
    public Result<List<Map<String, String>>> searchStocks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "搜索关键词不能为空");
        }
        validateSearchLimit(limit);
        List<Map<String, String>> result = marketService.searchStocks(keyword);
        // 限制返回数量
        if (result.size() > limit) {
            result = result.subList(0, limit);
        }
        return Result.ok(result);
    }

    @GetMapping("/stocks/{stockCode}")
    public Result<StockDetailVO> getStockDetail(@PathVariable String stockCode) {
        return Result.ok(marketService.getStockDetail(stockCode));
    }

    @GetMapping("/stocks/{stockCode}/kline")
    public Result<List<KlineVO>> getKlineData(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "day") String period,
            @RequestParam(defaultValue = "100") int limit) {
        validatePeriod(period);
        validateChartLimit(limit);
        return Result.ok(marketService.getKlineData(stockCode, period, limit));
    }

    @GetMapping("/stocks/{stockCode}/indicators")
    public Result<List<IndicatorVO>> getIndicators(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "macd") String indicators,
            @RequestParam(defaultValue = "day") String period,
            @RequestParam(defaultValue = "100") int limit) {
        validateIndicator(indicators);
        validatePeriod(period);
        validateChartLimit(limit);
        return Result.ok(marketService.getIndicators(stockCode, indicators, period, limit));
    }

    @GetMapping("/snapshots")
    public Result<List<MarketSnapshotVO>> getMarketSnapshots() {
        return Result.ok(marketService.getMarketSnapshots());
    }

    @GetMapping("/screener")
    public Result<List<ScreenerResultVO>> screenStocks(
            @RequestParam(required = false) String boards,
            @RequestParam(defaultValue = "all") String industryGroup,
            @RequestParam(required = false) java.math.BigDecimal minMarketCap,
            @RequestParam(required = false) java.math.BigDecimal maxMarketCap,
            @RequestParam(required = false) java.math.BigDecimal minPe,
            @RequestParam(required = false) java.math.BigDecimal maxPe,
            @RequestParam(required = false) java.math.BigDecimal minTurnoverRate,
            @RequestParam(required = false) java.math.BigDecimal maxTurnoverRate,
            @RequestParam(required = false) java.math.BigDecimal minChangeRate,
            @RequestParam(required = false) java.math.BigDecimal maxChangeRate,
            @RequestParam(defaultValue = "true") boolean excludeSt,
            @RequestParam(defaultValue = "true") boolean excludeDelisted,
            @RequestParam(defaultValue = "none") String technicalPattern) {
        validateTechnicalPattern(technicalPattern);
        return Result.ok(marketService.screenStocks(parseBoards(boards), industryGroup,
                minMarketCap, maxMarketCap, minPe, maxPe,
                minTurnoverRate, maxTurnoverRate, minChangeRate, maxChangeRate,
                excludeSt, excludeDelisted, technicalPattern));
    }

    @GetMapping("/screener/page")
    public Result<PageVO<ScreenerResultVO>> screenStocksPage(
            @RequestParam(required = false) String boards,
            @RequestParam(defaultValue = "all") String industryGroup,
            @RequestParam(required = false) java.math.BigDecimal minMarketCap,
            @RequestParam(required = false) java.math.BigDecimal maxMarketCap,
            @RequestParam(required = false) java.math.BigDecimal minPe,
            @RequestParam(required = false) java.math.BigDecimal maxPe,
            @RequestParam(required = false) java.math.BigDecimal minTurnoverRate,
            @RequestParam(required = false) java.math.BigDecimal maxTurnoverRate,
            @RequestParam(required = false) java.math.BigDecimal minChangeRate,
            @RequestParam(required = false) java.math.BigDecimal maxChangeRate,
            @RequestParam(defaultValue = "true") boolean excludeSt,
            @RequestParam(defaultValue = "true") boolean excludeDelisted,
            @RequestParam(defaultValue = "none") String technicalPattern,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        validateTechnicalPattern(technicalPattern);
        validatePage(page, pageSize);
        return Result.ok(marketService.screenStocksPage(parseBoards(boards), industryGroup,
                minMarketCap, maxMarketCap, minPe, maxPe,
                minTurnoverRate, maxTurnoverRate, minChangeRate, maxChangeRate,
                excludeSt, excludeDelisted, technicalPattern,
                page, pageSize));
    }

    private void validateSearchLimit(int limit) {
        if (limit < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "limit 最小为 1");
        }
        if (limit > 50) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "limit 最大为 50");
        }
    }

    private void validateChartLimit(int limit) {
        if (limit < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "limit 最小为 1");
        }
        if (limit > 500) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "limit 最大为 500");
        }
    }

    private void validatePeriod(String period) {
        if (!List.of("1s", "1min", "5min", "15min", "30min", "60min", "day", "week", "month").contains(period)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "period 仅支持 1s/1min/5min/15min/30min/60min/day/week/month");
        }
    }

    private void validateIndicator(String indicator) {
        String rawIndicator = indicator == null ? "macd" : indicator.toLowerCase();
        List<String> indicators = java.util.Arrays.stream(rawIndicator.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (indicators.isEmpty() || !List.of("macd", "kdj", "rsi", "ma", "boll").containsAll(indicators)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "indicators 仅支持 macd/kdj/rsi/ma/boll");
        }
    }

    private void validateTechnicalPattern(String technicalPattern) {
        if (!List.of("none", "ma_cross", "macd_golden", "volume_up", "breakout", "near_high", "low_vol_pullback")
                .contains(technicalPattern == null ? null : technicalPattern.toLowerCase())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "technicalPattern 仅支持 none/ma_cross/macd_golden/volume_up/breakout/near_high/low_vol_pullback");
        }
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "page 最小为 1");
        }
        if (pageSize < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "pageSize 最小为 1");
        }
        if (pageSize > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "pageSize 最大为 100");
        }
    }

    private List<String> parseBoards(String boards) {
        if (!StringUtils.hasText(boards)) {
            return List.of();
        }
        return java.util.Arrays.stream(boards.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
