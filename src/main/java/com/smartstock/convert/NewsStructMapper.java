package com.smartstock.convert;

import com.smartstock.entity.News;
import com.smartstock.vo.NewsFlashVO;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class NewsStructMapper {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public NewsFlashVO toFlashVO(News news) {
        if (news == null) {
            return null;
        }
        NewsFlashVO vo = new NewsFlashVO();
        vo.setTitle(news.getTitle());
        vo.setSummary(resolveSummary(news));
        vo.setSource(news.getSource());
        vo.setUrl(news.getUrl());
        vo.setStockCode(news.getStockCode());
        if (news.getPublishTime() != null) {
            vo.setPublishEpoch(news.getPublishTime().atZone(SHANGHAI_ZONE).toEpochSecond());
            vo.setPublishTime(news.getPublishTime().format(DISPLAY_TIME_FORMATTER));
        }
        return vo;
    }

    private String resolveSummary(News news) {
        if (news.getContent() != null && !news.getContent().isBlank()) {
            return news.getContent();
        }
        return news.getTitle();
    }
}
