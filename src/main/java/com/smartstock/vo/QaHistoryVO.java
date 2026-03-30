package com.smartstock.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QaHistoryVO {

    private Long id;
    private String question;
    private String answer;
    private LocalDateTime createdAt;
}
