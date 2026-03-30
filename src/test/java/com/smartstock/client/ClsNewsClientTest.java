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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClsNewsClientTest {

    @Mock
    private RemoteHttpClient remoteHttpClient;

    private ClsNewsClient clsNewsClient;

    @BeforeEach
    void setUp() {
        clsNewsClient = new ClsNewsClient(remoteHttpClient, new ObjectMapper());
    }

    @Test
    void fetchLatestShouldParseClsNextData() {
        String response = """
                <html>
                <head></head>
                <body>
                <script id="__NEXT_DATA__" type="application/json">
                {
                  "props": {
                    "initialState": {
                      "telegraph": {
                        "telegraphList": [
                          {
                            "title": "PCB概念快速走强",
                            "content": "财联社3月18日电，PCB概念快速走强。",
                            "shareurl": "https://www.cls.cn/detail/1",
                            "ctime": 1773798690,
                            "is_ad": 0
                          },
                          {
                            "title": "",
                            "content": "",
                            "shareurl": "https://www.cls.cn/detail/2",
                            "ctime": 1773798600,
                            "is_ad": 0
                          }
                        ]
                      }
                    }
                  }
                }
                </script>
                </body>
                </html>
                """;
        when(remoteHttpClient.get("https://www.cls.cn/telegraph")).thenReturn(response);

        List<NewsFlashVO> result = clsNewsClient.fetchLatest(5);

        assertEquals(1, result.size());
        assertEquals("财联社", result.get(0).getSource());
        assertEquals("PCB概念快速走强", result.get(0).getTitle());
        assertEquals("https://www.cls.cn/detail/1", result.get(0).getUrl());
        assertEquals("09:51", result.get(0).getPublishTime());
    }
}
