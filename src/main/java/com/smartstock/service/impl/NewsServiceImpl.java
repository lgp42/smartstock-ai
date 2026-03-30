package com.smartstock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstock.client.ClsNewsClient;
import com.smartstock.client.EastMoneyNewsClient;
import com.smartstock.convert.NewsStructMapper;
import com.smartstock.client.SinaNewsClient;
import com.smartstock.entity.News;
import com.smartstock.entity.StockInfo;
import com.smartstock.mapper.NewsMapper;
import com.smartstock.mapper.StockInfoMapper;
import com.smartstock.service.NewsService;
import com.smartstock.vo.NewsFlashVO;
import com.smartstock.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private static final String NEWS_CACHE_KEY = "news:flash:all";
    private static final Duration NEWS_CACHE_TTL = Duration.ofSeconds(60);
    private static final int MAX_CACHE_SIZE = 300;
    private static final int MAX_SYNC_SIZE = 2000;
    private static final int MIN_SYNC_SIZE = 300;
    private static final int RECENT_LOOKBACK_DAYS = 14;
    private static final int RECENT_EXISTING_LIMIT = 2000;
    private static final int TITLE_MAX_LENGTH = 255;
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern STOCK_CODE_PATTERN = Pattern.compile("(?<!\\d)(\\d{6})(?!\\d)");
    private static final AtomicBoolean NEWS_SYNC_RUNNING = new AtomicBoolean(false);
    private static final ExecutorService NEWS_SYNC_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "news-sync");
        thread.setDaemon(true);
        return thread;
    });

    private final SinaNewsClient sinaNewsClient;
    private final ClsNewsClient clsNewsClient;
    private final EastMoneyNewsClient eastMoneyNewsClient;
    private final NewsMapper newsMapper;
    private final StockInfoMapper stockInfoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final NewsStructMapper newsStructMapper;

    @Override
    public List<NewsFlashVO> getFlashNews(int limit) {
        return getFlashNews(limit, null, null, null);
    }

    @Override
    @Cacheable(cacheNames = "flashNews",
            key = "'list:' + #limit + ':' + (#source == null ? '' : #source) + ':' + (#stockCode == null ? '' : #stockCode) + ':' + (#keyword == null ? '' : #keyword)")
    public List<NewsFlashVO> getFlashNews(int limit, String source, String stockCode, String keyword) {
        boolean unfiltered = isUnfiltered(source, stockCode, keyword);
        if (unfiltered) {
            List<NewsFlashVO> cachedNews = getCachedNews(limit);
            if (!cachedNews.isEmpty()) {
                triggerAsyncSync(Math.max(limit, MIN_SYNC_SIZE), true);
                return cachedNews;
            }
        }

        List<NewsFlashVO> latestNews = loadStoredNews(Math.max(limit, MAX_CACHE_SIZE), source, stockCode, keyword);
        if (unfiltered) {
            cacheNews(latestNews);
        }
        triggerAsyncSync(Math.max(limit, MIN_SYNC_SIZE), unfiltered);
        return trimResult(latestNews, limit);
    }

    @Override
    public PageVO<NewsFlashVO> getFlashNewsPage(int page, int pageSize) {
        return getFlashNewsPage(page, pageSize, null, null, null);
    }

    @Override
    @Cacheable(cacheNames = "flashNews",
            key = "'page:' + #page + ':' + #pageSize + ':' + (#source == null ? '' : #source) + ':' + (#stockCode == null ? '' : #stockCode) + ':' + (#keyword == null ? '' : #keyword)")
    public PageVO<NewsFlashVO> getFlashNewsPage(int page, int pageSize, String source, String stockCode, String keyword) {
        LambdaQueryWrapper<News> queryWrapper = buildNewsQuery(source, stockCode, keyword);
        long requiredSize = (long) page * pageSize;
        Long total = newsMapper.selectCount(queryWrapper);
        if (total == null || total < requiredSize) {
            synchronizeNews((int) Math.min(Math.max(requiredSize + pageSize * 5L, MIN_SYNC_SIZE), MAX_SYNC_SIZE));
            total = newsMapper.selectCount(buildNewsQuery(source, stockCode, keyword));
        }

        Page<News> pageResult = newsMapper.selectPage(
                new Page<>(page, pageSize),
                buildNewsQuery(source, stockCode, keyword)
        );

        List<NewsFlashVO> records = new ArrayList<>();
        for (News news : pageResult.getRecords()) {
            records.add(toFlashVO(news));
        }
        return new PageVO<>(pageResult.getTotal(), page, pageSize, records);
    }

    private boolean isUnfiltered(String source, String stockCode, String keyword) {
        return !StrUtil.isNotBlank(source) && !StrUtil.isNotBlank(stockCode) && !StrUtil.isNotBlank(keyword);
    }

    private LambdaQueryWrapper<News> buildNewsQuery(String source, String stockCode, String keyword) {
        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(source)) {
            wrapper.eq(News::getSource, source.trim());
        }
        if (StrUtil.isNotBlank(stockCode)) {
            wrapper.eq(News::getStockCode, stockCode.trim());
        }
        if (StrUtil.isNotBlank(keyword)) {
            String normalizedKeyword = keyword.trim();
            wrapper.and(w -> w.like(News::getTitle, normalizedKeyword)
                    .or()
                    .like(News::getContent, normalizedKeyword));
        }
        return wrapper.orderByDesc(News::getPublishTime).orderByDesc(News::getId);
    }

    private void synchronizeNews(int targetSize) {
        int sourceLimit = Math.min(Math.max(targetSize, 50), MAX_CACHE_SIZE);
        List<NewsFlashVO> mergedNews = new ArrayList<>();
        mergedNews.addAll(sinaNewsClient.fetchLatest(sourceLimit));
        mergedNews.addAll(clsNewsClient.fetchLatest(sourceLimit));
        mergedNews.addAll(eastMoneyNewsClient.fetchLatest(sourceLimit));
        mergedNews.sort(Comparator.comparing(NewsFlashVO::getPublishEpoch,
                Comparator.nullsLast(Comparator.reverseOrder())));

        persistNews(deduplicate(mergedNews));
    }

    private void triggerAsyncSync(int targetSize, boolean refreshCache) {
        if (!NEWS_SYNC_RUNNING.compareAndSet(false, true)) {
            return;
        }
        NEWS_SYNC_EXECUTOR.submit(() -> {
            try {
                synchronizeNews(targetSize);
                if (refreshCache) {
                    cacheNews(loadStoredNews(MAX_CACHE_SIZE, null, null, null));
                }
            } catch (Exception e) {
                log.warn("Async news synchronization failed, error: {}", e.getMessage());
            } finally {
                NEWS_SYNC_RUNNING.set(false);
            }
        });
    }

    private List<NewsFlashVO> getCachedNews(int limit) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(NEWS_CACHE_KEY);
            if (!StrUtil.isNotBlank(cached)) {
                return new ArrayList<>();
            }
            List<NewsFlashVO> newsList = objectMapper.readValue(
                    cached,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NewsFlashVO.class)
            );
            if (newsList.size() < Math.min(limit, MAX_CACHE_SIZE)) {
                return new ArrayList<>();
            }
            return trimResult(newsList, limit);
        } catch (Exception e) {
            log.warn("News cache read failed, error: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<NewsFlashVO> loadStoredNews(int limit, String source, String stockCode, String keyword) {
        List<News> newsList = newsMapper.selectList(buildNewsQuery(source, stockCode, keyword)
                .last("limit " + Math.min(limit, MAX_CACHE_SIZE)));
        List<NewsFlashVO> result = new ArrayList<>();
        for (News news : newsList) {
            result.add(toFlashVO(news));
        }
        return result;
    }

    private void cacheNews(List<NewsFlashVO> newsList) {
        try {
            stringRedisTemplate.opsForValue().set(
                    NEWS_CACHE_KEY,
                    objectMapper.writeValueAsString(trimResult(newsList, MAX_CACHE_SIZE)),
                    NEWS_CACHE_TTL
            );
        } catch (Exception e) {
            log.warn("News cache write failed, error: {}", e.getMessage());
        }
    }

    private void persistNews(List<NewsFlashVO> newsList) {
        List<News> existingNews = newsMapper.selectList(new LambdaQueryWrapper<News>()
                .ge(News::getPublishTime, LocalDateTime.now(SHANGHAI_ZONE).minusDays(RECENT_LOOKBACK_DAYS))
                .orderByDesc(News::getPublishTime)
                .orderByDesc(News::getId)
                .last("limit " + RECENT_EXISTING_LIMIT));
        Set<String> existingKeys = new HashSet<>();
        for (News news : existingNews) {
            existingKeys.add(buildPersistKey(news.getTitle(), news.getUrl(), news.getPublishTime()));
        }

        for (NewsFlashVO news : newsList) {
            LocalDateTime publishTime = toPublishTime(news.getPublishEpoch());
            if (publishTime == null) {
                continue;
            }
            String persistKey = buildPersistKey(news.getTitle(), news.getUrl(), publishTime);
            if (!existingKeys.add(persistKey)) {
                continue;
            }
            String linkedStockCode = resolveLinkedStockCode(news);
            newsMapper.insert(News.builder()
                    .title(trimToDbTitle(news.getTitle(), news.getSummary()))
                    .content(news.getSummary())
                    .source(news.getSource())
                    .url(news.getUrl())
                    .stockCode(linkedStockCode)
                    .publishTime(publishTime)
                    .build());
        }
    }

    private List<NewsFlashVO> deduplicate(List<NewsFlashVO> mergedNews) {
        Map<String, NewsFlashVO> dedupedByTitle = new LinkedHashMap<>();
        Set<String> seenUrls = new HashSet<>();

        for (NewsFlashVO news : mergedNews) {
            if (news == null || !StrUtil.isNotBlank(news.getTitle())) {
                continue;
            }

            String normalizedUrl = normalizeUrl(news.getUrl());
            if (StrUtil.isNotBlank(normalizedUrl) && !seenUrls.add(normalizedUrl)) {
                continue;
            }

            String dedupeKey = normalizeTitle(news.getTitle());
            if (!StrUtil.isNotBlank(dedupeKey)) {
                dedupeKey = normalizeTitle(news.getSummary());
            }
            if (!StrUtil.isNotBlank(dedupeKey)) {
                continue;
            }
            String timeBucket = resolveTimeBucket(news.getPublishEpoch());
            String key = dedupeKey + "|" + timeBucket;
            NewsFlashVO existing = dedupedByTitle.get(key);
            if (existing == null || shouldReplace(existing, news)) {
                dedupedByTitle.put(key, mergeNews(existing, news));
            }
        }

        return new ArrayList<>(dedupedByTitle.values());
    }

    private List<NewsFlashVO> trimResult(List<NewsFlashVO> newsList, int limit) {
        if (newsList == null || newsList.isEmpty()) {
            return new ArrayList<>();
        }
        int end = Math.min(limit, newsList.size());
        return new ArrayList<>(newsList.subList(0, end));
    }

    private NewsFlashVO toFlashVO(News news) {
        return newsStructMapper.toFlashVO(news);
    }

    private LocalDateTime toPublishTime(Long publishEpoch) {
        if (publishEpoch == null || publishEpoch <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(publishEpoch), SHANGHAI_ZONE);
    }

    private String buildPersistKey(String title, String url, LocalDateTime publishTime) {
        String normalizedUrl = normalizeUrl(url);
        if (StrUtil.isNotBlank(normalizedUrl)) {
            return normalizedUrl;
        }
        return "%s|%s".formatted(
                normalizeTitle(title),
                publishTime == null ? "" : publishTime.toString()
        );
    }

    private String trimToDbTitle(String title, String summary) {
        String value = StrUtil.isNotBlank(title) ? title.trim() : StrUtil.blankToDefault(summary, "").trim();
        if (value.length() <= TITLE_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, TITLE_MAX_LENGTH);
    }

    private String normalizeUrl(String url) {
        if (!StrUtil.isNotBlank(url)) {
            return null;
        }
        return url.replace("https://", "")
                .replace("http://", "")
                .replaceAll("[?#].*$", "")
                .trim();
    }

    private String normalizeTitle(String title) {
        if (!StrUtil.isNotBlank(title)) {
            return null;
        }
        String normalized = title.trim();
        normalized = normalized.replaceFirst("^财联社\\d+月\\d+日电[，,:：]?", "");
        normalized = normalized.replaceFirst("^新浪财经讯[，,:：]?", "");
        normalized = normalized.replaceFirst("^【[^】]+】", "");
        normalized = normalized.replaceAll("[\\p{Punct}，。；：、“”‘’（）()【】\\s]+", "");
        normalized = normalized.toLowerCase(Locale.ROOT);
        return normalized;
    }

    private String resolveTimeBucket(Long publishEpoch) {
        if (publishEpoch == null || publishEpoch <= 0) {
            return "0";
        }
        return Long.toString(publishEpoch / 900);
    }

    private boolean shouldReplace(NewsFlashVO existing, NewsFlashVO incoming) {
        return newsScore(incoming) > newsScore(existing);
    }

    private int newsScore(NewsFlashVO news) {
        if (news == null) {
            return Integer.MIN_VALUE;
        }
        int score = 0;
        if (StrUtil.isNotBlank(news.getUrl())) {
            score += 30;
        }
        if (StrUtil.isNotBlank(news.getStockCode())) {
            score += 20;
        }
        score += Math.min(40, StrUtil.length(news.getSummary()) / 8);
        score += switch (Objects.toString(news.getSource(), "")) {
            case "财联社" -> 15;
            case "东方财富" -> 12;
            case "新浪财经" -> 10;
            default -> 0;
        };
        return score;
    }

    private NewsFlashVO mergeNews(NewsFlashVO base, NewsFlashVO incoming) {
        if (base == null) {
            return incoming;
        }
        NewsFlashVO preferred = shouldReplace(base, incoming) ? incoming : base;
        NewsFlashVO secondary = preferred == incoming ? base : incoming;
        if (!StrUtil.isNotBlank(preferred.getStockCode())) {
            preferred.setStockCode(secondary.getStockCode());
        }
        if (!StrUtil.isNotBlank(preferred.getUrl())) {
            preferred.setUrl(secondary.getUrl());
        }
        if (!StrUtil.isNotBlank(preferred.getSummary()) || preferred.getSummary().length() < secondary.getSummary().length()) {
            preferred.setSummary(secondary.getSummary());
        }
        if ((preferred.getPublishEpoch() == null || preferred.getPublishEpoch() <= 0) && secondary.getPublishEpoch() != null) {
            preferred.setPublishEpoch(secondary.getPublishEpoch());
            preferred.setPublishTime(secondary.getPublishTime());
        }
        return preferred;
    }

    private String resolveLinkedStockCode(NewsFlashVO news) {
        if (news == null) {
            return null;
        }
        if (StrUtil.isNotBlank(news.getStockCode())) {
            String directCode = news.getStockCode().trim();
            if (isTradableAStockCode(directCode)) {
                return directCode;
            }
        }
        String text = StrUtil.blankToDefault(news.getTitle(), "") + " " + StrUtil.blankToDefault(news.getSummary(), "");
        Matcher matcher = STOCK_CODE_PATTERN.matcher(text);
        if (matcher.find()) {
            String matched = matcher.group(1);
            if (isTradableAStockCode(matched)) {
                return matched;
            }
        }

        List<StockInfo> stockInfos = stockInfoMapper.selectList(new LambdaQueryWrapper<StockInfo>()
                .eq(StockInfo::getStatus, 1)
                .orderByDesc(StockInfo::getUpdatedAt)
                .last("limit 500"));
        for (StockInfo stockInfo : stockInfos) {
            if (StrUtil.isNotBlank(stockInfo.getStockName()) && text.contains(stockInfo.getStockName())) {
                return stockInfo.getStockCode();
            }
        }
        return null;
    }

    private boolean isTradableAStockCode(String stockCode) {
        if (!StrUtil.isNotBlank(stockCode)) {
            return false;
        }
        return stockCode.startsWith("600")
                || stockCode.startsWith("601")
                || stockCode.startsWith("603")
                || stockCode.startsWith("605")
                || stockCode.startsWith("688")
                || stockCode.startsWith("000")
                || stockCode.startsWith("001")
                || stockCode.startsWith("002")
                || stockCode.startsWith("003")
                || stockCode.startsWith("300")
                || stockCode.startsWith("301");
    }
}
