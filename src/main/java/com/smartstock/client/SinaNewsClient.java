package com.smartstock.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class SinaNewsClient {

    private static final String NEWS_URL = "https://app.cj.sina.com.cn/api/news/pc?page=%d&size=%d&tag=0";
    private static final DateTimeFormatter SOURCE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int PAGE_SIZE = 50;
    private static final int MAX_PAGES = 20;

    private final RemoteHttpClient remoteHttpClient;
    private final ObjectMapper objectMapper;

    public List<NewsFlashVO> fetchLatest(int limit) {
        List<NewsFlashVO> result = new ArrayList<>();
        try {
            int pages = Math.min(Math.max((limit + PAGE_SIZE - 1) / PAGE_SIZE, 1), MAX_PAGES);
            for (int page = 1; page <= pages && result.size() < limit; page++) {
                String response = remoteHttpClient.get(NEWS_URL.formatted(page, PAGE_SIZE));
                if (!StrUtil.isNotBlank(response)) {
                    break;
                }
                List<NewsFlashVO> pageItems = parseResponse(response, limit - result.size());
                if (pageItems.isEmpty()) {
                    break;
                }
                result.addAll(pageItems);
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch sina flash news, error: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    List<NewsFlashVO> parseResponse(String response, int limit) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode list = root.path("result").path("data").path("feed").path("list");
        if (!list.isArray()) {
            return new ArrayList<>();
        }

        List<NewsFlashVO> result = new ArrayList<>();
        for (JsonNode item : list) {
            if ("1".equals(item.path("is_repeat").asText())) {
                continue;
            }

            String title = cleanText(item.path("rich_text").asText());
            if (!StrUtil.isNotBlank(title)) {
                continue;
            }

            NewsFlashVO news = new NewsFlashVO();
            news.setTitle(title);
            news.setSummary(title);
            news.setSource("新浪财经");
            news.setPublishTime(formatDisplayTime(item.path("create_time").asText()));
            news.setPublishEpoch(parseEpoch(item.path("create_time").asText()));
            news.setUrl(resolveUrl(item));
            result.add(news);

            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private String resolveUrl(JsonNode item) {
        String docUrl = item.path("docurl").asText();
        if (StrUtil.isNotBlank(docUrl)) {
            return docUrl;
        }

        String ext = item.path("ext").asText();
        if (!StrUtil.isNotBlank(ext)) {
            return null;
        }

        try {
            JsonNode extNode = objectMapper.readTree(ext);
            return extNode.path("docurl").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String cleanText(String value) {
        if (!StrUtil.isNotBlank(value)) {
            return null;
        }
        String text = HtmlUtil.cleanHtmlTag(value);
        text = text.replace("&nbsp;", " ").trim();
        return StrUtil.isNotBlank(text) ? text : null;
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
                    .atZone(ZoneId.of("Asia/Shanghai"))
                    .toEpochSecond();
        } catch (Exception e) {
            return 0L;
        }
    }
}
