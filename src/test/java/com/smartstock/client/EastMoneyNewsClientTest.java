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
class EastMoneyNewsClientTest {

    @Mock
    private RemoteHttpClient remoteHttpClient;

    private EastMoneyNewsClient eastMoneyNewsClient;

    @BeforeEach
    void setUp() {
        eastMoneyNewsClient = new EastMoneyNewsClient(remoteHttpClient, new ObjectMapper());
    }

    @Test
    void fetchLatestShouldParseJsonpAndResolveStockCode() {
        String response = """
                callback({
                  "code":"1",
                  "data":{
                    "sortEnd":"1773814478044522",
                    "fastNewsList":[
                      {
                        "code":"202603183675846145",
                        "showTime":"2026-03-18 14:16:56",
                        "summary":"【科大讯飞被曝大规模裁员赔偿0.6N 官方辟谣：假消息】详细内容",
                        "title":"科大讯飞被曝大规模裁员赔偿0.6N 官方辟谣：假消息",
                        "stockList":["0.002230"]
                      },
                      {
                        "code":"202603183675844522",
                        "showTime":"2026-03-18 14:14:38",
                        "summary":"只有摘要",
                        "title":"",
                        "stockList":[]
                      }
                    ]
                  }
                })
                """;
        when(remoteHttpClient.get(anyString())).thenReturn(response);

        List<NewsFlashVO> result = eastMoneyNewsClient.fetchLatest(2);

        assertEquals(2, result.size());
        assertEquals("东方财富", result.get(0).getSource());
        assertEquals("科大讯飞被曝大规模裁员赔偿0.6N 官方辟谣：假消息", result.get(0).getTitle());
        assertEquals("14:16", result.get(0).getPublishTime());
        assertEquals("002230", result.get(0).getStockCode());
        assertEquals("https://finance.eastmoney.com/a/202603183675846145.html", result.get(0).getUrl());
        assertEquals("只有摘要", result.get(1).getTitle());
    }
}
