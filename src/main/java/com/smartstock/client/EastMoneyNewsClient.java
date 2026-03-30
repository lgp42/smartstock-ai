package com.smartstock.client;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstock.vo.NewsFlashVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class EastMoneyNewsClient {

    private static final String NEWS_URL = "https://np-weblist.eastmoney.com/comm/web/getFastNewsList"
            + "?client=web&biz=web_724&fastColumn=102&sortEnd=%s&pageSize=%d&req_trace=%d&callback=callback";
    private static final Pattern JSONP_PATTERN = Pattern.compile("^[^(]+\\((.*)\\)\\s*$", Pattern.DOTALL);
    private static final DateTimeFormatter SOURCE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int PAGE_SIZE = 50;
    private static final int MAX_PAGES = 40;

    private final RemoteHttpClient remoteHttpClient;
    private final ObjectMapper objectMapper;

    public List<NewsFlashVO> fetchLatest(int limit) {
        List<NewsFlashVO> result = new ArrayList<>();
        String sortEnd = "";
        int pages = Math.min(Math.max((limit + PAGE_SIZE - 1) / PAGE_SIZE, 1), MAX_PAGES);

        for (int page = 0; page < pages && result.size() < limit; page++) {
            try {
                String response = remoteHttpClient.get(
                        NEWS_URL.formatted(sortEnd, PAGE_SIZE, System.currentTimeMillis())
                );
                List<NewsFlashVO> pageItems = parseResponse(response, limit - result.size());
                if (pageItems.isEmpty()) {
                    break;
                }
                result.addAll(pageItems);

                String nextSortEnd = parseSortEnd(response);
                if (!StrUtil.isNotBlank(nextSortEnd) || nextSortEnd.equals(sortEnd)) {
                    break;
                }
                sortEnd = nextSortEnd;
            } catch (Exception e) {
                log.warn("Failed to fetch eastmoney flash news, error: {}", e.getMessage());
                break;
            }
        }
        return result;
    }

    List<NewsFlashVO> parseResponse(String response, int limit) throws Exception {
        JsonNode root = readJsonp(response);
        JsonNode list = root.path("data").path("fastNewsList");
        if (!list.isArray()) {
            return new ArrayList<>();
        }

        List<NewsFlashVO> result = new ArrayList<>();
        for (JsonNode item : list) {
            String title = item.path("title").asText();
            String summary = item.path("summary").asText();
            String displayTitle = StrUtil.isNotBlank(title) ? title : summary;
            if (!StrUtil.isNotBlank(displayTitle)) {
                continue;
            }

            String sourceTime = item.path("showTime").asText();
            Long publishEpoch = parseEpoch(sourceTime);

            NewsFlashVO news = new NewsFlashVO();
            news.setTitle(displayTitle.trim());
            news.setSummary(StrUtil.isNotBlank(summary) ? summary.trim() : displayTitle.trim());
            news.setSource("东方财富");
            news.setPublishEpoch(publishEpoch);
            news.setPublishTime(formatDisplayTime(sourceTime));
            news.setUrl(resolveUrl(item.path("code").asText()));
            news.setStockCode(resolveStockCode(item.path("stockList")));
            result.add(news);

            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private JsonNode readJsonp(String response) throws Exception {
        if (!StrUtil.isNotBlank(response)) {
            return objectMapper.createObjectNode();
        }
        Matcher matcher = JSONP_PATTERN.matcher(response);
        String json = matcher.find() ? matcher.group(1) : response;
        return objectMapper.readTree(json);
    }

    private String parseSortEnd(String response) {
        try {
            return readJsonp(response).path("data").path("sortEnd").asText();
        } catch (Exception e) {
            return "";
        }
    }

    private String formatDisplayTime(String sourceTime) {
        if (!StrUtil.isNotBlank(sourceTime)) {
            return "";
        }
        try {
            return LocalDateTime.parse(sourceTime, SOURCE_TIME_FORMATTER).format(DISPLAY_TIME_FORMATTER);
        } catch (Exception e) {
            return sourceTime.length() >= 16 ? sourceTime.substring(11, 16) : sourceTime;
        }
    }

    private Long parseEpoch(String sourceTime) {
        if (!StrUtil.isNotBlank(sourceTime)) {
            return 0L;
        }
        try {
            return LocalDateTime.parse(sourceTime, SOURCE_TIME_FORMATTER)
                    .atZone(SHANGHAI_ZONE)
                    .toEpochSecond();
        } catch (Exception e) {
            return 0L;
        }
    }

    private String resolveUrl(String articleCode) {
        return StrUtil.isNotBlank(articleCode) ? "https://finance.eastmoney.com/a/%s.html".formatted(articleCode) : null;
    }

    private String resolveStockCode(JsonNode stockList) {
        if (!stockList.isArray()) {
            return null;
        }
        for (JsonNode item : stockList) {
            String value = item.asText();
            if (value.matches("[01]\\.\\d{6}")) {
                return value.substring(2);
            }
        }
        return null;
    }
}
