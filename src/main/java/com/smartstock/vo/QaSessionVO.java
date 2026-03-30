package com.smartstock.vo;

import lombok.Data;

@Data
public class QaSessionVO {

    private String sessionId;
    private String stockCode;
    private String title;
    private Integer messageCount;
    private String createdAt;
    private String lastActive;
}
