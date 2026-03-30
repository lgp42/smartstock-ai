package com.smartstock.vo;

import lombok.Data;

@Data
public class NewsFlashVO {

    private String title;
    private String summary;
    private String source;
    private String publishTime;
    private Long publishEpoch;
    private String url;
    private String stockCode;
}
