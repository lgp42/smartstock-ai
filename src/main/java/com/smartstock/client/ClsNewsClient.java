package com.smartstock.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstock.vo.NewsFlashVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClsNewsClient {

    private static final String NEWS_URL = "https://www.cls.cn/telegraph";
    private static final Pattern NEXT_DATA_PATTERN = Pattern.compile(
            "<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>",
            Pattern.DOTALL
    );
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.of("Asia/Shanghai"));

    private final RemoteHttpClient remoteHttpClient;
    private final ObjectMapper objectMapper;

    public List<NewsFlashVO> fetchLatest(int limit) {
        try {
            String response = remoteHttpClient.get(NEWS_URL);
            if (!StrUtil.isNotBlank(response)) {
                return new ArrayList<>();
            }
            return parseResponse(response, limit);
        } catch (Exception e) {
            log.warn("Failed to fetch cls flash news, error: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    List<NewsFlashVO> parseResponse(String response, int limit) throws Exception {
        Matcher matcher = NEXT_DATA_PATTERN.matcher(response);
        if (!matcher.find()) {
            return new ArrayList<>();
        }

        JsonNode root = objectMapper.readTree(matcher.group(1));
        JsonNode props = root.path("props");
        JsonNode initialState = props.path("initialState");
        if (initialState.isMissingNode() || initialState.isNull()) {
            initialState = props.path("pageProps").path("initialState");
        }

        JsonNode list = initialState
                .path("telegraph")
                .path("telegraphList");
        if (!list.isArray()) {
            return new ArrayList<>();
        }

        List<NewsFlashVO> result = new ArrayList<>();
        for (JsonNode item : list) {
            if (item.path("is_ad").asBoolean(false)) {
                continue;
            }

            String title = cleanText(item.path("title").asText());
            String content = cleanText(item.path("content").asText());
            String displayTitle = StrUtil.isNotBlank(title) ? title : content;
            if (!StrUtil.isNotBlank(displayTitle)) {
                continue;
            }

            NewsFlashVO news = new NewsFlashVO();
            news.setTitle(displayTitle);
            news.setSummary(content);
            news.setSource("财联社");
            long epoch = item.path("ctime").asLong(0L);
            news.setPublishEpoch(epoch);
            news.setPublishTime(epoch > 0 ? DISPLAY_TIME_FORMATTER.format(Instant.ofEpochSecond(epoch)) : "");
            news.setUrl(item.path("shareurl").asText(null));
            result.add(news);

            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private String cleanText(String value) {
        if (!StrUtil.isNotBlank(value)) {
            return null;
        }
        String text = HtmlUtil.cleanHtmlTag(value).replace("&nbsp;", " ").trim();
        return StrUtil.isNotBlank(text) ? text : null;
    }
}
