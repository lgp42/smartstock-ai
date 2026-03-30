package com.smartstock.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstock.vo.NewsFlashVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SinaNewsClientTest {

    @Mock
    private RemoteHttpClient remoteHttpClient;

    private SinaNewsClient sinaNewsClient;

    @BeforeEach
    void setUp() {
        sinaNewsClient = new SinaNewsClient(remoteHttpClient, new ObjectMapper());
    }

    @Test
    void fetchLatestShouldParseSinaNewsAndSkipRepeatedItems() {
        String response = """
                {
                  "result": {
                    "data": {
                      "feed": {
                        "list": [
                          {
                            "rich_text": "【半导体板块拉升】龙头个股快速走强",
                            "create_time": "2026-03-18 09:51:42",
                            "docurl": "https://finance.sina.cn/news1",
                            "is_repeat": "0"
                          },
                          {
                            "rich_text": "重复快讯",
                            "create_time": "2026-03-18 09:50:00",
                            "docurl": "https://finance.sina.cn/news2",
                            "is_repeat": "1"
                          }
                        ]
                      }
                    }
                  }
                }
                """;
        when(remoteHttpClient.get(anyString())).thenReturn(response);

        List<NewsFlashVO> result = sinaNewsClient.fetchLatest(5);

        assertEquals(1, result.size());
        assertEquals("新浪财经", result.get(0).getSource());
        assertEquals("09:51", result.get(0).getPublishTime());
        assertEquals("https://finance.sina.cn/news1", result.get(0).getUrl());
        assertEquals("【半导体板块拉升】龙头个股快速走强", result.get(0).getTitle());
    }
}
