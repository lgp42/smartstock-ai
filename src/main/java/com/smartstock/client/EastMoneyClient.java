package com.smartstock.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstock.service.support.RealtimeKlineAssembler;
import com.smartstock.service.support.RealtimeSecondSeriesAssembler;
import com.smartstock.vo.KlineVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EastMoneyClient {

    private final RemoteHttpClient remoteHttpClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${eastmoney.base-url}")
    private String baseUrl;

    @Value("${eastmoney.his-url}")
    private String hisUrl;

    @Value("${eastmoney.search-url}")
    private String searchUrl;

    private static final String REALTIME_CACHE_PREFIX = "stock:realtime:v2:";
    private static final String KLINE_CACHE_PREFIX = "stock:kline:";
    private static final Duration REALTIME_CACHE_TTL = Duration.ofSeconds(1);
    private static final Duration KLINE_CACHE_TTL = Duration.ofMinutes(5);
    private static final String SCREENING_UT = "bd1d9ddb04089700cf9c27f6f7426281";
    private static final String TENCENT_QUOTE_URL = "https://qt.gtimg.cn/q=";
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TENCENT_QUOTE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private Clock clock = Clock.system(SHANGHAI_ZONE);
    private Clock secondSeriesClock = clock;
    private RealtimeSecondSeriesAssembler realtimeSecondSeriesAssembler = new RealtimeSecondSeriesAssembler(clock);

    /**
     * 获取实时报价
     * 上海市场 market="1"，深圳市场 market="0"
     */
    public StockRealtimeDTO getRealtimeQuote(String stockCode, String market) {
        String cacheKey = REALTIME_CACHE_PREFIX + market + ":" + stockCode;
        // 尝试从缓存获取
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, StockRealtimeDTO.class);
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed for key: {}, error: {}", cacheKey, e.getMessage());
        }

        StockRealtimeDTO tencentQuote = getTencentRealtimeQuote(stockCode, market);
        if (tencentQuote != null && tencentQuote.getCurrentPrice() != null) {
            return tencentQuote;
        }

        String secid = market + "." + stockCode;
        String url = baseUrl + "/api/qt/stock/get?fields=f43,f44,f45,f46,f47,f48,f57,f58,f60,f100,f116,f117,f124,f127,f162,f168,f169,f170&secid=" + secid +
                "&ut=fa5fd1943c7b386f172d6893dbfba10b&cb=";

        try {
            String response = remoteHttpClient.get(url);
            if (response == null) {
                log.warn("Empty response from EastMoney realtime API for stock: {}", stockCode);
                return getTencentRealtimeQuote(stockCode, market);
            }

            String json = stripJsonp(response);
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.path("data");

            if (data.isMissingNode() || data.isNull()) {
                log.warn("No data in EastMoney response for stock: {}", stockCode);
                return getTencentRealtimeQuote(stockCode, market);
            }

            StockRealtimeDTO dto = new StockRealtimeDTO();
            dto.setStockCode(data.path("f57").asText(stockCode));
            if (!data.path("f58").isMissingNode() && !data.path("f58").isNull()) {
                dto.setStockName(data.path("f58").asText(null));
            }

            // f43=当前价（需÷100）
            if (!data.path("f43").isMissingNode() && data.path("f43").asInt() != 0) {
                dto.setCurrentPrice(BigDecimal.valueOf(data.path("f43").asLong())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
            // f170=涨跌幅（需÷100）
            if (!data.path("f170").isMissingNode()) {
                dto.setChangeRate(BigDecimal.valueOf(data.path("f170").asLong())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
            // f169=涨跌额（需÷100）
            if (!data.path("f169").isMissingNode()) {
                dto.setChangeAmount(BigDecimal.valueOf(data.path("f169").asLong())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
            // f47=成交量
            if (!data.path("f47").isMissingNode()) {
                dto.setVolume(data.path("f47").asLong());
            }
            // f48=成交额
            if (!data.path("f48").isMissingNode()) {
                dto.setAmount(BigDecimal.valueOf(data.path("f48").asLong())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
            // f162=市盈率(静态/TTM，需÷100)
            if (!data.path("f162").isMissingNode() && data.path("f162").asLong() != 0) {
                dto.setPe(BigDecimal.valueOf(data.path("f162").asLong())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
            // f168=换手率（需÷100）
            if (!data.path("f168").isMissingNode() && data.path("f168").asLong() != 0) {
                dto.setTurnoverRate(BigDecimal.valueOf(data.path("f168").asLong())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
            // f116=总市值（元 -> 亿）
            if (!data.path("f116").isMissingNode() && data.path("f116").asDouble() != 0D) {
                dto.setTotalMarketCap(BigDecimal.valueOf(data.path("f116").asDouble())
                        .divide(BigDecimal.valueOf(100000000L), 2, RoundingMode.HALF_UP));
            }
            // f117=流通市值（元 -> 亿）
            if (!data.path("f117").isMissingNode() && data.path("f117").asDouble() != 0D) {
                dto.setCirculatingMarketCap(BigDecimal.valueOf(data.path("f117").asDouble())
                        .divide(BigDecimal.valueOf(100000000L), 2, RoundingMode.HALF_UP));
            }
            if (!data.path("f127").isMissingNode() && !data.path("f127").isNull()) {
                dto.setIndustry(data.path("f127").asText(null));
            } else if (!data.path("f100").isMissingNode() && !data.path("f100").isNull()) {
                dto.setIndustry(data.path("f100").asText(null));
            }
            if (!data.path("f124").isMissingNode() && data.path("f124").asLong() > 0) {
                dto.setQuoteTime(LocalDateTime.ofInstant(Instant.ofEpochSecond(data.path("f124").asLong()), SHANGHAI_ZONE));
            }
            // f44=最高价（需÷100）
            if (!data.path("f44").isMissingNode() && data.path("f44").asInt() != 0) {
                dto.setHigh(BigDecimal.valueOf(data.path("f44").asLong())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
            // f45=最低价（需÷100）
            if (!data.path("f45").isMissingNode() && data.path("f45").asInt() != 0) {
                dto.setLow(BigDecimal.valueOf(data.path("f45").asLong())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
            // f46=开盘价（需÷100）
            if (!data.path("f46").isMissingNode() && data.path("f46").asInt() != 0) {
                dto.setOpen(BigDecimal.valueOf(data.path("f46").asLong())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
            // f60=昨收价（需÷100）
            if (!data.path("f60").isMissingNode() && data.path("f60").asInt() != 0) {
                dto.setPreClose(BigDecimal.valueOf(data.path("f60").asLong())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }

            // 写入缓存
            try {
                stringRedisTemplate.opsForValue().set(cacheKey,
                        objectMapper.writeValueAsString(dto), REALTIME_CACHE_TTL);
            } catch (Exception e) {
                log.warn("Redis cache write failed for key: {}, error: {}", cacheKey, e.getMessage());
            }

            return dto;
        } catch (Exception e) {
            log.error("Failed to get realtime quote for stock: {}, error: {}", stockCode, e.getMessage());
            return getTencentRealtimeQuote(stockCode, market);
        }
    }

    /**
     * 获取历史K线数据
     * period: day->101, week->102, month->103, 30min->30, 60min->60
     */
    public List<KlineVO> getKlineData(String stockCode, String market, String period, int limit) {
        if ("1s".equalsIgnoreCase(period)) {
            StockRealtimeDTO realtimeDTO = getRealtimeQuote(stockCode, market);
            return realtimeSecondSeriesAssembler().assemble(market + ":" + stockCode, realtimeDTO, limit);
        }
        String cacheKey = KLINE_CACHE_PREFIX + stockCode + ":" + period;
        RealtimeKlineAssembler realtimeKlineAssembler = new RealtimeKlineAssembler(clock);
        StockRealtimeDTO realtimeDTO = supportsRealtimeKline(period) ? getRealtimeQuote(stockCode, market) : null;
        boolean liveMode = realtimeKlineAssembler.shouldUseLiveCache(period, realtimeDTO);
        if (!liveMode) {
            try {
                String cached = stringRedisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    List<KlineVO> cachedResult = objectMapper.readValue(cached,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, KlineVO.class));
                    return trimKlineResult(cachedResult, limit);
                }
            } catch (Exception e) {
                log.warn("Redis cache read failed for key: {}, error: {}", cacheKey, e.getMessage());
            }
        }

        String klt = convertPeriod(period);
        String secid = market + "." + stockCode;
        // 总是拉最近500条，返回时再截取
        String url = hisUrl + "/api/qt/stock/kline/get?fields1=f1,f2,f3,f4,f5,f6&fields2=f51,f52,f53,f54,f55,f56,f57,f58" +
                "&klt=" + klt + "&fqt=0&secid=" + secid + "&beg=0&end=20500101&lmt=500" +
                "&ut=fa5fd1943c7b386f172d6893dbfba10b&cb=";

        try {
            String response = remoteHttpClient.get(url);
            if (response == null) {
                log.warn("Empty response from EastMoney kline API for stock: {}", stockCode);
                return new ArrayList<>();
            }

            String json = stripJsonp(response);
            JsonNode root = objectMapper.readTree(json);
            JsonNode klines = root.path("data").path("klines");

            if (klines.isMissingNode() || klines.isNull() || !klines.isArray()) {
                log.warn("No kline data in EastMoney response for stock: {}", stockCode);
                return new ArrayList<>();
            }

            List<KlineVO> result = new ArrayList<>();
            for (JsonNode kline : klines) {
                String[] parts = kline.asText().split(",");
                if (parts.length < 8) {
                    continue;
                }
                KlineVO vo = new KlineVO();
                vo.setDate(parts[0]);
                vo.setOpen(parseBigDecimal(parts[1]));
                vo.setClose(parseBigDecimal(parts[2]));
                vo.setHigh(parseBigDecimal(parts[3]));
                vo.setLow(parseBigDecimal(parts[4]));
                vo.setVolume(parseLong(parts[5]));
                vo.setAmount(parseBigDecimal(parts[6]));
                vo.setChangeRate(parseBigDecimal(parts[7]));
                result.add(vo);
            }

            List<KlineVO> merged = realtimeKlineAssembler.merge(period, result, realtimeDTO);

            // 写入缓存（缓存全量数据）
            try {
                stringRedisTemplate.opsForValue().set(cacheKey,
                        objectMapper.writeValueAsString(merged),
                        liveMode ? realtimeKlineAssembler.resolveCacheTtl(period, realtimeDTO) : KLINE_CACHE_TTL);
            } catch (Exception e) {
                log.warn("Redis cache write failed for key: {}, error: {}", cacheKey, e.getMessage());
            }

            // 返回最近 limit 条（数据按时间升序，取末尾）
            return trimKlineResult(merged, limit);
        } catch (Exception e) {
            log.error("Failed to get kline data for stock: {}, error: {}", stockCode, e.getMessage());
            return trimKlineResult(realtimeKlineAssembler.merge(period, new ArrayList<>(), realtimeDTO), limit);
        }
    }

    private List<KlineVO> trimKlineResult(List<KlineVO> result, int limit) {
        if (result == null) {
            return new ArrayList<>();
        }
        if (limit > 0 && result.size() > limit) {
            return new ArrayList<>(result.subList(result.size() - limit, result.size()));
        }
        return result;
    }

    /**
     * 搜索股票
     */
    public List<Map<String, String>> searchStocks(String keyword) {
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String url = searchUrl + "/api/suggest/get?input=" + encodedKeyword +
                "&type=14&token=D43BF722C8E33BDC906FB84D85E326E8&count=10&cb=";

        try {
            String response = remoteHttpClient.get(url);
            if (response == null) {
                return new ArrayList<>();
            }

            String json = stripJsonp(response);
            JsonNode root = objectMapper.readTree(json);
            JsonNode dataArray = root.path("QuotationCodeTable").path("Data");
            if (dataArray.isMissingNode() || dataArray.isNull() || !dataArray.isArray()) {
                dataArray = root.path("QuoteSuggestData").path("Data");
            }

            if (dataArray.isMissingNode() || dataArray.isNull() || !dataArray.isArray()) {
                return new ArrayList<>();
            }

            List<Map<String, String>> result = new ArrayList<>();
            for (JsonNode item : dataArray) {
                if (!isSupportedSearchItem(item)) {
                    continue;
                }
                Map<String, String> stock = new HashMap<>();
                stock.put("stockCode", item.path("Code").asText());
                stock.put("stockName", item.path("Name").asText());
                stock.put("market", normalizeSearchMarket(item.path("MktNum").asText()));
                stock.put("board", resolveSearchBoard(item.path("Code").asText(), normalizeSearchMarket(item.path("MktNum").asText())));
                result.add(stock);
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to search stocks with keyword: {}, error: {}", keyword, e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<StockScreenerCandidateDTO> getScreenerCandidates(List<String> boards, int maxPages) {
        List<StockScreenerCandidateDTO> result = new ArrayList<>();
        String fs = resolveScreenerFs(boards);

        for (int page = 1; page <= Math.max(1, maxPages); page++) {
            String url = baseUrl + "/api/qt/clist/get?pn=" + page
                    + "&pz=100&po=1&np=1&fltt=2&invt=2&fid=f3"
                    + "&fs=" + fs
                    + "&fields=f12,f14,f13,f100,f115,f8,f3,f2,f20"
                    + "&ut=" + SCREENING_UT;
            try {
                String response = remoteHttpClient.get(url);
                if (response == null) {
                    break;
                }
                JsonNode diff = objectMapper.readTree(stripJsonp(response)).path("data").path("diff");
                if (!diff.isArray() || diff.isEmpty()) {
                    break;
                }
                for (JsonNode item : diff) {
                    StockScreenerCandidateDTO candidate = new StockScreenerCandidateDTO();
                    candidate.setStockCode(item.path("f12").asText());
                    candidate.setStockName(item.path("f14").asText());
                    candidate.setMarket("1".equals(item.path("f13").asText()) ? "SH" : "SZ");
                    candidate.setBoard(resolveSearchBoard(candidate.getStockCode(), candidate.getMarket()));
                    candidate.setIndustry(item.path("f100").asText());
                    candidate.setIsSt(isRiskStock(candidate.getStockName()) ? 1 : 0);
                    candidate.setIsDelisted(isDelistedStock(candidate.getStockName()) ? 1 : 0);
                    candidate.setCurrentPrice(parseBigDecimalNode(item.path("f2")));
                    candidate.setChangeRate(parseBigDecimalNode(item.path("f3")));
                    candidate.setTurnoverRate(parseBigDecimalNode(item.path("f8")));
                    candidate.setPe(parseBigDecimalNode(item.path("f115")));
                    candidate.setTotalMarketCap(parseMarketCap(item.path("f20")));
                    result.add(candidate);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch screener candidates on page {}, error: {}", page, e.getMessage());
                break;
            }
        }

        return result;
    }

    public Map<String, StockRealtimeDTO> getBatchRealtimeQuotes(List<StockScreenerCandidateDTO> candidates) {
        Map<String, StockRealtimeDTO> result = new HashMap<>();
        List<StockScreenerCandidateDTO> missing = new ArrayList<>(candidates);

        for (int i = 0; i < missing.size(); i += 60) {
            List<StockScreenerCandidateDTO> batch = missing.subList(i, Math.min(i + 60, missing.size()));
            String symbols = batch.stream()
                    .map(candidate -> ("SH".equalsIgnoreCase(candidate.getMarket()) ? "sh" : "sz") + candidate.getStockCode())
                    .collect(Collectors.joining(","));
            try {
                String response = remoteHttpClient.get(TENCENT_QUOTE_URL + symbols);
                if (response == null || response.isBlank()) {
                    continue;
                }
                for (String line : response.split(";\\s*")) {
                    StockRealtimeDTO dto = parseTencentRealtimeQuote(line, extractTencentStockCode(line));
                    if (dto == null || dto.getStockCode() == null) {
                        continue;
                    }
                    String market = line.contains("v_sh") ? "SH" : "SZ";
                    result.put(market + ":" + dto.getStockCode(), dto);
                }
            } catch (Exception e) {
                log.warn("Failed to get batch Tencent realtime quotes, error: {}", e.getMessage());
            }
        }

        return result;
    }

    /**
     * 去除 JSONP 包装，提取纯 JSON
     */
    private String stripJsonp(String response) {
        if (response == null) {
            return "{}";
        }
        String trimmed = response.trim();
        // 去掉 cb= 形式的 JSONP 包装，如 jQuery123({...}) 或 ({...})
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    /**
     * period 字符串转东方财富 klt 参数
     */
    private String convertPeriod(String period) {
        return switch (period) {
            case "1min" -> "1";
            case "5min" -> "5";
            case "15min" -> "15";
            case "week" -> "102";
            case "month" -> "103";
            case "30min" -> "30";
            case "60min" -> "60";
            default -> "101"; // day
        };
    }

    private BigDecimal parseBigDecimal(String s) {
        try {
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimalNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            return BigDecimal.valueOf(node.asDouble()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseMarketCap(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            double value = node.asDouble();
            if (value == 0D) {
                return null;
            }
            return BigDecimal.valueOf(value)
                    .divide(BigDecimal.valueOf(100000000L), 2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isSupportedSearchItem(JsonNode item) {
        String classify = item.path("Classify").asText();
        String securityTypeName = item.path("SecurityTypeName").asText();
        return "AStock".equalsIgnoreCase(classify)
                || "Index".equalsIgnoreCase(classify)
                || "23".equals(classify)
                || securityTypeName.contains("科创");
    }

    private StockRealtimeDTO getTencentRealtimeQuote(String stockCode, String market) {
        String prefix = "1".equals(market) || "SH".equalsIgnoreCase(market) ? "sh" : "sz";
        String url = TENCENT_QUOTE_URL + prefix + stockCode;

        try {
            String response = remoteHttpClient.get(url);
            StockRealtimeDTO dto = parseTencentRealtimeQuote(response, stockCode);
            if (dto != null) {
                cacheRealtimeQuote(dto, "1".equals(market) || "SH".equalsIgnoreCase(market) ? "1" : "0");
            }
            return dto;
        } catch (Exception e) {
            log.warn("Failed to get Tencent realtime quote for stock: {}, error: {}", stockCode, e.getMessage());
            return null;
        }
    }

    private StockRealtimeDTO parseTencentRealtimeQuote(String response, String stockCode) {
        if (response == null || response.isBlank() || !response.contains("~")) {
            return null;
        }
        int start = response.indexOf('"');
        int end = response.lastIndexOf('"');
        if (start < 0 || end <= start) {
            return null;
        }

        String[] parts = response.substring(start + 1, end).split("~", -1);
        if (parts.length < 45) {
            return null;
        }

        StockRealtimeDTO dto = new StockRealtimeDTO();
        dto.setStockCode(stockCode);
        dto.setCurrentPrice(parseBigDecimal(parts[3]));
        dto.setPreClose(parseBigDecimal(parts[4]));
        dto.setOpen(parseBigDecimal(parts[5]));
        dto.setHigh(parseBigDecimal(parts[33]));
        dto.setLow(parseBigDecimal(parts[34]));
        dto.setVolume(parseLong(parts[36]));
        dto.setAmount(parseTencentAmount(parts));
        dto.setTurnoverRate(parseBigDecimal(parts[38]));
        dto.setTotalMarketCap(parseBigDecimal(parts[44]));
        dto.setCirculatingMarketCap(parseBigDecimal(parts[45]));
        dto.setPe(parseBigDecimal(parts[52]));
        dto.setQuoteTime(parseTencentQuoteTime(parts[30]));

        if (dto.getCurrentPrice() != null && dto.getPreClose() != null) {
            dto.setChangeAmount(dto.getCurrentPrice().subtract(dto.getPreClose()).setScale(2, RoundingMode.HALF_UP));
            if (dto.getPreClose().compareTo(BigDecimal.ZERO) != 0) {
                dto.setChangeRate(dto.getCurrentPrice()
                        .subtract(dto.getPreClose())
                        .divide(dto.getPreClose(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP));
            }
        }
        return dto;
    }

    private String extractTencentStockCode(String response) {
        if (response == null) {
            return null;
        }
        int start = response.indexOf("v_");
        int end = response.indexOf('=');
        if (start < 0 || end <= start + 2) {
            return null;
        }
        String symbol = response.substring(start + 2, end);
        return symbol.length() > 2 ? symbol.substring(2) : null;
    }

    private void cacheRealtimeQuote(StockRealtimeDTO dto, String market) {
        String cacheKey = REALTIME_CACHE_PREFIX + market + ":" + dto.getStockCode();
        try {
            stringRedisTemplate.opsForValue().set(cacheKey,
                    objectMapper.writeValueAsString(dto), REALTIME_CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis cache write failed for key: {}, error: {}", cacheKey, e.getMessage());
        }
    }

    private BigDecimal parseTencentAmount(String[] parts) {
        if (parts.length > 37 && parts[37] != null && parts[37].contains("/")) {
            String[] ticks = parts[37].split("/");
            if (ticks.length >= 3) {
                BigDecimal amount = parseBigDecimal(ticks[2]);
                if (amount != null) {
                    return amount.setScale(2, RoundingMode.HALF_UP);
                }
            }
        }
        if (parts.length > 57) {
            BigDecimal amountWan = parseBigDecimal(parts[57]);
            if (amountWan != null) {
                return amountWan.multiply(BigDecimal.valueOf(10000)).setScale(2, RoundingMode.HALF_UP);
            }
        }
        return null;
    }

    private LocalDateTime parseTencentQuoteTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), TENCENT_QUOTE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String normalizeSearchMarket(String market) {
        return switch (market) {
            case "1" -> "SH";
            case "0", "2", "5" -> "SZ";
            default -> market;
        };
    }

    private String resolveSearchBoard(String stockCode, String market) {
        if (stockCode == null) {
            return null;
        }
        if (!isTradableAStock(stockCode, market)) {
            return null;
        }
        if (stockCode.startsWith("688")) {
            return "STAR";
        }
        if (stockCode.startsWith("300") || stockCode.startsWith("301")) {
            return "CYB";
        }
        if ("SH".equalsIgnoreCase(market)) {
            return "SH_MAIN";
        }
        if ("SZ".equalsIgnoreCase(market)) {
            return "SZ_MAIN";
        }
        return null;
    }

    private boolean isTradableAStock(String stockCode, String market) {
        if (stockCode == null || market == null) {
            return false;
        }
        if ("SH".equalsIgnoreCase(market)) {
            return stockCode.startsWith("600")
                    || stockCode.startsWith("601")
                    || stockCode.startsWith("603")
                    || stockCode.startsWith("605")
                    || stockCode.startsWith("688");
        }
        if ("SZ".equalsIgnoreCase(market)) {
            return stockCode.startsWith("000")
                    || stockCode.startsWith("001")
                    || stockCode.startsWith("002")
                    || stockCode.startsWith("003")
                    || stockCode.startsWith("300")
                    || stockCode.startsWith("301");
        }
        return false;
    }

    private boolean isRiskStock(String stockName) {
        if (stockName == null) {
            return false;
        }
        String normalized = stockName.trim().toUpperCase();
        return normalized.startsWith("ST")
                || normalized.startsWith("*ST")
                || normalized.contains(" ST")
                || normalized.contains("*ST");
    }

    private boolean supportsRealtimeKline(String period) {
        return List.of("1s", "1min", "5min", "15min", "30min", "60min", "day", "week", "month")
                .contains(period == null ? null : period.trim().toLowerCase());
    }

    private RealtimeSecondSeriesAssembler realtimeSecondSeriesAssembler() {
        if (realtimeSecondSeriesAssembler == null || secondSeriesClock != clock) {
            secondSeriesClock = clock;
            realtimeSecondSeriesAssembler = new RealtimeSecondSeriesAssembler(clock);
        }
        return realtimeSecondSeriesAssembler;
    }

    private boolean isDelistedStock(String stockName) {
        if (stockName == null) {
            return false;
        }
        String normalized = stockName.trim();
        return normalized.contains("退")
                || normalized.contains("摘牌")
                || normalized.toUpperCase().contains("DELIST");
    }

    private String resolveScreenerFs(List<String> boards) {
        List<String> fsList = new ArrayList<>();
        List<String> selectedBoards = boards == null ? List.of() : boards;
        if (selectedBoards.isEmpty() || selectedBoards.contains("sz_main")) {
            fsList.add("m:0+t:6");
        }
        if (selectedBoards.isEmpty() || selectedBoards.contains("cyb")) {
            fsList.add("m:0+t:80");
        }
        if (selectedBoards.isEmpty() || selectedBoards.contains("sh_main")) {
            fsList.add("m:1+t:2");
        }
        if (selectedBoards.isEmpty() || selectedBoards.contains("star")) {
            fsList.add("m:1+t:23");
        }
        return String.join(",", fsList);
    }
}
