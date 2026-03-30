package com.smartstock.vo;

import lombok.Data;

@Data
public class QaAnswerVO {

    private String sessionId;
    private String stockCode;
    private String question;
    private String answer;
    private String createdAt;
}
