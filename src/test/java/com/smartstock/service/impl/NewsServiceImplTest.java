package com.smartstock.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstock.client.ClsNewsClient;
import com.smartstock.client.EastMoneyNewsClient;
import com.smartstock.convert.NewsStructMapper;
import com.smartstock.entity.News;
import com.smartstock.entity.StockInfo;
import com.smartstock.mapper.NewsMapper;
import com.smartstock.client.SinaNewsClient;
import com.smartstock.mapper.StockInfoMapper;
import com.smartstock.vo.NewsFlashVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsServiceImplTest {

    @Mock
    private SinaNewsClient sinaNewsClient;

    @Mock
    private ClsNewsClient clsNewsClient;

    @Mock
    private EastMoneyNewsClient eastMoneyNewsClient;

    @Mock
    private NewsMapper newsMapper;

    @Mock
    private StockInfoMapper stockInfoMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private NewsStructMapper newsStructMapper;

    private NewsServiceImpl newsService;

    @BeforeEach
    void setUp() {
        newsService = new NewsServiceImpl(
                sinaNewsClient,
                clsNewsClient,
                eastMoneyNewsClient,
                newsMapper,
                stockInfoMapper,
                stringRedisTemplate,
                new ObjectMapper(),
                newsStructMapper
        );
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(newsStructMapper.toFlashVO(any())).thenAnswer((invocation) -> {
            News source = invocation.getArgument(0);
            NewsFlashVO vo = new NewsFlashVO();
            vo.setTitle(source.getTitle());
            vo.setSummary(source.getContent());
            vo.setSource(source.getSource());
            vo.setUrl(source.getUrl());
            vo.setStockCode(source.getStockCode());
            if (source.getPublishTime() != null) {
                vo.setPublishEpoch(source.getPublishTime().toEpochSecond(java.time.ZoneOffset.ofHours(8)));
                vo.setPublishTime(source.getPublishTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            }
            return vo;
        });
    }

    @Test
    void getFlashNewsShouldMergePersistAndReturnLatestNews() {
        when(valueOperations.get("news:flash:all")).thenReturn(null);
        when(newsMapper.selectList(any()))
                .thenReturn(List.of())
                .thenReturn(List.of(
                        storedNews("半导体板块拉升", "新浪财经", 300L, "https://sina/1"),
                        storedNews("港股异动", "财联社", 250L, "https://cls/2"),
                        storedNews("指数快速走强", "财联社", 200L, "https://cls/1")
                ));
        when(sinaNewsClient.fetchLatest(300)).thenReturn(List.of(
                news("半导体板块拉升", "新浪财经", 300L, "https://sina/1"),
                news("财联社3月18日电，指数快速走强", "新浪财经", 100L, "https://sina/2")
        ));
        when(clsNewsClient.fetchLatest(300)).thenReturn(List.of(
                news("指数快速走强", "财联社", 200L, "https://cls/1"),
                news("港股异动", "财联社", 250L, "https://cls/2")
        ));
        when(eastMoneyNewsClient.fetchLatest(300)).thenReturn(List.of());

        List<NewsFlashVO> result = newsService.getFlashNews(3);

        assertEquals(3, result.size());
        assertEquals("半导体板块拉升", result.get(0).getTitle());
        assertEquals("港股异动", result.get(1).getTitle());
        assertEquals("指数快速走强", result.get(2).getTitle());
        verify(valueOperations).set(eq("news:flash:all"), any(String.class), any());
        verify(newsMapper, org.mockito.Mockito.times(3)).insert(any(News.class));
    }

    @Test
    void getFlashNewsShouldUseCachedResultWhenAvailable() throws Exception {
        when(valueOperations.get("news:flash:all")).thenReturn(new ObjectMapper().writeValueAsString(List.of(
                news("缓存快讯1", "新浪财经", 200L, "https://cached/1"),
                news("缓存快讯2", "财联社", 100L, "https://cached/2")
        )));

        List<NewsFlashVO> result = newsService.getFlashNews(1);

        assertEquals(1, result.size());
        assertEquals("缓存快讯1", result.get(0).getTitle());
        verify(sinaNewsClient, never()).fetchLatest(any(Integer.class));
        verify(clsNewsClient, never()).fetchLatest(any(Integer.class));
        verify(eastMoneyNewsClient, never()).fetchLatest(any(Integer.class));
    }

    @Test
    void getFlashNewsShouldRefetchWhenCachedSizeIsSmallerThanRequestedLimit() throws Exception {
        when(valueOperations.get("news:flash:all")).thenReturn(new ObjectMapper().writeValueAsString(List.of(
                news("缓存快讯1", "新浪财经", 200L, "https://cached/1"),
                news("缓存快讯2", "财联社", 100L, "https://cached/2")
        )));
        when(newsMapper.selectList(any()))
                .thenReturn(List.of())
                .thenReturn(List.of(
                        storedNews("增量快讯1", "新浪财经", 300L, "https://sina/1"),
                        storedNews("增量快讯2", "新浪财经", 250L, "https://sina/2"),
                        storedNews("增量快讯3", "新浪财经", 240L, "https://sina/3")
                ));
        when(sinaNewsClient.fetchLatest(300)).thenReturn(List.of(
                news("增量快讯1", "新浪财经", 300L, "https://sina/1"),
                news("增量快讯2", "新浪财经", 250L, "https://sina/2"),
                news("增量快讯3", "新浪财经", 240L, "https://sina/3")
        ));
        when(clsNewsClient.fetchLatest(300)).thenReturn(List.of(
                news("增量快讯4", "财联社", 230L, "https://cls/1")
        ));
        when(eastMoneyNewsClient.fetchLatest(300)).thenReturn(List.of());

        List<NewsFlashVO> result = newsService.getFlashNews(3);

        assertEquals(3, result.size());
        assertEquals("增量快讯1", result.get(0).getTitle());
        verify(sinaNewsClient).fetchLatest(300);
        verify(clsNewsClient).fetchLatest(300);
        verify(eastMoneyNewsClient).fetchLatest(300);
    }

    @Test
    void getFlashNewsPageShouldFilterBySourceAndStockCode() {
        when(newsMapper.selectCount(any())).thenReturn(1L);
        when(newsMapper.selectPage(any(), any())).thenReturn(pageOf(List.of(
                storedNews("招商银行异动", "财联社", 300L, "https://cls/1", "600036")
        )));

        var result = newsService.getFlashNewsPage(1, 20, "财联社", "600036", null);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("招商银行异动", result.getRecords().get(0).getTitle());
    }

    @Test
    void getFlashNewsShouldLinkStockCodeFromTitleWhenMissing() {
        when(valueOperations.get("news:flash:all")).thenReturn(null);
        when(newsMapper.selectList(any())).thenReturn(List.of(), List.of());
        when(stockInfoMapper.selectList(any())).thenReturn(List.of(
                StockInfo.builder().stockCode("600036").stockName("招商银行").market("SH").status(1).build()
        ));
        when(sinaNewsClient.fetchLatest(300)).thenReturn(List.of(
                news("招商银行盘中拉升", "新浪财经", 300L, "https://sina/1")
        ));
        when(clsNewsClient.fetchLatest(300)).thenReturn(List.of());
        when(eastMoneyNewsClient.fetchLatest(300)).thenReturn(List.of());

        newsService.getFlashNews(10);

        verify(newsMapper).insert(org.mockito.ArgumentMatchers.<News>argThat(news -> "600036".equals(news.getStockCode())));
    }

    private NewsFlashVO news(String title, String source, Long publishEpoch, String url) {
        NewsFlashVO vo = new NewsFlashVO();
        vo.setTitle(title);
        vo.setSummary(title);
        vo.setSource(source);
        vo.setPublishEpoch(publishEpoch);
        vo.setPublishTime("09:30");
        vo.setUrl(url);
        return vo;
    }

    private News storedNews(String title, String source, Long publishEpoch, String url) {
        return storedNews(title, source, publishEpoch, url, null);
    }

    private News storedNews(String title, String source, Long publishEpoch, String url, String stockCode) {
        return News.builder()
                .title(title)
                .content(title)
                .source(source)
                .url(url)
                .stockCode(stockCode)
                .publishTime(LocalDateTime.ofEpochSecond(publishEpoch, 0, java.time.ZoneOffset.ofHours(8)))
                .build();
    }

    private com.baomidou.mybatisplus.extension.plugins.pagination.Page<News> pageOf(List<News> records) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<News> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        page.setTotal(records.size());
        page.setRecords(records);
        return page;
    }
}
