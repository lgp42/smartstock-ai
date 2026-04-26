package com.smartstock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartstock.client.EastMoneyClient;
import com.smartstock.client.StockRealtimeDTO;
import com.smartstock.client.StockScreenerCandidateDTO;
import com.smartstock.common.BusinessException;
import com.smartstock.common.ErrorCode;
import com.smartstock.entity.StockInfo;
import com.smartstock.mapper.StockInfoMapper;
import com.smartstock.service.MarketService;
import com.smartstock.util.TextEncodingUtils;
import com.smartstock.vo.IndicatorVO;
import com.smartstock.vo.KlineVO;
import com.smartstock.vo.MarketSnapshotVO;
import com.smartstock.vo.PageVO;
import com.smartstock.vo.ScreenerResultVO;
import com.smartstock.vo.StockDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketServiceImpl implements MarketService {

    private static final int CALC_SCALE = 8;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal TWO = new BigDecimal("2");
    private static final BigDecimal THREE = new BigDecimal("3");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal ONE_THIRD = ONE.divide(THREE, CALC_SCALE, RoundingMode.HALF_UP);
    private static final BigDecimal TWO_THIRDS = TWO.divide(THREE, CALC_SCALE, RoundingMode.HALF_UP);
    private static final List<MarketTarget> MARKET_SNAPSHOTS = List.of(
            new MarketTarget("000001", "1", "上证指数"),
            new MarketTarget("399001", "0", "深证成指")
    );
    private static final List<SearchPreset> SEARCH_PRESETS = List.of(
            new SearchPreset("000001", "SH", "上证指数", "指数", List.of("上证", "上证指数", "上证综指", "沪指")),
            new SearchPreset("399001", "SZ", "深证成指", "指数", List.of("深证", "深证成指", "深成指", "深指"))
    );
    private static final Map<String, Set<String>> INDUSTRY_GROUPS = Map.of(
            "tech", Set.of("计算机", "半导体", "电子", "通信", "软件", "互联网", "传媒", "设备"),
            "finance", Set.of("银行", "保险", "房地产", "证券", "多元金融"),
            "consumer", Set.of("白酒", "食品", "零售", "饮料", "家电", "服饰", "旅游", "医药"),
            "energy", Set.of("电力", "煤炭", "石油", "化工", "油气", "公用事业"),
            "ev", Set.of("新能源", "电池", "汽车", "光伏", "储能", "锂电")
    );
    private static final int SCREENING_MAX_PAGES = 12;
    private static final int SCREENING_RESULT_LIMIT = 300;
    private static final int FALLBACK_SCREENING_POOL_LIMIT = 180;
    private static final Duration SCREENER_CACHE_TTL = Duration.ofSeconds(90);
    private static final Map<String, List<String>> FALLBACK_BOARD_QUERIES = Map.of(
            "sh_main", List.of("600", "601", "603", "605", "银行", "证券", "白酒", "煤炭"),
            "sz_main", List.of("000", "001", "002", "003", "家电", "医药", "消费", "地产"),
            "cyb", List.of("3000", "3001", "3002", "3003", "3004", "3005", "3006", "3007", "3008", "3009",
                    "3010", "3011", "3012", "3013", "创业板", "新能源", "电池", "光伏"),
            "star", List.of("6880", "6881", "6882", "6883", "6885", "科创板", "芯片", "算力", "人工智能")
    );
    private static final List<String> FALLBACK_ALL_QUERIES = List.of(
            "银行", "证券", "白酒", "医药", "家电", "消费", "地产",
            "半导体", "芯片", "软件", "通信", "人工智能",
            "新能源", "电池", "光伏", "汽车",
            "煤炭", "电力", "石油", "化工"
    );

    private final StockInfoMapper stockInfoMapper;
    private final EastMoneyClient eastMoneyClient;
    private final Map<String, CachedScreenerResult> screenerCache = new ConcurrentHashMap<>();

    @Override
    @Cacheable(cacheNames = "stockDetail", key = "#stockCode", unless = "#result == null")
    public StockDetailVO getStockDetail(String stockCode) {
        StockInfo stockInfo = getStockInfo(stockCode);
        String marketCode = StockInfo.toEastMoneyMarketCode(stockInfo.getMarket());
        StockRealtimeDTO realtimeDTO = eastMoneyClient.getRealtimeQuote(stockCode, marketCode);

        StockDetailVO vo = new StockDetailVO();
        vo.setStockCode(stockInfo.getStockCode());
        String resolvedStockName = stockInfo.getStockName();
        if (TextEncodingUtils.hasCorruptedDisplayText(resolvedStockName)
                && realtimeDTO != null
                && StringUtils.hasText(realtimeDTO.getStockName())) {
            resolvedStockName = realtimeDTO.getStockName();
        }
        vo.setStockName(TextEncodingUtils.normalizeDisplayText(resolvedStockName));
        vo.setMarket(stockInfo.getMarket());
        vo.setBoard(stockInfo.getBoard());
        String resolvedIndustry = stockInfo.getIndustry();
        if ((!StringUtils.hasText(resolvedIndustry)
                || shouldRefreshIndustry(stockInfo, resolvedIndustry)
                || TextEncodingUtils.hasCorruptedDisplayText(resolvedIndustry))
                && realtimeDTO != null
                && StringUtils.hasText(realtimeDTO.getIndustry())) {
            resolvedIndustry = realtimeDTO.getIndustry();
        }
        vo.setIndustry(TextEncodingUtils.normalizeDisplayText(resolvedIndustry));
        vo.setSt(stockInfo.getIsSt() != null && stockInfo.getIsSt() == 1);
        vo.setDelisted(stockInfo.getIsDelisted() != null && stockInfo.getIsDelisted() == 1);

        if (realtimeDTO != null) {
            vo.setCurrentPrice(realtimeDTO.getCurrentPrice());
            vo.setChangeRate(realtimeDTO.getChangeRate());
            vo.setChangeAmount(realtimeDTO.getChangeAmount());
            vo.setVolume(realtimeDTO.getVolume());
            vo.setAmount(realtimeDTO.getAmount());
            vo.setHigh(realtimeDTO.getHigh());
            vo.setLow(realtimeDTO.getLow());
            vo.setPreClose(realtimeDTO.getPreClose());
            vo.setOpen(realtimeDTO.getOpen());
            vo.setClose(realtimeDTO.getCurrentPrice());
        }

        enrichAndSaveStockInfo(stockInfo, realtimeDTO, "DETAIL");

        return vo;
    }

    @Override
    public List<KlineVO> getKlineData(String stockCode, String period, int limit) {
        String normalizedPeriod = normalizePeriod(period);
        validateLimit(limit);
        StockInfo stockInfo = getStockInfo(stockCode);
        String marketCode = StockInfo.toEastMoneyMarketCode(stockInfo.getMarket());
        return eastMoneyClient.getKlineData(stockCode, marketCode, normalizedPeriod, limit);
    }

    @Override
    @Cacheable(cacheNames = "searchStocks", key = "#keyword", unless = "#result == null || #result.isEmpty()")
    public List<Map<String, String>> searchStocks(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        List<StockInfo> dbResults = stockInfoMapper.selectList(
                new LambdaQueryWrapper<StockInfo>()
                        .like(StockInfo::getStockCode, normalizedKeyword)
                        .or()
                        .like(StockInfo::getStockName, normalizedKeyword)
                        .last("LIMIT 20"));

        Map<String, Map<String, String>> merged = new LinkedHashMap<>();
        for (Map<String, String> preset : buildPresetSearchResults(normalizedKeyword)) {
            merged.put(uniqueSearchKey(preset), preset);
        }
        for (StockInfo stockInfo : dbResults) {
            Map<String, String> item = new HashMap<>();
            item.put("stockCode", stockInfo.getStockCode());
            item.put("stockName", stockInfo.getStockName());
            item.put("market", stockInfo.getMarket());
            item.put("industry", stockInfo.getIndustry());
            merged.put(uniqueSearchKey(item), item);
        }

        for (Map<String, String> item : eastMoneyClient.searchStocks(normalizedKeyword)) {
            item.putIfAbsent("board", StockInfo.resolveBoard(item.get("stockCode"), item.get("market")));
            String key = uniqueSearchKey(item);
            Map<String, String> existing = merged.get(key);
            if (existing == null) {
                merged.put(key, new HashMap<>(item));
                continue;
            }
            if (shouldPreferRemoteDisplayValue(existing.get("stockName"), item.get("stockName"))) {
                existing.put("stockName", item.get("stockName"));
            }
            if (!StringUtils.hasText(existing.get("market")) && StringUtils.hasText(item.get("market"))) {
                existing.put("market", item.get("market"));
            }
            if (shouldPreferRemoteDisplayValue(existing.get("industry"), item.get("industry"))) {
                existing.put("industry", item.get("industry"));
            }
        }

        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> item : merged.values()) {
            if (isTradeableAStock(item)) {
                enrichSearchResult(item);
                result.add(item);
            }
        }
        persistSearchResults(result);
        result.sort((left, right) -> {
            int scoreCompare = Integer.compare(searchScore(right, normalizedKeyword), searchScore(left, normalizedKeyword));
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return safeValue(left.get("stockCode")).compareTo(safeValue(right.get("stockCode")));
        });
        return result;
    }

    @Override
    public List<IndicatorVO> getIndicators(String stockCode, String indicatorType, String period, int limit) {
        List<String> normalizedIndicatorTypes = normalizeIndicatorTypes(indicatorType);
        String normalizedPeriod = normalizePeriod(period);
        validateLimit(limit);

        List<KlineVO> klines = getKlineData(stockCode, normalizedPeriod, limit);
        if (klines == null || klines.isEmpty()) {
            return new ArrayList<>();
        }

        List<IndicatorVO> result = new ArrayList<>();
        for (String normalizedIndicatorType : normalizedIndicatorTypes) {
            result.addAll(calculateIndicator(normalizedIndicatorType, klines));
        }
        return result;
    }

    @Override
    @Cacheable(cacheNames = "marketSnapshots", key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<MarketSnapshotVO> getMarketSnapshots() {
        List<MarketSnapshotVO> result = new ArrayList<>();
        for (MarketTarget target : MARKET_SNAPSHOTS) {
            StockRealtimeDTO realtimeDTO = eastMoneyClient.getRealtimeQuote(target.stockCode(), target.market());
            MarketSnapshotVO item = new MarketSnapshotVO();
            item.setStockCode(target.stockCode());
            item.setStockName(target.stockName());
            if (realtimeDTO != null) {
                item.setCurrentPrice(realtimeDTO.getCurrentPrice());
                item.setChangeRate(realtimeDTO.getChangeRate());
            }
            result.add(item);
        }
        return result;
    }

    @Override
    public List<ScreenerResultVO> screenStocks(List<String> boards, String industryGroup,
                                               BigDecimal minMarketCap, BigDecimal maxMarketCap,
                                               BigDecimal minPe, BigDecimal maxPe,
                                               BigDecimal minTurnoverRate, BigDecimal maxTurnoverRate,
                                               BigDecimal minChangeRate, BigDecimal maxChangeRate,
                                               boolean excludeSt, boolean excludeDelisted,
                                               String technicalPattern) {
        return buildScreenerResult(boards, industryGroup,
                minMarketCap, maxMarketCap, minPe, maxPe,
                minTurnoverRate, maxTurnoverRate, minChangeRate, maxChangeRate,
                excludeSt, excludeDelisted, technicalPattern);
    }

    @Override
    public PageVO<ScreenerResultVO> screenStocksPage(List<String> boards, String industryGroup,
                                                     BigDecimal minMarketCap, BigDecimal maxMarketCap,
                                                     BigDecimal minPe, BigDecimal maxPe,
                                                     BigDecimal minTurnoverRate, BigDecimal maxTurnoverRate,
                                                     BigDecimal minChangeRate, BigDecimal maxChangeRate,
                                                     boolean excludeSt, boolean excludeDelisted,
                                                     String technicalPattern,
                                                     int page, int pageSize) {
        List<ScreenerResultVO> allResults = buildScreenerResult(boards, industryGroup,
                minMarketCap, maxMarketCap, minPe, maxPe,
                minTurnoverRate, maxTurnoverRate, minChangeRate, maxChangeRate,
                excludeSt, excludeDelisted, technicalPattern);
        int fromIndex = Math.min((page - 1) * pageSize, allResults.size());
        int toIndex = Math.min(fromIndex + pageSize, allResults.size());
        return new PageVO<>((long) allResults.size(), page, pageSize, new ArrayList<>(allResults.subList(fromIndex, toIndex)));
    }

    private List<ScreenerResultVO> buildScreenerResult(List<String> boards, String industryGroup,
                                                       BigDecimal minMarketCap, BigDecimal maxMarketCap,
                                                       BigDecimal minPe, BigDecimal maxPe,
                                                       BigDecimal minTurnoverRate, BigDecimal maxTurnoverRate,
                                                       BigDecimal minChangeRate, BigDecimal maxChangeRate,
                                                       boolean excludeSt, boolean excludeDelisted,
                                                       String technicalPattern) {
        String cacheKey = buildScreenerCacheKey(boards, industryGroup,
                minMarketCap, maxMarketCap, minPe, maxPe,
                minTurnoverRate, maxTurnoverRate, minChangeRate, maxChangeRate,
                excludeSt, excludeDelisted, technicalPattern);
        CachedScreenerResult cached = screenerCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.results();
        }

        List<StockScreenerCandidateDTO> candidates = buildFallbackScreenerCandidates(boards, industryGroup);
        if (candidates.isEmpty()) {
            log.warn("Fallback screener pool returned no data, retrying EastMoney candidate API");
            candidates = eastMoneyClient.getScreenerCandidates(boards, SCREENING_MAX_PAGES);
            hydrateCandidatesFromStockInfo(candidates);
        }
        List<ScreenerResultVO> result = new ArrayList<>();
        for (StockScreenerCandidateDTO candidate : candidates) {
            if (!matchesBoard(candidate.getStockCode(), candidate.getMarket(), boards)) {
                continue;
            }
            if (!matchesIndustryGroup(candidate.getIndustry(), industryGroup)) {
                continue;
            }
            if (!matchesFlags(candidate, excludeSt, excludeDelisted)) {
                continue;
            }
            if (!matchesNumericFilters(candidate, minMarketCap, maxMarketCap, minPe, maxPe,
                    minTurnoverRate, maxTurnoverRate, minChangeRate, maxChangeRate)) {
                continue;
            }
            if (!matchesTechnicalPattern(candidate, technicalPattern)) {
                continue;
            }

            ScreenerResultVO item = new ScreenerResultVO();
            item.setStockCode(candidate.getStockCode());
            item.setStockName(candidate.getStockName());
            item.setMarket(candidate.getMarket());
            item.setBoard(candidate.getBoard());
            item.setIndustry(candidate.getIndustry());
            item.setSt(candidate.getIsSt() != null && candidate.getIsSt() == 1);
            item.setDelisted(candidate.getIsDelisted() != null && candidate.getIsDelisted() == 1);
            item.setCurrentPrice(candidate.getCurrentPrice());
            item.setChangeRate(candidate.getChangeRate());
            item.setTurnoverRate(candidate.getTurnoverRate());
            item.setPe(candidate.getPe());
            item.setTotalMarketCap(candidate.getTotalMarketCap());
            result.add(item);
            if (result.size() >= SCREENING_RESULT_LIMIT) {
                break;
            }
        }

        result.sort((left, right) -> {
            BigDecimal rightChange = right.getChangeRate() == null ? ZERO : right.getChangeRate();
            BigDecimal leftChange = left.getChangeRate() == null ? ZERO : left.getChangeRate();
            return rightChange.compareTo(leftChange);
        });
        screenerCache.put(cacheKey, new CachedScreenerResult(result, Instant.now().plus(SCREENER_CACHE_TTL)));
        return result;
    }

    private List<StockScreenerCandidateDTO> buildFallbackScreenerCandidates(List<String> boards, String industryGroup) {
        LinkedHashSet<String> queries = buildFallbackQueries(boards, industryGroup);
        Map<String, StockScreenerCandidateDTO> pool = new LinkedHashMap<>();

        for (String query : queries) {
            for (Map<String, String> item : eastMoneyClient.searchStocks(query)) {
                String stockCode = safeValue(item.get("stockCode"));
                String market = safeValue(item.get("market"));
                if (!StringUtils.hasText(stockCode) || !List.of("SH", "SZ").contains(market)) {
                    continue;
                }
                if (!matchesBoard(stockCode, market, boards)) {
                    continue;
                }

                String key = market + ":" + stockCode;
                StockScreenerCandidateDTO candidate = pool.computeIfAbsent(key, unused -> {
                    StockScreenerCandidateDTO dto = new StockScreenerCandidateDTO();
                    dto.setStockCode(stockCode);
                    dto.setStockName(item.get("stockName"));
                    dto.setMarket(market);
                    dto.setBoard(StockInfo.resolveBoard(stockCode, market));
                    dto.setIsSt(StockInfo.isStStock(item.get("stockName")) ? 1 : 0);
                    dto.setIsDelisted(StockInfo.isDelistedStock(item.get("stockName")) ? 1 : 0);
                    return dto;
                });

                if (!StringUtils.hasText(candidate.getIndustry())) {
                    candidate.setIndustry(resolveFallbackIndustry(query, item.get("stockName")));
                }

                if (pool.size() >= FALLBACK_SCREENING_POOL_LIMIT) {
                    break;
                }
            }
            if (pool.size() >= FALLBACK_SCREENING_POOL_LIMIT) {
                break;
            }
        }

        Map<String, StockRealtimeDTO> quoteMap = eastMoneyClient.getBatchRealtimeQuotes(new ArrayList<>(pool.values()));
        List<StockScreenerCandidateDTO> enriched = new ArrayList<>();
        for (StockScreenerCandidateDTO candidate : pool.values()) {
            StockRealtimeDTO realtimeDTO = quoteMap.get(candidate.getMarket() + ":" + candidate.getStockCode());
            if (realtimeDTO == null || realtimeDTO.getCurrentPrice() == null
                    || realtimeDTO.getCurrentPrice().compareTo(ZERO) <= 0) {
                continue;
            }
            candidate.setCurrentPrice(realtimeDTO.getCurrentPrice());
            candidate.setChangeRate(realtimeDTO.getChangeRate());
            candidate.setTurnoverRate(realtimeDTO.getTurnoverRate());
            candidate.setPe(realtimeDTO.getPe());
            candidate.setTotalMarketCap(realtimeDTO.getTotalMarketCap());
            if (!StringUtils.hasText(candidate.getIndustry()) && StringUtils.hasText(realtimeDTO.getIndustry())) {
                candidate.setIndustry(realtimeDTO.getIndustry());
            }
            enriched.add(candidate);
        }
        return enriched;
    }

    private void hydrateCandidatesFromStockInfo(List<StockScreenerCandidateDTO> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        Map<String, StockInfo> byCode = new HashMap<>();
        for (StockInfo stockInfo : stockInfoMapper.selectList(new LambdaQueryWrapper<>())) {
            byCode.put(stockInfo.getStockCode(), stockInfo);
        }

        for (StockScreenerCandidateDTO candidate : candidates) {
            StockInfo stockInfo = byCode.get(candidate.getStockCode());
            if (stockInfo != null) {
                if (!StringUtils.hasText(candidate.getStockName())) {
                    candidate.setStockName(stockInfo.getStockName());
                }
                if (!StringUtils.hasText(candidate.getMarket())) {
                    candidate.setMarket(stockInfo.getMarket());
                }
                if (!StringUtils.hasText(candidate.getBoard())) {
                    candidate.setBoard(stockInfo.getBoard());
                }
                if (!StringUtils.hasText(candidate.getIndustry())) {
                    candidate.setIndustry(stockInfo.getIndustry());
                }
                if (candidate.getIsSt() == null) {
                    candidate.setIsSt(stockInfo.getIsSt());
                }
                if (candidate.getIsDelisted() == null) {
                    candidate.setIsDelisted(stockInfo.getIsDelisted());
                }
            }

            if (!StringUtils.hasText(candidate.getBoard())) {
                candidate.setBoard(StockInfo.resolveBoard(candidate.getStockCode(), candidate.getMarket()));
            }
            if (candidate.getIsSt() == null) {
                candidate.setIsSt(StockInfo.isStStock(candidate.getStockName()) ? 1 : 0);
            }
            if (candidate.getIsDelisted() == null) {
                candidate.setIsDelisted(StockInfo.isDelistedStock(candidate.getStockName()) ? 1 : 0);
            }
        }
    }

    private LinkedHashSet<String> buildFallbackQueries(List<String> boards, String industryGroup) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        List<String> normalizedBoards = boards == null || boards.isEmpty()
                ? List.of("sh_main", "sz_main", "cyb", "star")
                : boards;

        for (String board : normalizedBoards) {
            queries.addAll(FALLBACK_BOARD_QUERIES.getOrDefault(safeValue(board).trim(), List.of()));
        }

        if (!StringUtils.hasText(industryGroup) || "all".equalsIgnoreCase(industryGroup)) {
            queries.addAll(FALLBACK_ALL_QUERIES);
            return queries;
        }

        Set<String> industryKeywords = INDUSTRY_GROUPS.get(safeValue(industryGroup).toLowerCase());
        if (industryKeywords != null) {
            queries.addAll(industryKeywords);
        }
        return queries;
    }

    private String resolveFallbackIndustry(String query, String stockName) {
        for (Map.Entry<String, Set<String>> entry : INDUSTRY_GROUPS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (safeValue(query).contains(keyword) || safeValue(stockName).contains(keyword)) {
                    return keyword;
                }
            }
        }
        return query.chars().allMatch(Character::isDigit) ? null : query;
    }

    /**
     * 计算 MACD 指标
     * EMA12, EMA26, DIF = EMA12 - EMA26, DEA = EMA(DIF, 9), MACD = (DIF - DEA) * 2
     */
    private List<IndicatorVO> calculateMacd(List<KlineVO> klines) {
        int n = klines.size();
        BigDecimal[] closes = extractCloses(klines);
        BigDecimal[] ema12 = calculateEma(closes, 12);
        BigDecimal[] ema26 = calculateEma(closes, 26);

        BigDecimal[] dif = new BigDecimal[n];
        for (int i = 0; i < n; i++) {
            dif[i] = ema12[i].subtract(ema26[i]);
        }

        BigDecimal[] dea = calculateEma(dif, 9);

        List<IndicatorVO> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            IndicatorVO vo = new IndicatorVO();
            vo.setDate(klines.get(i).getDate());
            vo.setType("macd");

            Map<String, Object> data = new HashMap<>();
            // 前25条数据不足以计算（EMA26需要26条）
            if (i < 25) {
                data.put("dif", null);
                data.put("dea", null);
                data.put("macd", null);
            } else {
                BigDecimal macdBar = dif[i].subtract(dea[i]).multiply(TWO);
                data.put("dif", round4(dif[i]));
                data.put("dea", round4(dea[i]));
                data.put("macd", round4(macdBar));
            }
            vo.setData(data);
            result.add(vo);
        }
        return result;
    }

    /**
     * 计算 KDJ 指标
     * 参数：周期9，K平滑3，D平滑3
     */
    private List<IndicatorVO> calculateKdj(List<KlineVO> klines) {
        int n = klines.size();
        BigDecimal[] k = new BigDecimal[n];
        BigDecimal[] d = new BigDecimal[n];
        BigDecimal[] j = new BigDecimal[n];

        // 初始化 K D
        k[0] = new BigDecimal("50");
        d[0] = new BigDecimal("50");

        for (int i = 0; i < n; i++) {
            // 计算前9天的最高价和最低价
            int start = Math.max(0, i - 8);
            BigDecimal highMax = null;
            BigDecimal lowMin = null;
            for (int x = start; x <= i; x++) {
                BigDecimal high = klines.get(x).getHigh();
                BigDecimal low = klines.get(x).getLow();
                if (high != null) {
                    highMax = highMax == null || high.compareTo(highMax) > 0 ? high : highMax;
                }
                if (low != null) {
                    lowMin = lowMin == null || low.compareTo(lowMin) < 0 ? low : lowMin;
                }
            }

            BigDecimal close = klines.get(i).getClose();
            BigDecimal closeVal = close != null ? close : ZERO;

            BigDecimal rsv;
            if (highMax == null || lowMin == null || highMax.compareTo(lowMin) == 0) {
                rsv = new BigDecimal("50");
            } else {
                rsv = closeVal.subtract(lowMin)
                        .divide(highMax.subtract(lowMin), CALC_SCALE, RoundingMode.HALF_UP)
                        .multiply(ONE_HUNDRED);
            }

            if (i == 0) {
                k[i] = rsv;
                d[i] = rsv;
            } else {
                k[i] = k[i - 1].multiply(TWO_THIRDS).add(rsv.multiply(ONE_THIRD));
                d[i] = d[i - 1].multiply(TWO_THIRDS).add(k[i].multiply(ONE_THIRD));
            }
            j[i] = k[i].multiply(THREE).subtract(d[i].multiply(TWO));
        }

        List<IndicatorVO> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            IndicatorVO vo = new IndicatorVO();
            vo.setDate(klines.get(i).getDate());
            vo.setType("kdj");

            Map<String, Object> data = new HashMap<>();
            if (i < 8) {
                data.put("k", null);
                data.put("d", null);
                data.put("j", null);
            } else {
                data.put("k", round2(k[i]));
                data.put("d", round2(d[i]));
                data.put("j", round2(j[i]));
            }
            vo.setData(data);
            result.add(vo);
        }
        return result;
    }

    /**
     * 计算 RSI 指标
     * 参数：6, 12, 24
     */
    private List<IndicatorVO> calculateRsi(List<KlineVO> klines) {
        int n = klines.size();
        BigDecimal[] closes = extractCloses(klines);

        BigDecimal[] rsi6 = calculateRsiPeriod(closes, 6);
        BigDecimal[] rsi12 = calculateRsiPeriod(closes, 12);
        BigDecimal[] rsi24 = calculateRsiPeriod(closes, 24);

        List<IndicatorVO> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            IndicatorVO vo = new IndicatorVO();
            vo.setDate(klines.get(i).getDate());
            vo.setType("rsi");

            Map<String, Object> data = new HashMap<>();
            data.put("rsi6", i < 5 ? null : round2(rsi6[i]));
            data.put("rsi12", i < 11 ? null : round2(rsi12[i]));
            data.put("rsi24", i < 23 ? null : round2(rsi24[i]));
            vo.setData(data);
            result.add(vo);
        }
        return result;
    }

    private List<IndicatorVO> calculateMa(List<KlineVO> klines) {
        List<IndicatorVO> result = new ArrayList<>();
        BigDecimal[] closes = extractCloses(klines);
        for (int i = 0; i < klines.size(); i++) {
            IndicatorVO vo = new IndicatorVO();
            vo.setDate(klines.get(i).getDate());
            vo.setType("ma");

            Map<String, Object> data = new HashMap<>();
            data.put("ma5", movingAverage(closes, i, 5));
            data.put("ma10", movingAverage(closes, i, 10));
            data.put("ma20", movingAverage(closes, i, 20));
            vo.setData(data);
            result.add(vo);
        }
        return result;
    }

    private List<IndicatorVO> calculateBoll(List<KlineVO> klines) {
        List<IndicatorVO> result = new ArrayList<>();
        BigDecimal[] closes = extractCloses(klines);
        for (int i = 0; i < klines.size(); i++) {
            IndicatorVO vo = new IndicatorVO();
            vo.setDate(klines.get(i).getDate());
            vo.setType("boll");

            Map<String, Object> data = new HashMap<>();
            if (i < 19) {
                data.put("mid", null);
                data.put("upper", null);
                data.put("lower", null);
            } else {
                BigDecimal mid = movingAverage(closes, i, 20);
                BigDecimal stdDev = standardDeviation(closes, i, 20, mid);
                data.put("mid", mid);
                data.put("upper", round2(mid.add(stdDev.multiply(TWO))));
                data.put("lower", round2(mid.subtract(stdDev.multiply(TWO))));
            }
            vo.setData(data);
            result.add(vo);
        }
        return result;
    }

    /**
     * 计算指定周期的 RSI
     */
    private BigDecimal[] calculateRsiPeriod(BigDecimal[] closes, int period) {
        int n = closes.length;
        BigDecimal[] rsi = new BigDecimal[n];

        BigDecimal avgGain = ZERO;
        BigDecimal avgLoss = ZERO;

        // 初始化前 period 个
        for (int i = 1; i <= period && i < n; i++) {
            BigDecimal change = closes[i].subtract(closes[i - 1]);
            if (change.compareTo(ZERO) > 0) {
                avgGain = avgGain.add(change);
            } else {
                avgLoss = avgLoss.add(change.abs());
            }
        }
        avgGain = avgGain.divide(BigDecimal.valueOf(period), CALC_SCALE, RoundingMode.HALF_UP);
        avgLoss = avgLoss.divide(BigDecimal.valueOf(period), CALC_SCALE, RoundingMode.HALF_UP);

        if (period < n) {
            rsi[period] = computeRsi(avgGain, avgLoss);
        }

        for (int i = period + 1; i < n; i++) {
            BigDecimal change = closes[i].subtract(closes[i - 1]);
            BigDecimal gain = change.compareTo(ZERO) > 0 ? change : ZERO;
            BigDecimal loss = change.compareTo(ZERO) < 0 ? change.abs() : ZERO;

            avgGain = avgGain.multiply(BigDecimal.valueOf(period - 1L))
                    .add(gain)
                    .divide(BigDecimal.valueOf(period), CALC_SCALE, RoundingMode.HALF_UP);
            avgLoss = avgLoss.multiply(BigDecimal.valueOf(period - 1L))
                    .add(loss)
                    .divide(BigDecimal.valueOf(period), CALC_SCALE, RoundingMode.HALF_UP);

            rsi[i] = computeRsi(avgGain, avgLoss);
        }

        return rsi;
    }

    /**
     * 计算指数移动平均线 EMA
     */
    private BigDecimal[] calculateEma(BigDecimal[] data, int period) {
        int n = data.length;
        BigDecimal[] ema = new BigDecimal[n];
        BigDecimal multiplier = TWO.divide(BigDecimal.valueOf(period + 1L), CALC_SCALE, RoundingMode.HALF_UP);
        BigDecimal complement = ONE.subtract(multiplier);

        ema[0] = data[0];
        for (int i = 1; i < n; i++) {
            ema[i] = data[i].multiply(multiplier).add(ema[i - 1].multiply(complement));
        }
        return ema;
    }

    private BigDecimal[] extractCloses(List<KlineVO> klines) {
        BigDecimal[] closes = new BigDecimal[klines.size()];
        for (int i = 0; i < klines.size(); i++) {
            BigDecimal close = klines.get(i).getClose();
            closes[i] = close != null ? close : ZERO;
        }
        return closes;
    }

    private BigDecimal computeRsi(BigDecimal avgGain, BigDecimal avgLoss) {
        if (avgLoss.compareTo(ZERO) == 0) {
            return ONE_HUNDRED.setScale(CALC_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal rs = avgGain.divide(avgLoss, CALC_SCALE, RoundingMode.HALF_UP);
        return ONE_HUNDRED.subtract(
                ONE_HUNDRED.divide(ONE.add(rs), CALC_SCALE, RoundingMode.HALF_UP)
        );
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "搜索关键词不能为空");
        }
        return keyword.trim();
    }

    private String normalizePeriod(String period) {
        String normalizedPeriod = period == null ? "day" : period.trim().toLowerCase();
        if (!List.of("1s", "1min", "5min", "15min", "30min", "60min", "day", "week", "month").contains(normalizedPeriod)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "period 仅支持 1s/1min/5min/15min/30min/60min/day/week/month");
        }
        return normalizedPeriod;
    }

    private List<String> normalizeIndicatorTypes(String indicatorType) {
        String rawType = indicatorType == null ? "macd" : indicatorType.trim().toLowerCase();
        List<String> normalizedTypes = java.util.Arrays.stream(rawType.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (normalizedTypes.isEmpty() || !List.of("macd", "kdj", "rsi", "ma", "boll").containsAll(normalizedTypes)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "不支持的指标类型: " + indicatorType + "，支持: macd, kdj, rsi, ma, boll");
        }
        return normalizedTypes;
    }

    private List<IndicatorVO> calculateIndicator(String indicatorType, List<KlineVO> klines) {
        return switch (indicatorType) {
            case "macd" -> calculateMacd(klines);
            case "kdj" -> calculateKdj(klines);
            case "rsi" -> calculateRsi(klines);
            case "ma" -> calculateMa(klines);
            case "boll" -> calculateBoll(klines);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "不支持的指标类型: " + indicatorType + "，支持: macd, kdj, rsi, ma, boll");
        };
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > 500) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "limit 需在 1 到 500 之间");
        }
    }

    private StockInfo getStockInfo(String stockCode) {
        List<StockInfo> storedMatches = stockInfoMapper.selectList(
                new LambdaQueryWrapper<StockInfo>().eq(StockInfo::getStockCode, stockCode));
        StockInfo stockInfo = selectPreferredStoredStockInfo(stockCode, storedMatches);
        if (stockInfo != null) {
            if (needsRemoteRefresh(stockInfo)) {
                StockInfo refreshed = refreshStockInfoFromSearch(stockCode, "SEARCH");
                if (refreshed != null) {
                    return refreshed;
                }
            }
            return stockInfo;
        }

        StockInfo created = refreshStockInfoFromSearch(stockCode, "SEARCH");
        if (created != null) {
            return created;
        }
        throw new BusinessException(ErrorCode.STOCK_NOT_FOUND, "股票不存在: " + stockCode);
    }

    private StockInfo selectPreferredStoredStockInfo(String stockCode, List<StockInfo> matches) {
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        String preferredMarket = preferredMarket(stockCode);
        for (StockInfo match : matches) {
            if (preferredMarket.equalsIgnoreCase(safeValue(match.getMarket()))
                    && StockInfo.isTradableAStock(match.getStockCode(), match.getMarket())) {
                return match;
            }
        }
        for (StockInfo match : matches) {
            if (StockInfo.isTradableAStock(match.getStockCode(), match.getMarket())) {
                return match;
            }
        }
        for (StockInfo match : matches) {
            if (preferredMarket.equalsIgnoreCase(safeValue(match.getMarket()))) {
                return match;
            }
        }
        return null;
    }

    private BigDecimal round2(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal round4(BigDecimal value) {
        return value == null ? null : value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal movingAverage(BigDecimal[] closes, int endIndex, int period) {
        if (endIndex + 1 < period) {
            return null;
        }
        BigDecimal sum = ZERO;
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            sum = sum.add(closes[i]);
        }
        return round2(sum.divide(BigDecimal.valueOf(period), CALC_SCALE, RoundingMode.HALF_UP));
    }

    private BigDecimal standardDeviation(BigDecimal[] closes, int endIndex, int period, BigDecimal average) {
        BigDecimal variance = ZERO;
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            BigDecimal diff = closes[i].subtract(average);
            variance = variance.add(diff.multiply(diff));
        }
        variance = variance.divide(BigDecimal.valueOf(period), CALC_SCALE, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue())).setScale(CALC_SCALE, RoundingMode.HALF_UP);
    }

    private String uniqueSearchKey(Map<String, String> item) {
        return safeValue(item.get("market")) + ":" + safeValue(item.get("stockCode"));
    }

    private int searchScore(Map<String, String> item, String keyword) {
        String normalizedKeyword = safeValue(keyword).toLowerCase();
        String stockCode = safeValue(item.get("stockCode")).toLowerCase();
        String stockName = safeValue(item.get("stockName")).toLowerCase();

        int score = 0;
        if (stockCode.equals(normalizedKeyword)) {
            score += 100;
        } else if (stockCode.startsWith(normalizedKeyword)) {
            score += 70;
        } else if (stockCode.contains(normalizedKeyword)) {
            score += 40;
        }

        if (stockName.equals(normalizedKeyword)) {
            score += 95;
        } else if (stockName.startsWith(normalizedKeyword)) {
            score += 65;
        } else if (stockName.contains(normalizedKeyword)) {
            score += 35;
        }

        if (StringUtils.hasText(item.get("industry")) && !"指数".equals(item.get("industry"))) {
            score += 1;
        }
        return score;
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private boolean shouldPreferRemoteDisplayValue(String existingValue, String remoteValue) {
        return StringUtils.hasText(remoteValue)
                && (!StringUtils.hasText(existingValue) || TextEncodingUtils.hasCorruptedDisplayText(existingValue));
    }

    private void enrichSearchResult(Map<String, String> item) {
        String stockCode = item.get("stockCode");
        String market = item.get("market");
        if (!StringUtils.hasText(stockCode) || !StringUtils.hasText(market)) {
            return;
        }
        if (!TextEncodingUtils.hasCorruptedDisplayText(item.get("stockName"))
                && !TextEncodingUtils.hasCorruptedDisplayText(item.get("industry"))) {
            return;
        }
        StockRealtimeDTO realtimeDTO = eastMoneyClient.getRealtimeQuote(stockCode, StockInfo.toEastMoneyMarketCode(market));
        if (realtimeDTO == null) {
            return;
        }
        if (shouldPreferRemoteDisplayValue(item.get("stockName"), realtimeDTO.getStockName())) {
            item.put("stockName", realtimeDTO.getStockName());
        }
        if (shouldPreferRemoteDisplayValue(item.get("industry"), realtimeDTO.getIndustry())) {
            item.put("industry", realtimeDTO.getIndustry());
        }
    }

    private boolean matchesBoard(String stockCode, String market, List<String> boards) {
        if (boards == null || boards.isEmpty()) {
            return true;
        }
        for (String board : boards) {
            if (matchesSingleBoard(stockCode, market, board)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesSingleBoard(String stockCode, String market, String board) {
        return switch (safeValue(board).trim()) {
            case "sh_main" -> "SH".equals(market) && !stockCode.startsWith("688");
            case "sz_main" -> "SZ".equals(market) && !stockCode.startsWith("300") && !stockCode.startsWith("301");
            case "cyb" -> stockCode.startsWith("300") || stockCode.startsWith("301");
            case "star" -> stockCode.startsWith("688");
            default -> false;
        };
    }

    private boolean matchesIndustryGroup(String industry, String industryGroup) {
        if (!StringUtils.hasText(industryGroup) || "all".equalsIgnoreCase(industryGroup)) {
            return true;
        }
        Set<String> industries = INDUSTRY_GROUPS.get(safeValue(industryGroup).toLowerCase());
        if (industries == null) {
            return true;
        }
        if (!StringUtils.hasText(industry)) {
            return false;
        }
        return industries.stream().anyMatch(industry::contains);
    }

    private boolean matchesNumericFilters(StockScreenerCandidateDTO candidate,
                                          BigDecimal minMarketCap, BigDecimal maxMarketCap,
                                          BigDecimal minPe, BigDecimal maxPe,
                                          BigDecimal minTurnoverRate, BigDecimal maxTurnoverRate,
                                          BigDecimal minChangeRate, BigDecimal maxChangeRate) {
        return candidate.getCurrentPrice() != null
                && candidate.getCurrentPrice().compareTo(ZERO) > 0
                && inRange(candidate.getTotalMarketCap(), minMarketCap, maxMarketCap)
                && inRange(candidate.getPe(), minPe, maxPe)
                && inRange(candidate.getTurnoverRate(), minTurnoverRate, maxTurnoverRate)
                && inRange(candidate.getChangeRate(), minChangeRate, maxChangeRate);
    }

    private boolean matchesFlags(StockScreenerCandidateDTO candidate, boolean excludeSt, boolean excludeDelisted) {
        if (excludeSt && candidate.getIsSt() != null && candidate.getIsSt() == 1) {
            return false;
        }
        if (excludeDelisted && candidate.getIsDelisted() != null && candidate.getIsDelisted() == 1) {
            return false;
        }
        return true;
    }

    private boolean inRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) {
            return min == null && max == null;
        }
        if (min != null && value.compareTo(min) < 0) {
            return false;
        }
        if (max != null && value.compareTo(max) > 0) {
            return false;
        }
        return true;
    }

    private boolean matchesTechnicalPattern(StockScreenerCandidateDTO candidate, String technicalPattern) {
        String pattern = safeValue(technicalPattern).trim().toLowerCase();
        if (!StringUtils.hasText(pattern) || "none".equals(pattern)) {
            return true;
        }

        List<KlineVO> klines = eastMoneyClient.getKlineData(
                candidate.getStockCode(),
                "SH".equals(candidate.getMarket()) ? "1" : "0",
                "day",
                30
        );
        if (klines.size() < 10) {
            return false;
        }

        return switch (pattern) {
            case "ma_cross" -> isMaCross(klines);
            case "macd_golden" -> isMacdGolden(candidate.getStockCode(), candidate.getMarket());
            case "volume_up" -> isVolumeUp(klines);
            case "breakout" -> isBreakout(klines);
            case "near_high" -> isNearHigh(klines);
            case "low_vol_pullback" -> isLowVolumePullback(klines);
            default -> true;
        };
    }

    private boolean isMaCross(List<KlineVO> klines) {
        BigDecimal ma5 = movingAverage(klines, 5);
        BigDecimal ma10 = movingAverage(klines, 10);
        BigDecimal ma20 = movingAverage(klines, 20);
        BigDecimal close = klines.get(klines.size() - 1).getClose();
        return close != null && ma5 != null && ma10 != null && ma20 != null
                && close.compareTo(ma5) > 0
                && ma5.compareTo(ma10) > 0
                && ma10.compareTo(ma20) > 0;
    }

    private boolean isMacdGolden(String stockCode, String market) {
        List<KlineVO> klines = eastMoneyClient.getKlineData(stockCode, "SH".equals(market) ? "1" : "0", "day", 30);
        List<IndicatorVO> indicators = calculateMacd(klines);
        if (indicators.size() < 2) {
            return false;
        }
        Map<String, Object> prev = indicators.get(indicators.size() - 2).getData();
        Map<String, Object> curr = indicators.get(indicators.size() - 1).getData();
        BigDecimal prevDif = asBigDecimal(prev.get("dif"));
        BigDecimal prevDea = asBigDecimal(prev.get("dea"));
        BigDecimal currDif = asBigDecimal(curr.get("dif"));
        BigDecimal currDea = asBigDecimal(curr.get("dea"));
        return prevDif != null && prevDea != null && currDif != null && currDea != null
                && prevDif.compareTo(prevDea) <= 0
                && currDif.compareTo(currDea) > 0;
    }

    private boolean isVolumeUp(List<KlineVO> klines) {
        if (klines.size() < 6) {
            return false;
        }
        KlineVO latest = klines.get(klines.size() - 1);
        BigDecimal avgVolume = averageVolume(klines.subList(klines.size() - 6, klines.size() - 1));
        return latest.getVolume() != null && avgVolume != null
                && BigDecimal.valueOf(latest.getVolume()).compareTo(avgVolume.multiply(new BigDecimal("1.5"))) >= 0;
    }

    private boolean isBreakout(List<KlineVO> klines) {
        if (klines.size() < 21) {
            return false;
        }
        KlineVO latest = klines.get(klines.size() - 1);
        BigDecimal latestClose = latest.getClose();
        BigDecimal highest = ZERO;
        for (int i = klines.size() - 21; i < klines.size() - 1; i++) {
            BigDecimal high = klines.get(i).getHigh();
            if (high != null && high.compareTo(highest) > 0) {
                highest = high;
            }
        }
        return latestClose != null && latestClose.compareTo(highest) >= 0;
    }

    private boolean isNearHigh(List<KlineVO> klines) {
        if (klines.size() < 20) {
            return false;
        }
        KlineVO latest = klines.get(klines.size() - 1);
        BigDecimal latestClose = latest.getClose();
        if (latestClose == null) {
            return false;
        }
        BigDecimal highest = ZERO;
        for (int i = klines.size() - 20; i < klines.size(); i++) {
            BigDecimal high = klines.get(i).getHigh();
            if (high != null && high.compareTo(highest) > 0) {
                highest = high;
            }
        }
        return highest.compareTo(ZERO) > 0
                && latestClose.divide(highest, CALC_SCALE, RoundingMode.HALF_UP).compareTo(new BigDecimal("0.97")) >= 0;
    }

    private boolean isLowVolumePullback(List<KlineVO> klines) {
        if (klines.size() < 12) {
            return false;
        }
        KlineVO latest = klines.get(klines.size() - 1);
        BigDecimal ma10 = movingAverage(klines, 10);
        BigDecimal latestClose = latest.getClose();
        BigDecimal recentVolumeAvg = averageVolume(klines.subList(klines.size() - 6, klines.size() - 1));
        BigDecimal priorVolumeAvg = averageVolume(klines.subList(klines.size() - 11, klines.size() - 6));
        return latestClose != null
                && ma10 != null
                && recentVolumeAvg != null
                && priorVolumeAvg != null
                && latestClose.compareTo(ma10) >= 0
                && recentVolumeAvg.compareTo(priorVolumeAvg) < 0;
    }

    private BigDecimal movingAverage(List<KlineVO> klines, int period) {
        if (klines.size() < period) {
            return null;
        }
        BigDecimal sum = ZERO;
        for (int i = klines.size() - period; i < klines.size(); i++) {
            BigDecimal close = klines.get(i).getClose();
            if (close == null) {
                return null;
            }
            sum = sum.add(close);
        }
        return sum.divide(BigDecimal.valueOf(period), CALC_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal averageVolume(List<KlineVO> klines) {
        if (klines.isEmpty()) {
            return null;
        }
        BigDecimal total = ZERO;
        for (KlineVO kline : klines) {
            if (kline.getVolume() == null) {
                return null;
            }
            total = total.add(BigDecimal.valueOf(kline.getVolume()));
        }
        return total.divide(BigDecimal.valueOf(klines.size()), CALC_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal asBigDecimal(Object value) {
        return value instanceof BigDecimal ? (BigDecimal) value : null;
    }

    private void persistSearchResults(List<Map<String, String>> results) {
        for (Map<String, String> item : results) {
            if (!List.of("SH", "SZ").contains(item.get("market"))) {
                continue;
            }
            upsertStockInfo(item, item.get("industry"), "SEARCH");
        }
    }

    private boolean isTradeableAStock(Map<String, String> item) {
        if (item == null) {
            return false;
        }
        String stockCode = safeValue(item.get("stockCode"));
        String market = safeValue(item.get("market")).toUpperCase();
        if (!StringUtils.hasText(stockCode) || !StringUtils.hasText(market)) {
            return false;
        }
        return StockInfo.isTradableAStock(stockCode, market);
    }

    private void persistCandidate(StockScreenerCandidateDTO candidate, String source) {
        if (candidate == null || !StringUtils.hasText(candidate.getStockCode())) {
            return;
        }
        Map<String, String> item = new HashMap<>();
        item.put("stockCode", candidate.getStockCode());
        item.put("stockName", candidate.getStockName());
        item.put("market", candidate.getMarket());
        item.put("industry", candidate.getIndustry());
        item.put("board", candidate.getBoard());
        upsertStockInfo(item, candidate.getIndustry(), source);
    }

    private StockInfo upsertStockInfo(Map<String, String> item, String fallbackIndustry, String source) {
        String stockCode = safeValue(item.get("stockCode"));
        String stockName = safeValue(item.get("stockName"));
        String market = safeValue(item.get("market"));
        if (!StringUtils.hasText(stockCode) || !StringUtils.hasText(stockName) || !List.of("SH", "SZ").contains(market)) {
            return null;
        }

        StockInfo existing = stockInfoMapper.selectOne(new LambdaQueryWrapper<StockInfo>()
                .eq(StockInfo::getStockCode, stockCode));
        String industry = StringUtils.hasText(item.get("industry")) ? item.get("industry") : fallbackIndustry;
        String board = StringUtils.hasText(item.get("board")) ? item.get("board") : StockInfo.resolveBoard(stockCode, market);
        int isSt = StockInfo.isStStock(stockName) ? 1 : 0;
        int isDelisted = StockInfo.isDelistedStock(stockName) ? 1 : 0;

        if (existing == null) {
            StockInfo created = StockInfo.builder()
                    .stockCode(stockCode)
                    .stockName(stockName)
                    .market(market)
                    .board(board)
                    .industry(industry)
                    .isSt(isSt)
                    .isDelisted(isDelisted)
                    .source(source)
                    .status(isDelisted == 1 ? 0 : 1)
                    .build();
            stockInfoMapper.insert(created);
            return created;
        }

        boolean changed = false;
        if (!stockName.equals(existing.getStockName())) {
            existing.setStockName(stockName);
            changed = true;
        }
        if (!market.equals(existing.getMarket())) {
            existing.setMarket(market);
            changed = true;
        }
        if (StringUtils.hasText(board) && !board.equals(existing.getBoard())) {
            existing.setBoard(board);
            changed = true;
        }
        if (StringUtils.hasText(industry) && !industry.equals(existing.getIndustry())) {
            existing.setIndustry(industry);
            changed = true;
        }
        if (!Integer.valueOf(isSt).equals(existing.getIsSt())) {
            existing.setIsSt(isSt);
            changed = true;
        }
        if (!Integer.valueOf(isDelisted).equals(existing.getIsDelisted())) {
            existing.setIsDelisted(isDelisted);
            changed = true;
        }
        if (StringUtils.hasText(source) && !source.equals(existing.getSource())) {
            existing.setSource(source);
            changed = true;
        }
        existing.setStatus(isDelisted == 1 ? 0 : 1);
        if (changed) {
            stockInfoMapper.updateById(existing);
        }
        return existing;
    }

    private String preferredMarket(String stockCode) {
        return stockCode != null && stockCode.startsWith("6") ? "SH" : "SZ";
    }

    private boolean needsRemoteRefresh(StockInfo stockInfo) {
        return stockInfo == null
                || TextEncodingUtils.hasCorruptedDisplayText(stockInfo.getStockName())
                || TextEncodingUtils.hasCorruptedDisplayText(stockInfo.getIndustry())
                || !StringUtils.hasText(stockInfo.getMarket());
    }

    private StockInfo refreshStockInfoFromSearch(String stockCode, String source) {
        for (Map<String, String> remote : eastMoneyClient.searchStocks(stockCode)) {
            if (!stockCode.equals(remote.get("stockCode")) || !isTradeableAStock(remote)) {
                continue;
            }
            String market = remote.get("market");
            if (StringUtils.hasText(market)) {
                StockRealtimeDTO realtimeDTO = eastMoneyClient.getRealtimeQuote(stockCode, StockInfo.toEastMoneyMarketCode(market));
                if (realtimeDTO != null) {
                    if (StringUtils.hasText(realtimeDTO.getStockName())) {
                        remote.put("stockName", realtimeDTO.getStockName());
                    }
                    if (StringUtils.hasText(realtimeDTO.getIndustry())) {
                        remote.put("industry", realtimeDTO.getIndustry());
                    }
                }
            }
            StockInfo created = upsertStockInfo(remote, remote.get("industry"), source);
            if (created != null) {
                return created;
            }
        }
        return null;
    }

    private boolean shouldRefreshIndustry(StockInfo stockInfo, String currentIndustry) {
        return stockInfo != null
                && StockInfo.isTradableAStock(stockInfo.getStockCode(), stockInfo.getMarket())
                && "指数".equals(currentIndustry);
    }

    private void enrichAndSaveStockInfo(StockInfo stockInfo, StockRealtimeDTO realtimeDTO, String source) {
        if (stockInfo == null) {
            return;
        }
        boolean changed = false;
        if (realtimeDTO != null
                && StringUtils.hasText(realtimeDTO.getStockName())
                && (TextEncodingUtils.hasCorruptedDisplayText(stockInfo.getStockName())
                || !realtimeDTO.getStockName().equals(stockInfo.getStockName()))) {
            stockInfo.setStockName(realtimeDTO.getStockName());
            changed = true;
        }
        if (!StringUtils.hasText(stockInfo.getBoard())) {
            stockInfo.setBoard(StockInfo.resolveBoard(stockInfo.getStockCode(), stockInfo.getMarket()));
            changed = true;
        }
        if ((!StringUtils.hasText(stockInfo.getIndustry()) || shouldRefreshIndustry(stockInfo, stockInfo.getIndustry()))
                && realtimeDTO != null
                && StringUtils.hasText(realtimeDTO.getIndustry())) {
            stockInfo.setIndustry(realtimeDTO.getIndustry());
            changed = true;
        } else if (realtimeDTO != null
                && StringUtils.hasText(realtimeDTO.getIndustry())
                && TextEncodingUtils.hasCorruptedDisplayText(stockInfo.getIndustry())) {
            stockInfo.setIndustry(realtimeDTO.getIndustry());
            changed = true;
        }
        int isSt = StockInfo.isStStock(stockInfo.getStockName()) ? 1 : 0;
        int isDelisted = StockInfo.isDelistedStock(stockInfo.getStockName()) ? 1 : 0;
        if (!Integer.valueOf(isSt).equals(stockInfo.getIsSt())) {
            stockInfo.setIsSt(isSt);
            changed = true;
        }
        if (!Integer.valueOf(isDelisted).equals(stockInfo.getIsDelisted())) {
            stockInfo.setIsDelisted(isDelisted);
            changed = true;
        }
        if (StringUtils.hasText(source) && !source.equals(stockInfo.getSource())) {
            stockInfo.setSource(source);
            changed = true;
        }
        stockInfo.setStatus(isDelisted == 1 ? 0 : 1);
        if (changed) {
            stockInfoMapper.updateById(stockInfo);
        }
    }

    private String buildScreenerCacheKey(List<String> boards, String industryGroup,
                                         BigDecimal minMarketCap, BigDecimal maxMarketCap,
                                         BigDecimal minPe, BigDecimal maxPe,
                                         BigDecimal minTurnoverRate, BigDecimal maxTurnoverRate,
                                         BigDecimal minChangeRate, BigDecimal maxChangeRate,
                                         boolean excludeSt, boolean excludeDelisted,
                                         String technicalPattern) {
        return String.join("|",
                boards == null ? "" : String.join(",", boards),
                safeValue(industryGroup),
                stringify(minMarketCap), stringify(maxMarketCap),
                stringify(minPe), stringify(maxPe),
                stringify(minTurnoverRate), stringify(maxTurnoverRate),
                stringify(minChangeRate), stringify(maxChangeRate),
                Boolean.toString(excludeSt),
                Boolean.toString(excludeDelisted),
                safeValue(technicalPattern));
    }

    private String stringify(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private record MarketTarget(String stockCode, String market, String stockName) {
    }

    private record CachedScreenerResult(List<ScreenerResultVO> results, Instant expiresAt) {
        private boolean isExpired() {
            return expiresAt == null || Instant.now().isAfter(expiresAt);
        }
    }

    private List<Map<String, String>> buildPresetSearchResults(String keyword) {
        String normalizedKeyword = safeValue(keyword).toLowerCase();
        List<Map<String, String>> result = new ArrayList<>();
        for (SearchPreset preset : SEARCH_PRESETS) {
            boolean matched = preset.stockCode().equalsIgnoreCase(normalizedKeyword)
                    || preset.stockName().toLowerCase().contains(normalizedKeyword)
                    || preset.aliases().stream().anyMatch(alias -> alias.toLowerCase().contains(normalizedKeyword));
            if (!matched) {
                continue;
            }

            Map<String, String> item = new HashMap<>();
            item.put("stockCode", preset.stockCode());
            item.put("stockName", preset.stockName());
            item.put("market", preset.market());
            item.put("industry", preset.industry());
            result.add(item);
        }
        return result;
    }

    private record SearchPreset(String stockCode, String market, String stockName, String industry, List<String> aliases) {
    }
}
